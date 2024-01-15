package com.canopas.catchme.ui.flow.onboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.canopas.catchme.data.service.user.UserService
import com.canopas.catchme.data.utils.AppDispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardViewModel @Inject constructor(
    private val userService: UserService,
    private val appDispatcher: AppDispatcher
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardScreenState())
    val state: StateFlow<OnboardScreenState> = _state

    private val currentUser get() = userService.currentUser

    init {
        val user = userService.currentUser
        _state.value = _state.value.copy(
            firstName = user?.first_name,
            lastName = user?.last_name,
            enablePickNameBtn = !user?.first_name?.trim()
                .isNullOrEmpty() || !user?.last_name?.trim()
                .isNullOrEmpty()
        )
    }

    fun onFirstNameChange(name: String) {
        _state.value = _state.value.copy(
            firstName = name,
            enablePickNameBtn = name.trim().isNotEmpty() || _state.value.lastName?.trim()
                .isNullOrEmpty()
        )
    }

    fun onLastNameChange(name: String) {
        _state.value = _state.value.copy(
            lastName = name,
            enablePickNameBtn = name.trim().isNotEmpty() || _state.value.firstName?.trim()
                .isNullOrEmpty()
        )
    }

    fun navigateToSpaceInfo() = viewModelScope.launch(appDispatcher.IO) {
        if (currentUser?.first_name != _state.value.firstName || currentUser?.last_name != _state.value.lastName) {
            val user = currentUser?.copy(
                first_name = _state.value.firstName,
                last_name = _state.value.lastName
            )
            user?.let { userService.updateUser(it) }
        }
        _state.value = _state.value.copy(
            currentStep = OnboardItems.SpaceIntro
        )
    }

    fun navigateToJoinOrCreateSpace() {
        _state.value = _state.value.copy(
            currentStep = OnboardItems.JoinOrCreateSpace
        )
    }

    fun navigateToSpaceName() {
        _state.value = _state.value.copy(
            currentStep = OnboardItems.CreateSpace
        )
    }

    fun navigateToJoinSpace(code: String) {
        _state.value = _state.value.copy(
            spaceCode = code,
            currentStep = OnboardItems.JoinSpace
        )
    }

    fun navigateToSpaceInvitationCode(spaceName: String) {
        _state.value = _state.value.copy(
            spaceName = spaceName,
            creatingSpace = true,
            currentStep = OnboardItems.ShareSpaceCodeOnboard
        )
    }

    fun navigateToPermission() {
    }

    fun joinSpace() {
    }
}

data class OnboardScreenState(
    val firstName: String? = "",
    val lastName: String? = "",
    val currentStep: OnboardItems = OnboardItems.PickName,
    val creatingSpace: Boolean = false,
    val spaceName: String? = "",
    val spaceCode: String? = "",
    val enablePickNameBtn: Boolean = false
)

sealed class OnboardItems {
    data object PickName : OnboardItems()
    data object SpaceIntro : OnboardItems()
    data object JoinOrCreateSpace : OnboardItems()
    data object CreateSpace : OnboardItems()
    data object ShareSpaceCodeOnboard : OnboardItems()
    data object JoinSpace : OnboardItems()
}
