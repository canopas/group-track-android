package com.canopas.yourspace.data.service.place

import com.canopas.yourspace.data.models.place.ApiPlace
import com.canopas.yourspace.data.models.place.ApiPlaceMemberSetting
import com.canopas.yourspace.data.utils.Config
import com.canopas.yourspace.data.utils.snapshotFlow
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class ApiPlaceService @Inject constructor(
    private val db: FirebaseFirestore,
    private val functions: FirebaseFunctions
) {

    private val spaceRef = db.collection(Config.FIRESTORE_COLLECTION_SPACES)

    private fun spacePlacesRef(spaceId: String) =
        spaceRef.document(spaceId).collection(Config.FIRESTORE_COLLECTION_SPACE_PLACES)

    private fun spacePlacesSettingsRef(spaceId: String, placeId: String) =
        spaceRef.document(spaceId).collection(Config.FIRESTORE_COLLECTION_SPACE_PLACES)
            .document(placeId).collection(
                Config.FIRESTORE_COLLECTION_SPACE_PLACES_MEMBER_SETTINGS
            )

    suspend fun addPlace(
        spaceId: String,
        createdBy: String,
        name: String,
        latitude: Double,
        longitude: Double,
        spaceMemberIds: List<String>
    ) {
        val placeDoc = spacePlacesRef(spaceId).document()
        val place = ApiPlace(
            id = placeDoc.id,
            created_by = createdBy,
            space_id = spaceId,
            name = name,
            latitude = latitude,
            longitude = longitude
        )

        placeDoc.set(place).await()
        val settings = spaceMemberIds.map { memberId ->
            val filterIds = spaceMemberIds.filter { it != memberId }
            ApiPlaceMemberSetting(
                place_id = place.id,
                user_id = memberId,
                arrival_alert_for = filterIds,
                leave_alert_for = filterIds
            )
        }

        settings.forEach { setting ->
            spacePlacesSettingsRef(spaceId, place.id).add(setting)
                .await()
        }

        val data = mapOf(
            "spaceId" to spaceId,
            "placeName" to name,
            "createdBy" to createdBy,
            "spaceMemberIds" to spaceMemberIds
        )
        functions.getHttpsCallable("sendNewPlaceNotification").call(data).await()
    }

    suspend fun getPlaces(spaceId: String): List<ApiPlace> {
        val places = spacePlacesRef(spaceId).get().await()
        return places.toObjects(ApiPlace::class.java).sortedByDescending { it.created_at }
    }

    fun listenAllPlaces(spaceId: String): Flow<List<ApiPlace>> {
        if (spaceId.isEmpty()) return emptyFlow()
        return spacePlacesRef(spaceId).snapshotFlow(ApiPlace::class.java)
    }

    suspend fun deletePlace(currentSpaceId: String, id: String) {
        spacePlacesRef(currentSpaceId).document(id).delete().await()
    }

    suspend fun getPlace(placeId: String): ApiPlace?{
        return db.collectionGroup(Config.FIRESTORE_COLLECTION_SPACE_PLACES)
            .whereEqualTo("id", placeId).limit(1)
            .snapshotFlow(ApiPlace::class.java).first().firstOrNull()
    }
}
