package com.canopas.catchme.data.service.auth

import com.canopas.catchme.data.models.auth.ApiUser
import com.canopas.catchme.data.models.auth.ApiUserSession
import com.canopas.catchme.data.models.auth.LOGIN_TYPE_GOOGLE
import com.canopas.catchme.data.models.auth.LOGIN_TYPE_PHONE
import com.canopas.catchme.data.utils.Device
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QueryDocumentSnapshot
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthService @Inject constructor(
    private val db: FirebaseFirestore,
    private val device: Device
) {
    val userRef = db.collection("users")
    val sessionRef = db.collection("user-sessions")

    suspend fun verifiedPhoneLogin(firebaseToken: String?, phoneNumber: String) {
        val snapshot = userRef.whereEqualTo("phone", phoneNumber).get().await().firstOrNull()
        processLogin(firebaseToken, null, phoneNumber, snapshot)
    }

    suspend fun verifiedGoogleLogin(firebaseToken: String?, account: GoogleSignInAccount) {
        val snapshot = userRef.whereEqualTo("email", account.email).get().await().firstOrNull()
        processLogin(firebaseToken, account, null, snapshot)
    }

    private suspend fun processLogin(
        firebaseToken: String?,
        account: GoogleSignInAccount? = null,
        phoneNumber: String? = null,
        snapshot: QueryDocumentSnapshot?
    ) {
        Timber.d("verifiedLogin")
        val isNewUser = snapshot?.data == null
        if (isNewUser) {
            val user = ApiUser(
                email = account?.email,
                phone = phoneNumber,
                auth_type = if (account != null) LOGIN_TYPE_GOOGLE else LOGIN_TYPE_PHONE,
                first_name = account?.givenName,
                last_name = account?.familyName,
                provider_firebase_id_token = firebaseToken,
                profile_image = account?.photoUrl?.toString(),
            )
            val docId = userRef.document().id
            val session = ApiUserSession(
                user_id = docId,
                device_id = device.getId(),
                device_name = device.deviceName(),
                session_active = true,
                app_version = device.versionCode,
                battery_status = null,
            )

            userRef.document(docId).set(user).await()
            sessionRef.document().set(session).await()
        } else {
            val docId = snapshot!!.id
            val session = ApiUserSession(
                user_id = docId,
                device_id = device.getId(),
                device_name = device.deviceName(),
                session_active = true,
                app_version = device.versionCode,
                battery_status = null,
            )
            sessionRef.document().set(session).await()
        }
    }
}