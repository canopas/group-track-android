package com.canopas.yourspace.ui.flow.journey.member

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.canopas.yourspace.data.models.location.LocationJourney
import com.canopas.yourspace.data.models.user.UserInfo
import com.canopas.yourspace.data.service.location.LocationJourneyService
import com.canopas.yourspace.data.utils.AppDispatcher
import com.canopas.yourspace.ui.navigation.AppDestinations
import com.canopas.yourspace.ui.navigation.AppNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MemberDetailViewModel @Inject constructor(
    private val journeyService: LocationJourneyService,
    private val appDispatcher: AppDispatcher,
    private val navigator: AppNavigator
) : ViewModel() {

    private val _state = MutableStateFlow(MemberDetailState())
    var state = _state.asStateFlow()

    fun fetchUserLocationHistory(
        from: Long,
        to: Long,
        userInfo: UserInfo? = _state.value.selectedUser
    ) = viewModelScope.launch(appDispatcher.IO) {
        _state.emit(
            _state.value.copy(
                selectedUser = userInfo,
                selectedTimeFrom = from,
                selectedTimeTo = to,
                locations = if (_state.value.selectedUser != userInfo) listOf() else _state.value.locations
            )
        )

        loadLocations()
    }

    private fun loadLocations() = viewModelScope.launch {
        if (state.value.selectedUser == null) return@launch

        _state.value = _state.value.copy(isLoading = state.value.locations.isEmpty())

        try {
            val locations = journeyService.getJourneyHistory(
                _state.value.selectedUser?.user?.id ?: "",
                _state.value.selectedTimeFrom ?: 0,
                _state.value.selectedTimeTo ?: 0
            ).sortedByDescending { it.update_at }

            val locationJourneys = (state.value.locations + locations).distinctBy { it.id }
            val hasMoreItems =
                !state.value.locations.map { it.id }.containsAll(locations.map { it.id })

            _state.value = _state.value.copy(
                locations = locationJourneys,
                hasMoreLocations = hasMoreItems,
                isLoading = false
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch location history")
            _state.value = _state.value.copy(error = e, isLoading = false)
        }
    }

    fun addPlace(latitud: Double, longitud: Double) {
        navigator.navigateTo(
            AppDestinations.ChoosePlaceName.setArgs(
                latitud,
                longitud
            ).path
        )
    }

    fun showJourneyDetails(journeyId: String) {
        navigator.navigateTo(
            AppDestinations.UserJourneyDetails.args(
                state.value.selectedUser?.user?.id ?: "",
                journeyId
            ).path
        )
    }

    fun showJourneyTimeline() {
        navigator.navigateTo(
            AppDestinations.JourneyTimeline.args(
                state.value.selectedUser?.user?.id ?: ""
            ).path
        )
    }
}

data class MemberDetailState(
    val selectedUser: UserInfo? = null,
    val selectedTimeFrom: Long? = null,
    val selectedTimeTo: Long? = null,
    val locations: List<LocationJourney> = listOf(),
    val hasMoreLocations: Boolean = true,
    val isLoading: Boolean = false,
    val error: Exception? = null
)
