package com.canopas.yourspace.data.service.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class FirebaseAuthService @Inject constructor(
    private val firebaseAuth: FirebaseAuth
) {
    val currentUserUid get() = firebaseAuth.currentUser?.uid

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
