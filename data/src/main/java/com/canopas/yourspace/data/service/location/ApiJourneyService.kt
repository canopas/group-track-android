package com.canopas.yourspace.data.service.location

import com.canopas.yourspace.data.models.location.EncryptedJourneyRoute
import com.canopas.yourspace.data.models.location.EncryptedLocationJourney
import com.canopas.yourspace.data.models.location.JourneyRoute
import com.canopas.yourspace.data.models.location.LocationJourney
import com.canopas.yourspace.data.models.space.ApiSpace
import com.canopas.yourspace.data.models.space.ApiSpaceMember
import com.canopas.yourspace.data.models.user.ApiUser
import com.canopas.yourspace.data.security.helper.SignalKeyHelper
import com.canopas.yourspace.data.security.session.EncryptedSpaceSession
import com.canopas.yourspace.data.security.session.SpaceKeyDistribution
import com.canopas.yourspace.data.service.user.ApiUserService
import com.canopas.yourspace.data.storage.UserPreferences
import com.canopas.yourspace.data.utils.Config
import com.canopas.yourspace.data.utils.Config.FIRESTORE_COLLECTION_SPACES
import com.canopas.yourspace.data.utils.Config.FIRESTORE_COLLECTION_SPACE_MEMBERS
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiJourneyService @Inject constructor(
    db: FirebaseFirestore,
    private val userPreferences: UserPreferences,
    private val signalKeyHelper: SignalKeyHelper,
    private val apiUserService: ApiUserService
) {
    var currentSpaceId: String = userPreferences.currentSpace ?: ""

    // App crashes sometimes because of the empty userId string passed to document().
    // java.lang.IllegalArgumentException: Invalid document reference.
    // Document references must have an even number of segments, but users has 1
    // https://stackoverflow.com/a/51195713/22508023 [Explanation can be found in comments]
    private val spaceRef = db.collection(FIRESTORE_COLLECTION_SPACES)
    private fun spaceMemberRef(spaceId: String) =
        spaceRef.document(spaceId.takeIf { it.isNotBlank() } ?: "null").collection(
            FIRESTORE_COLLECTION_SPACE_MEMBERS
        )

    private fun spaceMemberJourneyRef(spaceId: String, userId: String) =
        spaceMemberRef(spaceId)
            .document(userId.takeIf { it.isNotBlank() } ?: "null")
            .collection(Config.FIRESTORE_COLLECTION_USER_JOURNEYS)

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
                    val memberUser = apiUserService.getUser(member.user_id) ?: return
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
            val encryptedFromLatitude = spaceSession.encryptMessage(fromLatitude.toString())
            val encryptedFromLongitude = spaceSession.encryptMessage(fromLongitude.toString())
            val encryptedToLatitude = toLatitude?.let { spaceSession.encryptMessage(it.toString()) }
            val encryptedToLongitude = toLongitude?.let { spaceSession.encryptMessage(it.toString()) }
            val encryptedJourneyRoutes = routes.map {
                EncryptedJourneyRoute(
                    encrypted_latitude = spaceSession.encryptMessage(it.latitude.toString()),
                    encrypted_longitude = spaceSession.encryptMessage(it.longitude.toString())
                )
            }

            val docRef = spaceMemberJourneyRef(spaceId, userId).document()

            val encryptedJourney = EncryptedLocationJourney(
                id = docRef.id,
                user_id = userId,
                encrypted_from_latitude = encryptedFromLatitude,
                encrypted_from_longitude = encryptedFromLongitude,
                encrypted_to_latitude = encryptedToLatitude,
                encrypted_to_longitude = encryptedToLongitude,
                route_distance = routeDistance,
                route_duration = routeDuration,
                encrypted_routes = encryptedJourneyRoutes,
                created_at = createdAt ?: System.currentTimeMillis(),
                updated_at = updateAt ?: System.currentTimeMillis()
            )

            newJourneyId?.invoke(encryptedJourney.id)

            docRef.set(encryptedJourney).await()
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

    suspend fun updateLastLocationJourney(userId: String, journey: LocationJourney) {
        try {
            userPreferences.currentUser?.space_ids?.forEach {
                spaceMemberJourneyRef(it, userId).document(journey.id).set(journey).await()
            }
        } catch (e: Exception) {
            Timber.e(e, "Error while updating last location journey")
        }
    }

    suspend fun getLastJourneyLocation(userId: String) = try {
        spaceMemberJourneyRef(currentSpaceId, userId).whereEqualTo("user_id", userId)
            .orderBy("created_at", Query.Direction.DESCENDING).limit(1)
            .get().await().documents.firstOrNull()?.toObject<LocationJourney>()
    } catch (e: Exception) {
        Timber.e(e, "Error while getting last location journey")
        null
    }

    suspend fun getMoreJourneyHistory(
        userId: String,
        from: Long?
    ): List<LocationJourney> {
        val query = if (from == null) {
            spaceMemberJourneyRef(currentSpaceId, userId).whereEqualTo("user_id", userId)
                .orderBy("created_at", Query.Direction.DESCENDING)
                .limit(20)
        } else {
            spaceMemberJourneyRef(currentSpaceId, userId).whereEqualTo("user_id", userId)
                .orderBy("created_at", Query.Direction.DESCENDING)
                .whereLessThan("created_at", from)
                .limit(20)
        }
        return query.get().await().documents.mapNotNull { it.toObject<LocationJourney>() }
    }

    suspend fun getJourneyHistory(
        userId: String,
        from: Long,
        to: Long
    ): List<LocationJourney> {
        val previousDayJourney = spaceMemberJourneyRef(currentSpaceId, userId).whereEqualTo("user_id", userId)
            .whereLessThan("created_at", from)
            .whereGreaterThanOrEqualTo("update_at", from)
            .limit(1)
            .get().await().documents.mapNotNull { it.toObject<LocationJourney>() }

        val currentDayJourney = spaceMemberJourneyRef(currentSpaceId, userId).whereEqualTo("user_id", userId)
            .whereGreaterThanOrEqualTo("created_at", from)
            .whereLessThanOrEqualTo("created_at", to)
            .orderBy("created_at", Query.Direction.DESCENDING)
            .limit(20)
            .get().await().documents.mapNotNull { it.toObject<LocationJourney>() }

        return previousDayJourney + currentDayJourney
    }

    suspend fun getLocationJourneyFromId(journeyId: String, userId: String): LocationJourney? {
        return spaceMemberJourneyRef(currentSpaceId, userId = userId).document(journeyId).get().await()
            .toObject(LocationJourney::class.java)
    }
}
