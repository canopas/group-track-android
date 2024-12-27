package com.canopas.yourspace.ui.flow.geofence.add.locate

import android.location.Location
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.canopas.yourspace.data.repository.SpaceRepository
import com.canopas.yourspace.data.service.location.LocationManager
import com.canopas.yourspace.data.service.place.ApiPlaceService
import com.canopas.yourspace.data.storage.UserPreferences
import com.canopas.yourspace.data.utils.AppDispatcher
import com.canopas.yourspace.domain.utils.ConnectivityObserver
import com.canopas.yourspace.ui.flow.geofence.places.EXTRA_RESULT_PLACE_LATITUDE
import com.canopas.yourspace.ui.flow.geofence.places.EXTRA_RESULT_PLACE_LONGITUDE
import com.canopas.yourspace.ui.flow.geofence.places.EXTRA_RESULT_PLACE_NAME
import com.canopas.yourspace.ui.navigation.AppDestinations
import com.canopas.yourspace.ui.navigation.AppNavigator
import com.canopas.yourspace.ui.navigation.KEY_RESULT
import com.canopas.yourspace.ui.navigation.RESULT_OKAY
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class LocateOnMapViewModel @Inject constructor(
    val savedStateHandle: SavedStateHandle,
    private val appNavigator: AppNavigator,
    private val locationManager: LocationManager,
    private val appDispatcher: AppDispatcher,
    private val apiPlaceService: ApiPlaceService,
    private val spaceRepository: SpaceRepository,
    private val userPreferences: UserPreferences,
    private val connectivityObserver: ConnectivityObserver
) : ViewModel() {

    private val placeName =
        savedStateHandle.get<String>(AppDestinations.LocateOnMap.KEY_SELECTED_NAME) ?: ""

    private val _state = MutableStateFlow(
        LocateOnMapState(
            selectedPlaceName = placeName,
            updatedPlaceName = placeName
        )
    )
    val state = _state.asStateFlow()

    init {
        checkInternetConnection()
        viewModelScope.launch(appDispatcher.IO) {
            _state.emit(_state.value.copy(defaultLocation = locationManager.getLastLocation()))
        }
    }

    fun popBackStack() {
        appNavigator.navigateBack()
    }

    fun onNextClick(latitude: Double, longitude: Double) {
        if (placeName.isEmpty()) {
            appNavigator.navigateTo(
                AppDestinations.ChoosePlaceName.setArgs(
                    latitude,
                    longitude
                ).path
            )
        } else {
            addPlace(latitude, longitude)
        }
    }

    private fun addPlace(latitude: Double, longitude: Double) =
        viewModelScope.launch(appDispatcher.IO) {
            val currentSpaceId = userPreferences.currentSpace ?: return@launch
            val currentUser = userPreferences.currentUser ?: return@launch

            if (latitude == 0.0 || longitude == 0.0) {
                _state.value = _state.value.copy(
                    error = Exception("Invalid location.")
                )
                return@launch
            }

            _state.emit(state.value.copy(addingPlace = true))
            try {
                val memberIds =
                    spaceRepository.getMemberBySpaceId(currentSpaceId)?.map { it.user_id }
                        ?: emptyList()
                apiPlaceService.addPlace(
                    spaceId = currentSpaceId,
                    name = state.value.updatedPlaceName,
                    latitude = latitude,
                    longitude = longitude,
                    createdBy = currentUser.id,
                    spaceMemberIds = memberIds
                )
                navigateBack(latitude, longitude, state.value.updatedPlaceName)
                _state.emit(state.value.copy(addingPlace = false))
            } catch (e: Exception) {
                Timber.e(e, "Error while adding place")
                _state.emit(state.value.copy(error = e, addingPlace = false))
            }
        }

    fun onPlaceNameChanged(name: String) {
        _state.value = state.value.copy(updatedPlaceName = name)
    }

    fun navigateBack(latitude: Double, longitude: Double, placeName: String) {
        appNavigator.navigateBack(
            AppDestinations.places.path,
            result = mapOf(
                KEY_RESULT to RESULT_OKAY,
                EXTRA_RESULT_PLACE_LATITUDE to latitude,
                EXTRA_RESULT_PLACE_LONGITUDE to longitude,
                EXTRA_RESULT_PLACE_NAME to placeName
            )
        )
    }

    fun onMapLoaded() {
        viewModelScope.launch(appDispatcher.IO) {
            _state.emit(state.value.copy(isMapLoaded = true))
        }
    }

    fun checkInternetConnection() {
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

    fun resetErrorState() {
        _state.value = _state.value.copy(error = null)
    }
}

data class LocateOnMapState(
    val updatedPlaceName: String = "",
    val selectedPlaceName: String? = "",
    val defaultLocation: Location? = null,
    val addingPlace: Boolean = false,
    val isMapLoaded: Boolean = false,
    val error: Exception? = null,
    val connectivityStatus: ConnectivityObserver.Status = ConnectivityObserver.Status.Available
)
