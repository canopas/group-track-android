package com.canopas.yourspace.data.repository

import com.canopas.yourspace.data.service.auth.AuthService
import com.canopas.yourspace.data.service.place.ApiPlaceService
import com.canopas.yourspace.data.service.place.GeoFenceService
import com.canopas.yourspace.data.storage.UserPreferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeofenceRepository @Inject constructor(
    private val apiPlaceService: ApiPlaceService,
    private val spaceRepository: SpaceRepository,
    private val geoFenceService: GeoFenceService,
    private val userPreferences: UserPreferences,
    private val authService: AuthService
) {
    private var selectedSpace: String? = ""
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var placeJob: Job? = null

    fun init() {
        Timber.d("XXX GeofenceRepository init")
        listenForSpaceChange()
    }

    private fun listenForSpaceChange() {
        scope.launch {
            userPreferences.currentSpaceState
                .filter { it.isNotEmpty() }
                .collectLatest {
                    Timber.d("XXX Space changed to $it")
                    if (selectedSpace != it) {
                        selectedSpace = it
                        geoFenceService.deregisterGeofence()
                        listenForPlaces()
                    }
                }
        }
    }

    private fun listenForPlaces() {
        placeJob?.cancel()
        placeJob = scope.launch {
            val currentSpaceId = spaceRepository.currentSpaceId
            val currentUser = authService.currentUser ?: return@launch
            if (currentSpaceId.isEmpty()) return@launch

            apiPlaceService.listenAllPlaces(currentSpaceId)
                .collectLatest { places ->
                    Timber.d("XXX Places changed: ${places.size}")
                    geoFenceService.deregisterGeofence()
                    geoFenceService.addGeofence(places, currentUser.id)
                }
        }
    }

    suspend fun registerAllPlaces() {
        try {
            val currentUser = authService.currentUser ?: return
            val currentSpaceId = spaceRepository.getMemberBySpaceId()
            if (currentSpaceId.isEmpty()) return
            val places = apiPlaceService.getPlaces(currentSpaceId)
            geoFenceService.addGeofence(places, currentUser.id)

        } catch (e: Exception) {
            Timber.e(e, "Error while registering all places")
        }
    }
}
