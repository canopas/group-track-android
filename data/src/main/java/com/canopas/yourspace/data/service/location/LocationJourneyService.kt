package com.canopas.yourspace.data.service.location

import com.canopas.yourspace.data.models.location.LocationJourney
import com.canopas.yourspace.data.utils.Config
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationJourneyService @Inject constructor(
    db: FirebaseFirestore,
    private val locationManager: LocationManager
) {
    private val userRef = db.collection(Config.FIRESTORE_COLLECTION_USERS)
    private fun journeyRef(userId: String) =
        userRef.document(userId.replace('/', '_')).collection(Config.FIRESTORE_COLLECTION_USER_JOURNEYS)

    suspend fun saveLastKnownJourney(
        userId: String
    ) {
        val lastLocation = locationManager.getLastLocation() ?: return
        val docRef = journeyRef(userId).document()

        val journey = LocationJourney(
            id = docRef.id,
            user_id = userId,
            from_latitude = lastLocation.latitude,
            from_longitude = lastLocation.longitude,
            current_location_duration = System.currentTimeMillis() - lastLocation.time,
            created_at = Date().time
        )

        docRef.set(journey).await()
    }

    suspend fun saveCurrentJourney(
        userId: String,
        fromLatitude: Double,
        fromLongitude: Double,
        toLatitude: Double? = null,
        toLongitude: Double? = null,
        routeDistance: Double? = null,
        routeDuration: Long? = null,
        currentLocationDuration: Long? = null,
        recordedAt: Long
    ) {
        val docRef = journeyRef(userId).document()

        val journey = LocationJourney(
            id = docRef.id,
            user_id = userId,
            from_latitude = fromLatitude,
            from_longitude = fromLongitude,
            to_latitude = toLatitude,
            to_longitude = toLongitude,
            route_distance = routeDistance,
            route_duration = routeDuration,
            current_location_duration = currentLocationDuration,
            created_at = recordedAt
        )

        docRef.set(journey).await()
    }

    suspend fun getLastSteadyLocation(userId: String): LocationJourney? {
        return try {
            val journey = journeyRef(userId).whereEqualTo("user_id", userId)
                .whereEqualTo("to_latitude", null)
                .orderBy("created_at", Query.Direction.DESCENDING).limit(1)
                .get().await().documents.firstOrNull()?.toObject(LocationJourney::class.java)
            journey
        } catch (e: Exception) {
            Timber.e(e, "Error while getting last steady location")
            null
        }
    }

    suspend fun getLastMovingLocation(userId: String): LocationJourney? {
        return try {
            val journey = journeyRef(userId).whereEqualTo("user_id", userId)
                .whereNotEqualTo("to_latitude", null)
                .orderBy("created_at", Query.Direction.DESCENDING).limit(1)
                .get().await().documents.firstOrNull()?.toObject(LocationJourney::class.java)
            journey
        } catch (e: Exception) {
            Timber.e(e, "Error while getting last moving location")
            null
        }
    }

    suspend fun getLastJourneyLocation(userId: String) = try {
        journeyRef(userId).whereEqualTo("user_id", userId)
            .orderBy("created_at", Query.Direction.DESCENDING).limit(1)
            .get().await().documents.firstOrNull()?.toObject(LocationJourney::class.java)
    } catch (e: Exception) {
        Timber.e(e, "Error while getting last location journey")
        null
    }

    fun getJourneyHistoryQuery(userId: String, from: Long, to: Long) =
        journeyRef(userId).whereEqualTo("user_id", userId)
            .whereGreaterThanOrEqualTo("created_at", from)
            .whereLessThan("created_at", to)
            .orderBy("created_at", Query.Direction.DESCENDING).limit(10)

    fun updateLastMovingLocation(userId: String, newJourney: LocationJourney) {
        journeyRef(userId).document(newJourney.id).set(newJourney)
    }
}
