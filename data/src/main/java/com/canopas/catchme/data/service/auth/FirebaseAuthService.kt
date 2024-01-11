package com.canopas.catchme.data.service.auth

import android.app.Activity
import android.content.Context
import com.google.firebase.BuildConfig
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class FirebaseAuthService @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) {

    fun verifyPhoneNumber(
        context: Context,
        phoneNumber: String
    ): Flow<PhoneAuthState> = callbackFlow {
        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                trySend(PhoneAuthState.VerificationCompleted(credential))
            }

            override fun onVerificationFailed(e: FirebaseException) {
                trySend(PhoneAuthState.VerificationFailed(e))
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                trySend(PhoneAuthState.CodeSent(verificationId))
            }
        }
        if (BuildConfig.DEBUG) {
            firebaseAuth.firebaseAuthSettings.forceRecaptchaFlowForTesting(true)
        }

        val options = PhoneAuthOptions.newBuilder(firebaseAuth)
            .setPhoneNumber(phoneNumber) // Phone number to verify
            .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
            .setActivity(context as Activity) // Activity (for callback binding)
            .setCallbacks(callbacks) // OnVerificationStateChangedCallbacks
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
        awaitClose { channel.close() }
    }

    suspend fun signInWithPhoneAuthCredential(
        credential: PhoneAuthCredential
    ): String {
        val userCredential = firebaseAuth.signInWithCredential(credential).await()
        return userCredential.user?.getIdToken(true)?.await()?.token ?: ""
    }

    suspend fun signInWithPhoneAuthCredential(
        verificationId: String,
        smsCode: String
    ): String {
        val credential = PhoneAuthProvider.getCredential(verificationId, smsCode)
        val userCredential = firebaseAuth.signInWithCredential(credential).await()
        return userCredential.user?.getIdToken(true)?.await()?.token ?: ""
    }

    suspend fun signInWithGoogleAuthCredential(
        idToken: String?
    ): String {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val result = firebaseAuth.signInWithCredential(credential).await()
        return result.user?.getIdToken(true)?.await()?.token ?: ""
    }
}

sealed class PhoneAuthState {
    data class VerificationCompleted(val credential: PhoneAuthCredential) : PhoneAuthState()
    data class VerificationFailed(val e: Exception) : PhoneAuthState()
    data class CodeSent(val verificationId: String) : PhoneAuthState()
}
