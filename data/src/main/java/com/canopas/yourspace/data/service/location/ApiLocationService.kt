package com.canopas.yourspace.data.service.location

import android.util.Log
import com.canopas.yourspace.data.models.location.ApiLocation
import com.canopas.yourspace.data.storage.UserPreferences
import com.canopas.yourspace.data.utils.Config
import com.canopas.yourspace.data.utils.Config.FIRESTORE_COLLECTION_SPACES
import com.canopas.yourspace.data.utils.Config.FIRESTORE_COLLECTION_SPACE_MEMBERS
import com.canopas.yourspace.data.utils.snapshotFlow
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiLocationService @Inject constructor(
    db: FirebaseFirestore,
    private val locationManager: LocationManager,
    private val userPreferences: UserPreferences
) {
    var currentSpaceId: String = userPreferences.currentSpace ?: ""

    private val spaceRef = db.collection(FIRESTORE_COLLECTION_SPACES)
    private fun spaceMemberRef(spaceId: String) =
        spaceRef.document(spaceId.takeIf { it.isNotBlank() } ?: "null").collection(
            FIRESTORE_COLLECTION_SPACE_MEMBERS
        )
    private fun spaceMemberLocationRef(spaceId: String) =
        spaceMemberRef(spaceId)
            .document(currentSpaceId.takeIf { it.isNotBlank() } ?: "null")
            .collection(Config.FIRESTORE_COLLECTION_USER_LOCATIONS)

    suspend fun saveLastKnownLocation(
        userId: String
    ) {
        val lastLocation = locationManager.getLastLocation() ?: return
        userPreferences.currentUser?.space_ids?.forEach {
            val docRef = spaceMemberLocationRef(it).document()

            val location = ApiLocation(
                id = docRef.id,
                user_id = userId,
                latitude = lastLocation.latitude,
                longitude = lastLocation.longitude,
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
        Log.e("XXXXXX", "SpaceId: $currentSpaceId")
        userPreferences.currentUser?.space_ids?.forEach {
            val docRef = spaceMemberLocationRef(it).document()

            val location = ApiLocation(
                id = docRef.id,
                user_id = userId,
                latitude = latitude,
                longitude = longitude,
                created_at = recordedAt
            )

            docRef.set(location).await()
        }
    }

    fun getCurrentLocation(userId: String): Flow<List<ApiLocation>>? {
        return try {
            spaceMemberLocationRef(currentSpaceId).whereEqualTo("user_id", userId)
                .orderBy("created_at", Query.Direction.DESCENDING).limit(1)
                .snapshotFlow(ApiLocation::class.java)
        } catch (e: Exception) {
            Timber.e(e, "Error while getting current location")
            null
        }
    }
}
