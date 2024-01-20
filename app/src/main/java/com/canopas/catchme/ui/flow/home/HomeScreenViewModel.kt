package com.canopas.catchme.ui.flow.home

import androidx.lifecycle.ViewModel
import com.canopas.catchme.data.service.location.LocationManager
import com.canopas.catchme.ui.navigation.HomeNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class HomeScreenViewModel @Inject constructor(
    private val navigator: HomeNavigator,
    private val locationManager: LocationManager
) : ViewModel() {

    val navActions = navigator.navigationChannel

    private val _state = MutableStateFlow(HomeScreenState())
    val state: StateFlow<HomeScreenState> = _state

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
