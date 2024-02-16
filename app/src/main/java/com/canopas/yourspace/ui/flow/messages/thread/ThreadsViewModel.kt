package com.canopas.yourspace.ui.flow.messages.thread

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.canopas.yourspace.data.models.messages.ThreadInfo
import com.canopas.yourspace.data.models.space.SpaceInfo
import com.canopas.yourspace.data.repository.SpaceRepository
import com.canopas.yourspace.data.service.auth.AuthService
import com.canopas.yourspace.data.service.messages.ApiMessagesService
import com.canopas.yourspace.data.utils.AppDispatcher
import com.canopas.yourspace.ui.navigation.AppNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ThreadsViewModel @Inject constructor(
    private val messagesService: ApiMessagesService,
    private val authService: AuthService,
    private val spaceRepository: SpaceRepository,
    private val navigator: AppNavigator,
    private val appDispatcher: AppDispatcher
) : ViewModel() {

    private val _state = MutableStateFlow(ThreadsScreenState())
    val state = _state.asStateFlow()

    init {
        getCurrentSpace()
    }

    private fun getCurrentSpace() = viewModelScope.launch(appDispatcher.IO) {
        try {

            _state.emit(_state.value.copy(loading = true))
            val space = spaceRepository.getCurrentSpaceInfo()
            val members = space?.members ?: emptyList()

            _state.emit(
                _state.value.copy(
                    loading = false, currentSpace = space,
                    hasMembers = members.size > 1
                )
            )

        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch current space")
            _state.emit(_state.value.copy(error = e.message, loading = false))
        }
    }

    fun popBackStack() {
        navigator.navigateBack()
    }

    fun resetErrorState() {
        _state.value = _state.value.copy(error = null)
    }

}

data class ThreadsScreenState(
    val loading: Boolean = false,
    val currentSpace: SpaceInfo? = null,
    val hasMembers: Boolean = false,
    val threadInfo: List<ThreadInfo> = emptyList(),
    val error: String? = null
)