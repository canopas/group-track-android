package com.canopas.yourspace.data.service.location

import com.canopas.yourspace.data.models.location.ApiLocation
import com.canopas.yourspace.data.models.location.EncryptedApiLocation
import com.canopas.yourspace.data.models.space.ApiSpaceMember
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
import com.google.firebase.firestore.Blob
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
        spaceRef.document(spaceId.takeIf { it.isNotBlank() } ?: "null")
            .collection(FIRESTORE_COLLECTION_SPACE_MEMBERS)

    private fun spaceMemberLocationRef(spaceId: String, userId: String) =
        spaceMemberRef(spaceId.takeIf { it.isNotBlank() } ?: "null").document(userId)
            .collection(Config.FIRESTORE_COLLECTION_USER_LOCATIONS)

    private fun spaceGroupKeysRef(spaceId: String) =
        spaceRef.document(spaceId.takeIf { it.isNotBlank() } ?: "null")
            .collection(FIRESTORE_COLLECTION_SPACE_GROUP_KEYS)
            .document(FIRESTORE_COLLECTION_SPACE_GROUP_KEYS)

    suspend fun saveLastKnownLocation(userId: String) {
        val lastLocation = locationManager.getLastLocation() ?: return
        val currentUser = userPreferences.currentUser ?: return

        currentUser.space_ids?.forEach { spaceId ->
            if (spaceId.isBlank()) return@forEach

            saveEncryptedLocation(
                spaceId = spaceId,
                userId = userId,
                latitude = lastLocation.latitude,
                longitude = lastLocation.longitude,
                recordedAt = System.currentTimeMillis()
            )
        }
    }

    suspend fun saveCurrentLocation(
        userId: String,
        latitude: Double,
        longitude: Double,
        recordedAt: Long
    ) {
        val currentUser = userPreferences.currentUser ?: return

        currentUser.space_ids?.forEach { spaceId ->
            if (spaceId.isBlank()) return@forEach

            saveEncryptedLocation(
                spaceId = spaceId,
                userId = userId,
                latitude = latitude,
                longitude = longitude,
                recordedAt = recordedAt
            )
        }
    }

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

            val encryptedLatitude = groupCipher.encrypt(
                distributionMessage.distributionId,
                latitude.toString().toByteArray(Charsets.UTF_8)
            )
            val encryptedLongitude = groupCipher.encrypt(
                distributionMessage.distributionId,
                longitude.toString().toByteArray(Charsets.UTF_8)
            )

            val location = EncryptedApiLocation(
                user_id = userId,
                latitude = Blob.fromBytes(encryptedLatitude.serialize()),
                longitude = Blob.fromBytes(encryptedLongitude.serialize()),
                created_at = recordedAt
            )

            spaceMemberLocationRef(spaceId, userId).document(location.id).set(location).await()
        } catch (e: Exception) {
            when (e) {
                is NoSessionException -> {
                    Timber.e("No session found. Skipping save.")
                }

                is InvalidSenderKeySessionException -> {
                    Timber.e("Invalid sender key session. Skipping save.")
                }

                else -> {
                    Timber.e(e, "Failed to save encrypted location")
                }
            }
        }
    }

    fun getCurrentLocation(userId: String): Flow<List<ApiLocation?>> =
        spaceMemberLocationRef(currentSpaceId, userId)
            .whereEqualTo("user_id", userId)
            .orderBy("created_at", Query.Direction.DESCENDING)
            .limit(1)
            .snapshotFlow(EncryptedApiLocation::class.java)
            .map { encryptedLocations ->
                encryptedLocations.mapNotNull { encryptedLocation ->
                    try {
                        decryptLocation(encryptedLocation, userId)
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to decrypt location for userId: $userId")
                        null
                    }
                }
            }

    private suspend fun decryptLocation(
        encryptedLocation: EncryptedApiLocation,
        userId: String
    ): ApiLocation? {
        val groupCipher =
            getGroupCipherAndDistributionMessage(currentSpaceId, userId)?.second ?: return null

        return try {
            val latitudeBytes = groupCipher.decrypt(encryptedLocation.latitude.toBytes())
            val longitudeBytes =
                groupCipher.decrypt(encryptedLocation.longitude.toBytes())

            val latitude = latitudeBytes.toString(Charsets.UTF_8).toDoubleOrNull()
            val longitude = longitudeBytes.toString(Charsets.UTF_8).toDoubleOrNull()
            if (latitude == null || longitude == null) return null

            ApiLocation(
                id = encryptedLocation.id,
                user_id = userId,
                latitude = latitude,
                longitude = longitude,
                created_at = encryptedLocation.created_at
            )
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
        val memberKeyData = groupKeysDoc.memberKeys[userId] ?: return null

        val distribution = memberKeyData.distributions.firstOrNull {
            it.recipientId == currentUser.id
        } ?: return null

        val currentUserPrivateKey = getCurrentUserPrivateKey(currentUser) ?: return null

        val decryptedDistribution = EphemeralECDHUtils.decrypt(distribution, currentUserPrivateKey)
            ?: run {
                Timber.e("Failed to decrypt distribution for userId=$userId")
                return null
            }

        val distributionMessage = SenderKeyDistributionMessage(decryptedDistribution)
        val groupAddress = SignalProtocolAddress(spaceId, memberKeyData.memberDeviceId)

        bufferedSenderKeyStore.loadSenderKey(groupAddress, distributionMessage.distributionId)

        // If the sender key data is outdated, we need to distribute the sender key to the pending users
        if (memberKeyData.dataUpdatedAt < groupKeysDoc.docUpdatedAt && canDistributeSenderKey) {
            // Here means the sender key data is outdated, so we need to distribute the sender key to the users.
            val apiSpaceMembers = getSpaceMembers(spaceId)
            val membersPendingForSenderKey = apiSpaceMembers.filter { member ->
                memberKeyData.distributions.none { it.recipientId == member.user_id }
            }

            if (membersPendingForSenderKey.isNotEmpty()) {
                distributeSenderKeyToNewSpaceMembers(
                    spaceId = spaceId,
                    senderUserId = userId,
                    distributionMessage = distributionMessage,
                    senderDeviceId = memberKeyData.memberDeviceId,
                    apiSpaceMembers = membersPendingForSenderKey
                )
            }
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
            Timber.e(e, "Failed to retrieve private key for user ${currentUser.id}")
            return null
        }
        return try {
            Curve.decodePrivatePoint(privateKey)
        } catch (e: InvalidKeyException) {
            Timber.e(e, "Error decoding private key for userId=${currentUser.id}")
            PrivateKeyUtils.decryptPrivateKey(
                encryptedPrivateKey = privateKey ?: return null,
                salt = currentUser.identity_key_salt?.toBytes() ?: return null,
                passkey = userPreferences.getPasskey() ?: return null
            )?.let { Curve.decodePrivatePoint(it) }
        }
    }

    /**
     * Create a sender key distribution for the new users, and encrypt the distribution key
     * for each member using their public key(ECDH).
     **/
    private suspend fun distributeSenderKeyToNewSpaceMembers(
        spaceId: String,
        senderUserId: String,
        distributionMessage: SenderKeyDistributionMessage,
        senderDeviceId: Int,
        apiSpaceMembers: List<ApiSpaceMember>
    ) {
        db.runTransaction { transaction ->
            val docRef = spaceGroupKeysRef(spaceId)
            val distributionBytes = distributionMessage.serialize()
            val snapshot = transaction.get(docRef)
            val groupKeysDoc = snapshot.toObject(GroupKeysDoc::class.java) ?: GroupKeysDoc()
            val oldMemberKeyData = groupKeysDoc.memberKeys[senderUserId] ?: MemberKeyData()
            val distributions = oldMemberKeyData.distributions.toMutableList()

            for (member in apiSpaceMembers) {
                val publicBlob = member.identity_key_public ?: continue
                val publicKeyBytes = publicBlob.toBytes()
                if (publicKeyBytes.size != 33) { // Expected size for compressed EC public key
                    Timber.e("Invalid public key size for member ${member.user_id}")
                    continue
                }
                val publicKey = Curve.decodePoint(publicBlob.toBytes(), 0)

                // Encrypt distribution using member's public key
                distributions.add(
                    EphemeralECDHUtils.encrypt(
                        member.user_id,
                        distributionBytes,
                        publicKey
                    )
                )
            }

            val newMemberKeyData = oldMemberKeyData.copy(
                memberDeviceId = senderDeviceId,
                distributions = distributions,
                dataUpdatedAt = System.currentTimeMillis()
            )
            transaction.update(docRef, mapOf("memberKeys.$senderUserId" to newMemberKeyData))
        }.await()
    }
}
