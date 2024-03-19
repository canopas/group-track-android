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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

private const val MESSAGE_PAGE_LIMIT = 20

@HiltViewModel
class MessagesViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val navigator: AppNavigator,
    private val authService: AuthService,
    private val messagesRepository: MessagesRepository,
    private val spaceRepository: SpaceRepository,
    private val appDispatcher: AppDispatcher
) : ViewModel() {

    // For new Thread
    private var threads: List<ApiThread>? = emptyList()
    private var threadId: String = savedStateHandle.get<String>(KEY_THREAD_ID) ?: ""
    private var messagesJob: Job? = null
    private var threadJob: Job? = null

    private val _state = MutableStateFlow(
        MessagesScreenState(
            currentUserId = authService.currentUser?.id ?: ""
        )
    )
    val state = _state.asStateFlow()

    private var hasMoreData = true
    private var loadingData = false

    init {
        if (threadId.isEmpty()) {
            fetchSpaceForNewThread()
        } else {
            fetchThread(threadId)
        }
    }

    private fun listenMessages() {
        if (threadId.isEmpty()) return
        messagesJob?.cancel()
        messagesJob = viewModelScope.launch(appDispatcher.IO) {
            messagesRepository.getLatestMessages(threadId, MESSAGE_PAGE_LIMIT)
                .catch { e ->
                    Timber.e(e, "Error listening to messages")
                    _state.emit(state.value.copy(error = e.message))
                }
                .collectLatest { messages ->
                    val newMessages = state.value.messages + messages
                    _state.value =
                        state.value.copy(
                            messages = newMessages.distinctBy { it.id }
                                .sortedByDescending { it.created_at }
                        )
                    markMessagesAsSeen()
                }
        }
    }


    private fun markMessagesAsSeen() = viewModelScope.launch(appDispatcher.IO) {
        try {
            val unreadMessages = state.value.messages.distinct()
                .filter { !it.seen_by.contains(state.value.currentUserId) }
                .map { it.id }
            if (unreadMessages.isNotEmpty()) {
                messagesRepository.markMessagesAsSeen(
                    threadId,
                    unreadMessages,
                    state.value.currentUserId
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "Error marking messages as seen")
        }
    }

    fun loadMore() = viewModelScope.launch(appDispatcher.IO) {
        if (loadingData || !hasMoreData || threadId.isEmpty()) return@launch
        loadingData = true
        _state.emit(
            state.value.copy(
                loadingMessages = state.value.messages.isEmpty(),
                append = true
            )
        )
        try {
            val from =
                if (state.value.messages.isEmpty()) System.currentTimeMillis() else state.value.messages.minBy { it.created_at }.created_at
            val newMessages = messagesRepository.getMessages(
                threadId,
                from = from,
                limit = MESSAGE_PAGE_LIMIT
            ).first()

            hasMoreData = newMessages.isNotEmpty()

            _state.emit(
                state.value.copy(
                    messages = (state.value.messages + newMessages).distinctBy { it.id }
                        .sortedByDescending { it.created_at },
                    append = false,
                    loadingMessages = false,
                    error = null
                )
            )
            markMessagesAsSeen()
            loadingData = false
        } catch (e: Exception) {
            Timber.e(e, "Error loading messages")
            _state.emit(
                state.value.copy(
                    error = e.message,
                    append = false,
                    loadingMessages = false
                )
            )
        }
    }

    private fun fetchThread(threadId: String) {
        threadJob?.cancel()
        threadJob = viewModelScope.launch(appDispatcher.IO) {
            _state.emit(_state.value.copy(loading = state.value.thread == null))
            messagesRepository.getThread(threadId)
                .catch { e ->
                    Timber.e(e, "Failed to fetch thread info")
                    _state.emit(_state.value.copy(error = e.message, loading = false))
                }
                .collectLatest { info ->
                    if (info == null) {
                        navigator.navigateBack()
                        return@collectLatest
                    }
                    val thread = info.thread
                    val members = info.members

                    _state.emit(
                        _state.value.copy(
                            loading = false,
                            thread = thread,
                            threadMembers = members,
                            selectedMember = members.filter { it.user.id != state.value.currentUserId }
                        )
                    )
                    if (messagesJob == null) listenMessages()
                }
        }
    }

    private fun fetchSpaceForNewThread() = viewModelScope.launch(appDispatcher.IO) {
        try {
            _state.emit(_state.value.copy(loading = true))
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
                    loading = false,
                    selectAll = true,
                    selectedMember = selectedMember.filter { it.user.id != state.value.currentUserId }
                )
            )
            selectExistingThread()
        } catch (e: Exception) {
            Timber.e(e, "Failed to fetch space info")
            _state.emit(_state.value.copy(error = e.message, loading = false))
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
            selectedMember = _state.value.currentSpace?.members
                ?.filter { it.user.id != state.value.currentUserId }
                ?: emptyList(),
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
        if (selectedMember.isEmpty()) {
            selectAllMember()
        } else {
            _state.value = _state.value.copy(
                selectedMember = selectedMember,
                selectAll = selectedMember.isEmpty()
            )
            selectExistingThread()
        }
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
                messages = emptyList(),
                threadMembers = members.filter { member -> thread.member_ids.contains(member.user.id) }
            )
            threadId = thread.id
            fetchThread(threadId)
            listenMessages()
        } else {
            _state.value = state.value.copy(
                thread = null,
                messages = emptyList(),
                threadMembers = emptyList()
            )
            threadId = ""
            threadJob?.cancel()
            messagesJob?.cancel()
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
                val members =
                    _state.value.selectedMember.map { it.user.id }.filter { it != userId }
                threadId = messagesRepository.createThread(spaceId, userId, members)
                val threadMembers =
                    state.value.currentSpace?.members?.filter { members.contains(it.user.id) || it.user.id == userId }
                        ?: emptyList()
                _state.emit(_state.value.copy(threadMembers = threadMembers))
                listenMessages()
            }

            messagesRepository.sendMessage(message, userId, threadId)
        } catch (e: Exception) {
            Timber.e(e, "Failed to send message")
            _state.emit(_state.value.copy(error = e.message))
        }
    }
}

data class MessagesScreenState(
    val loading: Boolean = false,
    val loadingMessages: Boolean = false,
    val append: Boolean = false,
    val currentSpace: SpaceInfo? = null,
    val currentUserId: String = "",
    val messages: List<ApiThreadMessage> = emptyList(),
    val thread: ApiThread? = null,
    val threadMembers: List<UserInfo> = emptyList(),
    val selectedMember: List<UserInfo> = emptyList(),
    val selectAll: Boolean = true,
    val error: String? = null,
    val newMessage: String = "",
    val isNewThread: Boolean = false
)
