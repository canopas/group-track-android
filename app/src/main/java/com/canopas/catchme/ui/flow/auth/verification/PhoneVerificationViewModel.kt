package com.canopas.catchme.ui.flow.auth.verification

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.canopas.catchme.data.service.auth.AuthService
import com.canopas.catchme.data.service.auth.FirebaseAuthService
import com.canopas.catchme.ui.navigation.AppDestinations
import com.canopas.catchme.ui.navigation.AppDestinations.OtpVerificationNavigation.KEY_PHONE_NO
import com.canopas.catchme.ui.navigation.AppDestinations.OtpVerificationNavigation.KEY_VERIFICATION_ID
import com.canopas.catchme.ui.navigation.AppNavigator
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
class PhoneVerificationViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val appNavigator: AppNavigator,
    private val firebaseAuth: FirebaseAuthService,
    private val authService: AuthService
) : ViewModel() {

    private val _state = MutableStateFlow(PhoneVerificationState())
    val state: StateFlow<PhoneVerificationState> = _state

    private var firstAutoVerificationComplete: Boolean = false

    init {
        val verificationId = savedStateHandle.get<String>(KEY_VERIFICATION_ID) ?: ""
        val phone = savedStateHandle.get<String>(KEY_PHONE_NO) ?: ""
        _state.value = _state.value.copy(phone = phone, verificationId = verificationId)
    }

    fun popBack() = viewModelScope.launch {
        appNavigator.navigateBack()
    }

    fun updateOTP(otp: String) {
        _state.value = state.value.copy(
            otp = otp,
            enableVerify = otp.length == 6,
        )

        if (!firstAutoVerificationComplete && otp.length == 6) {
            verifyOTP()
            firstAutoVerificationComplete = true
        }
    }

    fun verifyOTP() = viewModelScope.launch(Dispatchers.IO) {
        try {
            _state.tryEmit(_state.value.copy(verifying = true))
            val credential = firebaseAuth.signInWithPhoneAuthCredential(
                _state.value.verificationId,
                _state.value.otp
            ).await()
            val firebaseIdToken = credential.user?.getIdToken(true)?.await()?.token ?: ""
            authService.verifiedPhoneLogin(firebaseIdToken, _state.value.phone)
            _state.tryEmit(_state.value.copy(verificationComplete = true))
        } catch (e: Exception) {
            Timber.e(e, "OTP Verification: Error while verifying OTP.")
            _state.tryEmit(_state.value.copy(verifying = false, error = e.message))
        }
    }

    fun resendCode(context: Context) {
        val phone = state.value.phone
        firebaseAuth.verifyPhoneNumber(context,
            phone,
            object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    viewModelScope.launch(Dispatchers.IO) {
                        val userCredential =
                            firebaseAuth.signInWithPhoneAuthCredential(credential).await()
                        val firebaseIdToken =
                            userCredential.user?.getIdToken(true)?.await()?.token ?: ""
                        authService.verifiedPhoneLogin(firebaseIdToken, _state.value.phone)
                        _state.tryEmit(_state.value.copy(verificationComplete = true))
                    }
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    Timber.e(e, "Unable to resend OTP")
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
                            AppDestinations.OtpVerificationNavigation.otpVerification(
                                verificationId = verificationId,
                                phoneNo = phone
                            ).path
                        )
                    }
                }
            })
    }
}


data class PhoneVerificationState(
    val phone: String = "",
    val otp: String = "",
    val verifying: Boolean = false,
    val enableVerify: Boolean = false,
    val verificationComplete: Boolean = false,
    val verificationId: String = "",
    val error: String? = null
)