package com.canopas.catchme.ui.flow.home.home

import androidx.lifecycle.ViewModel
import com.canopas.catchme.data.repository.SpaceRepository
import com.canopas.catchme.data.service.location.LocationManager
import com.canopas.catchme.data.utils.AppDispatcher
import com.canopas.catchme.ui.navigation.HomeNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class HomeScreenViewModel @Inject constructor(
    private val navigator: HomeNavigator,
    private val locationManager: LocationManager,
    private val spaceRepository: SpaceRepository,
    appDispatcher: AppDispatcher
) : ViewModel() {

    val navActions = navigator.navigationChannel

    private val _state = MutableStateFlow(HomeScreenState())
    val state: StateFlow<HomeScreenState> = _state

    init {
        // viewModelScope.launch(appDispatcher.IO) {  spaceRepository.listenMemberWithLocation() }
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
}

data class HomeScreenState(
    val currentTab: Int = 0,
    val shouldAskForBackgroundLocationPermission: Boolean = false
)
