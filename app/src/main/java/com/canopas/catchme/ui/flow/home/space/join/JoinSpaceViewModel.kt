package com.canopas.catchme.ui.flow.home.space.join

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.canopas.catchme.data.models.space.ApiSpace
import com.canopas.catchme.data.repository.SpaceRepository
import com.canopas.catchme.data.service.auth.AuthService
import com.canopas.catchme.data.service.space.SpaceInvitationService
import com.canopas.catchme.data.utils.AppDispatcher
import com.canopas.catchme.ui.navigation.AppNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class JoinSpaceViewModel @Inject constructor(
    private val appNavigator: AppNavigator,
    private val appDispatcher: AppDispatcher,
    private val invitationService: SpaceInvitationService,
    private val spaceRepository: SpaceRepository,
    private val authService: AuthService
) : ViewModel() {

    private val _state = MutableStateFlow(JoinSpaceState())
    var state = _state.asStateFlow()

    fun popBackStack() {
        appNavigator.navigateBack()
    }

    fun onCodeChanged(code: String) {
        _state.value = _state.value.copy(inviteCode = code)
    }

    fun verifyAndJoinSpace() = viewModelScope.launch(appDispatcher.IO) {
        val code = _state.value.inviteCode
        _state.emit(_state.value.copy(verifying = true))
        try {
            val invitation = invitationService.getInvitation(code)
            if (invitation == null) {
                _state.emit(
                    _state.value.copy(
                        verifying = false,
                        errorInvalidInviteCode = true
                    )
                )
                return@launch
            }

            val spaceId = invitation.space_id
            val joinedSpaces = authService.currentUser?.space_ids ?: emptyList()

            if (spaceId in joinedSpaces) {
                popBackStack()
                return@launch
            }

            spaceRepository.joinSpace(spaceId)
            val space = spaceRepository.getSpace(spaceId)

            _state.emit(_state.value.copy(verifying = false, joinedSpace = space))
        } catch (e: Exception) {
            Timber.e(e, "Unable to verify invite code")
            _state.emit(
                _state.value.copy(
                    verifying = false,
                    error = e.localizedMessage
                )
            )
        }
    }

    fun resetErrorState() {
        _state.value = _state.value.copy(
            error = null,
            errorInvalidInviteCode = false
        )
    }
}

data class JoinSpaceState(
    val inviteCode: String = "",
    val verifying: Boolean = false,
    val error: String? = null,
    val joinedSpace: ApiSpace? = null,
    val errorInvalidInviteCode: Boolean = false
)
