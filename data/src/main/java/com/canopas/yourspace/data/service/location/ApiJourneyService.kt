package com.canopas.yourspace.data.service.location

import com.canopas.yourspace.data.models.location.EncryptedLocationJourney
import com.canopas.yourspace.data.models.location.LocationJourney
import com.canopas.yourspace.data.models.space.GroupKeysDoc
import com.canopas.yourspace.data.models.user.ApiUser
import com.canopas.yourspace.data.service.user.ApiUserService
import com.canopas.yourspace.data.storage.UserPreferences
import com.canopas.yourspace.data.storage.bufferedkeystore.BufferedSenderKeyStore
import com.canopas.yourspace.data.utils.Config
import com.canopas.yourspace.data.utils.Config.FIRESTORE_COLLECTION_SPACES
import com.canopas.yourspace.data.utils.Config.FIRESTORE_COLLECTION_SPACE_GROUP_KEYS
import com.canopas.yourspace.data.utils.Config.FIRESTORE_COLLECTION_SPACE_MEMBERS
import com.canopas.yourspace.data.utils.EphemeralECDHUtils
import com.canopas.yourspace.data.utils.PrivateKeyUtils
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.tasks.await
import org.signal.libsignal.protocol.InvalidKeyException
import org.signal.libsignal.protocol.SignalProtocolAddress
import org.signal.libsignal.protocol.ecc.Curve
import org.signal.libsignal.protocol.ecc.ECPrivateKey
import org.signal.libsignal.protocol.groups.GroupCipher
import org.signal.libsignal.protocol.groups.GroupSessionBuilder
import org.signal.libsignal.protocol.message.SenderKeyDistributionMessage
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiJourneyService @Inject constructor(
    db: FirebaseFirestore,
    val apiUserService: ApiUserService,
    private val userPreferences: UserPreferences,
    private val bufferedSenderKeyStore: BufferedSenderKeyStore
) {
    private val spaceRef = db.collection(FIRESTORE_COLLECTION_SPACES)
    private val currentSpaceId get() = userPreferences.currentSpace ?: ""

    private fun spaceMemberRef(spaceId: String) =
        spaceRef.document(spaceId).collection(FIRESTORE_COLLECTION_SPACE_MEMBERS)

    private fun spaceMemberJourneyRef(spaceId: String, userId: String) =
        spaceMemberRef(spaceId).document(userId)
            .collection(Config.FIRESTORE_COLLECTION_USER_JOURNEYS)

    private fun spaceGroupKeysRef(spaceId: String) =
        spaceRef.document(spaceId).collection(FIRESTORE_COLLECTION_SPACE_GROUP_KEYS)
            .document(FIRESTORE_COLLECTION_SPACE_GROUP_KEYS)

    private suspend fun getGroupKeyDoc(spaceId: String) = try {
        spaceGroupKeysRef(spaceId).get().await().toObject<GroupKeysDoc>()
    } catch (e: Exception) {
        Timber.e(e, "Failed to fetch GroupKeysDoc")
        null
    }

    private suspend fun getGroupCipherByKeyId(
        spaceId: String,
        userId: String,
        keyId: String? = null,
        groupKeysDoc: GroupKeysDoc
    ): Triple<SenderKeyDistributionMessage, GroupCipher, String>? {
        val memberKeysData = groupKeysDoc.member_keys[userId] ?: return null
        val distribution = memberKeysData.distributions
            .sortedByDescending { it.created_at }
            .firstOrNull { it.recipient_id == userId && (keyId == null || it.id == keyId) }
            ?: return null

        val currentUser = userPreferences.currentUser ?: return null
        val privateKey = getCurrentUserPrivateKey(currentUser) ?: return null

        val decryptedBytes = EphemeralECDHUtils.decrypt(distribution, privateKey) ?: return null
        val distributionMessage = SenderKeyDistributionMessage(decryptedBytes)

        val groupAddress = SignalProtocolAddress(spaceId, memberKeysData.member_device_id)
        bufferedSenderKeyStore.loadSenderKey(groupAddress, distributionMessage.distributionId)

        return try {
            GroupSessionBuilder(bufferedSenderKeyStore).process(groupAddress, distributionMessage)
            Triple(
                distributionMessage,
                GroupCipher(bufferedSenderKeyStore, groupAddress),
                distribution.id
            )
        } catch (e: Exception) {
            Timber.e(e, "Error processing group session")
            null
        }
    }

    private suspend inline fun <T> runWithGroupCipher(
        spaceId: String,
        userId: String,
        groupKeysDoc: GroupKeysDoc,
        keyId: String? = null,
        defaultValue: T?,
        crossinline block: (cipher: GroupCipher) -> T?
    ) = try {
        getGroupCipherByKeyId(spaceId, userId, keyId, groupKeysDoc)?.let { (_, groupCipher) ->
            block(groupCipher) ?: defaultValue
        } ?: defaultValue
    } catch (e: Exception) {
        Timber.e(e, "Error executing run operation")
        defaultValue
    }

    private suspend fun getCurrentUserPrivateKey(currentUser: ApiUser): ECPrivateKey? {
        val privateKey = userPreferences.getPrivateKey() ?: currentUser.identity_key_private?.toBytes()
        return try {
            Curve.decodePrivatePoint(privateKey)
        } catch (e: InvalidKeyException) {
            Timber.e(e, "Error decoding private key")
            privateKey?.let { key ->
                currentUser.identity_key_salt?.toBytes()?.let { salt ->
                    userPreferences.getPasskey()?.let { passkey ->
                        PrivateKeyUtils.decryptPrivateKey(key, salt, passkey)?.let {
                            Curve.decodePrivatePoint(it)
                        }
                    }
                }
            }
        }
    }

    suspend fun addJourney(userId: String, newJourney: LocationJourney): LocationJourney {
        var journey = newJourney
        val currentUser = userPreferences.currentUser ?: return journey

        currentUser.space_ids?.forEach { spaceId ->
            if (currentUser.isPremiumUser) {
                val groupKeysDoc = getGroupKeyDoc(spaceId) ?: return@forEach
                val cipherResult = getGroupCipherByKeyId(spaceId, userId, null, groupKeysDoc) ?: run {
                    Timber.e("Failed to get group cipher")
                    return@forEach
                }

                val docRef = spaceMemberJourneyRef(spaceId, userId).document(newJourney.id)
                journey = newJourney.copy(id = docRef.id, key_id = cipherResult.third)

                journey.toEncryptedLocationJourney(cipherResult.second, cipherResult.first.distributionId)
                    ?.let { docRef.set(it).await() }
            } else {
                val docRef = spaceMemberJourneyRef(spaceId, userId).document()
                journey = newJourney.copy(id = docRef.id)
                docRef.set(journey).await()
            }
        }
        return journey
    }

    suspend fun updateJourney(userId: String, journey: LocationJourney) {
        val currentUser = userPreferences.currentUser ?: return
        currentUser.space_ids?.forEach { spaceId ->
            try {
                if (currentUser.isPremiumUser) {
                    val groupKeysDoc = getGroupKeyDoc(spaceId) ?: return@forEach
                    val (distributionMessage, groupCipher) = getGroupCipherByKeyId(
                        spaceId,
                        userId,
                        journey.key_id,
                        groupKeysDoc
                    ) ?: run {
                        Timber.e("Failed to get group cipher")
                        return@forEach
                    }

                    journey.toEncryptedLocationJourney(groupCipher, distributionMessage.distributionId)
                        ?.let { spaceMemberJourneyRef(spaceId, userId).document(journey.id).set(it).await() }
                } else {
                    spaceMemberJourneyRef(spaceId, userId).document(journey.id).set(journey).await()
                }
            } catch (e: Exception) {
                Timber.e(e, "Error updating journey")
            }
        }
    }

    suspend fun getLastJourneyLocation(userId: String): LocationJourney? {
        val user = apiUserService.getUser(userId) ?: return null
        return if (user.isPremiumUser) {
            val encryptedJourney = spaceMemberJourneyRef(currentSpaceId, userId)
                .orderBy("created_at", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()
                .documents
                .firstOrNull()
                ?.toObject<EncryptedLocationJourney>()
                ?: return null

            val groupKeysDoc = getGroupKeyDoc(currentSpaceId) ?: return null

            runWithGroupCipher(
                currentSpaceId,
                userId,
                groupKeysDoc,
                encryptedJourney.key_id,
                null
            ) { it.let { cipher -> encryptedJourney.toDecryptedLocationJourney(cipher) } }
        } else {
            spaceMemberJourneyRef(currentSpaceId, userId)
                .whereEqualTo("user_id", userId)
                .orderBy("created_at", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()
                .documents
                .firstOrNull()
                ?.toObject<LocationJourney>()
        }
    }

    suspend fun getMoreJourneyHistory(userId: String, from: Long?): List<LocationJourney> {
        val user = apiUserService.getUser(userId) ?: return emptyList()
        val groupKeysDoc = if (user.isPremiumUser) getGroupKeyDoc(currentSpaceId) ?: return emptyList() else null

        val query = spaceMemberJourneyRef(currentSpaceId, userId)
            .whereEqualTo("user_id", userId)
            .orderBy("created_at", Query.Direction.DESCENDING)
            .apply { from?.let { whereLessThan("created_at", it) } }
            .limit(20)

        val journeys = query.get().await().documents.mapNotNull {
            if (user.isPremiumUser) it.toObject<EncryptedLocationJourney>() else it.toObject<LocationJourney>()
        }

        return if (user.isPremiumUser) {
            journeys.mapNotNull { encrypted ->
                getGroupCipherByKeyId(
                    currentSpaceId,
                    userId,
                    (encrypted as EncryptedLocationJourney).key_id,
                    groupKeysDoc!!
                )?.let { (_, cipher) ->
                    encrypted.toDecryptedLocationJourney(cipher)
                }
            }
        } else {
            journeys as List<LocationJourney>
        }
    }

    suspend fun getJourneyHistory(userId: String, from: Long, to: Long): List<LocationJourney> {
        return try {
            val user = apiUserService.getUser(userId) ?: return emptyList()
            val groupKeysDoc = if (user.isPremiumUser) getGroupKeyDoc(currentSpaceId) ?: return emptyList() else null

            val previousDay = spaceMemberJourneyRef(currentSpaceId, userId)
                .whereEqualTo("user_id", userId)
                .whereLessThan("created_at", from)
                .whereGreaterThanOrEqualTo("updated_at", from)
                .limit(1)
                .get()
                .await()
                .documents

            val currentDay = spaceMemberJourneyRef(currentSpaceId, userId)
                .whereEqualTo("user_id", userId)
                .whereGreaterThanOrEqualTo("created_at", from)
                .whereLessThanOrEqualTo("created_at", to)
                .orderBy("created_at", Query.Direction.DESCENDING)
                .limit(20)
                .get()
                .await()
                .documents

            val allJourneys = (previousDay + currentDay).mapNotNull {
                if (user.isPremiumUser) it.toObject<EncryptedLocationJourney>() else it.toObject<LocationJourney>()
            }

            if (user.isPremiumUser) {
                allJourneys.mapNotNull { encrypted ->
                    getGroupCipherByKeyId(
                        currentSpaceId,
                        userId,
                        (encrypted as EncryptedLocationJourney).key_id,
                        groupKeysDoc!!
                    )?.let { (_, cipher) ->
                        encrypted.toDecryptedLocationJourney(cipher)
                    }
                }
            } else {
                allJourneys as List<LocationJourney>
            }
        } catch (e: Exception) {
            Timber.e(e, "Error while getting journey history")
            emptyList()
        }
    }
    suspend fun getLocationJourneyFromId(journeyId: String): LocationJourney? {
        val currentUser = userPreferences.currentUser ?: return null

        return try {
            if (currentUser.isPremiumUser) {
                val encryptedJourney = spaceMemberJourneyRef(currentSpaceId, currentUser.id)
                    .document(journeyId)
                    .get()
                    .await()
                    .toObject<EncryptedLocationJourney>() ?: return null

                val groupKeysDoc = getGroupKeyDoc(currentSpaceId) ?: return null

                runWithGroupCipher(
                    currentSpaceId,
                    currentUser.id,
                    groupKeysDoc,
                    encryptedJourney.key_id,
                    null
                ) { cipher -> encryptedJourney.toDecryptedLocationJourney(cipher) }
            } else {
                spaceMemberJourneyRef(currentSpaceId, currentUser.id)
                    .document(journeyId)
                    .get()
                    .await()
                    .toObject()
            }
        } catch (e: Exception) {
            Timber.e(e, "Error while getting journey")
            null
        }
    }
}
