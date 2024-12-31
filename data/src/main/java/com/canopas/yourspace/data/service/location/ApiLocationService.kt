package com.canopas.yourspace.data.service.location

import com.canopas.yourspace.data.models.location.ApiLocation
import com.canopas.yourspace.data.models.location.EncryptedApiLocation
import com.canopas.yourspace.data.models.space.ApiSpaceMember
import com.canopas.yourspace.data.models.space.SenderKeyDistribution
import com.canopas.yourspace.data.storage.UserPreferences
import com.canopas.yourspace.data.storage.bufferedkeystore.BufferedSenderKeyStore
import com.canopas.yourspace.data.utils.Config
import com.canopas.yourspace.data.utils.Config.FIRESTORE_COLLECTION_SPACES
import com.canopas.yourspace.data.utils.Config.FIRESTORE_COLLECTION_SPACE_MEMBERS
import com.canopas.yourspace.data.utils.EphemeralECDHUtils
import com.canopas.yourspace.data.utils.snapshotFlow
import com.google.firebase.firestore.Blob
import com.google.firebase.firestore.FieldValue
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
import java.util.UUID
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
            val cipherAndDistributionMessage =
                getGroupCipherAndDistributionMessage(spaceId, userId, canDistributeSenderKey = true)
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
            val cipherAndDistributionMessage =
                getGroupCipherAndDistributionMessage(spaceId, userId, canDistributeSenderKey = true)
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

            Timber.e("YYYYYY: LAt: $lat, Lon: $lon")
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
                Timber.e("YYYYYY: Here")
                val encryptedLocation =
                    spaceMemberLocationRef(currentSpaceId, userId).whereEqualTo("user_id", userId)
                        .orderBy("created_at", Query.Direction.DESCENDING).limit(1)
                        .snapshotFlow(EncryptedApiLocation::class.java)
                Timber.e("YYYYYY: Here - encryptedLocation: $encryptedLocation")
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
                    Timber.e("YYYYYY: Here - apiLocations: $apiLocations")
                    emit(apiLocations)
                }
            } catch (e: Exception) {
                Timber.e(e, "Error while getting current location")
            }
        }
    }

    private suspend fun getGroupCipherAndDistributionMessage(
        spaceId: String,
        userId: String,
        canDistributeSenderKey: Boolean = false
    ): Pair<SenderKeyDistributionMessage, GroupCipher?>? {
        val currentUser = userPreferences.currentUser ?: return null
        val senderKeyDistribution = spaceGroupKeysRef(spaceId)
            .document(userId).get().await().toObject(SenderKeyDistribution::class.java)

        val distributions = senderKeyDistribution?.distributions ?: return null
        val currentUserDistribution = distributions.firstOrNull { it.recipientId == currentUser.id }
        if (currentUserDistribution == null && canDistributeSenderKey) {
            Timber.e(
                "Sender key distribution not found for $userId in space $spaceId.\n+" +
                    "Can be a new member. Distributing sender key to new member..."
            )
            distributeSenderKeyToNewMember(
                spaceId = spaceId,
                senderUserId = senderKeyDistribution.senderId,
                newMember = ApiSpaceMember(
                    user_id = userId,
                    space_id = spaceId,
                    identity_key_public = currentUser.identity_key_public
                ),
                senderDeviceId = senderKeyDistribution.senderDeviceId
            )
            getGroupCipherAndDistributionMessage(spaceId, userId, canDistributeSenderKey = false)
        }
        if (currentUserDistribution == null) {
            return null
        }
        val currentUserPrivateKey =
            Curve.decodePrivatePoint(currentUser.identity_key_private?.toBytes())

        val decryptedDistribution =
            EphemeralECDHUtils.decrypt(currentUserDistribution, currentUserPrivateKey)

        val distributionMessage = SenderKeyDistributionMessage(decryptedDistribution)
        val senderAddress = SignalProtocolAddress(
            spaceId,
            senderKeyDistribution.senderDeviceId
        )

        // Load sender key for particular distributionId/space
        bufferedSenderKeyStore.loadSenderKey(senderAddress, distributionMessage.distributionId)

        val sessionBuilder = GroupSessionBuilder(bufferedSenderKeyStore)
        val receiverGroupCipher = GroupCipher(bufferedSenderKeyStore, senderAddress)
        sessionBuilder.process(senderAddress, distributionMessage)
        return Pair(distributionMessage, receiverGroupCipher)
    }

    private suspend fun distributeSenderKeyToNewMember(
        spaceId: String,
        senderUserId: String,
        newMember: ApiSpaceMember,
        senderDeviceId: Int
    ) {
        val groupAddress = SignalProtocolAddress(spaceId, senderDeviceId)
        val sessionBuilder = GroupSessionBuilder(bufferedSenderKeyStore)
        val distributionMessage = sessionBuilder.create(groupAddress, UUID.fromString(spaceId))
        val distributionBytes = distributionMessage.serialize()
        val publicBlob = newMember.identity_key_public ?: return
        val publicKey = Curve.decodePoint(publicBlob.toBytes(), 0)
        val docRef = spaceGroupKeysRef(spaceId).document(senderUserId)

        val distribution = EphemeralECDHUtils.encrypt(
            newMember.user_id,
            distributionBytes,
            publicKey
        )
        docRef.update(
            "distributions",
            FieldValue.arrayUnion(distribution)
        ).await()
        Timber.d("Sender key distribution uploaded for new member: ${newMember.user_id} for sender $senderUserId.")
    }
}
