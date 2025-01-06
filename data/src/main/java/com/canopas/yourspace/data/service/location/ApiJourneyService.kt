package com.canopas.yourspace.data.service.location

import com.canopas.yourspace.data.models.location.EncryptedLocationJourney
import com.canopas.yourspace.data.models.location.JourneyRoute
import com.canopas.yourspace.data.models.location.JourneyType
import com.canopas.yourspace.data.models.location.LocationJourney
import com.canopas.yourspace.data.models.location.toDecryptedLocationJourney
import com.canopas.yourspace.data.models.location.toEncryptedLocationJourney
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
import java.util.UUID
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
        spaceRef.document(spaceId.takeIf { it.isNotBlank() } ?: "null")
            .collection(FIRESTORE_COLLECTION_SPACE_MEMBERS)

    private fun spaceMemberJourneyRef(spaceId: String, userId: String) =
        spaceMemberRef(spaceId).document(userId.takeIf { it.isNotBlank() } ?: "null")
            .collection(Config.FIRESTORE_COLLECTION_USER_JOURNEYS)

    private fun spaceGroupKeysRef(spaceId: String) =
        spaceRef.document(spaceId.takeIf { it.isNotBlank() } ?: "null")
            .collection(FIRESTORE_COLLECTION_SPACE_GROUP_KEYS)
            .document(FIRESTORE_COLLECTION_SPACE_GROUP_KEYS)

    private suspend fun getGroupCipherAndDistributionMessage(
        spaceId: String,
        userId: String
    ): Pair<SenderKeyDistributionMessage, GroupCipher>? {
        val snapshot = spaceGroupKeysRef(spaceId).get().await()
        val groupKeysDoc = snapshot.toObject(GroupKeysDoc::class.java) ?: return null
        val memberKeyData = groupKeysDoc.memberKeys[userId] ?: return null

        val currentUser = userPreferences.currentUser ?: return null
        val privateKey = getCurrentUserPrivateKey(currentUser) ?: return null

        val distribution =
            memberKeyData.distributions.firstOrNull { it.recipientId == currentUser.id }
                ?: return null
        val decryptedDistributionBytes =
            EphemeralECDHUtils.decrypt(distribution, privateKey) ?: return null
        val distributionMessage = SenderKeyDistributionMessage(decryptedDistributionBytes)

        val groupAddress = SignalProtocolAddress(spaceId, memberKeyData.memberDeviceId)
        bufferedSenderKeyStore.loadSenderKey(groupAddress, distributionMessage.distributionId)

        // Initialize the session
        try {
            GroupSessionBuilder(bufferedSenderKeyStore).process(groupAddress, distributionMessage)
            val groupCipher = GroupCipher(bufferedSenderKeyStore, groupAddress)
            return Pair(distributionMessage, groupCipher)
        } catch (e: Exception) {
            Timber.e(e, "Error processing group session")
            return null
        }
    }

    private suspend fun <T> withGroupCipher(
        userId: String,
        block: suspend (GroupCipher) -> T?,
        defaultValue: T
    ): T {
        val (_, groupCipher) = getGroupCipherAndDistributionMessage(currentSpaceId, userId)
            ?: return defaultValue
        return try {
            block(groupCipher) ?: defaultValue
        } catch (e: Exception) {
            Timber.e(e, "Error executing operation for userId: $userId")
            defaultValue
        }
    }

    /**
     * Decrypts and retrieves the current user's private key.
     */
    private suspend fun getCurrentUserPrivateKey(currentUser: ApiUser): ECPrivateKey? {
        val privateKey = userPreferences.getPrivateKey() ?: currentUser.identity_key_private?.toBytes()
        return try {
            Curve.decodePrivatePoint(privateKey)
        } catch (e: InvalidKeyException) {
            Timber.e(e, "Error decoding private key for userId=${currentUser.id}")
            PrivateKeyUtils.decryptPrivateKey(
                privateKey ?: return null,
                currentUser.identity_key_salt?.toBytes() ?: return null,
                userPreferences.getPasskey() ?: return null
            )?.let { Curve.decodePrivatePoint(it) }
        }
    }

    suspend fun saveCurrentJourney(
        userId: String,
        fromLatitude: Double,
        fromLongitude: Double,
        toLatitude: Double? = null,
        toLongitude: Double? = null,
        routeDistance: Double? = null,
        routeDuration: Long? = null,
        routes: List<JourneyRoute> = emptyList(),
        createdAt: Long? = null,
        updateAt: Long? = null,
        type: JourneyType? = null,
        newJourneyId: ((String) -> Unit)? = null
    ) {
        userPreferences.currentUser?.space_ids?.forEach { spaceId ->
            val cipherAndMessage = getGroupCipherAndDistributionMessage(spaceId, userId) ?: run {
                Timber.e("Failed to retrieve GroupCipher and DistributionMessage for spaceId: $spaceId, userId: $userId")
                return@forEach
            }
            val (distributionMessage, groupCipher) = cipherAndMessage

            val journey = LocationJourney(
                id = UUID.randomUUID().toString(),
                user_id = userId,
                from_latitude = fromLatitude,
                from_longitude = fromLongitude,
                to_latitude = toLatitude,
                to_longitude = toLongitude,
                route_distance = routeDistance,
                route_duration = routeDuration,
                routes = routes,
                created_at = createdAt ?: System.currentTimeMillis(),
                updated_at = updateAt ?: System.currentTimeMillis(),
                type = type
            )

            val docRef = spaceMemberJourneyRef(spaceId, userId).document(journey.id)

            val encryptedJourney =
                journey.toEncryptedLocationJourney(groupCipher, distributionMessage.distributionId)
            newJourneyId?.invoke(encryptedJourney.id)

            docRef.set(encryptedJourney).await()
        }
    }

    suspend fun updateLastLocationJourney(userId: String, journey: LocationJourney) {
        userPreferences.currentUser?.space_ids?.forEach { spaceId ->
            val cipherAndMessage = getGroupCipherAndDistributionMessage(spaceId, userId) ?: run {
                Timber.e("Failed to retrieve GroupCipher and DistributionMessage for spaceId: $spaceId, userId: $userId")
                return@forEach
            }
            val (distributionMessage, groupCipher) = cipherAndMessage

            val encryptedJourney =
                journey.toEncryptedLocationJourney(groupCipher, distributionMessage.distributionId)
            try {
                spaceMemberJourneyRef(spaceId, userId).document(journey.id).set(encryptedJourney)
                    .await()
            } catch (e: Exception) {
                Timber.e(
                    e,
                    "Error while updating last location journey for spaceId: $spaceId, userId: $userId"
                )
            }
        }
    }

    suspend fun getLastJourneyLocation(userId: String): LocationJourney? =
        withGroupCipher(userId, { groupCipher ->
            spaceMemberJourneyRef(currentSpaceId, userId)
                .whereEqualTo("user_id", userId)
                .orderBy("created_at", Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .await()
                .documents
                .firstOrNull()
                ?.toObject<EncryptedLocationJourney>()
                ?.toDecryptedLocationJourney(groupCipher)
        }, null)

    suspend fun getMoreJourneyHistory(userId: String, from: Long?): List<LocationJourney> {
        val cipherAndMessage = getGroupCipherAndDistributionMessage(currentSpaceId, userId) ?: run {
            Timber.e("Failed to retrieve GroupCipher and DistributionMessage for spaceId: $currentSpaceId, userId: $userId")
            return emptyList()
        }
        val (_, groupCipher) = cipherAndMessage

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

        return try {
            query.get().await().documents.mapNotNull {
                it.toObject<EncryptedLocationJourney>()?.toDecryptedLocationJourney(groupCipher)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error while getting journey history for userId: $userId")
            emptyList()
        }
    }

    suspend fun getJourneyHistory(userId: String, from: Long, to: Long): List<LocationJourney> {
        val cipherAndMessage = getGroupCipherAndDistributionMessage(currentSpaceId, userId) ?: run {
            Timber.e("Failed to retrieve GroupCipher and DistributionMessage for spaceId: $currentSpaceId, userId: $userId")
            return emptyList()
        }
        val (_, groupCipher) = cipherAndMessage

        return try {
            val previousDayJourney = spaceMemberJourneyRef(currentSpaceId, userId)
                .whereEqualTo("user_id", userId)
                .whereLessThan("created_at", from)
                .whereGreaterThanOrEqualTo("updated_at", from)
                .limit(1)
                .get()
                .await()
                .documents
                .mapNotNull {
                    it.toObject<EncryptedLocationJourney>()?.toDecryptedLocationJourney(groupCipher)
                }

            val currentDayJourney = spaceMemberJourneyRef(currentSpaceId, userId)
                .whereEqualTo("user_id", userId)
                .whereGreaterThanOrEqualTo("created_at", from)
                .whereLessThanOrEqualTo("created_at", to)
                .orderBy("created_at", Query.Direction.DESCENDING)
                .limit(20)
                .get()
                .await()
                .documents
                .mapNotNull {
                    it.toObject<EncryptedLocationJourney>()?.toDecryptedLocationJourney(groupCipher)
                }

            previousDayJourney + currentDayJourney
        } catch (e: Exception) {
            Timber.e(e, "Error while getting journey history for userId: $userId")
            emptyList()
        }
    }

    suspend fun getLocationJourneyFromId(journeyId: String): LocationJourney? {
        val currentUser = userPreferences.currentUser ?: return null
        val cipherAndMessage =
            getGroupCipherAndDistributionMessage(currentSpaceId, currentUser.id) ?: run {
                Timber.e("Failed to retrieve GroupCipher and DistributionMessage for spaceId: $currentSpaceId, userId: ${currentUser.id}")
                return null
            }
        val (_, groupCipher) = cipherAndMessage

        return try {
            spaceMemberJourneyRef(currentSpaceId, currentUser.id).document(journeyId)
                .get()
                .await()
                .toObject<EncryptedLocationJourney>()
                ?.toDecryptedLocationJourney(groupCipher)
        } catch (e: Exception) {
            Timber.e(e, "Error while getting journey by ID: $journeyId")
            null
        }
    }
}
