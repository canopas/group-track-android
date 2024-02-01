package com.canopas.catchme.ui.flow.home.map.member

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.canopas.catchme.data.models.location.ApiLocation
import com.canopas.catchme.data.models.user.UserInfo
import com.canopas.catchme.data.service.location.ApiLocationService
import com.canopas.catchme.data.utils.AppDispatcher
import com.canopas.catchme.ui.flow.home.map.distanceTo
import com.google.android.gms.maps.model.LatLng
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

    fun fetchUserLocationHistory(userInfo: UserInfo, timestamp: Long) {
        _state.value = _state.value.copy(selectedUser = userInfo, selectedTime = timestamp)
        fetchLocationHistory(timestamp)
    }

    fun fetchLocationHistory(timestamp: Long) = viewModelScope.launch(appDispatcher.IO) {
        _state.emit(_state.value.copy(selectedTime = timestamp))

        try {
            _state.emit(_state.value.copy(isLoading = true))
            locationService.getLocationHistory(
                _state.value.selectedUser?.user?.id ?: "",
                timestamp
            ).collectLatest {
                val locations = it
                    .distinctBy { it.latitude }
                    .distinctBy { it.longitude }.sortedByDescending { it.created_at }

                Timber.e("XXX fetchLocationHistory ${locations.size}")
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
    val selectedTime: Long? = null,
    val location: List<ApiLocation> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)