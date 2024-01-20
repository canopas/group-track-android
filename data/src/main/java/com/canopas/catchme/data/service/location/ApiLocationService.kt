package com.canopas.catchme.data.service.location

import com.canopas.catchme.data.models.location.ApiLocation
import com.canopas.catchme.data.utils.FirestoreConst
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiLocationService @Inject constructor(
    private val db: FirebaseFirestore
) {
    private val locationRef = db.collection(FirestoreConst.FIRESTORE_COLLECTION_USER_LOCATIONS)

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
}
