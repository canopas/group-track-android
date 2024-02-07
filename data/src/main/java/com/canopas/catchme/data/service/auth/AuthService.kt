package com.canopas.catchme.data.service.auth

import com.canopas.catchme.data.models.user.ApiUser
import com.canopas.catchme.data.models.user.ApiUserSession
import com.canopas.catchme.data.models.user.LOGIN_TYPE_GOOGLE
import com.canopas.catchme.data.models.user.LOGIN_TYPE_PHONE
import com.canopas.catchme.data.service.location.ApiLocationService
import com.canopas.catchme.data.storage.UserPreferences
import com.canopas.catchme.data.utils.Device
import com.canopas.catchme.data.utils.FirestoreConst.FIRESTORE_COLLECTION_USERS
import com.canopas.catchme.data.utils.FirestoreConst.FIRESTORE_COLLECTION_USER_SESSIONS
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QueryDocumentSnapshot
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthService @Inject constructor(
    db: FirebaseFirestore,
    private val device: Device,
    private val userPreferences: UserPreferences,
    private val locationService: ApiLocationService
) {
    private val userRef = db.collection(FIRESTORE_COLLECTION_USERS)
    private val sessionRef = db.collection(FIRESTORE_COLLECTION_USER_SESSIONS)

    suspend fun verifiedPhoneLogin(firebaseToken: String?, phoneNumber: String): Boolean {
        val snapshot = userRef.whereEqualTo("phone", phoneNumber).get().await().firstOrNull()
        return processLogin(firebaseToken, null, phoneNumber, snapshot)
    }

    suspend fun verifiedGoogleLogin(firebaseToken: String?, account: GoogleSignInAccount): Boolean {
        val snapshot = userRef.whereEqualTo("email", account.email).get().await().firstOrNull()
        return processLogin(firebaseToken, account, null, snapshot)
    }

    private suspend fun processLogin(
        firebaseToken: String?,
        account: GoogleSignInAccount? = null,
        phoneNumber: String? = null,
        snapshot: QueryDocumentSnapshot?
    ): Boolean {
        val isNewUser = snapshot?.data == null
        if (isNewUser) {
            val userDocRef = userRef.document()
            val userId = userDocRef.id
            val user = ApiUser(
                id = userId,
                email = account?.email,
                phone = phoneNumber,
                auth_type = if (account != null) LOGIN_TYPE_GOOGLE else LOGIN_TYPE_PHONE,
                first_name = account?.givenName,
                last_name = account?.familyName,
                provider_firebase_id_token = firebaseToken,
                profile_image = account?.photoUrl?.toString()
            )
            val sessionDocRef = sessionRef.document()
            val session = ApiUserSession(
                id = sessionDocRef.id,
                user_id = userId,
                device_id = device.getId(),
                device_name = device.deviceName(),
                session_active = true,
                app_version = device.versionCode,
                battery_status = null
            )

            userDocRef.set(user).await()
            sessionDocRef.set(session).await()
            saveUser(user, session)
            locationService.saveLastKnownLocation(user.id)
        } else {
            val docId = snapshot!!.id
            val sessionDocRef = sessionRef.document()

            val session = ApiUserSession(
                id = sessionDocRef.id,
                user_id = docId,
                device_id = device.getId(),
                device_name = device.deviceName(),
                session_active = true,
                app_version = device.versionCode,
                battery_status = null
            )
            sessionDocRef.set(session).await()
            val currentUser = snapshot.toObject(ApiUser::class.java)
            saveUser(currentUser, session)
        }

        return isNewUser
    }

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
        userRef.document(user.id).set(user).await()
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
        userRef.document(currentUser.id).delete().await()
        sessionRef.whereEqualTo("user_id", currentUser.id).get().await().documents.forEach {
            it.reference.delete().await()
        }
        signOut()
    }

    suspend fun getUser(): ApiUser? =
        userRef.document(currentUser?.id ?: "").get().await().toObject(ApiUser::class.java)
}
