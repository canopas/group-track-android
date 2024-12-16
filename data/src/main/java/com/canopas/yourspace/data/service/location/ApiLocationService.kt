package com.canopas.yourspace.data.service.location

import com.canopas.yourspace.data.models.location.ApiLocation
import com.canopas.yourspace.data.models.location.EncryptedApiLocation
import com.canopas.yourspace.data.models.space.ApiSpace
import com.canopas.yourspace.data.models.space.ApiSpaceMember
import com.canopas.yourspace.data.models.user.ApiUser
import com.canopas.yourspace.data.security.helper.SignalKeyHelper
import com.canopas.yourspace.data.security.session.EncryptedSpaceSession
import com.canopas.yourspace.data.security.session.SpaceKeyDistribution
import com.canopas.yourspace.data.storage.UserPreferences
import com.canopas.yourspace.data.utils.Config
import com.canopas.yourspace.data.utils.Config.FIRESTORE_COLLECTION_SPACES
import com.canopas.yourspace.data.utils.Config.FIRESTORE_COLLECTION_SPACE_MEMBERS
import com.canopas.yourspace.data.utils.Config.FIRESTORE_COLLECTION_USERS
import com.canopas.yourspace.data.utils.snapshotFlow
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiLocationService @Inject constructor(
    db: FirebaseFirestore,
    private val locationManager: LocationManager,
    private val userPreferences: UserPreferences,
    private val signalKeyHelper: SignalKeyHelper
) {
    var currentSpaceId: String = userPreferences.currentSpace ?: ""

    private val spaceRef = db.collection(FIRESTORE_COLLECTION_SPACES)
    private val userRef = db.collection(FIRESTORE_COLLECTION_USERS)
    private fun spaceMemberRef(spaceId: String) =
        spaceRef.document(spaceId.takeIf { it.isNotBlank() } ?: "null").collection(
            FIRESTORE_COLLECTION_SPACE_MEMBERS
        )
    private fun spaceMemberLocationRef(spaceId: String, userId: String) =
        spaceMemberRef(spaceId)
            .document(userId.takeIf { it.isNotBlank() } ?: "null")
            .collection(Config.FIRESTORE_COLLECTION_USER_LOCATIONS)

    suspend fun saveLastKnownLocation(
        userId: String
    ) {
        val lastLocation = locationManager.getLastLocation() ?: return
        val user = userPreferences.currentUser ?: return
        val userDeviceId = userPreferences.currentUserSession?.device_id ?: return
        userPreferences.currentUser?.space_ids?.forEach { spaceId ->
            val (_, senderKeyRecord) = signalKeyHelper.createDistributionKey(
                user = user,
                deviceId = userDeviceId,
                spaceId = spaceId
            )
            val spaceSession = EncryptedSpaceSession(
                currentUser = user,
                keyRecord = senderKeyRecord,
                spaceId = spaceId
            )
            val spaceMembers = spaceMemberRef(spaceId).get().await().toObjects(ApiSpaceMember::class.java)
            val mSenderKeyDistributionModel = ArrayList<SpaceKeyDistribution>().apply {
                spaceMembers.forEach { member ->
                    val memberUser = getUser(member.user_id) ?: return
                    val decryptedSenderKey = getDecryptedSenderKey(
                        spaceId,
                        memberUser,
                        memberUser.public_key!!
                    )
                    add(
                        SpaceKeyDistribution(
                            member.user_id,
                            decryptedSenderKey
                        )
                    )
                }
            }
            spaceSession.createSession(mSenderKeyDistributionModel)
            val encryptedLatitude = spaceSession.encryptMessage(lastLocation.latitude.toString())
            val encryptedLongitude = spaceSession.encryptMessage(lastLocation.longitude.toString())
            val docRef = spaceMemberLocationRef(spaceId, userId).document()

            val encryptedLocation = EncryptedApiLocation(
                id = docRef.id,
                user_id = userId,
                encrypted_latitude = encryptedLatitude,
                encrypted_longitude = encryptedLongitude,
                created_at = System.currentTimeMillis()
            )

            docRef.set(encryptedLocation).await()
        }
    }

    suspend fun getUser(userId: String): ApiUser? {
        return try {
            userRef.document(userId).get().await().toObject(ApiUser::class.java)
        } catch (e: Exception) {
            Timber.e(e, "Error while getting user")
            null
        }
    }

    suspend fun saveCurrentLocation(
        userId: String,
        latitude: Double,
        longitude: Double,
        recordedAt: Long
    ) {
        val user = userPreferences.currentUser ?: return
        val userDeviceId = userPreferences.currentUserSession?.device_id ?: return
        userPreferences.currentUser?.space_ids?.forEach { spaceId ->
            val (_, senderKeyRecord) = signalKeyHelper.createDistributionKey(
                user = user,
                deviceId = userDeviceId,
                spaceId = spaceId
            )
            val spaceSession = EncryptedSpaceSession(
                currentUser = user,
                keyRecord = senderKeyRecord,
                spaceId = spaceId
            )
            val spaceMembers = spaceMemberRef(spaceId).get().await().toObjects(ApiSpaceMember::class.java)
            val mSenderKeyDistributionModel = ArrayList<SpaceKeyDistribution>().apply {
                spaceMembers.forEach { member ->
                    val memberUser = getUser(member.user_id) ?: return
                    val decryptedSenderKey = getDecryptedSenderKey(
                        spaceId,
                        memberUser,
                        memberUser.public_key!!
                    )
                    add(
                        SpaceKeyDistribution(
                            member.user_id,
                            decryptedSenderKey
                        )
                    )
                }
            }
            spaceSession.createSession(mSenderKeyDistributionModel)
            val encryptedLatitude = spaceSession.encryptMessage(latitude.toString())
            val encryptedLongitude = spaceSession.encryptMessage(longitude.toString())
            val docRef = spaceMemberLocationRef(spaceId, userId).document()

            val encryptedLocation = EncryptedApiLocation(
                id = docRef.id,
                user_id = userId,
                encrypted_latitude = encryptedLatitude,
                encrypted_longitude = encryptedLongitude,
                created_at = recordedAt
            )

            docRef.set(encryptedLocation).await()
        }
    }

    private suspend fun getDecryptedSenderKey(
        spaceId: String,
        recipient: ApiUser,
        senderPublicKey: String
    ): String {
        val space = spaceRef.document(spaceId).get().await().toObject(ApiSpace::class.java)
            ?: throw Exception("Space not found")

        val encryptedKeys = space.encryptedSenderKeys[recipient.id]
            ?: throw Exception("No keys found for recipient")

        return signalKeyHelper.decryptSenderKey(
            encryptedSenderKey = encryptedKeys["encryptedSenderKey"]!!,
            encryptedAESKey = encryptedKeys["encryptedAESKey"]!!,
            recipientPrivateKey = recipient.private_key!!,
            senderPublicKey = senderPublicKey
        )
    }

    fun getCurrentLocation(userId: String): Flow<List<ApiLocation>> {
        return flow {
            try {
                val encryptedLocation = spaceMemberLocationRef(currentSpaceId, userId)
                    .whereEqualTo("user_id", userId)
                    .orderBy("created_at", Query.Direction.DESCENDING)
                    .limit(1)
                    .snapshotFlow(EncryptedApiLocation::class.java)

                encryptedLocation.collect { encryptedLocationList ->
                    val apiLocations = encryptedLocationList.mapNotNull { encryptedLocation ->
                        val user = getUser(encryptedLocation.user_id) ?: return@mapNotNull null
                        val senderPublicKey = user.public_key ?: return@mapNotNull null
                        val space = spaceRef.document(currentSpaceId).get().await().toObject(ApiSpace::class.java)
                            ?: throw Exception("Space not found")

                        val encryptedKeys = space.encryptedSenderKeys[user.id]
                            ?: throw Exception("No keys found for recipient")

                        val decryptedSenderKey = signalKeyHelper.decryptSenderKey(
                            encryptedSenderKey = encryptedKeys["encryptedSenderKey"]!!,
                            encryptedAESKey = encryptedKeys["encryptedAESKey"]!!,
                            recipientPrivateKey = user.private_key!!,
                            senderPublicKey = senderPublicKey
                        )

                        val spaceSession = EncryptedSpaceSession(
                            currentUser = user,
                            keyRecord = decryptedSenderKey,
                            spaceId = currentSpaceId
                        )
                        val decryptedLatitude = spaceSession.decryptMessage(encryptedLocation.encrypted_latitude, user.id)
                        val decryptedLongitude = spaceSession.decryptMessage(encryptedLocation.encrypted_longitude, user.id)

                        ApiLocation(
                            user_id = user.id,
                            latitude = decryptedLatitude.toDouble(),
                            longitude = decryptedLongitude.toDouble()
                        )
                    }

                    emit(apiLocations) // Emit the list of ApiLocation
                }
            } catch (e: Exception) {
                Timber.e(e, "Error while getting current location")
                emit(emptyList()) // Emit an empty list in case of an error
            }
        }
    }
}
