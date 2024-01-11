package com.canopas.catchme.ui.flow.auth.methods

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.canopas.catchme.data.service.auth.AuthService
import com.canopas.catchme.data.service.auth.FirebaseAuthService
import com.canopas.catchme.data.utils.AppDispatcher
import com.canopas.catchme.ui.navigation.AppDestinations
import com.canopas.catchme.ui.navigation.AppNavigator
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
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
    private val appDispatcher: AppDispatcher
) : ViewModel() {

    private val _state = MutableStateFlow(SignInMethodScreenState())
    val state: StateFlow<SignInMethodScreenState> = _state

    fun signInWithPhone() = viewModelScope.launch {
        navigator.navigateTo(AppDestinations.phoneSignIn.path)
    }

    fun proceedGoogleSignIn(account: GoogleSignInAccount) =
        viewModelScope.launch(appDispatcher.IO) {
            _state.emit(_state.value.copy(showGoogleLoading = true))
            try {
                val firebaseToken = firebaseAuth.signInWithGoogleAuthCredential(account.idToken)
                authService.verifiedGoogleLogin(firebaseToken, account)
                navigateToHome()
                _state.emit(_state.value.copy(showGoogleLoading = false))
            } catch (e: Exception) {
                Timber.e(e, "Failed to sign in with google")
                _state.emit(
                    _state.value.copy(
                        showGoogleLoading = false,
                        error = "Failed to sign in with google"
                    )
                )
            }
        }

    fun navigateToHome() {
        navigator.navigateTo(
            AppDestinations.home.path,
            popUpToRoute = AppDestinations.intro.path,
            inclusive = true
        )
    }

    fun resetErrorState() {
        _state.value = _state.value.copy(error = null)
    }
}

data class SignInMethodScreenState(
    val showGoogleLoading: Boolean = false,
    val error: String? = null
)

