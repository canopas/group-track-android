package com.canopas.yourspace.data.service.location

import com.canopas.yourspace.data.models.location.ApiLocation
import com.canopas.yourspace.data.models.location.EncryptedApiLocation
import com.canopas.yourspace.data.models.space.ApiSpaceMember
import com.canopas.yourspace.data.models.space.EncryptedDistribution
import com.canopas.yourspace.data.models.space.GroupKeysDoc
import com.canopas.yourspace.data.models.space.MemberKeyData
import com.canopas.yourspace.data.models.user.ApiUser
import com.canopas.yourspace.data.storage.UserPreferences
import com.canopas.yourspace.data.storage.bufferedkeystore.BufferedSenderKeyStore
import com.canopas.yourspace.data.utils.Config
import com.canopas.yourspace.data.utils.Config.FIRESTORE_COLLECTION_SPACES
import com.canopas.yourspace.data.utils.Config.FIRESTORE_COLLECTION_SPACE_GROUP_KEYS
import com.canopas.yourspace.data.utils.Config.FIRESTORE_COLLECTION_SPACE_MEMBERS
import com.canopas.yourspace.data.utils.EphemeralECDHUtils
import com.canopas.yourspace.data.utils.PrivateKeyUtils
import com.canopas.yourspace.data.utils.snapshotFlow
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import org.signal.libsignal.protocol.InvalidKeyException
import org.signal.libsignal.protocol.NoSessionException
import org.signal.libsignal.protocol.SignalProtocolAddress
import org.signal.libsignal.protocol.ecc.Curve
import org.signal.libsignal.protocol.ecc.ECPrivateKey
import org.signal.libsignal.protocol.groups.GroupCipher
import org.signal.libsignal.protocol.groups.GroupSessionBuilder
import org.signal.libsignal.protocol.groups.InvalidSenderKeySessionException
import org.signal.libsignal.protocol.message.SenderKeyDistributionMessage
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiLocationService @Inject constructor(
    private val db: FirebaseFirestore,
    private val locationManager: LocationManager,
    private val userPreferences: UserPreferences,
    private val bufferedSenderKeyStore: BufferedSenderKeyStore
) {
    var currentSpaceId: String
        get() = userPreferences.currentSpace ?: ""
        set(value) {
            userPreferences.currentSpace = value
        }

    private val spaceRef by lazy { db.collection(FIRESTORE_COLLECTION_SPACES) }

    private fun spaceMemberRef(spaceId: String) =
        spaceRef.document(spaceId).collection(FIRESTORE_COLLECTION_SPACE_MEMBERS)

    private fun spaceMemberLocationRef(spaceId: String, userId: String) =
        spaceMemberRef(spaceId).document(userId)
            .collection(Config.FIRESTORE_COLLECTION_USER_LOCATIONS)

    private fun spaceGroupKeysRef(spaceId: String) =
        spaceRef.document(spaceId).collection(FIRESTORE_COLLECTION_SPACE_GROUP_KEYS)
            .document(FIRESTORE_COLLECTION_SPACE_GROUP_KEYS)

    suspend fun saveLastKnownLocation(user: ApiUser) {
        val lastLocation = locationManager.getLastLocation() ?: return
        saveCurrentLocation(
            user = user,
            latitude = lastLocation.latitude,
            longitude = lastLocation.longitude,
            recordedAt = System.currentTimeMillis()
        )
    }

    suspend fun saveCurrentLocation(
        user: ApiUser,
        latitude: Double,
        longitude: Double,
        recordedAt: Long
    ) {
        user.space_ids?.forEach { spaceId ->
            val userId = user.id
            if (spaceId.isBlank()) return@forEach

            // Check if user is premium before encrypting location
            // In future, need to check if encryption is enabled for the space
            if (user.isPremiumUser) {
                saveEncryptedLocation(spaceId, userId, latitude, longitude, recordedAt)
            } else {
                savePlainLocation(spaceId, userId, latitude, longitude, recordedAt)
            }
        }
    }

    /**
     * Saves the location of the current user in end-to-end encrypted form.
     */
    private suspend fun saveEncryptedLocation(
        spaceId: String,
        userId: String,
        latitude: Double,
        longitude: Double,
        recordedAt: Long
    ) {
        try {
            val cipherAndDistribution = getGroupCipherAndDistributionMessage(
                spaceId = spaceId,
                userId = userId,
                canDistributeSenderKey = true
            ) ?: return

            val (distributionMessage, groupCipher) = cipherAndDistribution

            val encryptedLocation = EncryptedApiLocation(
                user_id = userId,
                latitude = groupCipher.encrypt(
                    distributionMessage.distributionId,
                    latitude.toString().toByteArray()
                ).serialize().encodeToString(),
                longitude = groupCipher.encrypt(
                    distributionMessage.distributionId,
                    longitude.toString().toByteArray()
                ).serialize().encodeToString(),
                created_at = recordedAt
            )

            spaceMemberLocationRef(spaceId, userId).document(encryptedLocation.id)
                .set(encryptedLocation).await()
        } catch (e: NoSessionException) {
            Timber.e("No session found. Skipping save.")
        } catch (e: InvalidSenderKeySessionException) {
            Timber.e("Invalid sender key session. Skipping save.")
        } catch (e: Exception) {
            Timber.e(e, "Failed to save encrypted location")
        }
    }

    /**
     * Saves the location of the current user in plain text.
     */
    private suspend fun savePlainLocation(
        spaceId: String,
        userId: String,
        latitude: Double,
        longitude: Double,
        recordedAt: Long
    ) {
        val location = ApiLocation(
            id = spaceMemberLocationRef(spaceId, userId).document().id,
            user_id = userId,
            latitude = latitude,
            longitude = longitude,
            created_at = recordedAt
        )
        spaceMemberLocationRef(spaceId, userId).document(location.id).set(location).await()
    }

    suspend fun getCurrentLocation(user: ApiUser): Flow<List<ApiLocation?>> {
        val userId = user.id
        val locationRef = spaceMemberLocationRef(currentSpaceId, userId)
            .whereEqualTo("user_id", userId)
            .orderBy("created_at", Query.Direction.DESCENDING)
            .limit(1)

        return if (user.isPremiumUser) {
            locationRef.snapshotFlow(EncryptedApiLocation::class.java)
                .map { encryptedLocations ->
                    encryptedLocations.mapNotNull { decryptLocation(it, userId) }
                }
        } else {
            locationRef.snapshotFlow(ApiLocation::class.java)
        }
    }

    private suspend fun decryptLocation(
        encryptedLocation: EncryptedApiLocation,
        userId: String
    ): ApiLocation? {
        val groupCipher =
            getGroupCipherAndDistributionMessage(currentSpaceId, userId)?.second ?: return null

        return try {
            val latitude =
                groupCipher.decrypt(encryptedLocation.latitude.toBytes()).toString(Charsets.UTF_8)
                    .toDoubleOrNull()
            val longitude =
                groupCipher.decrypt(encryptedLocation.longitude.toBytes()).toString(Charsets.UTF_8)
                    .toDoubleOrNull()

            if (latitude != null && longitude != null) {
                ApiLocation(
                    id = encryptedLocation.id,
                    user_id = userId,
                    latitude = latitude,
                    longitude = longitude,
                    created_at = encryptedLocation.created_at
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to decrypt location for userId: $userId")
            null
        }
    }

    private suspend fun getSpaceMembers(spaceId: String): List<ApiSpaceMember> {
        return spaceMemberRef(spaceId).get().await().toObjects(ApiSpaceMember::class.java)
    }

    /**
     * Provide group cipher and sender key distribution message for a particular space and user.
     */
    private suspend fun getGroupCipherAndDistributionMessage(
        spaceId: String,
        userId: String,
        canDistributeSenderKey: Boolean = false
    ): Pair<SenderKeyDistributionMessage, GroupCipher>? {
        val currentUser = userPreferences.currentUser ?: return null

        val snapshot = spaceGroupKeysRef(spaceId).get().await()
        val groupKeysDoc = snapshot.toObject(GroupKeysDoc::class.java) ?: return null
        val memberKeyData = groupKeysDoc.member_keys[userId] ?: return null

        val distribution = memberKeyData.distributions.sortedByDescending {
            it.created_at
        }.firstOrNull {
            it.recipient_id == currentUser.id
        } ?: return null

        val currentUserPrivateKey = getCurrentUserPrivateKey(currentUser) ?: return null

        val decryptedDistribution = EphemeralECDHUtils.decrypt(distribution, currentUserPrivateKey)
            ?: run {
                Timber.e("Failed to decrypt distribution for userId=$userId")
                return null
            }

        val distributionMessage = SenderKeyDistributionMessage(decryptedDistribution)
        val groupAddress = SignalProtocolAddress(spaceId, memberKeyData.member_device_id)

        bufferedSenderKeyStore.loadSenderKey(groupAddress, distributionMessage.distributionId)

        // If the sender key data is outdated, we need to distribute the sender key to the pending users
        if (memberKeyData.data_updated_at < groupKeysDoc.doc_updated_at && canDistributeSenderKey) {
            // Here means the sender key data is outdated, so we need to distribute the sender key to the users.
            rotateSenderKey(spaceId = spaceId, deviceId = memberKeyData.member_device_id)
        }

        return try {
            GroupSessionBuilder(bufferedSenderKeyStore).process(groupAddress, distributionMessage)
            val groupCipher = GroupCipher(bufferedSenderKeyStore, groupAddress)
            Pair(distributionMessage, groupCipher)
        } catch (e: Exception) {
            Timber.e(e, "Error processing group session for userId=$userId")
            null
        }
    }

    /**
     * Decrypts and retrieves the current user's private key.
     */
    private suspend fun getCurrentUserPrivateKey(currentUser: ApiUser): ECPrivateKey? {
        val privateKey = try {
            userPreferences.getPrivateKey() ?: currentUser.identity_key_private?.toBytes()
        } catch (e: Exception) {
            Timber.e(e, "Failed to retrieve private key")
            return null
        }
        return try {
            Curve.decodePrivatePoint(privateKey)
        } catch (e: InvalidKeyException) {
            Timber.e(e, "Error decoding private key")
            PrivateKeyUtils.decryptPrivateKey(
                encryptedPrivateKey = privateKey ?: return null,
                salt = currentUser.identity_key_salt?.toBytes() ?: return null,
                passkey = userPreferences.getPasskey() ?: return null
            )?.let { Curve.decodePrivatePoint(it) }
        }
    }

    /**
     * Rotates the sender key for a given space.
     */
    private suspend fun rotateSenderKey(spaceId: String, deviceId: Int) {
        val user = userPreferences.currentUser ?: return

        val groupAddress = SignalProtocolAddress(spaceId, deviceId)
        val sessionBuilder = GroupSessionBuilder(bufferedSenderKeyStore)
        val newDistributionMessage = sessionBuilder.create(groupAddress, UUID.randomUUID())
        val newDistributionBytes = newDistributionMessage.serialize()

        val apiSpaceMembers = getSpaceMembers(spaceId)
        val memberIds = apiSpaceMembers.map { it.user_id }.toSet()
        val newDistributions = mutableListOf<EncryptedDistribution>()

        for (member in apiSpaceMembers) {
            val publicBlob = member.identity_key_public ?: continue
            val publicKey = Curve.decodePoint(publicBlob.toBytes(), 0)

            newDistributions.add(
                EphemeralECDHUtils.encrypt(
                    member.user_id,
                    newDistributionBytes,
                    publicKey
                )
            )
        }

        db.runTransaction { transaction ->
            val docRef = spaceGroupKeysRef(spaceId)
            val snapshot = transaction.get(docRef)
            val groupKeysDoc = snapshot.toObject(GroupKeysDoc::class.java) ?: GroupKeysDoc()

            val oldKeyData = groupKeysDoc.member_keys[user.id] ?: MemberKeyData()

            // Filter out distributions for members who are no longer in the space
            val filteredOldDistributions =
                oldKeyData.distributions.filter { it.recipient_id in memberIds }

            val rotatedKeyData = oldKeyData.copy(
                member_device_id = deviceId,
                distributions = newDistributions + filteredOldDistributions,
                data_updated_at = System.currentTimeMillis()
            )

            val updates = mapOf(
                "member_keys.${user.id}" to rotatedKeyData,
                "doc_updated_at" to System.currentTimeMillis()
            )

            transaction.update(docRef, updates)
        }.await()
    }
}
