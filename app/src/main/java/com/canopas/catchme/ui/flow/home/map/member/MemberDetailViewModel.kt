package com.canopas.catchme.ui.flow.home.map.member

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.canopas.catchme.data.models.location.ApiLocation
import com.canopas.catchme.data.models.user.UserInfo
import com.canopas.catchme.data.service.location.ApiLocationService
import com.canopas.catchme.data.utils.AppDispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MemberDetailViewModel @Inject constructor(
    private val locationService: ApiLocationService,
    private val appDispatcher: AppDispatcher
) : ViewModel() {

    private val _state = MutableStateFlow(MemberDetailState())
    var state = _state.asStateFlow()

    fun fetchUserLocationHistory(userInfo: UserInfo, from: Long, to: Long) {
        _state.value =
            _state.value.copy(selectedUser = userInfo, selectedTimeFrom = from, selectedTimeTo = to)
        fetchLocationHistory(from, to)
    }

    fun fetchLocationHistory(from: Long, to: Long) = viewModelScope.launch(appDispatcher.IO) {
        _state.emit(_state.value.copy(selectedTimeFrom = from, selectedTimeTo = to))

        try {
            _state.emit(_state.value.copy(isLoading = true))
            locationService.getLocationHistory(
                _state.value.selectedUser?.user?.id ?: "",
                from, to
            ).collectLatest {
                val locations = it
                    .distinctBy { it.latitude }
                    .distinctBy { it.longitude }.sortedByDescending { it.created_at }

                _state.emit(_state.value.copy(isLoading = false, location = locations))
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch location history")
            _state.emit(_state.value.copy(isLoading = false, error = e.message))

        }
    }
}

data class MemberDetailState(
    val selectedUser: UserInfo? = null,
    val selectedTimeFrom: Long? = null,
    val selectedTimeTo: Long? = null,
    val location: List<ApiLocation> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)