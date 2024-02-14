package com.canopas.yourspace.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.canopas.yourspace.data.models.user.ApiUserSession
import com.canopas.yourspace.data.service.auth.AuthService
import com.canopas.yourspace.data.service.user.ApiUserService
import com.canopas.yourspace.data.storage.UserPreferences
import com.canopas.yourspace.data.utils.AppDispatcher
import com.canopas.yourspace.ui.navigation.AppDestinations
import com.canopas.yourspace.ui.navigation.AppNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val userPreferences: UserPreferences,
    private val navigator: AppNavigator,
    private val appDispatcher: AppDispatcher,
    private val apiUserService: ApiUserService,
    private val authService: AuthService
) : ViewModel() {

    val navActions = navigator.navigationChannel
    private var listenSessionJob: Job? = null

    private val _sessionExpiredState = MutableStateFlow(false)
    val sessionExpiredState = _sessionExpiredState.asStateFlow()

    init {
        viewModelScope.launch {
            if (userPreferences.isIntroShown()) {
                if (userPreferences.currentUser == null) {
                    navigator.navigateTo(
                        AppDestinations.signIn.path,
                        popUpToRoute = AppDestinations.intro.path,
                        inclusive = true
                    )
                } else if (!userPreferences.isOnboardShown()) {
                    navigator.navigateTo(
                        AppDestinations.onboard.path,
                        popUpToRoute = AppDestinations.intro.path,
                        inclusive = true
                    )
                }
            } else {
                navigator.navigateTo(
                    AppDestinations.intro.path,
                    popUpToRoute = AppDestinations.home.path,
                    inclusive = true
                )
            }
            userPreferences.currentUserSessionState.collectLatest { userSession ->
                listenSessionJob?.cancel()
                listenUserSession(userSession)
            }
        }
    }

    private suspend fun listenUserSession(userSession: ApiUserSession?) {
        listenSessionJob = viewModelScope.launch(appDispatcher.IO) {
            userSession?.let {
                apiUserService.getUserSessionFlow(it.user_id, it.id).collectLatest { session ->
                    if (session != null && !session.session_active) {
                        _sessionExpiredState.emit(true)
                    }
                }
            }
        }
    }

    fun signOut() = viewModelScope.launch {
        authService.signOut()
        navigator.navigateTo(
            AppDestinations.signIn.path,
            AppDestinations.home.path,
            true
        )
        _sessionExpiredState.emit(false)
    }
}
