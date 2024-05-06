package com.canopas.yourspace.data.service.place

import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.api.net.SearchByTextRequest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.tasks.await
import java.util.Arrays
import javax.inject.Inject


class FindPlaceService @Inject constructor(
    private val placesClient: PlacesClient
) {

//    @OptIn(ExperimentalCoroutinesApi::class)
//    suspend fun findPlace(query: String): List<AutocompletePrediction> {
//
//        val cancellationTokenSource = CancellationTokenSource()
//        val newRequest = FindAutocompletePredictionsRequest.builder()
//            .setQuery(query)
//            .setCancellationToken(cancellationTokenSource.token)
//            .build()
//        val response =
//            placesClient.findAutocompletePredictions(newRequest).await(cancellationTokenSource)
//        return response.autocompletePredictions
//    }

    suspend fun findPlace(query: String): List<Place> {
        if(query.trim().isEmpty()) return emptyList()
        val placeFields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG)

//        if (query.trim().isEmpty()) {
//            val request = FindCurrentPlaceRequest.newInstance(placeFields)
//            return placesClient.findCurrentPlace(request).await().placeLikelihoods.map { it.place }
//        }

        val searchByTextRequest = SearchByTextRequest.builder(query, placeFields)
            .setMaxResultCount(20)
            .build()
        val response = placesClient.searchByText(searchByTextRequest).await()

        return response.places

    }
}

