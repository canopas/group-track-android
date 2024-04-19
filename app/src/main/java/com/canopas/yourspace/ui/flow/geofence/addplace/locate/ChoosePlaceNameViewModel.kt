package com.canopas.yourspace.ui.flow.geofence.addplace.locate

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.canopas.yourspace.data.repository.SpaceRepository
import com.canopas.yourspace.data.service.place.ApiPlaceService
import com.canopas.yourspace.data.utils.AppDispatcher
import com.canopas.yourspace.ui.navigation.AppDestinations
import com.canopas.yourspace.ui.navigation.AppDestinations.ChoosePlaceName
import com.canopas.yourspace.ui.navigation.AppNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChoosePlaceNameViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val appDispatcher: AppDispatcher,
    private val navigator: AppNavigator,
    private val apiPlaceService: ApiPlaceService,
    private val spaceRepository: SpaceRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ChoosePlaceNameScreenState())
    val state = _state.asStateFlow()

    private val selectedLatitude =
        savedStateHandle.get<String>(ChoosePlaceName.KEY_SELECTED_LAT)!!.toDouble()
    private val selectedLongitude =
        savedStateHandle.get<String>(ChoosePlaceName.KEY_SELECTED_LONG)!!.toDouble()

    fun popBackStack() {
        navigator.navigateBack()
    }

    fun onPlaceNameChange(spaceName: String) {
        _state.value = _state.value.copy(placeName = spaceName)
    }

    fun resetErrorState() {
        _state.value = _state.value.copy(error = null)
    }

    fun addPlace() = viewModelScope.launch(appDispatcher.IO) {
        _state.value = _state.value.copy(addingPlace = true)
        try {
//            apiPlaceService.addPlace(
//                spaceId = spaceRepository.currentSpaceId,
//                name = state.value.placeName,
//                latitude = selectedLatitude,
//                longitude = selectedLongitude
//            )
            navigator.navigateBack(
                AppDestinations.home.path,
                true
            ) // TODO navigate to places screen
        } catch (e: Exception) {
            _state.value = _state.value.copy(error = e)
        } finally {
            _state.value = _state.value.copy(addingPlace = false)
        }
    }
}

data class ChoosePlaceNameScreenState(
    val placeName: String = "",
    val addingPlace: Boolean = false,
    val error: Exception? = null
)