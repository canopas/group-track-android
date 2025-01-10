package com.canopas.yourspace.ui.flow.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.canopas.yourspace.data.service.auth.AuthService
import com.canopas.yourspace.data.service.auth.FirebaseAuthService
import com.canopas.yourspace.data.storage.UserPreferences
import com.canopas.yourspace.data.utils.AppDispatcher
import com.canopas.yourspace.domain.utils.ConnectivityObserver
import com.canopas.yourspace.ui.navigation.AppDestinations
import com.canopas.yourspace.ui.navigation.AppNavigator
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.auth.AuthResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SignInMethodViewModel @Inject constructor(
    private val navigator: AppNavigator,
    private val firebaseAuth: FirebaseAuthService,
    private val authService: AuthService,
    private val appDispatcher: AppDispatcher,
    private val userPreferences: UserPreferences,
    private val connectivityObserver: ConnectivityObserver
) : ViewModel() {

    private val _state = MutableStateFlow(SignInMethodScreenState())
    val state: StateFlow<SignInMethodScreenState> = _state

    init {
        checkInternetConnection()
    }

    fun proceedGoogleSignIn(account: GoogleSignInAccount) =
        viewModelScope.launch(appDispatcher.IO) {
            _state.emit(_state.value.copy(showGoogleLoading = true))
            try {
                val firebaseToken = firebaseAuth.signInWithGoogleAuthCredential(account.idToken)
                authService.verifiedGoogleLogin(
                    firebaseAuth.currentUserUid,
                    firebaseToken,
                    account
                )
                onSignUp()
                _state.emit(_state.value.copy(showGoogleLoading = false))
            } catch (e: Exception) {
                Timber.e(e, "Failed to sign in with google")
                _state.emit(
                    _state.value.copy(
                        showGoogleLoading = false,
                        error = e
                    )
                )
            }
        }

    fun proceedAppleSignIn(authResult: AuthResult) =
        viewModelScope.launch(appDispatcher.IO) {
            _state.emit(_state.value.copy(showAppleLoading = true))
            try {
                val firebaseToken = authResult.user?.getIdToken(true)?.await()
                authService.verifiedAppleLogin(
                    firebaseAuth.currentUserUid,
                    firebaseToken?.token ?: "",
                    authResult.user ?: run {
                        _state.emit(
                            _state.value.copy(
                                showAppleLoading = false,
                                error = Exception("Failed to sign in with Apple\nUser is null")
                            )
                        )
                        return@launch
                    }
                )
                onSignUp()
                _state.emit(_state.value.copy(showAppleLoading = false))
            } catch (e: Exception) {
                Timber.e(e, "Failed to sign in with Apple")
                _state.emit(
                    _state.value.copy(
                        showAppleLoading = false,
                        error = e
                    )
                )
            }
        }

    fun resetErrorState() {
        _state.value = _state.value.copy(error = null)
    }

    private fun onSignUp() = viewModelScope.launch(appDispatcher.MAIN) {
        val currentUser = authService.currentUser ?: return@launch
        val showSetPinScreen = currentUser.identity_key_public?.toBytes()
            .contentEquals(currentUser.identity_key_private?.toBytes())
        val showEnterPinScreen = !showSetPinScreen && userPreferences.getPasskey()
            .isNullOrEmpty()

        if (showSetPinScreen) {
            navigator.navigateTo(
                AppDestinations.setPin.path,
                popUpToRoute = AppDestinations.signIn.path,
                inclusive = true
            )
        } else if (showEnterPinScreen) {
            navigator.navigateTo(
                AppDestinations.enterPin.path,
                popUpToRoute = AppDestinations.signIn.path,
                inclusive = true
            )
        }
    }

    fun showAppleLoadingState(show: Boolean = true) {
        _state.value = _state.value.copy(showAppleLoading = show)
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
}

data class SignInMethodScreenState(
    val showGoogleLoading: Boolean = false,
    val showAppleLoading: Boolean = false,
    val connectivityStatus: ConnectivityObserver.Status = ConnectivityObserver.Status.Available,
    val error: Exception? = null
)
