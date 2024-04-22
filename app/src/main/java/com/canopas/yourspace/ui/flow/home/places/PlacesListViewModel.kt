package com.canopas.yourspace.ui.flow.home.places

import androidx.lifecycle.ViewModel
import com.canopas.yourspace.data.models.place.ApiPlace
import com.canopas.yourspace.data.repository.SpaceRepository
import com.canopas.yourspace.data.service.place.ApiPlaceService
import com.canopas.yourspace.data.storage.UserPreferences
import com.canopas.yourspace.data.utils.AppDispatcher
import com.canopas.yourspace.ui.navigation.AppDestinations
import com.canopas.yourspace.ui.navigation.AppNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class PlacesListViewModel @Inject constructor(
    private val appNavigator: AppNavigator,
    private val appDispatcher: AppDispatcher,
    private val apiPlaceService: ApiPlaceService,
    private val spaceRepository: SpaceRepository,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _state = MutableStateFlow(PlacesListScreenState())
    val state = _state.asStateFlow()

    fun navigateBack() {
        appNavigator.navigateBack()
    }

    fun navigateToAddPlace() {
        appNavigator.navigateTo(AppDestinations.locateOnMap.path)
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
}

data class PlacesListScreenState(
    val placeAdded: Boolean = false,
    val addedPlaceLat: Double = 0.0,
    val addedPlaceLng: Double = 0.0,
    val addedPlaceName: String = "",

    val placesLoading:Boolean = false,
    val places:List<ApiPlace> = emptyList(),
    val error: Exception? = null
)
