package com.canopas.catchme.ui.flow.onboard

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class OnboardViewModel @Inject constructor() : ViewModel() {

    private val _state = MutableStateFlow(OnboardScreenState())
    val state: StateFlow<OnboardScreenState> = _state

    fun navigateToSpaceInfo(firstName: String, lastName: String) {
        _state.value = _state.value.copy(
            firstName = firstName, lastName = lastName,
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
            spaceCode = code, currentStep = OnboardItems.JoinSpace
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
    val firstName: String = "",
    val lastName: String = "",
    val currentStep: OnboardItems = OnboardItems.PickName,
    val creatingSpace: Boolean = false,
    val spaceName: String = "",
    val spaceCode: String = ""
)

sealed class OnboardItems {
    data object PickName : OnboardItems()
    data object SpaceIntro : OnboardItems()
    data object JoinOrCreateSpace : OnboardItems()
    data object CreateSpace : OnboardItems()
    data object ShareSpaceCodeOnboard : OnboardItems()
    data object JoinSpace : OnboardItems()

}