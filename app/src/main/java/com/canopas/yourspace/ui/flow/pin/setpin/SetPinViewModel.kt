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

enum class PinErrorState {
    LENGTH_ERROR,
    CHARACTERS_ERROR,
    INVALID_PIN
}

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
            _state.value = _state.value.copy(pinError = null)
        }
    }

    private fun validatePin() {
        val pin = state.value.pin
        if (pin.length < 4) {
            _state.value = _state.value.copy(pinError = PinErrorState.LENGTH_ERROR, showLoader = false)
        } else if (pin.length == 4 && !pin.all { it.isDigit() }) {
            _state.value = _state.value.copy(pinError = PinErrorState.CHARACTERS_ERROR, showLoader = false)
        } else {
            _state.value = _state.value.copy(pinError = null)
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
            try {
                authService.generateAndSaveUserKeys(passKey = pin)
                val userId = authService.getUser()?.id

                if (userId == null) {
                    _state.value = _state.value.copy(
                        error = IllegalStateException("Failed to get user ID after key generation")
                    )
                    return@launch
                }

                val userSpaces = spaceRepository.getUserSpaces(userId)

                val userHasSpaces =
                    userSpaces.firstOrNull() != null && userSpaces.firstOrNull()?.isNotEmpty() == true
                if (userHasSpaces) {
                    userPreferences.setOnboardShown(true)
                    try {
                        spaceRepository.generateAndDistributeSenderKeysForExistingSpaces(
                            spaceIds = userSpaces.firstOrNull()?.map { it.id } ?: emptyList()
                        )
                    } catch (e: Exception) {
                        _state.value = _state.value.copy(error = e)
                        return@launch
                    }

                    navigator.navigateTo(
                        AppDestinations.home.path,
                        popUpToRoute = AppDestinations.signIn.path,
                        inclusive = true
                    )
                } else {
                    navigator.navigateTo(
                        AppDestinations.onboard.path,
                        popUpToRoute = AppDestinations.signIn.path,
                        inclusive = true
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = e, showLoader = false)
            }
        }
    }
}

data class EnterPinScreenState(
    val showLoader: Boolean = false,
    val pin: String = "",
    val pinError: PinErrorState? = null,
    val connectivityStatus: ConnectivityObserver.Status = ConnectivityObserver.Status.Available,
    val error: Throwable? = null
)
