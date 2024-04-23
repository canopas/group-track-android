package com.canopas.yourspace.ui.flow.geofence.places

import androidx.lifecycle.ViewModel
import com.canopas.yourspace.data.models.place.ApiPlace
import com.canopas.yourspace.ui.navigation.AppDestinations
import com.canopas.yourspace.ui.navigation.AppNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class PlacesListViewModel @Inject constructor(
    private val appNavigator: AppNavigator
) : ViewModel() {

    private val _state = MutableStateFlow(PlacesListScreenState())
    val state = _state.asStateFlow()

    fun navigateBack() {
        appNavigator.navigateBack()
    }

    fun navigateToAddPlace() {
        appNavigator.navigateTo(AppDestinations.LocateOnMap.setArgs("").path)
    }

    fun addPlace(lat: Double, long: Double, name: String) {
        if (name.isEmpty() || lat == 0.0 || long == 0.0) return
        _state.value = state.value.copy(
            placeAdded = true,
            addedPlaceLat = lat,
            addedPlaceLng = long,
            addedPlaceName = name
        )
    }

    fun dismissPlaceAddedPopup() {
        _state.value = state.value.copy(placeAdded = false, addedPlaceLat = 0.0, addedPlaceLng = 0.0, addedPlaceName = "")
    }

    fun selectedSuggestion(name: String) {
        appNavigator.navigateTo(AppDestinations.LocateOnMap.setArgs(name).path)
    }
}

data class PlacesListScreenState(
    val placeAdded: Boolean = false,
    val addedPlaceLat: Double = 0.0,
    val addedPlaceLng: Double = 0.0,
    val addedPlaceName: String = "",

    val placesLoading: Boolean = false,
    val places: List<ApiPlace> = emptyList(),
    val error: Exception? = null
)
