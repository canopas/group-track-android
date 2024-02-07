package com.canopas.catchme.data.service.location

import com.canopas.catchme.data.models.location.ApiLocation
import com.canopas.catchme.data.utils.FirestoreConst
import com.canopas.catchme.data.utils.snapshotFlow
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiLocationService @Inject constructor(
    private val db: FirebaseFirestore,
    private val locationManager: LocationManager
) {
    private val locationRef = db.collection(FirestoreConst.FIRESTORE_COLLECTION_USER_LOCATIONS)

    suspend fun saveLastKnownLocation(
        userId: String,
    ) {
        val lastLocation = locationManager.getLastLocation() ?: return
        val docRef = locationRef.document()

        val location = ApiLocation(
            id = docRef.id,
            user_id = userId,
            latitude = lastLocation.latitude,
            longitude = lastLocation.longitude,
            created_at = System.currentTimeMillis()
        )

        locationRef.document(location.id).set(location).await()
    }

    suspend fun saveCurrentLocation(
        userId: String,
        latitude: Double,
        longitude: Double,
        recordedAt: Long
    ) {
        val docRef = locationRef.document()

        val location = ApiLocation(
            id = docRef.id,
            user_id = userId,
            latitude = latitude,
            longitude = longitude,
            created_at = recordedAt
        )

        locationRef.document(location.id).set(location).await()
    }

    suspend fun getCurrentLocation(userId: String) =
        locationRef.whereEqualTo("user_id", userId)
            .orderBy("created_at", Query.Direction.DESCENDING).limit(1)
            .snapshotFlow(ApiLocation::class.java)

    fun getLocationHistoryQuery(userId: String, from: Long, to: Long) =
        locationRef.whereEqualTo("user_id", userId)
            .whereGreaterThanOrEqualTo("created_at", from)
            .whereLessThan("created_at", to)
            .orderBy("created_at", Query.Direction.DESCENDING).limit(8)

    suspend fun deleteLocations(userId: String) {
        locationRef.whereEqualTo("user_id", userId).get().await().documents.forEach {
            it.reference.delete().await()
        }
    }
}
