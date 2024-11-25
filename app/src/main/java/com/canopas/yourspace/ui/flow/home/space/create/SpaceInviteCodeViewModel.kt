package com.canopas.yourspace.ui.flow.home.space.create

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.canopas.yourspace.data.repository.SpaceRepository
import com.canopas.yourspace.data.service.auth.AuthService
import com.canopas.yourspace.data.service.space.SpaceInvitationService
import com.canopas.yourspace.data.utils.AppDispatcher
import com.canopas.yourspace.ui.navigation.AppDestinations.SpaceInvitation.KEY_INVITE_CODE
import com.canopas.yourspace.ui.navigation.AppDestinations.SpaceInvitation.KEY_SPACE_NAME
import com.canopas.yourspace.ui.navigation.AppNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SpaceInviteCodeViewModel @Inject constructor(
    private val appNavigator: AppNavigator,
    private val savedStateHandle: SavedStateHandle,
    private val appDispatcher: AppDispatcher,
    private val spaceRepository: SpaceRepository,
    private val spaceInvitationService: SpaceInvitationService,
    private val authService: AuthService
) : ViewModel() {

    val spaceInviteCode = savedStateHandle.get<String>(KEY_INVITE_CODE) ?: ""
    val spaceName = savedStateHandle.get<String>(KEY_SPACE_NAME) ?: ""

    private val _state = MutableStateFlow(InviteCodeState())
    val state: StateFlow<InviteCodeState> = _state.asStateFlow()

    fun onStart() {
        viewModelScope.launch(appDispatcher.IO) {
            try {
                _state.emit(
                    state.value.copy(
                        inviteCode = spaceInviteCode,
                        spaceName = spaceName
                    )
                )

                val currentUserId = authService.currentUser?.id ?: ""
                val adminId = spaceRepository.getCurrentSpace()?.admin_id

                _state.emit(
                    state.value.copy(
                        isUserAdmin = currentUserId == adminId
                    )
                )
                fetchInviteCode()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun popBackStack() {
        appNavigator.navigateBack()
    }

    private fun fetchInviteCode() {
        viewModelScope.launch(appDispatcher.IO) {
            try {
                val inviteCodeData =
                    spaceInvitationService.getSpaceInviteCode(spaceRepository.currentSpaceId)
                inviteCodeData?.let {
                    _state.emit(state.value.copy(inviteCode = it.code))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun regenerateInviteCode() = viewModelScope.launch(appDispatcher.IO) {
        if (state.value.isUserAdmin) {
            spaceRepository.regenerateInviteCode(spaceRepository.currentSpaceId)
        }
        fetchInviteCode()
    }
}

data class InviteCodeState(
    var inviteCode: String = "",
    val spaceName: String = "",
    var isUserAdmin: Boolean = false
)
