package com.canopas.yourspace.data.service.location

import com.canopas.yourspace.data.models.location.ApiLocation
import com.canopas.yourspace.data.models.location.EncryptedApiLocation
import com.canopas.yourspace.data.storage.UserPreferences
import com.canopas.yourspace.data.utils.Config
import com.canopas.yourspace.data.utils.Config.FIRESTORE_COLLECTION_SPACES
import com.canopas.yourspace.data.utils.Config.FIRESTORE_COLLECTION_SPACE_MEMBERS
import com.canopas.yourspace.data.utils.EphemeralECDHUtils
import com.canopas.yourspace.data.utils.snapshotFlow
import com.google.firebase.firestore.Blob
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import org.signal.libsignal.protocol.SignalProtocolAddress
import org.signal.libsignal.protocol.ecc.ECPrivateKey
import org.signal.libsignal.protocol.groups.GroupCipher
import org.signal.libsignal.protocol.groups.GroupSessionBuilder
import org.signal.libsignal.protocol.groups.state.InMemorySenderKeyStore
import org.signal.libsignal.protocol.message.SenderKeyDistributionMessage
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiLocationService @Inject constructor(
    db: FirebaseFirestore,
    private val locationManager: LocationManager,
    private val userPreferences: UserPreferences
) {
    var currentSpaceId: String = userPreferences.currentSpace ?: ""

    private val spaceRef = db.collection(FIRESTORE_COLLECTION_SPACES)
    private fun spaceMemberRef(spaceId: String) =
        spaceRef.document(spaceId.takeIf { it.isNotBlank() } ?: "null").collection(
            FIRESTORE_COLLECTION_SPACE_MEMBERS
        )

    private fun spaceMemberLocationRef(spaceId: String, userId: String) =
        spaceMemberRef(spaceId)
            .document(userId.takeIf { it.isNotBlank() } ?: "null")
            .collection(Config.FIRESTORE_COLLECTION_USER_LOCATIONS)

    private fun spaceGroupKeysRef(spaceId: String) =
        spaceRef.document(spaceId.takeIf { it.isNotBlank() } ?: "null").collection(
            Config.FIRESTORE_COLLECTION_SPACE_GROUP_KEYS
        )

    suspend fun saveLastKnownLocation(
        userId: String
    ) {
        val lastLocation = locationManager.getLastLocation() ?: return
        userPreferences.currentUser?.space_ids?.forEach { spaceId ->
            val cipherAndDistributionMessage = getGroupCipherAndDistributionMessage(spaceId, userId)
            val groupCipher = cipherAndDistributionMessage?.second ?: return
            val distributionMessage = cipherAndDistributionMessage.first
            val lat = groupCipher.encrypt(
                distributionMessage.distributionId,
                lastLocation.latitude.toString().toByteArray(Charsets.UTF_8)
            )
            val lon = groupCipher.encrypt(
                distributionMessage.distributionId,
                lastLocation.longitude.toString().toByteArray(Charsets.UTF_8)
            )

            Timber.d("Last known location: $lastLocation\nLat: $lat\nLon: $lon")
            val docRef = spaceMemberLocationRef(spaceId, userId).document()

            val location = EncryptedApiLocation(
                id = docRef.id,
                user_id = userId,
                encrypted_latitude = Blob.fromBytes(lat.serialize()),
                encrypted_longitude = Blob.fromBytes(lon.serialize()),
                created_at = System.currentTimeMillis()
            )

            docRef.set(location).await()
        }
    }

    suspend fun saveCurrentLocation(
        userId: String,
        latitude: Double,
        longitude: Double,
        recordedAt: Long
    ) {
        Timber.e("Saving current location for user $userId")
        userPreferences.currentUser?.space_ids?.forEach { spaceId ->
            val cipherAndDistributionMessage = getGroupCipherAndDistributionMessage(spaceId, userId)
            Timber.e("Cipher and distribution message: $cipherAndDistributionMessage")
            val groupCipher = cipherAndDistributionMessage?.second ?: return
            val distributionMessage = cipherAndDistributionMessage.first
            val lat = groupCipher.encrypt(
                distributionMessage.distributionId,
                latitude.toString().toByteArray(Charsets.UTF_8)
            )
            val lon = groupCipher.encrypt(
                distributionMessage.distributionId,
                longitude.toString().toByteArray(Charsets.UTF_8)
            )

            Timber.d("Current location: $latitude, $longitude\nLat: $lat\nLon: $lon")

            val docRef = spaceMemberLocationRef(spaceId, userId).document()

            val location = EncryptedApiLocation(
                id = docRef.id,
                user_id = userId,
                encrypted_latitude = Blob.fromBytes(lat.serialize()),
                encrypted_longitude = Blob.fromBytes(lon.serialize()),
                created_at = recordedAt
            )

            docRef.set(location).await()
        }
    }

    suspend fun getCurrentLocation(userId: String): Flow<List<ApiLocation?>> {
        return flow {
            try {
                val encryptedLocation =
                    spaceMemberLocationRef(currentSpaceId, userId).whereEqualTo("user_id", userId)
                        .orderBy("created_at", Query.Direction.DESCENDING).limit(1)
                        .snapshotFlow(EncryptedApiLocation::class.java)
                encryptedLocation.collect { encryptedLocationList ->
                    val apiLocations = encryptedLocationList.map { encryptedLocation ->
                        val receiverGroupCipher =
                            getGroupCipherAndDistributionMessage(currentSpaceId, userId)?.second
                                ?: return@map null
                        Timber.e("Receiver group cipher: $receiverGroupCipher")
                        val lat =
                            receiverGroupCipher.decrypt(encryptedLocation.encrypted_latitude.toBytes())
                        val lon =
                            receiverGroupCipher.decrypt(encryptedLocation.encrypted_longitude.toBytes())

                        Timber.d("Decrypted location: $lat, $lon")

                        ApiLocation(
                            user_id = userId,
                            latitude = lat.toString(Charsets.UTF_8).toDouble(),
                            longitude = lon.toString(Charsets.UTF_8).toDouble()
                        )
                    }
                    emit(apiLocations)
                }
            } catch (e: Exception) {
                Timber.e(e, "Error while getting current location")
            }
        }
    }

    private suspend fun getGroupCipherAndDistributionMessage(
        spaceId: String,
        userId: String
    ): Pair<SenderKeyDistributionMessage, GroupCipher?>? {
        val currentUser = userPreferences.currentUser ?: return null
        Timber.e("Getting group cipher for space $spaceId\tUser: $userId")
        val sharedDistributionMessage = spaceGroupKeysRef(spaceId)
            .document(userId).get().await()
        val distributions =
            sharedDistributionMessage["distributions"] as? List<Map<String, Any?>> ?: emptyList()
        val currentUserDistribution =
            distributions.firstOrNull { it["recipientId"] == currentUser.id } ?: return null
        val currentUserCiphertext =
            (currentUserDistribution["ciphertext"] as? Blob)?.toBytes() ?: ByteArray(0)
        val deviceIdBytes =
            (currentUserDistribution["deviceId"] as? Blob)?.toBytes() ?: ByteArray(0)
        val currentUserPrivateKey =
            ECPrivateKey(currentUser.identity_key_private?.toBytes() ?: ByteArray(0))
        val distributionBytes =
            EphemeralECDHUtils.decrypt(currentUserCiphertext, currentUserPrivateKey)
        val deviceId = EphemeralECDHUtils.decrypt(deviceIdBytes, currentUserPrivateKey)
        val distributionMessage = SenderKeyDistributionMessage(distributionBytes)
        val receiverKeyStore = InMemorySenderKeyStore()
        val senderAddress = SignalProtocolAddress(
            spaceId,
            deviceId.toString(Charsets.UTF_8).toInt()
        )
        val sessionBuilder = GroupSessionBuilder(receiverKeyStore)
        val receiverGroupCipher = GroupCipher(receiverKeyStore, senderAddress)
        sessionBuilder.process(senderAddress, distributionMessage)
        Timber.e("Group cipher created for space $spaceId")
        // Log everything created here
        Timber.e("Distribution message: $distributionMessage\nGroup cipher: $receiverGroupCipher")
        return Pair(distributionMessage, receiverGroupCipher)
    }
}
