package com.canopas.catchme.data.service.auth

import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthService @Inject constructor(

) {

    fun verifiedLogin(firebaseToken: String?, phoneNumber: String? = null, email: String? = null) {
        Timber.d("verifiedLogin")
    }

}