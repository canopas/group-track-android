package com.canopas.yourspace.ui.flow.journey.timeline

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.canopas.yourspace.data.models.location.LocationJourney
import com.canopas.yourspace.data.models.user.ApiUser
import com.canopas.yourspace.data.service.auth.AuthService
import com.canopas.yourspace.data.service.location.ApiJourneyService
import com.canopas.yourspace.data.service.space.ApiSpaceService
import com.canopas.yourspace.data.service.user.ApiUserService
import com.canopas.yourspace.data.storage.UserPreferences
import com.canopas.yourspace.data.utils.AppDispatcher
import com.canopas.yourspace.domain.utils.ConnectivityObserver
import com.canopas.yourspace.ui.navigation.AppDestinations
import com.canopas.yourspace.ui.navigation.AppNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class JourneyTimelineViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val navigator: AppNavigator,
    private val journeyService: ApiJourneyService,
    private val apiUserService: ApiUserService,
    private val authService: AuthService,
    private val appDispatcher: AppDispatcher,
    private val connectivityObserver: ConnectivityObserver,
    userPreferences: UserPreferences,
    private val spaceRepository: ApiSpaceService
) : ViewModel() {

    private var userId: String =
        savedStateHandle.get<String>(AppDestinations.JourneyTimeline.KEY_SELECTED_USER_ID)
            ?: throw IllegalArgumentException("User id is required")

    private val spaceId: String =
        savedStateHandle.get<String>(AppDestinations.JourneyTimeline.KEY_SELECTED_SPACE_ID)
            ?: throw IllegalArgumentException("Space id is required")

    val _state = MutableStateFlow(
        JourneyTimelineState(
            selectedTimeFrom = getTodayStartTimestamp(),
            selectedTimeTo = getTodayEndTimestamp(),
            weekStartDate = getStartOfWeekTimestamp()
        )
    )
    var state = _state.asStateFlow()

    private val allJourneys: List<LocationJourney>
        get() = state.value.groupedLocation.values.flatten()

    init {
        checkInternetConnection()
        getSpaceInfo()
        fetchUser()
        setSelectedTimeRange()
        loadLocations()
        _state.value = state.value.copy(
            selectedMapStyle = userPreferences.currentMapStyle ?: ""
        )
    }

    private fun setSelectedTimeRange() {
        _state.value = _state.value.copy(
            selectedTimeFrom = getTodayStartTimestamp(),
            selectedTimeTo = getTodayEndTimestamp()
        )
    }

    private fun fetchUser() = viewModelScope.launch(appDispatcher.IO) {
        try {
            val user =
                if (authService.currentUser?.id == userId) {
                    authService.currentUser
                } else {
                    apiUserService.getUser(userId)
                }
            _state.value =
                _state.value.copy(
                    selectedUser = user,
                    isCurrentUserTimeline = authService.currentUser?.id == userId
                )
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch user")
            _state.value = _state.value.copy(error = e.localizedMessage, isLoading = false)
        }
    }

    private fun loadLocations(loadMore: Boolean = false) = viewModelScope.launch(appDispatcher.IO) {
        if (loadMore && !state.value.hasMoreLocations) return@launch
        _state.value = _state.value.copy(
            isLoading = allJourneys.isEmpty(),
            appending = loadMore,
            isLoadingMore = loadMore
        )

        try {
            val from = _state.value.selectedTimeFrom
            val to = _state.value.selectedTimeTo
            val lastJourneyTime = allJourneys.minOfOrNull { it.updated_at }

            val locations = if (loadMore) {
                journeyService.getMoreJourneyHistory(userId, lastJourneyTime)
            } else {
                journeyService.getJourneyHistory(userId, from, to)
            }

            val filteredLocations = locations.filter {
                it.created_at in from..to ||
                    it.updated_at in from..to
            }

            val locationJourneys = (allJourneys + filteredLocations).groupByDate()
            val hasMoreItems = filteredLocations.isNotEmpty()

            _state.value = _state.value.copy(
                groupedLocation = locationJourneys,
                hasMoreLocations = hasMoreItems,
                appending = false,
                isLoading = false
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch location history")
            _state.value =
                _state.value.copy(
                    error = e.localizedMessage,
                    isLoading = false,
                    appending = false
                )
        }
    }

    private fun getSpaceInfo() = viewModelScope.launch(appDispatcher.IO) {
        try {
            val spaceInfo = spaceRepository.getSpace(spaceId)
            if (spaceInfo != null) {
                _state.value = spaceInfo.created_at?.let { _state.value.copy(groupCreationDate = it) }!!
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch space info")
            _state.value = _state.value.copy(error = e.localizedMessage)
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
            if (it.hasMoreLocations && !it.appending && !it.isLoadingMore) {
                _state.value = _state.value.copy(isLoadingMore = true)
                loadLocations(true)
            }
        }
    }

    private fun List<LocationJourney>.groupByDate(): Map<Long, List<LocationJourney>> {
        val journeys = this.distinctBy { it.id }
            .sortedByDescending { it.updated_at }

        val groupedItems = mutableMapOf<Long, MutableList<LocationJourney>>()

        for (journey in journeys) {
            val date = getDayStartTimestamp(journey.created_at)

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

    private fun getTodayStartTimestamp(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        return calendar.timeInMillis
    }

    private fun getTodayEndTimestamp(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        return calendar.timeInMillis
    }

    private fun getStartOfWeekTimestamp(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        return calendar.timeInMillis
    }

    fun navigateBack() {
        navigator.navigateBack()
    }

    fun resetErrorState() {
        _state.value = _state.value.copy(error = null)
    }

    fun showDatePicker() {
        _state.value = _state.value.copy(showDatePicker = true)
    }

    fun setSelectedDate(date: Long?, isPickerDate: Boolean = false) {
        val groupCreationDate = state.value.groupCreationDate
        if (date != null && date < groupCreationDate) {
            _state.update { it.copy(selectedTimeTo = groupCreationDate) }
            return
        }

        _state.update { it.copy(selectedTimeTo = date ?: getTodayStartTimestamp()) }
        if (isPickerDate && date != null) {
            onSelectDateFromDatePicker(date)
        }
    }

    private fun onSelectDateFromDatePicker(date: Long) {
        _state.update { it.copy(weekStartDate = date) }
        setContainsToday()
    }

    private fun setContainsToday() {
        val today = System.currentTimeMillis()
        val endOfWeek = state.value.weekStartDate
        val containsToday = today in state.value.weekStartDate..endOfWeek
        _state.update { it.copy(containsToday = containsToday) }
    }

    fun onFilterByDate(selectedTimeStamp: Long) {
        dismissDatePicker()
        val calendar = Calendar.getInstance().apply {
            timeInMillis = selectedTimeStamp
        }
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        val timestampFrom = calendar.timeInMillis

        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        val timestampTo = calendar.timeInMillis

        _state.value = _state.value.copy(
            selectedTimeFrom = timestampFrom,
            selectedTimeTo = timestampTo,
            groupedLocation = emptyMap()
        )
        loadLocations()
    }

    fun dismissDatePicker() {
        _state.value = _state.value.copy(showDatePicker = false)
    }

    fun checkInternetConnection() {
        viewModelScope.launch(appDispatcher.IO) {
            connectivityObserver.observe().collectLatest { status ->
                _state.emit(
                    _state.value.copy(
                        connectivityStatus = status
                    )
                )
            }
        }
    }
}

data class JourneyTimelineState(
    val isCurrentUserTimeline: Boolean = false,
    val selectedUser: ApiUser? = null,
    val selectedTimeFrom: Long = System.currentTimeMillis(),
    val selectedTimeTo: Long = System.currentTimeMillis(),
    val weekStartDate: Long = System.currentTimeMillis(),
    val containsToday: Boolean = true,
    val isLoading: Boolean = false,
    val appending: Boolean = false,
    val groupedLocation: Map<Long, List<LocationJourney>> = emptyMap(),
    val hasMoreLocations: Boolean = true,
    val showDatePicker: Boolean = false,
    val connectivityStatus: ConnectivityObserver.Status = ConnectivityObserver.Status.Available,
    val error: String? = null,
    val isLoadingMore: Boolean = true,
    val selectedMapStyle: String = "",
    val groupCreationDate: Long = System.currentTimeMillis()
)
