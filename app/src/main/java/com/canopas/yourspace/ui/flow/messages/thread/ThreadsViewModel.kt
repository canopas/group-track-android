package com.canopas.yourspace.ui.flow.messages.thread

import androidx.lifecycle.ViewModel
import com.canopas.yourspace.data.models.messages.ThreadInfo
import com.canopas.yourspace.data.models.space.ApiSpace
import com.canopas.yourspace.data.repository.SpaceRepository
import com.canopas.yourspace.data.service.auth.AuthService
import com.canopas.yourspace.data.service.messages.ApiMessagesService
import com.canopas.yourspace.ui.navigation.AppNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class ThreadsViewModel @Inject constructor(
    private val messagesService: ApiMessagesService,
    private val authService: AuthService,
    private val spaceRepository: SpaceRepository,
    private val navigator: AppNavigator
) : ViewModel() {

    private val _state = MutableStateFlow(ThreadsScreenState())
    val state = _state.asStateFlow()

    init {

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
    val currentSpace: ApiSpace? = null,
    val hasMembers: Boolean = false,
    val threadInfo: List<ThreadInfo> = emptyList(),
    val error: String? = null
)