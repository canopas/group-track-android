package com.canopas.yourspace.ui.flow.geofence.edit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.canopas.yourspace.data.models.place.ApiPlace
import com.canopas.yourspace.data.models.place.ApiPlaceMemberSetting
import com.canopas.yourspace.data.models.user.ApiUser
import com.canopas.yourspace.data.models.user.UserInfo
import com.canopas.yourspace.data.repository.SpaceRepository
import com.canopas.yourspace.data.service.auth.AuthService
import com.canopas.yourspace.data.service.place.ApiPlaceService
import com.canopas.yourspace.data.utils.AppDispatcher
import com.canopas.yourspace.ui.navigation.AppDestinations
import com.canopas.yourspace.ui.navigation.AppNavigator
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class EditPlaceViewModel @Inject constructor(
    val savedStateHandle: SavedStateHandle,
    val appNavigator: AppNavigator,
    private val appDispatcher: AppDispatcher,
    private val apiPlaceService: ApiPlaceService,
    private val spaceRepository: SpaceRepository,
    private val authService: AuthService
) : ViewModel() {

    private val placeId = savedStateHandle.get<String>(AppDestinations.EditPlace.KEY_PLACE_ID) ?: ""
    private val currentUser: ApiUser?
        get() = authService.currentUser
    private val _state = MutableStateFlow(EditPlaceState())
    val state = _state.asStateFlow()

    init {
        if (placeId.isEmpty()) {
            throw IllegalArgumentException("Place id is required")
        }
        fetchPlace()
    }

    private fun fetchPlace() = viewModelScope.launch(appDispatcher.IO) {
        try {
            _state.emit(_state.value.copy(isLoading = true, error = null))
            val place = apiPlaceService.getPlace(placeId)

            if (place == null) {
                _state.emit(_state.value.copy(error = "Place not found", isLoading = false))
                return@launch
            }
            val spaceInfo = spaceRepository.getSpaceInfo(place.space_id)
            val spaceMembers = spaceInfo?.members ?: emptyList()
            val settings =
                apiPlaceService.getPlaceMemberSetting(placeId, place.space_id, currentUser?.id!!)
            _state.emit(
                _state.value.copy(
                    place = place,
                    updatePlace = place,
                    isAdmin = place.created_by == currentUser?.id,
                    setting = settings,
                    updatedSetting = settings,
                    spaceMembers = spaceMembers.filter { it.user.id != currentUser?.id },
                    isLoading = false
                )
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch place : $placeId")
            _state.emit(_state.value.copy(error = e.localizedMessage, isLoading = false))
        }
    }

    fun onPlaceRadiusChanged(radius: Double) {
        _state.value.updatePlace?.let {
            _state.value = _state.value.copy(updatePlace = it.copy(radius = radius))
        }
        enableSave()
    }

    fun onPlaceNameChanged(name: String) {
        _state.value.updatePlace?.let {
            _state.value = _state.value.copy(updatePlace = it.copy(name = name))
        }
        enableSave()
    }

    fun onPlaceLocationChanged(latLng: LatLng) {
        _state.value.updatePlace?.let {
            _state.value = _state.value.copy(
                updatePlace = it.copy(
                    latitude = latLng.latitude,
                    longitude = latLng.longitude
                )
            )
        }
        enableSave()
    }

    fun toggleArrives(userId: String, arrives: Boolean) {
        _state.value.updatedSetting?.let {
            val updatedSetting = it.copy(
                arrival_alert_for = if (arrives) {
                    it.arrival_alert_for + userId
                } else {
                    it.arrival_alert_for - userId
                }
            )
            _state.value = _state.value.copy(updatedSetting = updatedSetting)
        }
        enableSave()
    }

    fun toggleLeaves(userId: String, leaves: Boolean) {
        _state.value.updatedSetting?.let {
            val updatedSetting = it.copy(
                leave_alert_for = if (leaves) {
                    it.leave_alert_for + userId
                } else {
                    it.leave_alert_for - userId
                }
            )
            _state.value = _state.value.copy(updatedSetting = updatedSetting)
        }
        enableSave()
    }

    private fun enableSave() {
        _state.value = _state.value.copy(
            enableSave = _state.value.updatePlace != _state.value.place ||
                (_state.value.setting != null && _state.value.updatedSetting != _state.value.setting)
        )
    }

    fun savePlace() = viewModelScope.launch(appDispatcher.IO) {
        try {
            _state.emit(_state.value.copy(saving = true, error = null))
            val updatedPlace = _state.value.updatePlace ?: return@launch
            val updatedSetting = _state.value.updatedSetting
            if (state.value.place != updatedPlace) {
                apiPlaceService.updatePlace(updatedPlace)
            }

            if (updatedSetting != null && state.value.setting != updatedSetting) {
                apiPlaceService.updatePlaceSettings(
                    updatedPlace,
                    currentUser?.id!!,
                    updatedSetting
                )
            }

            appNavigator.navigateBack()
        } catch (e: Exception) {
            Timber.e(e, "Failed to save place")
            _state.emit(_state.value.copy(error = e.localizedMessage, saving = false))
        }
    }

    fun popBackStack() {
        appNavigator.navigateBack()
    }

    fun showDeletePlaceConfirmation() {
        _state.value = _state.value.copy(showDeletePlaceConfirmation = true)
    }

    fun dismissDeletePlaceConfirmation() {
        _state.value = _state.value.copy(showDeletePlaceConfirmation = false)
    }

    fun onDeletePlace() = viewModelScope.launch(appDispatcher.IO) {
        val place = _state.value.place ?: return@launch

        try {
            _state.emit(
                _state.value.copy(
                    deleting = true,
                    error = null,
                    showDeletePlaceConfirmation = false
                )
            )
            apiPlaceService.deletePlace(place.space_id, place.id)
            _state.emit(_state.value.copy(deleting = false))
            appNavigator.navigateBack()
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete place")
            _state.emit(_state.value.copy(error = e.localizedMessage, deleting = false))
        }
    }

    fun resetErrorState() {
        _state.value = _state.value.copy(error = null)
    }
}

data class EditPlaceState(
    val isLoading: Boolean = false,
    val isAdmin: Boolean = false,
    val place: ApiPlace? = null,
    val updatePlace: ApiPlace? = null,
    val enableSave: Boolean = false,
    val setting: ApiPlaceMemberSetting? = null,
    val updatedSetting: ApiPlaceMemberSetting? = null,
    val spaceMembers: List<UserInfo> = emptyList(),
    val saving: Boolean = false,
    val deleting: Boolean = false,
    val showDeletePlaceConfirmation: Boolean = false,
    val error: String? = null
)
