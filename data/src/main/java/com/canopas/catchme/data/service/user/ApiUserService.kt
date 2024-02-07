package com.canopas.catchme.data.service.user

import com.canopas.catchme.data.models.user.ApiUser
import com.canopas.catchme.data.models.user.ApiUserSession
import com.canopas.catchme.data.models.user.LOGIN_TYPE_GOOGLE
import com.canopas.catchme.data.models.user.LOGIN_TYPE_PHONE
import com.canopas.catchme.data.service.location.ApiLocationService
import com.canopas.catchme.data.utils.Device
import com.canopas.catchme.data.utils.FirestoreConst
import com.canopas.catchme.data.utils.FirestoreConst.FIRESTORE_COLLECTION_USERS
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiUserService @Inject constructor(
    db: FirebaseFirestore,
    private val device: Device,
    private val locationService: ApiLocationService
) {
    private val userRef = db.collection(FIRESTORE_COLLECTION_USERS)
    private val sessionRef = db.collection(FirestoreConst.FIRESTORE_COLLECTION_USER_SESSIONS)

    suspend fun getUser(userId: String): ApiUser? {
        return userRef.document(userId).get().await().toObject(ApiUser::class.java)
    }

    suspend fun saveUser(
        uid: String?,
        firebaseToken: String?,
        account: GoogleSignInAccount? = null,
        phoneNumber: String? = null,
    ): Triple<Boolean, ApiUser, ApiUserSession> {

        val savedUser = if (uid.isNullOrEmpty()) null else getUser(uid)
        val isExists = savedUser != null

        if (isExists) {
            val sessionDocRef = sessionRef.document()
            val session = ApiUserSession(
                id = sessionDocRef.id,
                user_id = savedUser!!.id,
                device_id = device.getId(),
                device_name = device.deviceName(),
                session_active = true,
                app_version = device.versionCode,
                battery_status = null
            )
            sessionDocRef.set(session).await()
            return Triple(false, savedUser, session)
        } else {
            val user = ApiUser(
                id = uid!!,
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
                user_id = user.id,
                device_id = device.getId(),
                device_name = device.deviceName(),
                session_active = true,
                app_version = device.versionCode,
                battery_status = null
            )

            userRef.document().set(user).await()
            sessionDocRef.set(session).await()
            locationService.saveLastKnownLocation(user.id)
            return Triple(true, user, session)
        }
    }

    suspend fun deleteUser(userId: String) {
        sessionRef.whereEqualTo("user_id", userId).get().await().documents.forEach {
            it.reference.delete().await()
        }
        userRef.document(userId).delete().await()
    }

    suspend fun updateUser(user: ApiUser) {
        userRef.document(user.id).set(user).await()
    }

}
