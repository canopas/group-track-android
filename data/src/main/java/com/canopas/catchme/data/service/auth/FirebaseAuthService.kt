package com.canopas.catchme.data.service.auth

import android.app.Activity
import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.firebase.BuildConfig
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class FirebaseAuthService @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
) {

    fun verifyPhoneNumber(
        context: Context,
        phoneNumber: String,
        callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks
    ) {
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
    }

    fun signInWithPhoneAuthCredential(
        credential: PhoneAuthCredential
    ): Task<AuthResult> {
        return firebaseAuth.signInWithCredential(credential)
    }

    fun signInWithPhoneAuthCredential(
        verificationId: String,
        smsCode: String
    ): Task<AuthResult> {
        val credential = PhoneAuthProvider.getCredential(verificationId, smsCode)
        return firebaseAuth.signInWithCredential(credential)
    }


    fun signInWithGoogleAuthCredential(
        idToken: String?,
    ): Task<AuthResult> {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        return firebaseAuth.signInWithCredential(credential)
    }

}