package com.canopas.catchme.ui.flow.home.map

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.canopas.catchme.data.models.user.UserInfo
import com.canopas.catchme.data.repository.SpaceRepository
import com.canopas.catchme.data.service.location.LocationManager
import com.canopas.catchme.data.storage.UserPreferences
import com.canopas.catchme.data.utils.AppDispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    private val spaceRepository: SpaceRepository,
    private val userPreferences: UserPreferences,
    private val locationManager: LocationManager,
    private val appDispatcher: AppDispatcher
) : ViewModel() {

    private val _state = MutableStateFlow(MapScreenState())
    val state = _state.asStateFlow()

    private var locationJob: Job? = null

    init {
        viewModelScope.launch {
            _state.value = _state.value.copy(currentUserId = userPreferences.currentUser?.id)
            withContext(appDispatcher.IO) {
                _state.emit(_state.value.copy(currentLocation = locationManager.getLastLocation()))
            }
            userPreferences.currentSpaceState.collectLatest {
                locationJob?.cancel()
                listenMemberLocation()
            }
        }
    }

    private fun listenMemberLocation() {
        locationJob = viewModelScope.launch(appDispatcher.IO) {
            spaceRepository.getMemberWithLocation().collectLatest {
                val currentLocation = locationManager.getLastLocation()
                _state.emit(_state.value.copy(members = it, currentLocation = currentLocation))
            }
        }
    }
}

data class MapScreenState(
    val members: List<UserInfo> = emptyList(),
    val currentUserId: String? = "",
    val currentLocation: Location? = null
)
