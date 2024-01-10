package com.canopas.catchme.ui.flow.auth.methods

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.canopas.catchme.data.service.auth.AuthService
import com.canopas.catchme.data.service.auth.FirebaseAuthService
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
    private val authService: AuthService
) : ViewModel() {

    private val _state = MutableStateFlow(SignInMethodScreenState())
    val state: StateFlow<SignInMethodScreenState> = _state

    fun signInWithPhone() = viewModelScope.launch {
        navigator.navigateTo(AppDestinations.phoneSignIn.path)
    }

    fun proceedGoogleSignIn(account: GoogleSignInAccount) =
        viewModelScope.launch(Dispatchers.IO) {
            _state.emit(_state.value.copy(showGoogleLoading = true))
            try {
                val result = firebaseAuth.signInWithGoogleAuthCredential(account.idToken).await()
                val firebaseToken = result.user?.getIdToken(true)?.await()?.token ?: ""
                authService.verifiedGoogleLogin(firebaseToken, account)
                _state.emit(_state.value.copy(socialSignInCompleted = false))
            } catch (e: Exception) {
                Timber.e(e, "Failed to sign in with google")
                _state.emit(_state.value.copy(showGoogleLoading = false, error = e.message))
            }
        }
}

data class SignInMethodScreenState(
    val showGoogleLoading: Boolean = false,
    val socialSignInCompleted: Boolean = false,
    val error: String? = null
)

