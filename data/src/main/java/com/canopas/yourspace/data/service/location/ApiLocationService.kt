package com.canopas.yourspace.data.service.location

import com.canopas.yourspace.data.models.location.ApiLocation
import com.canopas.yourspace.data.utils.Config
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
    private val locationManager: LocationManager
) {
    private val userRef = db.collection(Config.FIRESTORE_COLLECTION_USERS)
    private fun locationRef(userId: String) =
        userRef.document(userId.takeIf { it.isNotBlank() } ?: "null")
            .collection(Config.FIRESTORE_COLLECTION_USER_LOCATIONS)

    suspend fun saveLastKnownLocation(
        userId: String
    ) {
        val lastLocation = locationManager.getLastLocation() ?: return
        val docRef = locationRef(userId).document()

        val location = ApiLocation(
            id = docRef.id,
            user_id = userId,
            latitude = lastLocation.latitude,
            longitude = lastLocation.longitude,
            created_at = System.currentTimeMillis()
        )

        docRef.set(location).await()
    }

    suspend fun saveCurrentLocation(
        userId: String,
        latitude: Double,
        longitude: Double,
        recordedAt: Long
    ) {
        val docRef = locationRef(userId).document()

        val location = ApiLocation(
            id = docRef.id,
            user_id = userId,
            latitude = latitude,
            longitude = longitude,
            created_at = recordedAt
        )

        docRef.set(location).await()
    }

    fun getCurrentLocation(userId: String): Flow<List<ApiLocation>>? {
        return try {
            locationRef(userId).whereEqualTo("user_id", userId)
                .orderBy("created_at", Query.Direction.DESCENDING).limit(1)
                .snapshotFlow(ApiLocation::class.java)
        } catch (e: Exception) {
            Timber.e(e, "Error while getting current location")
            null
        }
    }

    suspend fun getLastFiveMinuteLocations(userId: String): Flow<List<ApiLocation>> {
        val currentTime = System.currentTimeMillis()
        val locations = mutableListOf<ApiLocation>()

        for (i in 0 until 5) {
            try {
                val startTime = currentTime - (i + 1) * 60000
                val endTime = startTime - 60000
                val reference = locationRef(userId)
                val apiLocation = reference
                    .whereEqualTo("user_id", userId)
                    .whereGreaterThanOrEqualTo("created_at", endTime)
                    .whereLessThan("created_at", startTime)
                    .orderBy("created_at", Query.Direction.DESCENDING).limit(1)
                    .get().await().documents
                    .firstOrNull()?.toObject(ApiLocation::class.java)

                apiLocation?.let {
                    locations.add(it)
                }
            } catch (e: Exception) {
                Timber.e(e, "Error while getting last $i minute locations")
            }
        }

        return flow {
            emit(locations)
        }
    }
}
