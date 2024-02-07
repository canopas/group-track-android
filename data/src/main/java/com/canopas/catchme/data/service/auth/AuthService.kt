package com.canopas.catchme.data.service.auth

import com.canopas.catchme.data.models.user.ApiUser
import com.canopas.catchme.data.models.user.ApiUserSession
import com.canopas.catchme.data.service.user.ApiUserService
import com.canopas.catchme.data.storage.UserPreferences
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthService @Inject constructor(
    private val userPreferences: UserPreferences,
    private val apiUserService: ApiUserService,
) {
    suspend fun verifiedPhoneLogin(
        uid: String?,
        firebaseToken: String?,
        phoneNumber: String
    ): Boolean {
        return processLogin(uid, firebaseToken, null, phoneNumber)
    }

    suspend fun verifiedGoogleLogin(
        uid: String?,
        firebaseToken: String?,
        account: GoogleSignInAccount
    ): Boolean {
        return processLogin(uid, firebaseToken, account, null)
    }

    private suspend fun processLogin(
        uid: String?,
        firebaseToken: String?,
        account: GoogleSignInAccount? = null,
        phoneNumber: String? = null,
    ): Boolean {

        val (isNewUser, user, session) =
            apiUserService.saveUser(uid, firebaseToken, account, phoneNumber)
        saveUser(user, session)
        return isNewUser
    }

    var currentUser: ApiUser?
        get() {
            return userPreferences.currentUser
        }
        private set(newUser) {
            userPreferences.currentUser = newUser
        }

    private var currentUserSession: ApiUserSession?
        get() {
            return userPreferences.currentUserSession
        }
        private set(newSession) {
            userPreferences.currentUserSession = newSession
        }

    private fun saveUser(user: ApiUser, session: ApiUserSession) {
        currentUser = user
        currentUserSession = session
    }

    fun saveUser(user: ApiUser) {
        currentUser = user
    }

    fun saveUserSession(session: ApiUserSession) {
        currentUserSession = session
    }

    suspend fun updateUser(user: ApiUser) {
        apiUserService.updateUser(user)
        currentUser = user
    }

    fun signOut() {
        currentUser = null
        currentUserSession = null
        userPreferences.setOnboardShown(false)
        userPreferences.currentSpace = ""
    }

    suspend fun deleteAccount() {
        val currentUser = currentUser ?: return
        apiUserService.deleteUser(currentUser.id)
        signOut()
    }

    suspend fun getUser(): ApiUser? = apiUserService.getUser(currentUser?.id ?: "")
}
