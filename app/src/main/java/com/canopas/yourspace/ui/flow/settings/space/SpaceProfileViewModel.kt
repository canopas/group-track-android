package com.canopas.yourspace.ui.flow.settings.space

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.canopas.yourspace.data.models.space.SpaceInfo
import com.canopas.yourspace.data.repository.SpaceRepository
import com.canopas.yourspace.data.service.auth.AuthService
import com.canopas.yourspace.data.utils.AppDispatcher
import com.canopas.yourspace.ui.navigation.AppDestinations
import com.canopas.yourspace.ui.navigation.AppNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SpaceProfileViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val spaceRepository: SpaceRepository,
    private val navigator: AppNavigator,
    private val authService: AuthService,
    private val appDispatcher: AppDispatcher
) : ViewModel() {

    val spaceID =
        savedStateHandle.get<String>(AppDestinations.SpaceProfileScreen.KEY_SPACE_ID) ?: ""

    private val _state = MutableStateFlow(SpaceProfileState())
    val state = _state.asStateFlow()

    init {
        fetchSpaceDetail()
    }

    private fun fetchSpaceDetail() = viewModelScope.launch(appDispatcher.IO) {
        _state.emit(_state.value.copy(isLoading = true))
        try {

            val spaceInfo = spaceRepository.getSpaceInfo(spaceID)
            val locationEnabled =
                spaceInfo?.members?.firstOrNull { it.user.id == authService.currentUser?.id }?.isLocationEnable
                    ?: false
            _state.emit(
                _state.value.copy(
                    isLoading = false, spaceInfo = spaceInfo,
                    currentUserId = authService.currentUser?.id,
                    spaceName = spaceInfo?.space?.name, locationEnabled = locationEnabled
                )
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch space detail")
            _state.emit(_state.value.copy(error = e.message, isLoading = false))
        }
    }

    fun popBackStack() {
        navigator.navigateBack()
    }

    fun onNameChanged(name: String) {
        _state.value = state.value.copy(spaceName = name)
        onChange()
    }

    private fun onChange() {
        val spaceName = _state.value.spaceInfo?.space?.name
        val validFirstName = (_state.value.spaceName ?: "").trim().length >= 3

        val locationEnabled =
            _state.value.spaceInfo?.members?.firstOrNull { it.user.id == authService.currentUser?.id }?.isLocationEnable
                ?: false

        val changes =
            spaceName != _state.value.spaceName || locationEnabled != _state.value.locationEnabled

        _state.value = state.value.copy(allowSave = validFirstName && changes)

    }

    fun onLocationEnabledChanged(enable: Boolean) {
        _state.value = state.value.copy(locationEnabled = enable)
        onChange()
    }

    fun saveUser() {

    }

    fun resetErrorState() {
        _state.value = _state.value.copy(error = null)
    }

    fun showDeleteSpaceConfirmation(show: Boolean) {
        _state.value = state.value.copy(showDeleteSpaceConfirmation = show)
    }

    fun showLeaveSpaceConfirmation(show: Boolean) {
        _state.value = state.value.copy(showLeaveSpaceConfirmation = show)
    }

}

data class SpaceProfileState(
    val isLoading: Boolean = false,
    val currentUserId: String? = null,
    val spaceInfo: SpaceInfo? = null,
    val spaceName: String? = null,
    val deletingSpace: Boolean = false,
    val leavingSpace: Boolean = false,
    val locationEnabled: Boolean = false,
    val showDeleteSpaceConfirmation: Boolean = false,
    val showLeaveSpaceConfirmation: Boolean = false,
    val allowSave: Boolean = false,
    val error: String? = null
)