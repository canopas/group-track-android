package com.canopas.catchme.data.service.auth

import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthService @Inject constructor(

) {

    fun processLogin(firebaseToken: String?, phoneNumber: String? = null) {
        Timber.d("verifiedLogin")

    }

    fun processLogin(firebaseToken: String?, account: GoogleSignInAccount) {
        Timber.d("verifiedLogin")

    }

}