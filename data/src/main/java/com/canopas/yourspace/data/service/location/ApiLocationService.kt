package com.canopas.yourspace.data.service.location

import com.canopas.yourspace.data.models.location.ApiLocation
import com.canopas.yourspace.data.models.location.LocationTable
import com.canopas.yourspace.data.models.location.UserState
import com.canopas.yourspace.data.storage.room.LocationTableDatabase
import com.canopas.yourspace.data.utils.Config
import com.canopas.yourspace.data.utils.LocationConverters
import com.canopas.yourspace.data.utils.snapshotFlow
import com.google.firebase.firestore.CollectionReference
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
    private val locationTableDatabase: LocationTableDatabase,
    private val converters: LocationConverters
) {
    private val userRef = db.collection(Config.FIRESTORE_COLLECTION_USERS)
    private fun locationRef(userId: String): CollectionReference? {
        return try {
            userRef.document(userId).collection(Config.FIRESTORE_COLLECTION_USER_LOCATIONS)
        } catch (e: Exception) {
            Timber.e(e, "Error while getting location reference")
            null
        }
    }

    suspend fun saveLastKnownLocation(
        userId: String
    ) {
        val lastLocation = locationManager.getLastLocation() ?: return
        val docRef = locationRef(userId)?.document()

        docRef ?: return

        val location = ApiLocation(
            id = docRef.id,
            user_id = userId,
            latitude = lastLocation.latitude,
            longitude = lastLocation.longitude,
            created_at = System.currentTimeMillis(),
            user_state = UserState.STEADY.value
        )

        setLocationTableData(userId, location)

        docRef.set(location).await()
    }

    suspend fun saveCurrentLocation(
        userId: String,
        latitude: Double,
        longitude: Double,
        recordedAt: Long,
        userState: Int?
    ) {
        val docRef = locationRef(userId)?.document()
        docRef ?: return

        val location = ApiLocation(
            id = docRef.id,
            user_id = userId,
            latitude = latitude,
            longitude = longitude,
            created_at = recordedAt,
            user_state = userState
        )

        setLocationTableData(userId, location)

        docRef.set(location).await()
    }

    private suspend fun setLocationTableData(
        userId: String,
        location: ApiLocation
    ) {
        locationTableDatabase.locationTableDao().getLocationData(userId).let { locationTable ->
            locationTable?.latestLocation?.let {
                locationTableDatabase.locationTableDao().updateLocationTable(
                    locationTable.copy(
                        latestLocation = converters.locationToString(location)
                    )
                )
            } ?: run {
                locationTableDatabase.locationTableDao().insertLocationData(
                    LocationTable(
                        userId = userId,
                        latestLocation = converters.locationToString(location)
                    )
                )
            }
        }
    }

    suspend fun getCurrentLocation(userId: String): Flow<List<ApiLocation>>? {
        return try {
            locationRef(userId)?.whereEqualTo("user_id", userId)
                ?.orderBy("created_at", Query.Direction.DESCENDING)?.limit(1)
                ?.snapshotFlow(ApiLocation::class.java)
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

                val reference = locationRef(userId) ?: continue
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

    suspend fun getLastLocation(userId: String): ApiLocation? {
        return try {
            locationRef(userId)?.whereEqualTo("user_id", userId)
                ?.orderBy("created_at", Query.Direction.DESCENDING)
                ?.limit(1)
                ?.get()?.await()?.documents?.firstOrNull()?.toObject(ApiLocation::class.java)
        } catch (e: Exception) {
            Timber.e(e, "Error while getting last location")
            null
        }
    }

    suspend fun getLocationsBetweenTime(
        userId: String,
        from: Long,
        to: Long
    ): List<ApiLocation>? {
        try {
            val minutesInMilliseconds = 60000
            val minutesThreshold5 = 5
            val minutesThreshold60 = 60
            val minutesThreshold120 = 120
            val minutesThreshold180 = 180

            val minutes = (to - from) / minutesInMilliseconds
            val locations = mutableListOf<ApiLocation>()

            val reference = locationRef(userId) ?: return null

            val count = when (minutes) {
                in 0..minutesThreshold5 -> 0
                in minutesThreshold5..minutesThreshold60 -> 5
                in minutesThreshold60..minutesThreshold120 -> 10
                in minutesThreshold120..minutesThreshold180 -> 15
                else -> 20
            }

            if (count == 0) {
                val apiLocation = reference
                    .whereEqualTo("user_id", userId)
                    .whereGreaterThanOrEqualTo("created_at", from)
                    .whereLessThan("created_at", to)
                    .orderBy("created_at", Query.Direction.DESCENDING).limit(10)
                    .get().await().documents.mapNotNull {
                        it.toObject(ApiLocation::class.java)
                    }
                locations.addAll(apiLocation)
                return locations
            }
            for (i in 0 until count) {
                val startTime = to - (i + 1) * (minutes / count) * minutesInMilliseconds
                val endTime = to - i * (minutes / count) * minutesInMilliseconds

                val apiLocation = reference
                    .whereEqualTo("user_id", userId)
                    .whereGreaterThanOrEqualTo("created_at", startTime)
                    .whereLessThan("created_at", endTime)
                    .orderBy("created_at", Query.Direction.DESCENDING).limit(1)
                    .get().await().documents
                    .randomOrNull()?.toObject(ApiLocation::class.java)

                apiLocation?.let {
                    locations.add(it)
                }
            }

            return locations
        } catch (e: Exception) {
            Timber.e(e, "Error while getting locations between time")
            return null
        }
    }
}
