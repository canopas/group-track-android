package com.canopas.yourspace.ui.flow.geofence.addplace.placename

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.canopas.yourspace.data.repository.SpaceRepository
import com.canopas.yourspace.data.service.place.ApiPlaceService
import com.canopas.yourspace.data.storage.UserPreferences
import com.canopas.yourspace.data.utils.AppDispatcher
import com.canopas.yourspace.ui.flow.geofence.places.EXTRA_RESULT_PLACE_LATITUDE
import com.canopas.yourspace.ui.flow.geofence.places.EXTRA_RESULT_PLACE_LONGITUDE
import com.canopas.yourspace.ui.flow.geofence.places.EXTRA_RESULT_PLACE_NAME
import com.canopas.yourspace.ui.navigation.AppDestinations
import com.canopas.yourspace.ui.navigation.AppDestinations.ChoosePlaceName
import com.canopas.yourspace.ui.navigation.AppNavigator
import com.canopas.yourspace.ui.navigation.KEY_RESULT
import com.canopas.yourspace.ui.navigation.RESULT_OKAY
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ChoosePlaceNameViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val appDispatcher: AppDispatcher,
    private val navigator: AppNavigator,
    private val apiPlaceService: ApiPlaceService,
    private val spaceRepository: SpaceRepository,
    private val userPreferences: UserPreferences
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
        val currentSpaceId = userPreferences.currentSpace ?: return@launch
        val currentUser = userPreferences.currentUser ?: return@launch

        _state.emit(state.value.copy(addingPlace = true))
        try {
            val memberIds = spaceRepository.getMemberBySpaceId(currentSpaceId)?.map { it.user_id }
                ?: emptyList()
            apiPlaceService.addPlace(
                spaceId = currentSpaceId,
                name = state.value.placeName,
                latitude = selectedLatitude,
                longitude = selectedLongitude,
                createdBy = currentUser.id,
                spaceMemberIds = memberIds
            )
            navigator.navigateBack(
                AppDestinations.places.path,
                result = mapOf(
                    KEY_RESULT to RESULT_OKAY,
                    EXTRA_RESULT_PLACE_LATITUDE to selectedLatitude,
                    EXTRA_RESULT_PLACE_LONGITUDE to selectedLongitude,
                    EXTRA_RESULT_PLACE_NAME to state.value.placeName
                )
            )
            _state.emit(state.value.copy(addingPlace = false))
        } catch (e: Exception) {
            Timber.e(e, "Error while adding place")
            _state.emit(state.value.copy(error = e, addingPlace = false))
        }
    }
}

data class ChoosePlaceNameScreenState(
    val placeName: String = "",
    val addingPlace: Boolean = false,
    val placeAdded: Boolean = false,
    val error: Exception? = null
)
