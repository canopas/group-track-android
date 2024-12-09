package com.canopas.yourspace.ui.flow.geofence.places

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.canopas.yourspace.data.models.place.ApiPlace
import com.canopas.yourspace.data.models.space.SpaceInfo
import com.canopas.yourspace.data.models.user.ApiUser
import com.canopas.yourspace.data.repository.SpaceRepository
import com.canopas.yourspace.data.service.auth.AuthService
import com.canopas.yourspace.data.service.place.ApiPlaceService
import com.canopas.yourspace.data.utils.AppDispatcher
import com.canopas.yourspace.domain.utils.ConnectivityObserver
import com.canopas.yourspace.ui.navigation.AppDestinations
import com.canopas.yourspace.ui.navigation.AppNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class PlacesListViewModel @Inject constructor(
    private val appNavigator: AppNavigator,
    private val appDispatcher: AppDispatcher,
    private val spaceRepository: SpaceRepository,
    private val placeService: ApiPlaceService,
    private val authService: AuthService,
    private val connectivityObserver: ConnectivityObserver
) : ViewModel() {

    private val _state =
        MutableStateFlow(PlacesListScreenState(currentUser = authService.currentUser))
    val state = _state.asStateFlow()

    init {
        checkInternetConnection()
        loadPlaces()
        getCurrentSpace()
    }

    private fun getCurrentSpace() = viewModelScope.launch(appDispatcher.IO) {
        try {
            _state.emit(_state.value.copy(loadingSpace = true))
            val space = spaceRepository.getCurrentSpaceInfo()
            val members = space?.members ?: emptyList()

            _state.emit(
                _state.value.copy(
                    currentSpace = space,
                    loadingSpace = false,
                    hasMembers = members.size > 1
                )
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch current space")
            _state.emit(_state.value.copy(error = e.message, loadingSpace = false))
        }
    }

    fun addMember() = viewModelScope.launch(appDispatcher.IO) {
        try {
            val space = spaceRepository.getCurrentSpace() ?: return@launch
            var inviteCode = _state.value.inviteCode
            if (inviteCode.isEmpty()) {
                _state.emit(_state.value.copy(loadingInviteCode = true))
                inviteCode = spaceRepository.getInviteCode(space.id) ?: return@launch
                _state.emit(_state.value.copy(loadingInviteCode = false, inviteCode = inviteCode))
            }
            appNavigator.navigateTo(
                AppDestinations.SpaceInvitation.spaceInvitation(inviteCode, space.name).path
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to get invite code")
            _state.emit(_state.value.copy(error = e.message, loadingInviteCode = false))
        }
    }

    private fun loadPlaces() = viewModelScope.launch(appDispatcher.IO) {
        _state.emit(state.value.copy(placesLoading = state.value.places.isEmpty(), error = null))
        placeService.listenAllPlaces(spaceRepository.currentSpaceId)
            .catch {
                Timber.e(it, "Error loading places")
                _state.emit(state.value.copy(placesLoading = false, error = it.localizedMessage))
            }
            .collectLatest {
                _state.emit(state.value.copy(placesLoading = false, places = it))
            }
    }

    fun navigateBack() {
        appNavigator.navigateBack()
    }

    fun navigateToAddPlace() {
        appNavigator.navigateTo(AppDestinations.addNewPlace.path)
    }

    fun showPlaceAddedPopup(lat: Double, long: Double, name: String) {
        if (name.isEmpty() || lat == 0.0 || long == 0.0) return
        _state.value = state.value.copy(
            placeAdded = true,
            addedPlaceLat = lat,
            addedPlaceLng = long,
            addedPlaceName = name
        )
    }

    fun dismissPlaceAddedPopup() {
        _state.value = state.value.copy(
            placeAdded = false,
            addedPlaceLat = 0.0,
            addedPlaceLng = 0.0,
            addedPlaceName = ""
        )
    }

    fun selectedSuggestion(name: String) {
        appNavigator.navigateTo(AppDestinations.LocateOnMap.setArgs(name).path)
    }

    fun navigateToEditPlace(place: ApiPlace) {
        appNavigator.navigateTo(AppDestinations.EditPlace.setArgs(place.id).path)
    }

    fun showDeletePlaceConfirmation(place: ApiPlace) {
        _state.value = state.value.copy(placeToDelete = place)
    }

    fun dismissDeletePlaceConfirmation() {
        _state.value = state.value.copy(placeToDelete = null)
    }

    fun onDeletePlace() = viewModelScope.launch(appDispatcher.IO) {
        val place = state.value.placeToDelete ?: return@launch
        val deletingPlace = state.value.deletingPlaces.toMutableList()
        deletingPlace.add(place)
        _state.value = state.value.copy(deletingPlaces = deletingPlace, placeToDelete = null)

        try {
            placeService.deletePlace(spaceRepository.currentSpaceId, place.id)
            val places = state.value.deletingPlaces.toMutableList()
            places.remove(place)
            _state.value = state.value.copy(deletingPlaces = places)
        } catch (e: Exception) {
            val places = state.value.deletingPlaces.toMutableList()
            places.remove(place)
            _state.value =
                state.value.copy(
                    deletingPlaces = places,
                    error = e.localizedMessage,
                    placeToDelete = null
                )
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

data class PlacesListScreenState(
    val placeAdded: Boolean = false,
    val addedPlaceLat: Double = 0.0,
    val addedPlaceLng: Double = 0.0,
    val addedPlaceName: String = "",

    val placeToDelete: ApiPlace? = null,
    val deletingPlaces: List<ApiPlace> = emptyList(),
    val connectivityStatus: ConnectivityObserver.Status = ConnectivityObserver.Status.Available,
    val currentUser: ApiUser? = null,
    val placesLoading: Boolean = false,
    val places: List<ApiPlace> = emptyList(),
    val error: String? = null,
    val loadingInviteCode: Boolean = false,
    val inviteCode: String = "",
    val loadingSpace: Boolean = false,
    val currentSpace: SpaceInfo? = null,
    val hasMembers: Boolean = false
)
