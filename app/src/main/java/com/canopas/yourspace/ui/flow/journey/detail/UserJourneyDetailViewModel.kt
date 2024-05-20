package com.canopas.yourspace.ui.flow.journey.detail

import android.location.Location
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.canopas.yourspace.data.models.location.LocationJourney
import com.canopas.yourspace.data.models.user.ApiUser
import com.canopas.yourspace.data.service.location.LocationJourneyService
import com.canopas.yourspace.data.service.location.LocationManager
import com.canopas.yourspace.data.service.user.ApiUserService
import com.canopas.yourspace.data.utils.AppDispatcher
import com.canopas.yourspace.ui.navigation.AppDestinations.UserJourneyDetails.KEY_JOURNEY_ID
import com.canopas.yourspace.ui.navigation.AppDestinations.UserJourneyDetails.KEY_SELECTED_USER_ID
import com.canopas.yourspace.ui.navigation.AppNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class UserJourneyDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val journeyService: LocationJourneyService,
    private val appDispatcher: AppDispatcher,
    private val locationManager: LocationManager,
    private val apiUserService: ApiUserService,
    private val navigator: AppNavigator
) : ViewModel() {

    private var journeyId: String = savedStateHandle.get<String>(KEY_JOURNEY_ID)
        ?: throw IllegalArgumentException("Journey id is required")
    private var userId: String = savedStateHandle.get<String>(KEY_SELECTED_USER_ID)
        ?: throw IllegalArgumentException("User id is required")

    private val _state = MutableStateFlow(UserJourneyDetailState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch(appDispatcher.IO) {
            val user = apiUserService.getUser(userId)
            val currentLocation = locationManager.getLastLocation()
            _state.value = _state.value.copy(
                journeyId = journeyId,
                user = user,
                currentLocation = currentLocation
            )
        }
        fetchJourney()
    }

    private fun fetchJourney() = viewModelScope.launch(appDispatcher.IO) {
        try {
            _state.value = _state.value.copy(isLoading = true)
            val journey = journeyService.getLocationJourneyFromId(userId, journeyId)
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

            _state.value = state.value.copy(
                journey = journey,
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

    fun onDateSelected(selectedTimeTo: Long, selectedTimeFrom: Long) {
        _state.value = _state.value.copy(
            selectedTimeFrom = selectedTimeFrom,
            selectedTimeTo = selectedTimeTo
        )
        fetchJourney()
    }

    fun navigateBack() {
        navigator.navigateBack()
    }
}

data class UserJourneyDetailState(
    val isLoading: Boolean = false,
    val user: ApiUser? = null,
    val selectedTimeFrom: Long = System.currentTimeMillis(),
    val selectedTimeTo: Long = System.currentTimeMillis(),
    val currentLocation: Location? = null,
    val journeyId: String? = null,
    val journey: LocationJourney? = null,
    val error: String? = null
)
