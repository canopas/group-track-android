package com.canopas.catchme.ui.flow.auth.verification

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.canopas.catchme.data.service.auth.AuthService
import com.canopas.catchme.data.service.auth.FirebaseAuthService
import com.canopas.catchme.data.service.auth.PhoneAuthState
import com.canopas.catchme.data.utils.AppDispatcher
import com.canopas.catchme.ui.navigation.AppDestinations
import com.canopas.catchme.ui.navigation.AppDestinations.OtpVerificationNavigation.KEY_PHONE_NO
import com.canopas.catchme.ui.navigation.AppDestinations.OtpVerificationNavigation.KEY_VERIFICATION_ID
import com.canopas.catchme.ui.navigation.AppNavigator
import com.canopas.catchme.ui.navigation.KEY_RESULT
import com.canopas.catchme.ui.navigation.RESULT_OKAY
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class PhoneVerificationViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val appNavigator: AppNavigator,
    private val firebaseAuth: FirebaseAuthService,
    private val authService: AuthService,
    private val dispatcher: AppDispatcher
) : ViewModel() {

    private val _state = MutableStateFlow(PhoneVerificationState())
    val state: StateFlow<PhoneVerificationState> = _state

    private var firstAutoVerificationComplete: Boolean = false

    init {
        val verificationId = savedStateHandle.get<String>(KEY_VERIFICATION_ID) ?: ""
        val phone = savedStateHandle.get<String>(KEY_PHONE_NO) ?: ""
        _state.value = _state.value.copy(phone = phone, verificationId = verificationId)
    }

    fun popBack() {
        appNavigator.navigateBack()
    }

    fun updateOTP(otp: String) {
        _state.value = state.value.copy(
            otp = otp,
            enableVerify = otp.length == 6
        )

        if (!firstAutoVerificationComplete && otp.length == 6) {
            verifyOTP()
            firstAutoVerificationComplete = true
        }
    }

    fun verifyOTP() = viewModelScope.launch(dispatcher.IO) {
        try {
            _state.tryEmit(_state.value.copy(verifying = true))
            val firebaseIdToken = firebaseAuth.signInWithPhoneAuthCredential(
                _state.value.verificationId,
                _state.value.otp
            )

            authService.verifiedPhoneLogin(firebaseIdToken, _state.value.phone)
            appNavigator.navigateBack(
                route = AppDestinations.signIn.path,
                result = mapOf(KEY_RESULT to RESULT_OKAY)
            )
            _state.tryEmit(_state.value.copy(verifying = false))
        } catch (e: Exception) {
            Timber.e(e, "OTP Verification: Error while verifying OTP.")
            _state.tryEmit(_state.value.copy(verifying = false, error = e.message))
        }
    }

    fun resendCode(context: Context) = viewModelScope.launch(dispatcher.IO) {
        val phone = state.value.phone
        firebaseAuth.verifyPhoneNumber(context, phone)
            .collect { result ->
                when (result) {
                    is PhoneAuthState.VerificationCompleted -> {
                        val firebaseIdToken =
                            firebaseAuth.signInWithPhoneAuthCredential(result.credential)
                        authService.verifiedPhoneLogin(firebaseIdToken, _state.value.phone)
                        appNavigator.navigateBack(
                            route = AppDestinations.signIn.path,
                            result = mapOf(KEY_RESULT to RESULT_OKAY)
                        )
                    }

                    is PhoneAuthState.VerificationFailed -> {
                        Timber.e(result.e, "Unable to resend OTP")
                        _state.tryEmit(
                            _state.value.copy(
                                verifying = false,
                                error = result.e.message
                            )
                        )
                    }

                    is PhoneAuthState.CodeSent -> {
                        _state.tryEmit(
                            _state.value.copy(
                                verifying = false,
                                verificationId = result.verificationId
                            )
                        )
                    }
                }
            }
    }

    fun resetErrorState() {
        _state.value = _state.value.copy(error = null)
    }
}

data class PhoneVerificationState(
    val phone: String = "",
    val otp: String = "",
    val verifying: Boolean = false,
    val enableVerify: Boolean = false,
    val verificationId: String = "",
    val error: String? = null
)
