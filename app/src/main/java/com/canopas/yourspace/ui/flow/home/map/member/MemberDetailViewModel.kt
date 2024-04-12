package com.canopas.yourspace.ui.flow.home.map.member

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.canopas.yourspace.data.models.location.LocationJourney
import com.canopas.yourspace.data.models.user.UserInfo
import com.canopas.yourspace.data.service.location.LocationJourneyService
import com.canopas.yourspace.data.utils.AppDispatcher
import com.google.firebase.firestore.Query
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MemberDetailViewModel @Inject constructor(
    private val journeyService: LocationJourneyService,
    private val appDispatcher: AppDispatcher
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
                locations = listOf()
            )
        )
        loadLocations()
    }

    private fun loadLocations() = viewModelScope.launch {
        if (state.value.selectedUser == null) return@launch

        _state.value = _state.value.copy(isLoading = true)

        try {
            val query = getQuery()
            val querySnapshot = query.get().await()
            val locations = querySnapshot.documents.mapNotNull { it.toObject(LocationJourney::class.java) }

            val locationJourneys = (state.value.locations + locations).distinctBy { it.id }
            val hasMoreItems = !state.value.locations.map { it.id }.containsAll(locations.map { it.id })

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

    private fun getQuery(): Query {
        return journeyService.getJourneyHistoryQuery(
            _state.value.selectedUser?.user?.id ?: "",
            _state.value.selectedTimeFrom ?: 0,
            _state.value.locations.lastOrNull()?.created_at ?: _state.value.selectedTimeTo
                ?: 0
        )
    }

    fun loadMoreLocations() {
        state.value.let {
            if (it.hasMoreLocations && !it.isLoading) {
                loadLocations()
            }
        }
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
