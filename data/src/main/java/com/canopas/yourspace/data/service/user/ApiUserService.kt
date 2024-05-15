package com.canopas.yourspace.data.service.user

import com.canopas.yourspace.data.models.user.ApiUser
import com.canopas.yourspace.data.models.user.ApiUserSession
import com.canopas.yourspace.data.models.user.LOGIN_TYPE_GOOGLE
import com.canopas.yourspace.data.models.user.LOGIN_TYPE_PHONE
import com.canopas.yourspace.data.service.location.ApiLocationService
import com.canopas.yourspace.data.utils.Config
import com.canopas.yourspace.data.utils.Config.FIRESTORE_COLLECTION_USERS
import com.canopas.yourspace.data.utils.Device
import com.canopas.yourspace.data.utils.snapshotFlow
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.firestore.FieldValue
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
    private fun sessionRef(userId: String) =
        userRef.document(userId).collection(Config.FIRESTORE_COLLECTION_USER_SESSIONS)

    suspend fun getUser(userId: String): ApiUser? {
        return userRef.document(userId).get().await().toObject(ApiUser::class.java)
    }

    suspend fun getUserFlow(userId: String) =
        userRef.document(userId).snapshotFlow(ApiUser::class.java)

    suspend fun getUserSessionFlow(userId: String, sessionId: String) =
        sessionRef(userId).document(sessionId).snapshotFlow(ApiUserSession::class.java)

    suspend fun saveUser(
        uid: String?,
        firebaseToken: String?,
        account: GoogleSignInAccount? = null,
        phoneNumber: String? = null
    ): Triple<Boolean, ApiUser, ApiUserSession> {
        val savedUser = if (uid.isNullOrEmpty()) null else getUser(uid)
        val isExists = savedUser != null

        if (isExists) {
            val sessionDocRef = sessionRef(savedUser!!.id).document()
            val session = ApiUserSession(
                id = sessionDocRef.id,
                user_id = savedUser.id,
                device_id = device.getId(),
                device_name = device.deviceName(),
                session_active = true,
                app_version = device.versionCode
            )
            deactivateOldSessions(savedUser.id)
            sessionDocRef.set(session).await()
            return Triple(false, savedUser, session)
        } else {
            val user = ApiUser(
                id = uid!!,
                email = account?.email ?: "",
                phone = phoneNumber ?: "",
                auth_type = if (account != null) LOGIN_TYPE_GOOGLE else LOGIN_TYPE_PHONE,
                first_name = account?.givenName ?: "",
                last_name = account?.familyName ?: "",
                provider_firebase_id_token = firebaseToken,
                profile_image = account?.photoUrl?.toString() ?: ""
            )
            userRef.document(uid).set(user).await()
            val sessionDocRef = sessionRef(user.id).document()
            val session = ApiUserSession(
                id = sessionDocRef.id,
                user_id = user.id,
                device_id = device.getId(),
                device_name = device.deviceName(),
                session_active = true,
                app_version = device.versionCode
            )
            sessionDocRef.set(session).await()
            locationService.saveLastKnownLocation(user.id)
            return Triple(true, user, session)
        }
    }

    private suspend fun deactivateOldSessions(userId: String) {
        sessionRef(userId).whereEqualTo("session_active", true).get().await().documents.forEach {
            it.reference.update("session_active", false).await()
        }
    }

    suspend fun deleteUser(userId: String) {
        userRef.document(userId).delete().await()
    }

    suspend fun updateUser(user: ApiUser) {
        userRef.document(user.id).set(user).await()
    }

    suspend fun registerFcmToken(userId: String, token: String) {
        userRef.document(userId).update("fcm_token", token).await()
    }

    suspend fun addSpaceId(userId: String, spaceId: String) {
        userRef.document(userId).update("space_ids", FieldValue.arrayUnion(spaceId)).await()
    }

    suspend fun updateBatteryPct(userId: String, sessionId: String, batteryPct: Float) {
        sessionRef(userId).document(sessionId).update(
            "battery_pct",
            batteryPct,
            "updated_at",
            FieldValue.serverTimestamp()
        ).await()
    }

    suspend fun updateSessionState(id: String, id1: String, state: Int) {
        sessionRef(id).document(id1).update(
            "user_state",
            state,
            "updated_at",
            FieldValue.serverTimestamp()
        ).await()
    }

    suspend fun getUserSession(userId: String): ApiUserSession? {
        return sessionRef(userId).whereEqualTo("session_active", true)
            .get().await().documents.firstOrNull()?.toObject(ApiUserSession::class.java)

    }
}
