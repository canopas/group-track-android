package com.canopas.yourspace.ui.flow.messages.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.canopas.yourspace.data.models.messages.ApiThread
import com.canopas.yourspace.data.models.messages.ApiThreadMessage
import com.canopas.yourspace.data.models.space.SpaceInfo
import com.canopas.yourspace.data.models.user.UserInfo
import com.canopas.yourspace.data.repository.MessagesRepository
import com.canopas.yourspace.data.repository.SpaceRepository
import com.canopas.yourspace.data.service.auth.AuthService
import com.canopas.yourspace.data.utils.AppDispatcher
import com.canopas.yourspace.ui.navigation.AppDestinations.ThreadMessages.KEY_THREAD_ID
import com.canopas.yourspace.ui.navigation.AppNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MessagesViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val navigator: AppNavigator,
    private val authService: AuthService,
    private val messagesRepository: MessagesRepository,
    private val spaceRepository: SpaceRepository,
    private val appDispatcher: AppDispatcher
) : ViewModel() {

    private val _state = MutableStateFlow(
        MessagesScreenState(
            threadId = savedStateHandle.get<String>(KEY_THREAD_ID) ?: "",
            currentUserId = authService.currentUser?.id ?: ""
        )
    )
    val state = _state.asStateFlow()

    init {

        fetchSpaceInfo()
        listenThreadMessages()
    }

    private fun listenThreadMessages() {
        viewModelScope.launch(appDispatcher.IO) {
            val threadId = _state.value.threadId
            if (threadId.isEmpty()) return@launch
            messagesRepository.getMessages(threadId).collectLatest { messages ->
                _state.emit(_state.value.copy(messages = messages.sortedByDescending { it.created_at }))
            }
        }
    }

    private fun fetchSpaceInfo() = viewModelScope.launch(appDispatcher.IO) {
        try {
            _state.emit(_state.value.copy(isLoading = true))
            val currentSpace = spaceRepository.getCurrentSpaceInfo() ?: return@launch
            val selectedMember = currentSpace.members
            _state.emit(
                _state.value.copy(
                    currentSpace = currentSpace,
                    isNewThread = state.value.threadId.trim().isEmpty(),
                    isLoading = false,
                    selectAll = true,
                    selectedMember = selectedMember
                )
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch space info")
            _state.emit(_state.value.copy(error = e.message, isLoading = false))
        }
    }


    fun popBackStack() {
        navigator.navigateBack()
    }

    fun resetErrorState() {
        _state.value = _state.value.copy(error = null)
    }

    fun selectAllMember() {
        _state.value = _state.value.copy(
            selectedMember = _state.value.currentSpace?.members ?: emptyList(),
            selectAll = true
        )
    }

    fun toggleMemberSelection(user: UserInfo) {
        var previousSelectedMember = _state.value.selectedMember
        if (state.value.selectAll) {
            previousSelectedMember = emptyList()
        }

        val selectedMember =
            if (previousSelectedMember.contains(user)) {
                previousSelectedMember.filter { it.user.id != user.user.id }
            } else {
                previousSelectedMember.plus(user)
            }

        _state.value = _state.value.copy(
            selectedMember = selectedMember.ifEmpty {
                _state.value.currentSpace?.members ?: emptyList()
            },
            selectAll = selectedMember.isEmpty()
        )
    }

    fun onMessageChanged(message: String) {
        _state.value = _state.value.copy(newMessage = message)
    }

    fun sendNewMessage() = viewModelScope.launch(appDispatcher.IO) {
        val message = _state.value.newMessage.trim()
        if (message.isEmpty()) return@launch
        val userId = authService.currentUser?.id ?: return@launch
        try {
            _state.emit(_state.value.copy(newMessage = "", isNewThread = false))
            var threadId = _state.value.threadId
            if (threadId.isEmpty()) {
                val members = _state.value.selectedMember.map { it.user.id }.filter { it != userId }
                val spaceId = _state.value.currentSpace?.space?.id ?: return@launch
                threadId = messagesRepository.createThread(spaceId, userId, members)
                _state.value = _state.value.copy(threadId = threadId, threadMembers = _state.value.selectedMember)
                listenThreadMessages()
            }

            messagesRepository.sendMessage(
                message,
                userId,
                threadId,
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to send message")
            _state.emit(_state.value.copy(error = e.message))
        }
    }

}

data class MessagesScreenState(
    val isLoading: Boolean = false,
    val currentSpace: SpaceInfo? = null,
    val currentUserId: String = "",
    val threadId: String = "",
    val thread: ApiThread? = null,
    val threadMembers: List<UserInfo> = emptyList(),
    val selectedMember: List<UserInfo> = emptyList(),
    val selectAll: Boolean = true,
    val messages: List<ApiThreadMessage> = emptyList(),
    val error: String? = null,
    val newMessage: String = "",
    val isNewThread: Boolean = false
)
