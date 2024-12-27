package com.canopas.yourspace.ui.flow.settings.space.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.canopas.yourspace.data.models.space.SpaceInfo
import com.canopas.yourspace.data.models.user.UserInfo
import com.canopas.yourspace.data.repository.SpaceRepository
import com.canopas.yourspace.data.service.auth.AuthService
import com.canopas.yourspace.data.utils.AppDispatcher
import com.canopas.yourspace.domain.utils.ConnectivityObserver
import com.canopas.yourspace.ui.navigation.AppNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ChangeAdminViewModel @Inject constructor(
    private val appDispatcher: AppDispatcher,
    private val connectivityObserver: ConnectivityObserver,
    private val appNavigator: AppNavigator,
    private val spaceRepository: SpaceRepository,
    private val authService: AuthService
) : ViewModel() {

    private val _state = MutableStateFlow(ChangeAdminState())
    val state = _state.asStateFlow()

    init {
        checkInternetConnection()
    }

    fun fetchSpaceDetail(spaceID: String) = viewModelScope.launch(appDispatcher.IO) {
        _state.emit(_state.value.copy(isLoading = true))
        try {
            val spaceInfo = spaceRepository.getSpaceInfo(spaceID)
            _state.emit(
                _state.value.copy(
                    isLoading = false,
                    spaceInfo = spaceInfo,
                    spaceID = spaceID,
                    currentUserId = authService.currentUser?.id,
                    isAdmin = spaceInfo?.space?.admin_id == authService.currentUser?.id,
                    spaceName = spaceInfo?.space?.name,
                    members = spaceInfo?.members ?: emptyList()
                )
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch space detail")
            _state.emit(_state.value.copy(error = e, isLoading = false))
        }
    }

    fun changeAdmin(newAdminId: String) {
        viewModelScope.launch(appDispatcher.IO) {
            try {
                spaceRepository.changeSpaceAdmin(state.value.spaceID, newAdminId)
                _state.emit(
                    _state.value.copy(
                        isAdmin = true,
                        currentUserId = newAdminId
                    )
                )
                popBackStack()
            } catch (e: Exception) {
                Timber.e(e, "Failed to change admin")
                _state.emit(_state.value.copy(error = e))
            }
        }
    }

    fun popBackStack() {
        appNavigator.navigateBack()
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

data class ChangeAdminState(
    val isLoading: Boolean = false,
    var spaceID: String = "",
    val currentUserId: String? = null,
    val isAdmin: Boolean = false,
    val members: List<UserInfo>? = null,
    val spaceInfo: SpaceInfo? = null,
    val spaceName: String? = null,
    val error: Exception? = null,
    val connectivityStatus: ConnectivityObserver.Status = ConnectivityObserver.Status.Available
)
