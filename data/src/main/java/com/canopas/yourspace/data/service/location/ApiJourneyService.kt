package com.canopas.yourspace.data.service.location

import com.canopas.yourspace.data.models.location.EncryptedJourneyRoute
import com.canopas.yourspace.data.models.location.EncryptedLocationJourney
import com.canopas.yourspace.data.models.location.JourneyRoute
import com.canopas.yourspace.data.models.location.LocationJourney
import com.canopas.yourspace.data.models.space.SenderKeyDistribution
import com.canopas.yourspace.data.models.user.ApiUser
import com.canopas.yourspace.data.storage.UserPreferences
import com.canopas.yourspace.data.storage.bufferedkeystore.BufferedSenderKeyStore
import com.canopas.yourspace.data.utils.Config
import com.canopas.yourspace.data.utils.Config.FIRESTORE_COLLECTION_SPACES
import com.canopas.yourspace.data.utils.Config.FIRESTORE_COLLECTION_SPACE_MEMBERS
import com.canopas.yourspace.data.utils.EphemeralECDHUtils
import com.canopas.yourspace.data.utils.PrivateKeyUtils
import com.google.firebase.firestore.Blob
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

    private fun spaceMemberRef(spaceId: String) =
        spaceRef.document(spaceId.takeIf { it.isNotBlank() } ?: "null")
            .collection(FIRESTORE_COLLECTION_SPACE_MEMBERS)

    private fun spaceMemberJourneyRef(spaceId: String, userId: String) =
        spaceMemberRef(spaceId).document(userId.takeIf { it.isNotBlank() } ?: "null")
            .collection(Config.FIRESTORE_COLLECTION_USER_JOURNEYS)

    private fun spaceGroupKeysRef(spaceId: String) =
        spaceRef.document(spaceId.takeIf { it.isNotBlank() } ?: "null")
            .collection(Config.FIRESTORE_COLLECTION_SPACE_GROUP_KEYS)

    private suspend fun getGroupCipher(spaceId: String, userId: String): GroupCipher? {
        val senderKeyDistributionRef = spaceGroupKeysRef(spaceId).document(userId).get().await()
        val senderKeyDistribution =
            senderKeyDistributionRef.toObject(SenderKeyDistribution::class.java) ?: return null

        val currentUser = userPreferences.currentUser ?: return null

        val privateKey = getCurrentUserPrivateKey(currentUser) ?: return null

        val distribution =
            senderKeyDistribution.distributions.firstOrNull { it.recipientId == currentUser.id }
                ?: return null
        val decryptedDistributionBytes =
            EphemeralECDHUtils.decrypt(distribution, privateKey) ?: return null
        val distributionMessage = SenderKeyDistributionMessage(decryptedDistributionBytes)

        val groupAddress = SignalProtocolAddress(spaceId, senderKeyDistribution.senderDeviceId)
        val sessionBuilder = GroupSessionBuilder(bufferedSenderKeyStore)
        sessionBuilder.process(groupAddress, distributionMessage)

        return GroupCipher(bufferedSenderKeyStore, groupAddress)
    }

    /**
     * Decrypts and retrieves the current user's private key.
     */
    private suspend fun getCurrentUserPrivateKey(currentUser: ApiUser): ECPrivateKey? {
        return try {
            Curve.decodePrivatePoint(currentUser.identity_key_private?.toBytes())
        } catch (e: InvalidKeyException) {
            Timber.e(e, "Error decoding private key for userId=${currentUser.id}")
            PrivateKeyUtils.decryptPrivateKey(
                currentUser.identity_key_private?.toBytes() ?: return null,
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
        newJourneyId: ((String) -> Unit)? = null
    ) {
        userPreferences.currentUser?.space_ids?.forEach { spaceId ->
            val groupCipher = getGroupCipher(spaceId, userId) ?: run {
                Timber.e("Failed to retrieve GroupCipher for spaceId: $spaceId, userId: $userId")
                return@forEach
            }

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
                update_at = updateAt ?: System.currentTimeMillis()
            )

            val docRef = spaceMemberJourneyRef(spaceId, userId).document(journey.id)

            val encryptedJourney = journey.toEncryptedLocationJourney(groupCipher)
            newJourneyId?.invoke(encryptedJourney.id)

            docRef.set(encryptedJourney).await()
        }
    }

    private fun LocationJourney.toEncryptedLocationJourney(groupCipher: GroupCipher): EncryptedLocationJourney {
        val encryptedFromLat = groupCipher.encrypt(
            UUID.fromString(id),
            from_latitude.toString().toByteArray(Charsets.UTF_8)
        )
        val encryptedFromLong = groupCipher.encrypt(
            UUID.fromString(id),
            from_longitude.toString().toByteArray(Charsets.UTF_8)
        )
        val encryptedToLat = to_latitude?.let {
            groupCipher.encrypt(
                UUID.fromString(id),
                it.toString().toByteArray(Charsets.UTF_8)
            )
        }
        val encryptedToLong = to_longitude?.let {
            groupCipher.encrypt(
                UUID.fromString(id),
                it.toString().toByteArray(Charsets.UTF_8)
            )
        }

        val encryptedRoutes = routes.map {
            EncryptedJourneyRoute(
                encrypted_latitude = Blob.fromBytes(
                    groupCipher.encrypt(
                        UUID.fromString(id),
                        it.latitude.toString().toByteArray(Charsets.UTF_8)
                    ).serialize()
                ),
                encrypted_longitude = Blob.fromBytes(
                    groupCipher.encrypt(
                        UUID.fromString(id),
                        it.longitude.toString().toByteArray(Charsets.UTF_8)
                    ).serialize()
                )
            )
        }

        return EncryptedLocationJourney(
            id = id,
            user_id = user_id,
            encrypted_from_latitude = Blob.fromBytes(encryptedFromLat.serialize()),
            encrypted_from_longitude = Blob.fromBytes(encryptedFromLong.serialize()),
            encrypted_to_latitude = encryptedToLat?.let { Blob.fromBytes(it.serialize()) },
            encrypted_to_longitude = encryptedToLong?.let { Blob.fromBytes(it.serialize()) },
            route_distance = route_distance,
            route_duration = route_duration,
            encrypted_routes = encryptedRoutes,
            created_at = created_at,
            updated_at = update_at
        )
    }

    private fun EncryptedLocationJourney.toDecryptedLocationJourney(groupCipher: GroupCipher): LocationJourney {
        val decryptedFromLat = groupCipher.decrypt(encrypted_from_latitude.toBytes())
        val decryptedFromLong = groupCipher.decrypt(encrypted_from_longitude.toBytes())
        val decryptedToLat = encrypted_to_latitude?.let { groupCipher.decrypt(it.toBytes()) }
        val decryptedToLong = encrypted_to_longitude?.let { groupCipher.decrypt(it.toBytes()) }

        val decryptedRoutes = encrypted_routes.map {
            JourneyRoute(
                latitude = groupCipher.decrypt(it.encrypted_latitude.toBytes())
                    .toString(Charsets.UTF_8).toDouble(),
                longitude = groupCipher.decrypt(it.encrypted_longitude.toBytes())
                    .toString(Charsets.UTF_8).toDouble()
            )
        }

        return LocationJourney(
            id = id,
            user_id = user_id,
            from_latitude = decryptedFromLat.toString(Charsets.UTF_8).toDouble(),
            from_longitude = decryptedFromLong.toString(Charsets.UTF_8).toDouble(),
            to_latitude = decryptedToLat?.toString(Charsets.UTF_8)?.toDouble(),
            to_longitude = decryptedToLong?.toString(Charsets.UTF_8)?.toDouble(),
            route_distance = route_distance,
            route_duration = route_duration,
            routes = decryptedRoutes,
            created_at = created_at,
            update_at = updated_at
        )
    }

    suspend fun updateLastLocationJourney(userId: String, journey: LocationJourney) {
        userPreferences.currentUser?.space_ids?.forEach { spaceId ->
            val groupCipher = getGroupCipher(spaceId, userId) ?: run {
                Timber.e("Failed to retrieve GroupCipher for spaceId: $spaceId, userId: $userId")
                return@forEach
            }

            val encryptedJourney = journey.toEncryptedLocationJourney(groupCipher)
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

    suspend fun getLastJourneyLocation(userId: String): LocationJourney? {
        val currentSpaceId = userPreferences.currentSpace ?: return null
        val groupCipher = getGroupCipher(currentSpaceId, userId) ?: run {
            Timber.e("Failed to retrieve GroupCipher for spaceId: $currentSpaceId, userId: $userId")
            return null
        }

        return try {
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
        } catch (e: Exception) {
            Timber.e(e, "Error while getting last location journey for userId: $userId")
            null
        }
    }

    suspend fun getMoreJourneyHistory(userId: String, from: Long?): List<LocationJourney> {
        val currentSpaceId = userPreferences.currentSpace ?: return emptyList()
        val groupCipher = getGroupCipher(currentSpaceId, userId) ?: run {
            Timber.e("Failed to retrieve GroupCipher for spaceId: $currentSpaceId, userId: $userId")
            return emptyList()
        }

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
        val currentSpaceId = userPreferences.currentSpace ?: return emptyList()
        val groupCipher = getGroupCipher(currentSpaceId, userId) ?: run {
            Timber.e("Failed to retrieve GroupCipher for spaceId: $currentSpaceId, userId: $userId")
            return emptyList()
        }

        return try {
            val previousDayJourney = spaceMemberJourneyRef(currentSpaceId, userId)
                .whereEqualTo("user_id", userId)
                .whereLessThan("created_at", from)
                .whereGreaterThanOrEqualTo("update_at", from)
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
        val currentSpaceId = userPreferences.currentSpace ?: return null
        val currentUser = userPreferences.currentUser ?: return null
        val groupCipher = getGroupCipher(currentSpaceId, currentUser.id) ?: return null

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
