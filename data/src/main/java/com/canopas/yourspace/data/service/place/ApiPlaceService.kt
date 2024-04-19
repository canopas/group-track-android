package com.canopas.yourspace.data.service.place

import com.canopas.yourspace.data.utils.Config
import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject

class ApiPlaceService @Inject constructor(
    private val db: FirebaseFirestore
) {

    private val spaceRef = db.collection(Config.FIRESTORE_COLLECTION_SPACES)

    private fun spacePlacesRef(spaceId: String) =
        spaceRef.document(spaceId).collection(Config.FIRESTORE_COLLECTION_SPACE_PLACES)

    suspend fun addPlace(spaceId: String, placeId: String) {}

    suspend fun getPlaces(spaceId: String) {}

    suspend fun getPlace(spaceId: String, placeId: String) {}
}
