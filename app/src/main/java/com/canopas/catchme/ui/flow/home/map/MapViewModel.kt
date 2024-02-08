package com.canopas.catchme.ui.flow.home.map

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.canopas.catchme.data.models.user.UserInfo
import com.canopas.catchme.data.repository.SpaceRepository
import com.canopas.catchme.data.service.location.LocationManager
import com.canopas.catchme.data.storage.UserPreferences
import com.canopas.catchme.data.utils.AppDispatcher
import com.canopas.catchme.ui.navigation.AppDestinations
import com.canopas.catchme.ui.navigation.AppNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    private val spaceRepository: SpaceRepository,
    private val userPreferences: UserPreferences,
    private val locationManager: LocationManager,
    private val appDispatcher: AppDispatcher,
    private val navigator: AppNavigator
) : ViewModel() {

    private val _state = MutableStateFlow(MapScreenState())
    val state = _state.asStateFlow()

    private var locationJob: Job? = null

    init {
        viewModelScope.launch {
            _state.value = _state.value.copy(currentUserId = userPreferences.currentUser?.id)
            withContext(appDispatcher.IO) {
                _state.emit(_state.value.copy(defaultCameraPosition = locationManager.getLastLocation()))
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
                _state.emit(
                    _state.value.copy(
                        members = it,
                        defaultCameraPosition = currentLocation
                    )
                )
            }
        }
    }

    fun showMemberDetail(userInfo: UserInfo) = viewModelScope.launch(appDispatcher.IO) {
        val selectedUser = _state.value.selectedUser
        if (selectedUser != null && selectedUser.user.id == userInfo.user.id) {
            dismissMemberDetail()
        } else {
            _state.emit(_state.value.copy(selectedUser = userInfo, showUserDetails = true))
        }
    }

    fun dismissMemberDetail() {
        _state.value = _state.value.copy(showUserDetails = false, selectedUser = null)
    }

    fun addMember() = viewModelScope.launch(appDispatcher.IO) {
        try {
            _state.emit(_state.value.copy(loadingInviteCode = true))
            val space = spaceRepository.getCurrentSpace() ?: return@launch
            val inviteCode = spaceRepository.getInviteCode(space.id) ?: return@launch
            _state.emit(_state.value.copy(loadingInviteCode = false))
            navigator.navigateTo(
                AppDestinations.SpaceInvitation.spaceInvitation(inviteCode, space.name).path,
                AppDestinations.createSpace.path,
                inclusive = true
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to get invite code")
            _state.emit(_state.value.copy(error = e.message, loadingInviteCode = false))
        }
    }

    fun navigateToPermissionScreen() {
        navigator.navigateTo(AppDestinations.enablePermissions.path)
    }

    fun startLocationTracking() {
        viewModelScope.launch(appDispatcher.IO) {
            val currentLocation = locationManager.getLastLocation()
            _state.emit(_state.value.copy(defaultCameraPosition = currentLocation))
            locationManager.startService()
        }
    }
}

data class MapScreenState(
    val members: List<UserInfo> = emptyList(),
    val currentUserId: String? = "",
    val defaultCameraPosition: Location? = null,
    val selectedUser: UserInfo? = null,
    val showUserDetails: Boolean = false,
    val loadingInviteCode: Boolean = false,
    val error: String? = null
)
