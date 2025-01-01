package com.canopas.yourspace.ui.flow.pin.setpin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.canopas.yourspace.data.repository.SpaceRepository
import com.canopas.yourspace.data.service.auth.AuthService
import com.canopas.yourspace.data.storage.UserPreferences
import com.canopas.yourspace.data.utils.AppDispatcher
import com.canopas.yourspace.domain.utils.ConnectivityObserver
import com.canopas.yourspace.ui.navigation.AppDestinations
import com.canopas.yourspace.ui.navigation.AppNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SetPinViewModel @Inject constructor(
    private val navigator: AppNavigator,
    private val authService: AuthService,
    private val appDispatcher: AppDispatcher,
    private val spaceRepository: SpaceRepository,
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
            _state.value = _state.value.copy(pinError = "")
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

    fun processPin(lengthError: String) = viewModelScope.launch(appDispatcher.MAIN) {
        _state.value = _state.value.copy(showLoader = true)
        val pin = state.value.pin
        if (pin.length < 4) {
            _state.value = _state.value.copy(pinError = lengthError)
            return@launch
        }
        if (pin.length == 4) {
            authService.generateAndSaveUserKeys(passKey = pin)
            val userId = authService.getUser()?.id
            val userHasSpaces = userId?.let {
                spaceRepository.getUserSpaces(it).firstOrNull()?.isNotEmpty() ?: false
            }
            if (userHasSpaces == false) {
                navigator.navigateTo(
                    AppDestinations.onboard.path,
                    popUpToRoute = AppDestinations.signIn.path,
                    inclusive = true
                )
            } else {
                userPreferences.setOnboardShown(true)
                navigator.navigateTo(
                    AppDestinations.home.path,
                    popUpToRoute = AppDestinations.signIn.path,
                    inclusive = true
                )
            }
        } else {
            _state.value = _state.value.copy(pinError = "Pin must be 4 characters")
        }
    }
}

data class EnterPinScreenState(
    val showLoader: Boolean = false,
    val pin: String = "",
    val confirmPin: String = "",
    val pinError: String? = null,
    val connectivityStatus: ConnectivityObserver.Status = ConnectivityObserver.Status.Available,
    val error: Exception? = null
)
