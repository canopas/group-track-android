package com.canopas.yourspace.ui.flow.pin.enterpin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.canopas.yourspace.data.service.auth.AuthService
import com.canopas.yourspace.data.storage.UserPreferences
import com.canopas.yourspace.data.utils.AppDispatcher
import com.canopas.yourspace.domain.utils.ConnectivityObserver
import com.canopas.yourspace.ui.flow.pin.setpin.PinErrorState
import com.canopas.yourspace.ui.navigation.AppDestinations
import com.canopas.yourspace.ui.navigation.AppNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EnterPinViewModel @Inject constructor(
    private val navigator: AppNavigator,
    private val authService: AuthService,
    private val appDispatcher: AppDispatcher,
    private val userPreferences: UserPreferences,
    private val connectivityObserver: ConnectivityObserver
) : ViewModel() {
    private val _state = MutableStateFlow(EnterPinScreenState())
    val state: StateFlow<EnterPinScreenState> = _state

    init {
        checkInternetConnection()
    }

    fun onPinChanged(newPin: String) {
        _state.value = _state.value.copy(pin = newPin)
        if (newPin.length == 4) {
            _state.value = _state.value.copy(pinError = null)
        }
    }

    private fun validatePin() {
        val pin = state.value.pin
        if (pin.length < 4) {
            _state.value = _state.value.copy(pinError = PinErrorState.LENGTH_ERROR, showLoader = false)
        } else if (pin.length == 4 && !pin.all { it.isDigit() }) {
            _state.value = _state.value.copy(pinError = PinErrorState.CHARACTERS_ERROR, showLoader = false)
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

    fun processPin() = viewModelScope.launch(appDispatcher.MAIN) {
        _state.value = _state.value.copy(showLoader = true)
        val pin = state.value.pin
        validatePin()
        if (state.value.pinError == null) {
            val isPinValid = authService.validatePasskey(passKey = pin)
            if (isPinValid) {
                userPreferences.setOnboardShown(true)
                navigator.navigateTo(
                    AppDestinations.home.path,
                    popUpToRoute = AppDestinations.signIn.path,
                    inclusive = true
                )
            } else {
                _state.value = _state.value.copy(pinError = PinErrorState.INVALID_PIN, showLoader = false)
            }
        }
    }
}

data class EnterPinScreenState(
    val showLoader: Boolean = false,
    val pin: String = "",
    val pinError: PinErrorState? = null,
    val connectivityStatus: ConnectivityObserver.Status = ConnectivityObserver.Status.Available
)
