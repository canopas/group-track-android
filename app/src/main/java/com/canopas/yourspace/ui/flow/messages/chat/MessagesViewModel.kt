package com.canopas.yourspace.ui.flow.messages.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import com.canopas.yourspace.data.models.messages.ApiThread
import com.canopas.yourspace.data.models.messages.ApiThreadMessage
import com.canopas.yourspace.data.models.messages.ThreadInfo
import com.canopas.yourspace.data.models.space.SpaceInfo
import com.canopas.yourspace.data.models.user.UserInfo
import com.canopas.yourspace.data.repository.MessagesRepository
import com.canopas.yourspace.data.repository.SpaceRepository
import com.canopas.yourspace.data.service.auth.AuthService
import com.canopas.yourspace.data.utils.AppDispatcher
import com.canopas.yourspace.ui.flow.home.map.member.LocationHistoryPagingSource
import com.canopas.yourspace.ui.flow.messages.chat.components.MessagesPagingSource
import com.canopas.yourspace.ui.navigation.AppDestinations.ThreadMessages.KEY_THREAD_ID
import com.canopas.yourspace.ui.navigation.AppNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
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

    //For new Thread
    private var threads: List<ApiThread>? = emptyList()
    private var threadId: String = savedStateHandle.get<String>(KEY_THREAD_ID) ?: ""

    private val _state = MutableStateFlow(
        MessagesScreenState(
            currentUserId = authService.currentUser?.id ?: ""
        )
    )
    val state = _state.asStateFlow()
    private var _dataPagingSource: MessagesPagingSource? = null

    val messages = Pager(
        config = PagingConfig(1)
    ) {
        MessagesPagingSource(
            query = messagesRepository.getMessagesQuery(threadId)
        ).also {
            _dataPagingSource = it
        }
    }.flow.cachedIn(viewModelScope)

    init {
        if (threadId.isEmpty()) {
            fetchSpaceInfo()
        } else {
            fetchThreadInfo()
        }

    }

    private fun fetchThreadInfo() = viewModelScope.launch(appDispatcher.IO) {
        if (threadId.isEmpty()) return@launch
        try {
            _state.emit(_state.value.copy(isLoading = true))
            messagesRepository.getThread(threadId).collectLatest { info ->
                if (info == null) navigator.navigateBack()
                val thread = info!!.thread
                val members = info.members
                _state.emit(
                    _state.value.copy(
                        isLoading = false, thread = thread,
                        threadMembers = members, selectedMember = members
                    )
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch thread info")
            _state.emit(_state.value.copy(error = e.message, isLoading = false))
        }
    }


    private fun fetchSpaceInfo() = viewModelScope.launch(appDispatcher.IO) {
        try {
            _state.emit(_state.value.copy(isLoading = true))
            val currentSpace = spaceRepository.getCurrentSpaceInfo() ?: return@launch
            val selectedMember = currentSpace.members

            val spaceId = currentSpace.space.id
            val threads =
                messagesRepository.getThreads(spaceId).firstOrNull() ?: emptyList()
            this@MessagesViewModel.threads = threads

            _state.emit(
                _state.value.copy(
                    currentSpace = currentSpace,
                    isNewThread = true,
                    isLoading = false,
                    selectAll = true,
                    selectedMember = selectedMember
                )
            )
            selectExistingThread()
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
        selectExistingThread()
    }

    fun toggleMemberSelection(user: UserInfo) {
        var previousSelectedMember = _state.value.selectedMember
        if (state.value.selectAll) {
            previousSelectedMember = emptyList()
        }

        val selectedMember =
            if (previousSelectedMember.contains(user)) {
                previousSelectedMember - user
            } else {
                previousSelectedMember + user
            }
        _state.value = _state.value.copy(
            selectedMember = selectedMember.ifEmpty {
                _state.value.currentSpace?.members ?: emptyList()
            },
            selectAll = selectedMember.isEmpty()
        )

        selectExistingThread()
    }

    private fun selectExistingThread() {
        val selectedMember = _state.value.selectedMember
        val selectedMemberIds =
            (selectedMember.map { it.user.id } + _state.value.currentUserId).distinct()
        val thread =
            threads?.firstOrNull { it.member_ids.sorted() == selectedMemberIds.sorted() }

        if (thread != null && state.value.thread?.id == thread.id) {
            return
        }
        if (thread != null) {
            val members = _state.value.currentSpace?.members ?: emptyList()
            _state.value = state.value.copy(
                thread = thread,
                threadMembers = members.filter { member -> thread.member_ids.contains(member.user.id) },
            )
            threadId = thread.id
            _dataPagingSource?.invalidate()
        } else {
            _state.value = state.value.copy(
                thread = null,
                threadMembers = emptyList(),
            )
            threadId = ""
            _dataPagingSource?.invalidate()
        }
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
            if (threadId.isEmpty()) {
                val spaceId = _state.value.currentSpace?.space?.id ?: return@launch
                val members = _state.value.selectedMember.map { it.user.id }.filter { it != userId }
                threadId = messagesRepository.createThread(spaceId, userId, members)
                _state.value = _state.value.copy(
                    threadMembers = _state.value.selectedMember
                )

            }

            messagesRepository.sendMessage(
                message,
                userId,
                threadId,
            )
           // _dataPagingSource?.invalidate()
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
    val thread: ApiThread? = null,
    val threadMembers: List<UserInfo> = emptyList(),
    val selectedMember: List<UserInfo> = emptyList(),
    val selectAll: Boolean = true,
    val error: String? = null,
    val newMessage: String = "",
    val isNewThread: Boolean = false
)
