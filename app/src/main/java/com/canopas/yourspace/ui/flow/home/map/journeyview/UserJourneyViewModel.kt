package com.canopas.yourspace.ui.flow.home.map.journeyview

import android.location.Location
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.canopas.yourspace.data.models.location.ApiLocation
import com.canopas.yourspace.data.models.location.LocationJourney
import com.canopas.yourspace.data.models.location.isSteadyLocation
import com.canopas.yourspace.data.models.user.ApiUser
import com.canopas.yourspace.data.service.location.ApiLocationService
import com.canopas.yourspace.data.service.location.LocationJourneyService
import com.canopas.yourspace.data.service.location.LocationManager
import com.canopas.yourspace.data.service.user.ApiUserService
import com.canopas.yourspace.data.utils.AppDispatcher
import com.canopas.yourspace.ui.navigation.AppDestinations.UserJourney.KEY_JOURNEY_ID
import com.canopas.yourspace.ui.navigation.AppDestinations.UserJourney.KEY_SELECTED_USER_ID
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class UserJourneyViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val journeyService: LocationJourneyService,
    private val apiLocationService: ApiLocationService,
    private val appDispatcher: AppDispatcher,
    private val locationManager: LocationManager,
    private val apiUserService: ApiUserService
) : ViewModel() {

    private var journeyId: String? = savedStateHandle.get<String>(KEY_JOURNEY_ID)
    private var userId: String? = savedStateHandle.get<String>(KEY_SELECTED_USER_ID)
    private val _state = MutableStateFlow(UserJourneyState())
    val state = _state.asStateFlow()

    init {

        viewModelScope.launch(appDispatcher.IO) {
            val user = apiUserService.getUser(userId!!)
            val currentLocation = locationManager.getLastLocation()
            _state.value = _state.value.copy(
                journeyId = journeyId,
                user = user,
                currentLocation = currentLocation
            )

            Timber.d("XXX UserJourneyViewModel init ${_state.value.journeyId} isNullOrEmpty ${_state.value.journeyId.isNullOrEmpty()}")
        }
    }

    private fun fetchJourney() = viewModelScope.launch(appDispatcher.IO) {
        if (userId == null) {
            return@launch
        }
        if (journeyId.isNullOrEmpty()) {
            Timber.d("XXX fetchJourneyList")
            fetchJourneyList(userId!!)
        } else {
            fetchJourneyFromId(userId!!)
        }
    }

    private fun fetchJourneyFromId(userId: String) = viewModelScope.launch(appDispatcher.IO) {
        try {
            _state.value = _state.value.copy(isLoading = true)
            val journey = journeyService.getLocationJourneyFromId(userId, journeyId!!)
            if (journey == null) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Journey not found"
                )
                return@launch
            }
            val createdTime = journey.created_at!!
            val selectedTimeFrom = createdTime - createdTime % TimeUnit.DAYS.toMillis(1)
            val selectedTimeTo = selectedTimeFrom + TimeUnit.DAYS.toMillis(1) - 1

            val apiLocations = if (journey.isSteadyLocation().not()) {
                apiLocationService.getLocationsBetweenTime(
                    userId,
                    createdTime,
                    createdTime + (journey.route_duration ?: 0)
                ) ?: emptyList()
            } else {
                emptyList()
            }
            _state.value = _state.value.copy(
                cameraTarget = LatLng(journey.from_latitude, journey.from_longitude),
                journeyWithLocation = JourneyWithLocations(journey, apiLocations),
                isLoading = false,
                selectedTimeFrom = selectedTimeFrom,
                selectedTimeTo = selectedTimeTo
            )
        } catch (e: Exception) {
            _state.value = _state.value.copy(
                isLoading = false,
                error = e.message
            )
        }
    }

    private fun fetchJourneyList(userId: String) = viewModelScope.launch(appDispatcher.IO) {
        val journeys = journeyService.getJourneyHistory(
            userId,
            _state.value.selectedTimeFrom,
            _state.value.selectedTimeTo
        )
        val filteredJourneys = journeys.sortedBy {
            it.from_latitude.toInt()
        }.filterIndexed { index, it ->
            val distance = FloatArray(1)
            val previousJourney = journeys.getOrNull(index - 1)
            Location.distanceBetween(
                it.from_latitude,
                it.from_longitude,
                previousJourney?.from_latitude ?: it.from_latitude,
                previousJourney?.from_longitude ?: it.from_longitude,
                distance
            )
            distance[0] > 1000
        }.sortedBy {
            it.created_at
        }

        val journeyWithLocations = filteredJourneys.map {
            JourneyWithLocations(
                journey = it,
                locationsList = if (!it.isSteadyLocation()) {
                    apiLocationService.getLocationsBetweenTime(
                        userId,
                        it.created_at!!,
                        it.created_at!! + (it.route_duration ?: 0)
                    ) ?: emptyList()
                } else {
                    emptyList()
                }
            )
        }

        Timber.d("XXX fetchJourneyList ${journeyWithLocations.size}")
        _state.value = _state.value.copy(
            cameraTarget = journeyWithLocations.firstOrNull()?.let {
                LatLng(it.journey.from_latitude, it.journey.from_longitude)
            },
            journeyWithLocations = journeyWithLocations,
            isLoading = false
        )
    }

    fun onDateSelected(selectedTimeTo: Long, selectedTimeFrom: Long) {
        _state.value = _state.value.copy(
            selectedTimeFrom = selectedTimeFrom,
            selectedTimeTo = selectedTimeTo
        )
        fetchJourney()
    }
}

data class UserJourneyState(
    val isLoading: Boolean = false,
    val user: ApiUser? = null,
    val selectedTimeFrom: Long = System.currentTimeMillis(),
    val selectedTimeTo: Long = System.currentTimeMillis(),
    val currentLocation: Location? = null,
    val journeyId: String? = null,
    val journeyWithLocation: JourneyWithLocations? = null,
    val journeyWithLocations: List<JourneyWithLocations> = emptyList(),
    val cameraTarget: LatLng? = null,
    val error: String? = null
)

data class JourneyWithLocations(
    val journey: LocationJourney,
    val locationsList: List<ApiLocation> = emptyList()
)
