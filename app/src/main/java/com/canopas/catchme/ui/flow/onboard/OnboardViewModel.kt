package com.canopas.catchme.ui.flow.onboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.canopas.catchme.data.service.space.SpaceService
import com.canopas.catchme.data.service.user.UserService
import com.canopas.catchme.data.storage.UserPreferences
import com.canopas.catchme.data.utils.AppDispatcher
import com.canopas.catchme.ui.navigation.AppDestinations
import com.canopas.catchme.ui.navigation.AppNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class OnboardViewModel @Inject constructor(
    private val userService: UserService,
    private val appDispatcher: AppDispatcher,
    private val spaceService: SpaceService,
    private val userPreferences: UserPreferences,
    private val navigator: AppNavigator
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardScreenState())
    val state: StateFlow<OnboardScreenState> = _state

    private val currentUser get() = userService.currentUser

    init {
        val user = userService.currentUser
        _state.value = _state.value.copy(
            firstName = user?.first_name ?: "",
            lastName = user?.last_name ?: ""
        )
    }

    fun onFirstNameChange(name: String) {
        _state.value = _state.value.copy(
            firstName = name
        )
    }

    fun onLastNameChange(name: String) {
        _state.value = _state.value.copy(
            lastName = name
        )
    }

    fun navigateToSpaceInfo() = viewModelScope.launch(appDispatcher.IO) {
        if (currentUser?.first_name != _state.value.firstName || currentUser?.last_name != _state.value.lastName) {
            _state.emit(_state.value.copy(updatingUserName = true))
            val user = currentUser?.copy(
                first_name = _state.value.firstName.trim(),
                last_name = _state.value.lastName.trim()
            )
            user?.let { userService.updateUser(it) }
        }
        _state.value = _state.value.copy(
            updatingUserName = false,
            currentStep = OnboardItems.SpaceIntro
        )
    }

    fun navigateToJoinOrCreateSpace() {
        _state.value = _state.value.copy(
            currentStep = OnboardItems.JoinOrCreateSpace
        )
    }

    fun navigateToCreateSpace() {
        _state.value = _state.value.copy(currentStep = OnboardItems.CreateSpace)
    }

    fun navigateToJoinSpace(code: String) {
        _state.value = _state.value.copy(
            spaceCode = code,
            currentStep = OnboardItems.JoinSpace
        )
    }

    fun createSpace(spaceName: String) = viewModelScope.launch(appDispatcher.IO) {
        try {
            _state.emit(
                _state.value.copy(
                    spaceName = spaceName,
                    creatingSpace = true
                )
            )
            val invitationCode = spaceService.createSpace(spaceName)
            _state.emit(
                _state.value.copy(
                    creatingSpace = false,
                    spaceCode = invitationCode,
                    currentStep = OnboardItems.ShareSpaceCodeOnboard
                )
            )
        } catch (e: Exception) {
            Timber.e(e, "Unable to create space")
            _state.value = _state.value.copy(error = e.localizedMessage)
        }
    }

    fun navigateToPermission() = viewModelScope.launch {
        userPreferences.setOnboardShown(true)
        navigator.navigateTo(
            AppDestinations.home.path,
            popUpToRoute = AppDestinations.onboard.path,
            inclusive = true
        )
    }

    fun joinSpace() {
    }
}

data class OnboardScreenState(
    val firstName: String = "",
    val lastName: String = "",
    val updatingUserName: Boolean = false,
    val currentStep: OnboardItems = OnboardItems.PickName,
    val creatingSpace: Boolean = false,
    val spaceName: String? = "",
    val spaceCode: String? = "",
    val error: String? = null
)

sealed class OnboardItems {
    data object PickName : OnboardItems()
    data object SpaceIntro : OnboardItems()
    data object JoinOrCreateSpace : OnboardItems()
    data object CreateSpace : OnboardItems()
    data object ShareSpaceCodeOnboard : OnboardItems()
    data object JoinSpace : OnboardItems()
}
