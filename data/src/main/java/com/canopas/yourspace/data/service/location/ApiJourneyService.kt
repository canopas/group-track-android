package com.canopas.yourspace.data.service.location

import com.canopas.yourspace.data.models.location.EncryptedLocationJourney
import com.canopas.yourspace.data.models.location.LocationJourney
import com.canopas.yourspace.data.models.space.GroupKeysDoc
import com.canopas.yourspace.data.models.user.ApiUser
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
    private val userPreferences: UserPreferences,
    private val bufferedSenderKeyStore: BufferedSenderKeyStore
) {
    private val spaceRef = db.collection(FIRESTORE_COLLECTION_SPACES)
    private val currentSpaceId: String
        get() = userPreferences.currentSpace ?: ""

    private fun spaceMemberRef(spaceId: String) =
        spaceRef.document(spaceId).collection(FIRESTORE_COLLECTION_SPACE_MEMBERS)

    private fun spaceMemberJourneyRef(spaceId: String, userId: String) =
        spaceMemberRef(spaceId).document(userId).collection(Config.FIRESTORE_COLLECTION_USER_JOURNEYS)

    private fun spaceGroupKeysRef(spaceId: String) =
        spaceRef.document(spaceId).collection(FIRESTORE_COLLECTION_SPACE_GROUP_KEYS)
            .document(FIRESTORE_COLLECTION_SPACE_GROUP_KEYS)

    private suspend fun getGroupKeyDoc(spaceId: String): GroupKeysDoc? {
        return try {
            spaceGroupKeysRef(spaceId).get().await().toObject<GroupKeysDoc>()
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch GroupKeysDoc")
            null
        }
    }

    /**
     * Loads the group cipher for the given [spaceId], [userId], [keyId], and already-loaded [groupKeysDoc].
     */
    private suspend fun getGroupCipherByKeyId(
        spaceId: String,
        userId: String,
        keyId: String? = null,
        groupKeysDoc: GroupKeysDoc
    ): Triple<SenderKeyDistributionMessage, GroupCipher, String>? {
        val memberKeysData = groupKeysDoc.member_keys[userId] ?: return null
        val distribution = memberKeysData.distributions
            .sortedByDescending { it.created_at }
            .firstOrNull {
                it.recipient_id == userId && (keyId == null || it.id == keyId)
            } ?: return null

        val currentUser = userPreferences.currentUser ?: return null
        val privateKey = getCurrentUserPrivateKey(currentUser) ?: return null

        // Decrypt the distribution message
        val decryptedBytes = EphemeralECDHUtils.decrypt(distribution, privateKey) ?: return null
        val distributionMessage = SenderKeyDistributionMessage(decryptedBytes)

        val groupAddress = SignalProtocolAddress(spaceId, memberKeysData.member_device_id)
        // Ensures the distribution ID is loaded into the store
        bufferedSenderKeyStore.loadSenderKey(groupAddress, distributionMessage.distributionId)

        return try {
            GroupSessionBuilder(bufferedSenderKeyStore).process(groupAddress, distributionMessage)
            Triple(distributionMessage, GroupCipher(bufferedSenderKeyStore, groupAddress), distribution.id)
        } catch (e: Exception) {
            Timber.e(e, "Error processing group session")
            null
        }
    }

    /**
     * Helper to run some block of code that needs a [GroupCipher], using an already-loaded [GroupKeysDoc].
     * Returns `defaultValue` if we fail to load the cipher or if the block returns null.
     */
    private suspend inline fun <T> runWithGroupCipher(
        spaceId: String,
        userId: String,
        groupKeysDoc: GroupKeysDoc,
        keyId: String? = null,
        defaultValue: T?,
        crossinline block: (cipher: GroupCipher) -> T?
    ): T? {
        val (_, groupCipher) = getGroupCipherByKeyId(spaceId, userId, keyId, groupKeysDoc)
            ?: return defaultValue
        return try {
            block(groupCipher) ?: defaultValue
        } catch (e: Exception) {
            Timber.e(e, "Error executing run operation")
            defaultValue
        }
    }

    /**
     * Decrypts and retrieves the current user's private key.
     */
    private suspend fun getCurrentUserPrivateKey(currentUser: ApiUser): ECPrivateKey? {
        val privateKey =
            userPreferences.getPrivateKey() ?: currentUser.identity_key_private?.toBytes()
        return try {
            Curve.decodePrivatePoint(privateKey)
        } catch (e: InvalidKeyException) {
            Timber.e(e, "Error decoding private key")
            PrivateKeyUtils.decryptPrivateKey(
                privateKey ?: return null,
                currentUser.identity_key_salt?.toBytes() ?: return null,
                userPreferences.getPasskey() ?: return null
            )?.let { Curve.decodePrivatePoint(it) }
        }
    }

    /**
     * Saves a new [LocationJourney] in all of the current user's spaces.
     */
    suspend fun addJourney(
        userId: String,
        newJourney: LocationJourney
    ): LocationJourney {
        var journey: LocationJourney = newJourney
        userPreferences.currentUser?.space_ids?.forEach { spaceId ->
            val groupKeysDoc = getGroupKeyDoc(spaceId) ?: return@forEach

            val (distributionMessage, groupCipher, keyId) = getGroupCipherByKeyId(
                spaceId,
                userId,
                null,
                groupKeysDoc
            )
                ?: run {
                    Timber.e("Failed to get group cipher")
                    return@forEach
                }

            val docRef = spaceMemberJourneyRef(spaceId, userId).document(newJourney.id)

            journey = newJourney.copy(id = docRef.id, key_id = keyId)

            val encryptedJourney =
                journey.toEncryptedLocationJourney(groupCipher, distributionMessage.distributionId)

            encryptedJourney?.let { docRef.set(it).await() }
        }
        return journey
    }

    /**
     * Updates the last [LocationJourney] for [userId].
     */
    suspend fun updateJourney(userId: String, journey: LocationJourney) {
        userPreferences.currentUser?.space_ids?.forEach { spaceId ->
            val groupKeysDoc = getGroupKeyDoc(spaceId) ?: return@forEach

            val (distributionMessage, groupCipher) = getGroupCipherByKeyId(
                spaceId,
                userId,
                journey.key_id,
                groupKeysDoc
            )
                ?: run {
                    Timber.e("Failed to get group cipher")
                    return@forEach
                }

            val encryptedJourney =
                journey.toEncryptedLocationJourney(groupCipher, distributionMessage.distributionId)
            try {
                encryptedJourney?.let {
                    spaceMemberJourneyRef(spaceId, userId)
                        .document(journey.id)
                        .set(it)
                        .await()
                }
            } catch (e: Exception) {
                Timber.e(e, "Error updating journey")
            }
        }
    }

    /**
     * Fetches the most recent [LocationJourney] for [userId] in the current space.
     */
    suspend fun getLastJourneyLocation(userId: String): LocationJourney? {
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

        // Decrypt
        return runWithGroupCipher(
            spaceId = currentSpaceId,
            userId = userId,
            groupKeysDoc = groupKeysDoc,
            keyId = encryptedJourney.key_id,
            defaultValue = null
        ) { cipher ->
            encryptedJourney.toDecryptedLocationJourney(cipher)
        }
    }

    /**
     * Fetch more journey history, older than [from], in pages of up to 20.
     * Loads GroupKeysDoc once, then decrypts each journey.
     */
    suspend fun getMoreJourneyHistory(userId: String, from: Long?): List<LocationJourney> {
        val groupKeysDoc = getGroupKeyDoc(currentSpaceId) ?: return emptyList()

        val query = if (from == null) {
            spaceMemberJourneyRef(currentSpaceId, userId)
                .whereEqualTo("user_id", userId)
                .orderBy("created_at", Query.Direction.DESCENDING)
                .limit(20)
        } else {
            spaceMemberJourneyRef(currentSpaceId, userId)
                .whereEqualTo("user_id", userId)
                .orderBy("created_at", Query.Direction.DESCENDING)
                .whereLessThan("created_at", from)
                .limit(20)
        }

        val encryptedJourneys = query.get().await().documents.mapNotNull {
            it.toObject<EncryptedLocationJourney>()
        }

        return encryptedJourneys.mapNotNull { encrypted ->
            val cipherAndMessage =
                getGroupCipherByKeyId(currentSpaceId, userId, encrypted.key_id, groupKeysDoc)
                    ?: return@mapNotNull null
            encrypted.toDecryptedLocationJourney(cipherAndMessage.second)
        }
    }

    /**
     * Fetch journey history between [from] and [to].
     * Loads GroupKeysDoc once, then decrypts each journey.
     */
    suspend fun getJourneyHistory(userId: String, from: Long, to: Long): List<LocationJourney> {
        return try {
            val groupKeysDoc = getGroupKeyDoc(currentSpaceId) ?: return emptyList()

            val previousDayEncrypted = spaceMemberJourneyRef(currentSpaceId, userId)
                .whereEqualTo("user_id", userId)
                .whereLessThan("created_at", from)
                .whereGreaterThanOrEqualTo("updated_at", from)
                .limit(1)
                .get()
                .await()
                .documents
                .mapNotNull { it.toObject<EncryptedLocationJourney>() }

            val currentDayEncrypted = spaceMemberJourneyRef(currentSpaceId, userId)
                .whereEqualTo("user_id", userId)
                .whereGreaterThanOrEqualTo("created_at", from)
                .whereLessThanOrEqualTo("created_at", to)
                .orderBy("created_at", Query.Direction.DESCENDING)
                .limit(20)
                .get()
                .await()
                .documents
                .mapNotNull { it.toObject<EncryptedLocationJourney>() }

            val allEncrypted = previousDayEncrypted + currentDayEncrypted

            allEncrypted.mapNotNull { encrypted ->
                val cipherAndMessage =
                    getGroupCipherByKeyId(currentSpaceId, userId, encrypted.key_id, groupKeysDoc)
                        ?: return@mapNotNull null
                encrypted.toDecryptedLocationJourney(cipherAndMessage.second)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error while getting journey history")
            emptyList()
        }
    }

    /**
     * Retrieves a specific [LocationJourney] by its [journeyId].
     */
    suspend fun getLocationJourneyFromId(journeyId: String): LocationJourney? {
        val currentUser = userPreferences.currentUser ?: return null

        val encryptedJourney = try {
            spaceMemberJourneyRef(currentSpaceId, currentUser.id)
                .document(journeyId)
                .get()
                .await()
                .toObject<EncryptedLocationJourney>()
        } catch (e: Exception) {
            Timber.e(e, "Error while getting journey")
            return null
        }

        val groupKeysDoc = getGroupKeyDoc(currentSpaceId) ?: return null

        return runWithGroupCipher(
            spaceId = currentSpaceId,
            userId = currentUser.id,
            groupKeysDoc = groupKeysDoc,
            keyId = encryptedJourney?.key_id,
            defaultValue = null
        ) { cipher ->
            encryptedJourney?.toDecryptedLocationJourney(cipher)
        }
    }
}
