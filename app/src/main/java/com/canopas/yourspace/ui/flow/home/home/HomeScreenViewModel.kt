package com.canopas.yourspace.ui.flow.home.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.canopas.yourspace.data.models.space.SpaceInfo
import com.canopas.yourspace.data.repository.SpaceRepository
import com.canopas.yourspace.data.service.auth.AuthService
import com.canopas.yourspace.data.service.location.LocationManager
import com.canopas.yourspace.data.storage.UserPreferences
import com.canopas.yourspace.data.utils.AppDispatcher
import com.canopas.yourspace.domain.utils.ConnectivityObserver
import com.canopas.yourspace.ui.navigation.AppDestinations
import com.canopas.yourspace.ui.navigation.AppNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class HomeScreenViewModel @Inject constructor(
    private val navigator: AppNavigator,
    private val locationManager: LocationManager,
    private val spaceRepository: SpaceRepository,
    private val userPreferences: UserPreferences,
    private val authService: AuthService,
    private val appDispatcher: AppDispatcher,
    private val connectivityObserver: ConnectivityObserver
) : ViewModel() {

    private val _state = MutableStateFlow(HomeScreenState())
    val state: StateFlow<HomeScreenState> = _state

    init {
        observeConnectivity()
        if (userPreferences.currentUser != null) {
            updateUser()
            getAllSpaces()
            listenCurrentSpaceChanges()
        }
    }

    private fun listenCurrentSpaceChanges() = viewModelScope.launch(appDispatcher.IO) {
        userPreferences.currentSpaceState.collectLatest { currentSpaceState ->
            if (state.value.selectedSpaceId != currentSpaceState) {
                val selectedSpace =
                    state.value.spaces.firstOrNull { it.space.id == currentSpaceState }
                _state.value = _state.value.copy(
                    selectedSpaceId = currentSpaceState,
                    selectedSpace = selectedSpace
                )
            }
        }
    }

    private fun updateUser() = viewModelScope.launch(appDispatcher.IO) {
        val user = authService.getUser()
        if (user == null) {
            authService.signOut()
            navigator.navigateTo(
                AppDestinations.signIn.path,
                clearStack = true
            )
        } else {
            authService.saveUser(user)
        }
    }

    fun onTabChange(index: Int) {
        _state.value = _state.value.copy(currentTab = index)
    }

    fun startTracking() {
        locationManager.startService()
    }

    private fun getAllSpaces() = viewModelScope.launch(appDispatcher.IO) {
        try {
            _state.emit(_state.value.copy(isLoadingSpaces = _state.value.spaces.isEmpty()))
            spaceRepository.getAllSpaceInfo().collectLatest { spaces ->
                if (spaceRepository.currentSpaceId.isEmpty() && spaces.isNotEmpty()) {
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
            _state.emit(_state.value.copy(error = e, isLoadingSpaces = false))
        }
    }

    fun toggleSpaceSelection() {
        _state.value =
            _state.value.copy(showSpaceSelectionPopup = !state.value.showSpaceSelectionPopup)
    }

    fun dismissSpaceSelection() {
        _state.value = _state.value.copy(showSpaceSelectionPopup = false)
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
            _state.emit(_state.value.copy(error = e, isLoadingSpaces = false))
        }
    }

    fun joinSpace() {
        navigator.navigateTo(AppDestinations.joinSpace.path)
    }

    fun toggleLocation() = viewModelScope.launch(appDispatcher.IO) {
        try {
            val userId = userPreferences.currentUser?.id ?: return@launch
            val spaceId = spaceRepository.currentSpaceId
            if (spaceId.isEmpty()) return@launch
            _state.emit(_state.value.copy(enablingLocation = true))
            val locationEnabled = !_state.value.locationEnabled

            spaceRepository.enableLocation(spaceId, userId, locationEnabled)
            _state.emit(
                _state.value.copy(
                    enablingLocation = false,
                    locationEnabled = locationEnabled
                )
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to get invite code")
            _state.emit(_state.value.copy(error = e))
        }
    }

    fun navigateToSettings() {
        navigator.navigateTo(AppDestinations.settings.path)
    }

    fun navigateToThreads() {
        if (spaceRepository.currentSpaceId.isEmpty()) return
        navigator.navigateTo(AppDestinations.spaceThreads.path)
    }

    fun showBatteryOptimizationDialog() = viewModelScope.launch(appDispatcher.IO) {
        delay(500)

        val lastDate = userPreferences.getLastBatteryDialogDate()
        val currentTime = System.currentTimeMillis()

        val shouldShowDialog = if (lastDate.isNullOrEmpty()) {
            true
        } else {
            currentTime - lastDate.toLong() >= TimeUnit.DAYS.toMillis(1)
        }

        if (shouldShowDialog) {
            _state.value = _state.value.copy(showBatteryOptimizationPopup = true)
            userPreferences.setLastBatteryDialogDate(currentTime.toString())
        }
    }

    fun observeConnectivity() {
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

    fun dismissBatteryOptimizationDialog() {
        _state.value = _state.value.copy(showBatteryOptimizationPopup = false)
    }
}

data class HomeScreenState(
    val currentTab: Int = 0,
    val spaces: List<SpaceInfo> = emptyList(),
    val selectedSpaceId: String? = "",
    val selectedSpace: SpaceInfo? = null,
    val isLoadingSpaces: Boolean = false,
    val showSpaceSelectionPopup: Boolean = false,
    val locationEnabled: Boolean = true,
    val enablingLocation: Boolean = false,
    val showBatteryOptimizationPopup: Boolean = false,
    val error: Exception? = null,
    val connectivityStatus: ConnectivityObserver.Status = ConnectivityObserver.Status.Available
)
