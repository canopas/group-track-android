package com.canopas.yourspace.data.service.location

import com.canopas.yourspace.data.models.location.ApiLocation
import com.canopas.yourspace.data.models.location.EncryptedApiLocation
import com.canopas.yourspace.data.models.space.SenderKeyDistribution
import com.canopas.yourspace.data.storage.UserPreferences
import com.canopas.yourspace.data.storage.bufferedkeystore.BufferedSenderKeyStore
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
import org.signal.libsignal.protocol.ecc.Curve
import org.signal.libsignal.protocol.groups.GroupCipher
import org.signal.libsignal.protocol.groups.GroupSessionBuilder
import org.signal.libsignal.protocol.message.SenderKeyDistributionMessage
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiLocationService @Inject constructor(
    db: FirebaseFirestore,
    private val locationManager: LocationManager,
    private val userPreferences: UserPreferences,
    private val bufferedSenderKeyStore: BufferedSenderKeyStore
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
            Timber.e("XXXXXX: Distribution id: ${distributionMessage.distributionId}")
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
        userPreferences.currentUser?.space_ids?.forEach { spaceId ->
            val cipherAndDistributionMessage = getGroupCipherAndDistributionMessage(spaceId, userId)
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
                        val lat =
                            receiverGroupCipher.decrypt(encryptedLocation.encrypted_latitude.toBytes())
                        val lon =
                            receiverGroupCipher.decrypt(encryptedLocation.encrypted_longitude.toBytes())

                        ApiLocation(
                            id = encryptedLocation.id,
                            user_id = userId,
                            latitude = lat.toString(Charsets.UTF_8).toDouble(),
                            longitude = lon.toString(Charsets.UTF_8).toDouble(),
                            created_at = encryptedLocation.created_at
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
        val senderKeyDistribution = spaceGroupKeysRef(spaceId)
            .document(userId).get().await().toObject(SenderKeyDistribution::class.java)
        val distributions = senderKeyDistribution?.distributions ?: return null
        val currentUserDistribution = distributions.firstOrNull { it.recipientId == currentUser.id }
            ?: return null
        val currentUserPrivateKey = Curve.decodePrivatePoint(currentUser.identity_key_private?.toBytes())

        val decryptedDistribution =
            EphemeralECDHUtils.decrypt(currentUserDistribution, currentUserPrivateKey)

        val distributionMessage = SenderKeyDistributionMessage(decryptedDistribution)
        val senderAddress = SignalProtocolAddress(
            spaceId,
            senderKeyDistribution.senderDeviceId
        )
        val sessionBuilder = GroupSessionBuilder(bufferedSenderKeyStore)
        val receiverGroupCipher = GroupCipher(bufferedSenderKeyStore, senderAddress)
        sessionBuilder.process(senderAddress, distributionMessage)
        return Pair(distributionMessage, receiverGroupCipher)
    }
}
