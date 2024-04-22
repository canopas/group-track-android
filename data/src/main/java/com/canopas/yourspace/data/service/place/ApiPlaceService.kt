package com.canopas.yourspace.data.service.place

import com.canopas.yourspace.data.models.place.ApiPlace
import com.canopas.yourspace.data.models.place.ApiPlaceMemberSetting
import com.canopas.yourspace.data.utils.Config
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class ApiPlaceService @Inject constructor(
    private val db: FirebaseFirestore
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
    }

    suspend fun getPlaces(spaceId: String) {}

    suspend fun getPlace(spaceId: String, placeId: String) {}
}
