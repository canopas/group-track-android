package com.canopas.yourspace.ui.flow.geofence.add.addnew

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.canopas.yourspace.data.service.place.ApiPlaceService
import com.canopas.yourspace.data.utils.AppDispatcher
import com.canopas.yourspace.ui.navigation.AppDestinations
import com.canopas.yourspace.ui.navigation.AppNavigator
import com.google.android.libraries.places.api.model.Place
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class AddNewPlaceViewModel @Inject constructor(
    private val appNavigator: AppNavigator,
    private val appDispatcher: AppDispatcher,
    private val apiPlaceService: ApiPlaceService
) : ViewModel() {

    private val _state = MutableStateFlow(AddNewPlaceScreenState())
    val state = _state.asStateFlow()

    private val searchQuery = MutableStateFlow("")

    init {
        viewModelScope.launch {
            searchQuery
                .debounce(500)
                .collectLatest {
                    findPlace(it)
                }
        }
    }

    fun onPlaceNameChanged(placeName: String) {
        searchQuery.value = placeName
        _state.value = _state.value.copy(loading = true, searchQuery = placeName)
    }

    private fun findPlace(query: String) = viewModelScope.launch(appDispatcher.IO) {
        try {
            _state.value = _state.value.copy(loading = true)
            apiPlaceService.findPlace(query.trim()).let { places ->
                _state.value = _state.value.copy(places = places, loading = false)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error finding place: ${searchQuery.value}")
            _state.value = _state.value.copy(error = e.message, loading = false)
        }
    }

    fun onPlaceSelected(place: Place) {
        val latLng = place.latLng ?: return
        val name = place.name ?: return

        appNavigator.navigateTo(
            AppDestinations.ChoosePlaceName.setArgs(
                latitude = latLng.latitude,
                longitude = latLng.longitude,
                placeName = name
            ).path
        )
    }

    fun resetErrorState() {
        _state.value = _state.value.copy(error = null)
    }

    fun popBackStack() {
        appNavigator.navigateBack()
    }

    fun navigateToLocateOnMap() {
        appNavigator.navigateTo(AppDestinations.LocateOnMap.setArgs("").path)
    }
}

data class AddNewPlaceScreenState(
    val loading: Boolean = false,
    val searchQuery: String = "",
    val places: List<Place> = emptyList(),
    val error: String? = null
)
