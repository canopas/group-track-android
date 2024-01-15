package com.canopas.catchme.data.service.auth

import com.canopas.catchme.data.models.auth.ApiUser
import com.canopas.catchme.data.models.auth.ApiUserSession
import com.canopas.catchme.data.models.auth.LOGIN_TYPE_GOOGLE
import com.canopas.catchme.data.models.auth.LOGIN_TYPE_PHONE
import com.canopas.catchme.data.service.user.UserService
import com.canopas.catchme.data.utils.Device
import com.canopas.catchme.data.utils.FirestoreConst
import com.canopas.catchme.data.utils.FirestoreConst.FIRESTORE_COLLECTION_USER_SESSIONS
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QueryDocumentSnapshot
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthService @Inject constructor(
    private val db: FirebaseFirestore,
    private val device: Device,
    private val userService: UserService
) {
    private val userRef = db.collection(FirestoreConst.FIRESTORE_COLLECTION_USERS)
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
            userService.saveUser(user, session)
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
            userService.saveUser(currentUser, session)
        }

        return isNewUser
    }
}
