package com.canopas.yourspace.ui.flow.journey.timeline

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.canopas.yourspace.data.models.location.LocationJourney
import com.canopas.yourspace.data.models.user.ApiUser
import com.canopas.yourspace.data.service.auth.AuthService
import com.canopas.yourspace.data.service.location.LocationJourneyService
import com.canopas.yourspace.data.service.user.ApiUserService
import com.canopas.yourspace.data.utils.AppDispatcher
import com.canopas.yourspace.ui.navigation.AppDestinations
import com.canopas.yourspace.ui.navigation.AppNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class JourneyTimelineViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val navigator: AppNavigator,
    private val journeyService: LocationJourneyService,
    private val apiUserService: ApiUserService,
    private val authService: AuthService,
    private val appDispatcher: AppDispatcher
) : ViewModel() {

    private var userId: String =
        savedStateHandle.get<String>(AppDestinations.JourneyTimeline.KEY_SELECTED_USER_ID)
            ?: throw IllegalArgumentException("User id is required")

    private val _state = MutableStateFlow(JourneyTimelineState())
    var state = _state.asStateFlow()

    private val allJourneys: List<LocationJourney>
        get() = state.value.groupedLocation.values.flatten()

    init {
        fetchUserLocationHistory()
    }

    private fun fetchUserLocationHistory(
        from: Long? = null,
        to: Long? = null
    ) = viewModelScope.launch(appDispatcher.IO) {
        try {
            val user =
                if (authService.currentUser?.id == userId) {
                    authService.currentUser
                } else {
                    apiUserService.getUser(userId)
                }

            _state.emit(
                _state.value.copy(
                    isCurrentUserTimeline = authService.currentUser?.id == userId,
                    selectedUser = user,
                    selectedTimeFrom = from,
                    selectedTimeTo = to,
                    groupedLocation = if (_state.value.selectedUser != user ||
                        _state.value.selectedTimeTo != to ||
                        _state.value.selectedTimeFrom != from
                    ) {
                        emptyMap()
                    } else {
                        _state.value.groupedLocation
                    }
                )
            )
            loadLocations()
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch user")
            _state.value = _state.value.copy(error = e.localizedMessage, isLoading = false)
        }
    }

    private fun loadLocations() = viewModelScope.launch {
        if (state.value.selectedUser == null) return@launch

        _state.value = _state.value.copy(isLoading = allJourneys.isEmpty(), appending = allJourneys.isNotEmpty())

        try {
            val from =
                if (allJourneys.isEmpty()) _state.value.selectedTimeFrom else allJourneys.minOfOrNull { it.created_at!! }
            val to = _state.value.selectedTimeTo
            val locations = journeyService.getJourneyHistory(
                _state.value.selectedUser?.id ?: "",
                from = from,
                to = to
            )

            val locationJourneys = (allJourneys + locations).groupByDate()
            val hasMoreItems = locations.isNotEmpty()

            _state.value = _state.value.copy(
                groupedLocation = locationJourneys,
                hasMoreLocations = hasMoreItems,
                appending = false,
                isLoading = false
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch location history")
            _state.value = _state.value.copy(error = e.localizedMessage, isLoading = false, appending = false)
        }
    }

    fun addPlace(latitude: Double, longitude: Double) {
        navigator.navigateTo(
            AppDestinations.ChoosePlaceName.setArgs(
                latitude,
                longitude
            ).path
        )
    }

    fun showJourneyDetails(journeyId: String) {
        navigator.navigateTo(
            AppDestinations.UserJourneyDetails.args(
                state.value.selectedUser?.id ?: "",
                journeyId
            ).path
        )
    }

    fun loadMoreLocations() {
        state.value.let {
            if (it.hasMoreLocations && !it.appending) {
                loadLocations()
            }
        }
    }

    private fun List<LocationJourney>.groupByDate(): Map<Long, List<LocationJourney>> {
        val journeys = this.distinctBy { it.id }
            .sortedByDescending { it.created_at!! }

        val groupedItems = mutableMapOf<Long, MutableList<LocationJourney>>()

        for (journey in journeys) {
            val date = getDayStartTimestamp(journey.created_at!!)

            if (!groupedItems.containsKey(date)) {
                groupedItems[date] = mutableListOf()
            }
            groupedItems[date]?.add(journey)
        }

        return groupedItems
    }

    private fun getDayStartTimestamp(timestamp: Long): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    fun navigateBack() {
        navigator.navigateBack()
    }

    fun resetErrorState() {
        _state.value = _state.value.copy(error = null)
    }

    fun showDatePicker() {
    }
}

data class JourneyTimelineState(
    val isCurrentUserTimeline: Boolean = false,
    val selectedUser: ApiUser? = null,
    val selectedTimeFrom: Long? = null,
    val selectedTimeTo: Long? = null,
    val isLoading: Boolean = false,
    val appending: Boolean = false,
    val groupedLocation: Map<Long, List<LocationJourney>> = emptyMap(),
    val hasMoreLocations: Boolean = true,
    val error: String? = null
)
