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

    private val _spaceInviteCode = MutableStateFlow(savedStateHandle.get<String>(KEY_INVITE_CODE) ?: "")
    val spaceInviteCode: StateFlow<String> get() = _spaceInviteCode

    val spaceName = savedStateHandle.get<String>(KEY_SPACE_NAME) ?: ""
    var isUserAdmin: Boolean = false

    init {
        checkIfUserIsAdmin()
        observeInviteCode()
    }

    fun popBackStack() {
        appNavigator.navigateBack()
    }

    private fun checkIfUserIsAdmin() {
        viewModelScope.launch(appDispatcher.IO) {
            val currentUserId = authService.currentUser?.id ?: ""
            val adminId = spaceRepository.getCurrentSpace()?.admin_id
            isUserAdmin = currentUserId == adminId
        }
    }

    private fun observeInviteCode() =
        viewModelScope.launch(appDispatcher.IO) {
            spaceInvitationService.getInviteCodeFlow(spaceRepository.currentSpaceId)
                .collect { updatedCode ->
                    _spaceInviteCode.value = updatedCode
                }
        }


    fun regenerateInviteCode() {
        viewModelScope.launch(appDispatcher.IO) {
            if (isUserAdmin) {
                spaceRepository.regenerateInviteCode(spaceRepository.currentSpaceId)
            }
        }
    }
}
