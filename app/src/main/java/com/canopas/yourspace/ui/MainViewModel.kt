package com.canopas.yourspace.ui

import android.util.Log
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

    private val _initialRoute = MutableStateFlow(AppDestinations.home.path)
    val initialRoute = _initialRoute.asStateFlow()

    init {
        viewModelScope.launch {
            if (userPreferences.isIntroShown()) {
                if (userPreferences.currentUser == null) {
                    _initialRoute.value = AppDestinations.signIn.path
                } else if (!userPreferences.isOnboardShown()) {
                    _initialRoute.value = AppDestinations.onboard.path
                }
            } else {
                _initialRoute.value = AppDestinations.intro.path
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
                Log.e("XXX", "User Session: ${it.user_id}")
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
            clearStack = true
        )
        _sessionExpiredState.emit(false)
    }
}
