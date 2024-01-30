package com.canopas.catchme.ui.flow.home.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.canopas.catchme.data.models.space.SpaceInfo
import com.canopas.catchme.data.repository.SpaceRepository
import com.canopas.catchme.data.service.location.LocationManager
import com.canopas.catchme.data.storage.UserPreferences
import com.canopas.catchme.data.utils.AppDispatcher
import com.canopas.catchme.ui.navigation.AppDestinations
import com.canopas.catchme.ui.navigation.HomeNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class HomeScreenViewModel @Inject constructor(
    private val navigator: HomeNavigator,
    private val locationManager: LocationManager,
    private val spaceRepository: SpaceRepository,
    private val userPreferences: UserPreferences,
    private val appDispatcher: AppDispatcher
) : ViewModel() {

    val navActions = navigator.navigationChannel

    private val _state = MutableStateFlow(HomeScreenState())
    val state: StateFlow<HomeScreenState> = _state

    init {
        getAllSpaces()
    }

    fun onTabChange(index: Int) {
        _state.value = _state.value.copy(currentTab = index)
    }

    fun shouldAskForBackgroundLocationPermission(ask: Boolean) {
        _state.value = _state.value.copy(shouldAskForBackgroundLocationPermission = ask)
    }

    fun startTracking() {
        shouldAskForBackgroundLocationPermission(false)
        locationManager.startService()
    }

    private fun getAllSpaces() = viewModelScope.launch(appDispatcher.IO) {
        try {
            _state.emit(_state.value.copy(isLoadingSpaces = _state.value.spaces.isEmpty()))
            spaceRepository.getAllSpaceInfo().collectLatest { spaces ->
                if (spaceRepository.currentSpaceId.isEmpty()) {
                    spaceRepository.currentSpaceId = spaces.firstOrNull()?.space?.id ?: ""
                }
                val tempSpaces = spaces.toMutableList()
                val index =
                    tempSpaces.indexOfFirst { it.space.id == spaceRepository.currentSpaceId }

                if (index != -1) {
                    tempSpaces.add(0, tempSpaces.removeAt(index))
                }
                val selectedSpace =
                    spaces.firstOrNull { it.space.id == spaceRepository.currentSpaceId }
                val locationEnabled =
                    selectedSpace?.members?.firstOrNull { it.user.id == userPreferences.currentUser?.id }?.isLocationEnable
                        ?: true

                _state.emit(
                    _state.value.copy(
                        selectedSpace = selectedSpace,
                        locationEnabled = locationEnabled,
                        spaces = tempSpaces,
                        isLoadingSpaces = false,
                        selectedSpaceId = spaceRepository.currentSpaceId
                    )
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to get all spaces")
            _state.emit(_state.value.copy(error = e.message, isLoadingSpaces = false))
        }
    }

    fun toggleSpaceSelection() {
        _state.value =
            _state.value.copy(showSpaceSelectionPopup = !state.value.showSpaceSelectionPopup)
    }

    fun navigateToCreateSpace() {
        navigator.navigateTo(AppDestinations.createSpace.path)
    }

    fun selectSpace(spaceId: String) = viewModelScope.launch(appDispatcher.IO) {
        spaceRepository.currentSpaceId = spaceId
        val space = _state.value.spaces.firstOrNull { it.space.id == spaceId }
        val locationEnabled =
            space?.members?.firstOrNull { it.user.id == userPreferences.currentUser?.id }?.isLocationEnable
                ?: true
        _state.value =
            _state.value.copy(
                selectedSpaceId = spaceId,
                selectedSpace = space,
                locationEnabled = locationEnabled,
                showSpaceSelectionPopup = false
            )
    }

    fun addMember() = viewModelScope.launch(appDispatcher.IO) {
        try {
            val space = _state.value.selectedSpace?.space ?: return@launch
            _state.emit(_state.value.copy(isLoadingSpaces = true))
            val inviteCode = spaceRepository.getInviteCode(space.id) ?: return@launch
            _state.emit(_state.value.copy(isLoadingSpaces = false))
            navigator.navigateTo(
                AppDestinations.SpaceInvitation.spaceInvitation(inviteCode, space.name).path,
                AppDestinations.createSpace.path,
                inclusive = true
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to get invite code")
            _state.emit(_state.value.copy(error = e.message, isLoadingSpaces = false))
        }
    }

    fun joinSpace() {
        navigator.navigateTo(AppDestinations.joinSpace.path)
    }

    fun toggleLocation() = viewModelScope.launch(appDispatcher.IO) {
        try {
            _state.emit(_state.value.copy(enablingLocation = true))
            val locationEnabled = !_state.value.locationEnabled
            val spaceId = _state.value.selectedSpace?.space?.id ?: return@launch
            val userId = userPreferences.currentUser?.id ?: return@launch
            spaceRepository.enableLocation(spaceId, userId, locationEnabled)
            _state.emit(
                _state.value.copy(
                    enablingLocation = false,
                    locationEnabled = locationEnabled
                )
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to get invite code")
            _state.emit(_state.value.copy(error = e.message))
        }
    }
}

data class HomeScreenState(
    val currentTab: Int = 0,
    val shouldAskForBackgroundLocationPermission: Boolean = false,
    val spaces: List<SpaceInfo> = emptyList(),
    val selectedSpaceId: String = "",
    val selectedSpace: SpaceInfo? = null,
    val isLoadingSpaces: Boolean = false,
    val showSpaceSelectionPopup: Boolean = false,
    val locationEnabled: Boolean = true,
    val enablingLocation: Boolean = false,
    val error: String? = null
)
