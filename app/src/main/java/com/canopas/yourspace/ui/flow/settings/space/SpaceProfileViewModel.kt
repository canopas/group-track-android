package com.canopas.yourspace.ui.flow.settings.space

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.canopas.yourspace.data.models.space.SpaceInfo
import com.canopas.yourspace.data.repository.SpaceRepository
import com.canopas.yourspace.data.service.auth.AuthService
import com.canopas.yourspace.data.utils.AppDispatcher
import com.canopas.yourspace.domain.utils.ConnectivityObserver
import com.canopas.yourspace.ui.navigation.AppDestinations
import com.canopas.yourspace.ui.navigation.AppNavigator
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltViewModel
class SpaceProfileViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val spaceRepository: SpaceRepository,
    private val navigator: AppNavigator,
    private val authService: AuthService,
    private val appDispatcher: AppDispatcher,
    private val connectivityObserver: ConnectivityObserver
) : ViewModel() {

    private val spaceID =
        savedStateHandle.get<String>(AppDestinations.SpaceProfileScreen.KEY_SPACE_ID)
            ?: throw IllegalArgumentException("Space ID is required")

    private val _state = MutableStateFlow(SpaceProfileState())
    val state = _state.asStateFlow()

    init {
        checkInternetConnection()
    }

    fun fetchSpaceDetail() = viewModelScope.launch(appDispatcher.IO) {
        _state.emit(_state.value.copy(isLoading = true))
        try {
            val spaceInfo = spaceRepository.getSpaceInfo(spaceID)
            val locationEnabled =
                spaceInfo?.members?.firstOrNull { it.user.id == authService.currentUser?.id }?.isLocationEnable
                    ?: false
            _state.emit(
                _state.value.copy(
                    isLoading = false,
                    spaceInfo = spaceInfo,
                    currentUserId = authService.currentUser?.id,
                    isAdmin = spaceInfo?.space?.admin_id == authService.currentUser?.id,
                    spaceName = spaceInfo?.space?.name,
                    locationEnabled = locationEnabled,
                    spaceMemberCount = spaceInfo?.members?.size ?: 1
                )
            )
            fetchInviteCode(spaceInfo?.space?.id ?: "")
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch space detail")
            _state.emit(_state.value.copy(error = e, isLoading = false))
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
        val changes = spaceName != _state.value.spaceName

        _state.value = state.value.copy(allowSave = validFirstName && changes)
    }

    fun onLocationEnabledChanged(enable: Boolean) {
        viewModelScope.launch {
            try {
                _state.value = state.value.copy(locationEnabled = enable)
                spaceRepository.enableLocation(spaceID, authService.currentUser?.id ?: "", enable)
            } catch (e: Exception) {
                Timber.e(e, "Failed to update location")
                _state.value = state.value.copy(error = e, locationEnabled = !enable)
            }
            onChange()
        }
    }

    fun updateMemberLocation(memberId: String, enableLocation: Boolean) {
        viewModelScope.launch(appDispatcher.IO) {
            try {
                spaceRepository.enableLocation(spaceID, memberId, enableLocation)
                val spaceInfo = spaceRepository.getSpaceInfo(spaceID)
                _state.emit(
                    _state.value.copy(
                        spaceInfo = spaceInfo,
                        locationEnabledChanges = _state.value.locationEnabledChanges + (memberId to enableLocation)
                    )
                )
            } catch (e: Exception) {
                Timber.e(e, "Failed to update member location")
                _state.emit(_state.value.copy(error = e))
            }
        }
    }

    private fun fetchInviteCode(spaceId: String) {
        viewModelScope.launch(appDispatcher.IO) {
            try {
                val inviteCodeData = spaceRepository.getInviteCode(spaceId)
                val inviteCodeCratedAt = spaceRepository.getCurrentSpaceInviteCodeExpireTime(spaceId)
                inviteCodeData?.let {
                    val expireTime = getRemainingTime(inviteCodeCratedAt ?: 0)
                    _state.emit(state.value.copy(inviteCode = it, codeExpireTime = expireTime))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun regenerateInviteCode() = viewModelScope.launch(appDispatcher.IO) {
        _state.emit(_state.value.copy(isCodeLoading = true))
        try {
            spaceRepository.regenerateInviteCode(spaceRepository.currentSpaceId)
            fetchInviteCode(spaceID)
            _state.emit(_state.value.copy(isCodeLoading = false))
        } catch (e: Exception) {
            Timber.e(e, "Failed to regenerate invite code")
            _state.emit(_state.value.copy(isCodeLoading = false, error = e))
        }
    }

    private fun getRemainingTime(
        createTimeMillis: Long,
        durationMillis: Long = TimeUnit.HOURS.toMillis(48)
    ): String {
        val currentTimeMillis = System.currentTimeMillis()
        val expireTimeMillis = createTimeMillis + durationMillis
        val diffInMillis = expireTimeMillis - currentTimeMillis

        if (diffInMillis <= 0) {
            Timber.e("The invite code is expired.")
            return "Expired"
        }

        val hours = TimeUnit.MILLISECONDS.toHours(diffInMillis) % 48
        val minutes = TimeUnit.MILLISECONDS.toMinutes(diffInMillis) % 60
        return String.format("%02d hours %02d minutes", hours, minutes)
    }

    fun saveSpace() = viewModelScope.launch(appDispatcher.IO) {
        if (state.value.saving) return@launch
        if (state.value.connectivityStatus != ConnectivityObserver.Status.Available) {
            _state.emit(_state.value.copy(error = Exception()))
            return@launch
        }
        val space = _state.value.spaceInfo?.space ?: return@launch

        val locationEnabled =
            _state.value.spaceInfo?.members?.firstOrNull { it.user.id == authService.currentUser?.id }?.isLocationEnable
                ?: false

        val isLocationStateUpdated = locationEnabled != _state.value.locationEnabled
        val isNameUpdated = space.name != _state.value.spaceName?.trim()

        try {
            _state.emit(_state.value.copy(saving = true))
            if (isNameUpdated) {
                _state.emit(_state.value.copy(isNameChanging = true))
                spaceRepository.updateSpace(
                    space.copy(name = _state.value.spaceName?.trim() ?: "")
                )
            }
            if (isLocationStateUpdated) {
                spaceRepository.enableLocation(
                    spaceID,
                    authService.currentUser?.id ?: "",
                    _state.value.locationEnabled
                )
            }
            val spaceInfo = spaceRepository.getSpaceInfo(spaceID)
            _state.emit(_state.value.copy(saving = false, allowSave = false, spaceInfo = spaceInfo, isNameChanging = false))
        } catch (e: Exception) {
            Timber.e(e, "Failed to save space")
            _state.emit(_state.value.copy(saving = false, error = e, allowSave = false, isNameChanging = false))
        }
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

    fun deleteSpace() = viewModelScope.launch(appDispatcher.IO) {
        try {
            _state.emit(
                _state.value.copy(
                    deletingSpace = true,
                    showDeleteSpaceConfirmation = false
                )
            )
            spaceRepository.deleteSpace(spaceID)
            navigator.navigateBack()
        } catch (e: Exception) {
            Timber.e(e, "Failed to delete space")
            _state.emit(
                _state.value.copy(
                    error = e,
                    deletingSpace = false,
                    showDeleteSpaceConfirmation = false
                )
            )
        }
    }

    fun leaveSpace() = viewModelScope.launch(appDispatcher.IO) {
        try {
            _state.emit(
                _state.value.copy(
                    leavingSpace = true,
                    showLeaveSpaceConfirmation = false
                )
            )
            spaceRepository.leaveSpace(spaceID)
            navigator.navigateBack()
        } catch (e: Exception) {
            Timber.e(e, "Failed to leave space")
            _state.emit(
                _state.value.copy(
                    error = e,
                    leavingSpace = false,
                    showLeaveSpaceConfirmation = false
                )
            )
        }
    }

    fun onChangeAdminClicked() {
        val spaceInfo = _state.value.spaceInfo
        if (spaceInfo != null) {
            navigateToChangeAdminScreen(spaceInfo)
            _state.value = _state.value.copy(showChangeAdminDialog = false)
        }
    }

    fun onAdminMenuExpanded(value: Boolean) {
        _state.value = _state.value.copy(isMenuExpanded = value)
    }

    fun showChangeAdminDialog(show: Boolean) {
        _state.value = _state.value.copy(showChangeAdminDialog = show)
    }

    fun navigateToChangeAdminScreen(spaceInfo: SpaceInfo?) {
        val spaceDetail = Uri.encode(Gson().toJson(spaceInfo))
        navigator.navigateTo(AppDestinations.ChangeAdminScreen.getSpaceDetail(spaceDetail).path)
    }

    fun showRemoveMemberConfirmationWithId(show: Boolean, memberId: String) {
        _state.value = state.value.copy(showRemoveMemberConfirmation = show, memberToRemove = memberId)
    }

    fun removeMember(memberId: String) = viewModelScope.launch(appDispatcher.IO) {
        try {
            _state.emit(
                _state.value.copy(
                    showRemoveMemberConfirmation = false,
                    isLoading = true
                )
            )
            spaceRepository.removeUserFromSpace(spaceID, memberId)
            fetchSpaceDetail()
        } catch (e: Exception) {
            Timber.e(e, "Failed to remove member")
            _state.emit(
                _state.value.copy(
                    error = e,
                    isLoading = false
                )
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
}

data class SpaceProfileState(
    val isLoading: Boolean = false,
    val saving: Boolean = false,
    val currentUserId: String? = null,
    val isAdmin: Boolean = false,
    val spaceInfo: SpaceInfo? = null,
    val spaceName: String? = null,
    val deletingSpace: Boolean = false,
    val leavingSpace: Boolean = false,
    val locationEnabled: Boolean = false,
    val showDeleteSpaceConfirmation: Boolean = false,
    val showLeaveSpaceConfirmation: Boolean = false,
    val allowSave: Boolean = false,
    val error: Exception? = null,
    val connectivityStatus: ConnectivityObserver.Status = ConnectivityObserver.Status.Available,
    val showRemoveMemberConfirmation: Boolean = false,
    val memberToRemove: String? = null,
    val spaceMemberCount: Int = 1,
    val showChangeAdminDialog: Boolean = false,
    var isMenuExpanded: Boolean = false,
    val inviteCode: String = "",
    val codeExpireTime: String = "",
    val isCodeLoading: Boolean = false,
    val locationEnabledChanges: Map<String, Boolean> = emptyMap(),
    val isLocationSettingChange: Boolean = false,
    val isNameChanging: Boolean = false
)
