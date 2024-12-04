package com.canopas.yourspace.data.service.location

import com.canopas.yourspace.data.models.location.JourneyRoute
import com.canopas.yourspace.data.models.location.LocationJourney
import com.canopas.yourspace.data.storage.UserPreferences
import com.canopas.yourspace.data.utils.Config
import com.canopas.yourspace.data.utils.Config.FIRESTORE_COLLECTION_SPACES
import com.canopas.yourspace.data.utils.Config.FIRESTORE_COLLECTION_SPACE_MEMBERS
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.toObject
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiJourneyService @Inject constructor(
    db: FirebaseFirestore,
    private val userPreferences: UserPreferences
) {
    var currentSpaceId: String = userPreferences.currentSpace ?: ""

    // App crashes sometimes because of the empty userId string passed to document().
    // java.lang.IllegalArgumentException: Invalid document reference.
    // Document references must have an even number of segments, but users has 1
    // https://stackoverflow.com/a/51195713/22508023 [Explanation can be found in comments]
    private val spaceRef = db.collection(FIRESTORE_COLLECTION_SPACES)
    internal fun spaceMemberRef(spaceId: String) =
        spaceRef.document(spaceId.takeIf { it.isNotBlank() } ?: "null").collection(
            FIRESTORE_COLLECTION_SPACE_MEMBERS
        )

    private fun spaceMemberJourneyRef(spaceId: String) =
        spaceMemberRef(spaceId)
            .document(currentSpaceId.takeIf { it.isNotBlank() } ?: "null")
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
        userPreferences.currentUser?.space_ids?.forEach {
            val docRef = spaceMemberJourneyRef(it).document()

            val journey = LocationJourney(
                id = docRef.id,
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

            newJourneyId?.invoke(journey.id)

            docRef.set(journey).await()
        }
    }

    suspend fun updateLastLocationJourney(userId: String, journey: LocationJourney) {
        try {
            userPreferences.currentUser?.space_ids?.forEach {
                spaceMemberJourneyRef(it).document(journey.id).set(journey).await()
            }
        } catch (e: Exception) {
            Timber.e(e, "Error while updating last location journey")
        }
    }

    suspend fun getLastJourneyLocation(userId: String) = try {
        spaceMemberJourneyRef(currentSpaceId).whereEqualTo("user_id", userId)
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
            spaceMemberJourneyRef(currentSpaceId).whereEqualTo("user_id", userId)
                .orderBy("created_at", Query.Direction.DESCENDING)
                .limit(20)
        } else {
            spaceMemberJourneyRef(currentSpaceId).whereEqualTo("user_id", userId)
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
        val previousDayJourney = spaceMemberJourneyRef(currentSpaceId).whereEqualTo("user_id", userId)
            .whereLessThan("created_at", from)
            .whereGreaterThanOrEqualTo("update_at", from)
            .limit(1)
            .get().await().documents.mapNotNull { it.toObject<LocationJourney>() }

        val currentDayJourney = spaceMemberJourneyRef(currentSpaceId).whereEqualTo("user_id", userId)
            .whereGreaterThanOrEqualTo("created_at", from)
            .whereLessThanOrEqualTo("created_at", to)
            .orderBy("created_at", Query.Direction.DESCENDING)
            .limit(20)
            .get().await().documents.mapNotNull { it.toObject<LocationJourney>() }

        return previousDayJourney + currentDayJourney
    }

    suspend fun getLocationJourneyFromId(journeyId: String): LocationJourney? {
        return spaceMemberJourneyRef(currentSpaceId).document(journeyId).get().await()
            .toObject(LocationJourney::class.java)
    }
}
