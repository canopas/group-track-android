package com.canopas.catchme.ui.flow.onboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.canopas.catchme.data.repository.SpaceRepository
import com.canopas.catchme.data.service.auth.AuthService
import com.canopas.catchme.data.service.space.ApiSpaceService
import com.canopas.catchme.data.service.space.SpaceInvitationService
import com.canopas.catchme.data.storage.UserPreferences
import com.canopas.catchme.data.utils.AppDispatcher
import com.canopas.catchme.ui.navigation.AppDestinations
import com.canopas.catchme.ui.navigation.MainNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class OnboardViewModel @Inject constructor(
    private val authService: AuthService,
    private val appDispatcher: AppDispatcher,
    private val spaceRepository: SpaceRepository,
    private val userPreferences: UserPreferences,
    private val navigator: MainNavigator,
    private val invitationService: SpaceInvitationService
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardScreenState())
    val state: StateFlow<OnboardScreenState> = _state

    private val currentUser get() = authService.currentUser

    init {
        val user = authService.currentUser
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
            user?.let { authService.updateUser(it) }
        }
        _state.emit(
            _state.value.copy(
                updatingUserName = false,
                currentStep = OnboardItems.SpaceIntro
            )
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

    fun createSpace(spaceName: String) = viewModelScope.launch(appDispatcher.IO) {
        try {
            _state.emit(
                _state.value.copy(
                    spaceName = spaceName,
                    creatingSpace = true
                )
            )
            val invitationCode = spaceRepository.createSpaceAndGetInviteCode(spaceName)
            _state.emit(
                _state.value.copy(
                    creatingSpace = false,
                    spaceInviteCode = invitationCode,
                    currentStep = OnboardItems.ShareSpaceCodeOnboard
                )
            )
        } catch (e: Exception) {
            Timber.e(e, "Unable to create space")
            _state.emit(_state.value.copy(error = e.localizedMessage))
        }
    }

    fun navigateToPermission() = viewModelScope.launch {
        userPreferences.setOnboardShown(true)
        navigator.navigateTo(
            AppDestinations.enablePermissions.path,
            popUpToRoute = AppDestinations.onboard.path,
            inclusive = true
        )
    }

    fun submitInviteCode() = viewModelScope.launch(appDispatcher.IO) {
        val code = _state.value.spaceInviteCode ?: return@launch
        _state.emit(_state.value.copy(verifyingInviteCode = true))

        try {
            val invitation = invitationService.getInvitation(code)
            if (invitation == null) {
                _state.emit(
                    _state.value.copy(
                        verifyingInviteCode = false,
                        errorInvalidInviteCode = true
                    )
                )
                return@launch
            }

            val space = spaceRepository.getSpace(invitation.space_id)

            _state.emit(
                _state.value.copy(
                    spaceName = space?.name,
                    spaceId = invitation.space_id,
                    verifyingInviteCode = false,
                    errorInvalidInviteCode = false,
                    currentStep = OnboardItems.JoinSpace
                )
            )
        } catch (e: Exception) {
            Timber.e(e, "Unable to verify invite code")
            _state.emit(
                _state.value.copy(
                    verifyingInviteCode = false,
                    error = e.localizedMessage
                )
            )
        }
    }

    fun joinSpace() = viewModelScope.launch(appDispatcher.IO) {
        _state.emit(_state.value.copy(joiningSpace = true))
        val spaceId = _state.value.spaceId
        try {
            if (spaceId != null) {
                spaceRepository.joinSpace(spaceId)
            }
            _state.emit(_state.value.copy(joiningSpace = false))
            navigateToPermission()
        } catch (e: Exception) {
            Timber.e(e, "Unable to join space")
            _state.emit(
                _state.value.copy(
                    joiningSpace = false,
                    error = e.localizedMessage
                )
            )
        }
    }

    fun onInviteCodeChanged(inviteCode: String) {
        _state.value = _state.value.copy(
            spaceInviteCode = inviteCode,
            errorInvalidInviteCode = false
        )

        if (inviteCode.length == 6) {
            submitInviteCode()
        }
    }

    fun resetErrorState() {
        _state.value = _state.value.copy(
            error = null,
            errorInvalidInviteCode = false
        )
    }
}

data class OnboardScreenState(
    val currentStep: OnboardItems = OnboardItems.PickName,
    val firstName: String = "",
    val lastName: String = "",
    val updatingUserName: Boolean = false,
    val spaceName: String? = "",
    val spaceId: String? = null,
    val spaceInviteCode: String? = "",
    val creatingSpace: Boolean = false,
    val verifyingInviteCode: Boolean = false,
    val joiningSpace: Boolean = false,
    val errorInvalidInviteCode: Boolean = false,
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
