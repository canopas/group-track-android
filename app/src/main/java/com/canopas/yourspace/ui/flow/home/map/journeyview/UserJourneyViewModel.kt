package com.canopas.yourspace.ui.flow.home.map.journeyview

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.canopas.yourspace.data.models.location.LocationJourney
import com.canopas.yourspace.data.models.user.ApiUser
import com.canopas.yourspace.data.service.location.LocationJourneyService
import com.canopas.yourspace.data.service.user.ApiUserService
import com.canopas.yourspace.data.utils.AppDispatcher
import com.canopas.yourspace.ui.navigation.AppDestinations.UserJourney.KEY_JOURNEY_ID
import com.canopas.yourspace.ui.navigation.AppDestinations.UserJourney.KEY_SELECTED_USER_ID
import com.canopas.yourspace.ui.navigation.AppNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class UserJourneyViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val navigator: AppNavigator,
    private val journeyService: LocationJourneyService,
    private val appDispatcher: AppDispatcher,
    private val apiUserService: ApiUserService
) : ViewModel() {

    private var journeyId: String? = savedStateHandle[KEY_JOURNEY_ID]
    private var userId: String? = savedStateHandle[KEY_SELECTED_USER_ID]
    private val _state = MutableStateFlow(UserJourneyState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch(appDispatcher.IO) {
            val user = apiUserService.getUser(userId!!)
            _state.value = _state.value.copy(journeyId = journeyId, user = user)
        }
    }

    private fun fetchJourney() = viewModelScope.launch(appDispatcher.IO) {
        if (userId == null) {
            return@launch
        }
        _state.value = _state.value.copy(isLoading = true)
        journeyId?.let {
            fetchJourneyFromId(userId!!)
        } ?: fetchJourneyList(userId!!)
    }

    private fun fetchJourneyFromId(userId: String) = viewModelScope.launch(appDispatcher.IO) {
        journeyService.getLocationJourneyFromId(userId, journeyId!!)?.let {
            val calendar = Calendar.getInstance().apply {
                timeInMillis = it.created_at!!
            }
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            val startTimeStamp = calendar.timeInMillis
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            _state.value = _state.value.copy(
                journey = it,
                isLoading = false,
                selectedTimeFrom = startTimeStamp,
                selectedTimeTo = calendar.timeInMillis
            )
        }
    }

    private fun fetchJourneyList(userId: String) = viewModelScope.launch(appDispatcher.IO) {
        val journeyList = journeyService.getJourneyHistory(
            userId,
            _state.value.selectedTimeFrom,
            _state.value.selectedTimeTo
        )
        _state.value = _state.value.copy(
            journeyList = journeyList,
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
    val journeyId: String? = null,
    val journey: LocationJourney? = null,
    val journeyList: List<LocationJourney> = emptyList()
)
