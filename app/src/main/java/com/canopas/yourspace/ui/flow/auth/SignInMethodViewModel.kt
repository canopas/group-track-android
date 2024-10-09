package com.canopas.yourspace.ui.flow.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.canopas.yourspace.data.service.auth.AuthService
import com.canopas.yourspace.data.service.auth.FirebaseAuthService
import com.canopas.yourspace.data.storage.UserPreferences
import com.canopas.yourspace.data.utils.AppDispatcher
import com.canopas.yourspace.ui.navigation.AppDestinations
import com.canopas.yourspace.ui.navigation.AppNavigator
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.auth.AuthResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _state = MutableStateFlow(SignInMethodScreenState())
    val state: StateFlow<SignInMethodScreenState> = _state

    fun proceedGoogleSignIn(account: GoogleSignInAccount) =
        viewModelScope.launch(appDispatcher.IO) {
            _state.emit(_state.value.copy(showGoogleLoading = true))
            try {
                val firebaseToken = firebaseAuth.signInWithGoogleAuthCredential(account.idToken)
                val isNewUser = authService.verifiedGoogleLogin(
                    firebaseAuth.currentUserUid,
                    firebaseToken,
                    account
                )
                onSignUp(isNewUser)
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
                val isNewUser = authService.verifiedAppleLogin(
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
                onSignUp(isNewUser)
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

    private fun onSignUp(isNewUser: Boolean) = viewModelScope.launch(appDispatcher.MAIN) {
        if (isNewUser) {
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
    }
}

data class SignInMethodScreenState(
    val showGoogleLoading: Boolean = false,
    val showAppleLoading: Boolean = false,
    val error: Exception? = null
)
