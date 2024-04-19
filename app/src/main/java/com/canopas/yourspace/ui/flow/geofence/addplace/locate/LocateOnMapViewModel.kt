package com.canopas.yourspace.ui.flow.geofence.addplace.locate

import android.location.Location
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.canopas.yourspace.data.service.location.LocationManager
import com.canopas.yourspace.data.utils.AppDispatcher
import com.canopas.yourspace.ui.navigation.AppNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class LocateOnMapViewModel @Inject constructor(
    private val appNavigator: AppNavigator,
    private val locationManager: LocationManager,
    private val appDispatcher: AppDispatcher
) : ViewModel() {

    private val _state = MutableStateFlow(LocateOnMapState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch(appDispatcher.IO) {
            _state.emit(_state.value.copy(defaultLocation = locationManager.getLastLocation()))
            Timber.d("XXX: defaultLocation: ${locationManager.getLastLocation()}")
        }
    }

    fun popBackStack() {
        appNavigator.navigateBack()
    }

    fun onNextClick(latitude: Double, longitude: Double) {
        Timber.d("XXX: selectedLocation: latitude $latitude, longitude $longitude")
    }
}

data class LocateOnMapState(
    val defaultLocation: Location? = null
)
