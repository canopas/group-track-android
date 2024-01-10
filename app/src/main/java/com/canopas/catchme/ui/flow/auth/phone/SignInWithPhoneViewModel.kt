package com.canopas.catchme.ui.flow.auth.phone

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.canopas.catchme.data.service.auth.AuthService
import com.canopas.catchme.data.service.auth.FirebaseAuthService
import com.canopas.catchme.ui.navigation.AppDestinations
import com.canopas.catchme.ui.navigation.AppDestinations.OtpVerificationNavigation
import com.canopas.catchme.ui.navigation.AppNavigator
import com.canopas.catchme.ui.navigation.KEY_RESULT
import com.canopas.catchme.ui.navigation.RESULT_OKAY
import com.google.firebase.FirebaseException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SignInWithPhoneViewModel @Inject constructor(
    private val appNavigator: AppNavigator,
    private val fbAuthService: FirebaseAuthService,
    private val authService: AuthService
) : ViewModel() {

    private val _state = MutableStateFlow(SignInWithPhoneState())
    val state: StateFlow<SignInWithPhoneState> = _state

    fun onPhoneChange(phone: String) {
        _state.value = _state.value.copy(phone = phone, enableNext = phone.length > 3)
    }

    fun onCodeChange(code: String) {
        _state.value = _state.value.copy(code = code)
    }

    fun verifyPhoneNumber(context: Context) {
        _state.value = _state.value.copy(verifying = true)

        val phone = _state.value.code + _state.value.phone
        fbAuthService.verifyPhoneNumber(
            context,
            phone,
            object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    viewModelScope.launch(Dispatchers.IO) {
                        val userCredential =
                            fbAuthService.signInWithPhoneAuthCredential(credential).await()
                        val firebaseIdToken =
                            userCredential.user?.getIdToken(true)?.await()?.token ?: ""
                        authService.verifiedPhoneLogin(firebaseIdToken, _state.value.phone)
                        appNavigator.navigateBack(
                            route = AppDestinations.signIn.path,
                            result = mapOf(KEY_RESULT to RESULT_OKAY)
                        )
                        _state.tryEmit(_state.value.copy(verifying = false))
                    }
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    Timber.e(e, "Unable to send OTP verification failed")
                    _state.tryEmit(_state.value.copy(verifying = false, error = e.message))
                }

                override fun onCodeSent(
                    verificationId: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    _state.tryEmit(
                        _state.value.copy(verifying = false, verificationId = verificationId)
                    )

                    viewModelScope.launch {
                        appNavigator.navigateTo(
                            OtpVerificationNavigation.otpVerification(
                                verificationId = verificationId,
                                phoneNo = phone
                            ).path
                        )
                    }
                }
            })
    }

    fun popBack() = viewModelScope.launch {
        appNavigator.navigateBack()
    }

    fun showCountryPicker(show: Boolean = true) {
        _state.value = _state.value.copy(showCountryPicker = show)
    }

    fun resetErrorState() {
        _state.value = _state.value.copy(error = null)
    }
}


data class SignInWithPhoneState(
    val code: String = "",
    val phone: String = "",
    val verifying: Boolean = false,
    val verificationId: String? = null,
    val error: String? = null,
    val enableNext: Boolean = false,
    val showCountryPicker: Boolean = false
)