package com.canopas.catchme.data.service.user

import com.canopas.catchme.data.models.auth.ApiUser
import com.canopas.catchme.data.models.auth.ApiUserSession
import com.canopas.catchme.data.storage.UserPreferences
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserService @Inject constructor(
    private val userPreferences: UserPreferences,
    private val db: FirebaseFirestore

) {
    private val userRef = db.collection("users")

    var currentUser: ApiUser?
        get() {
            return userPreferences.currentUser
        }
        private set(newUser) {
            userPreferences.currentUser = newUser
        }

    var currentUserSession: ApiUserSession?
        get() {
            return userPreferences.currentUserSession
        }
        private set(newSession) {
            userPreferences.currentUserSession = newSession
        }

    fun saveUser(user: ApiUser, session: ApiUserSession) {
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
        userRef.document(user.id).set(user).await()
        currentUser = user
    }
}
