package com.canopas.yourspace.data.service.user

import com.canopas.yourspace.data.models.user.ApiUser
import com.canopas.yourspace.data.models.user.ApiUserSession
import com.canopas.yourspace.data.models.user.LOGIN_TYPE_APPLE
import com.canopas.yourspace.data.models.user.LOGIN_TYPE_GOOGLE
import com.canopas.yourspace.data.service.location.ApiLocationService
import com.canopas.yourspace.data.utils.Config
import com.canopas.yourspace.data.utils.Config.FIRESTORE_COLLECTION_USERS
import com.canopas.yourspace.data.utils.Device
import com.canopas.yourspace.data.utils.snapshotFlow
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

const val NETWORK_STATUS_CHECK_INTERVAL = 3 * 60 * 1000

@Singleton
class ApiUserService @Inject constructor(
    db: FirebaseFirestore,
    private val device: Device,
    private val locationService: ApiLocationService,
    private val functions: FirebaseFunctions
) {
    private val userRef = db.collection(FIRESTORE_COLLECTION_USERS)
    private fun sessionRef(userId: String) =
        userRef.document(userId.takeIf { it.isNotBlank() } ?: "null")
            .collection(Config.FIRESTORE_COLLECTION_USER_SESSIONS)

    suspend fun getUser(userId: String): ApiUser? {
        return try {
            userRef.document(userId).get().await().toObject(ApiUser::class.java)
        } catch (e: Exception) {
            Timber.e(e, "Error while getting user")
            null
        }
    }

    suspend fun getUserFlow(userId: String) =
        userRef.document(userId).snapshotFlow(ApiUser::class.java)

    suspend fun getUserSessionFlow(userId: String, sessionId: String) =
        sessionRef(userId).document(sessionId).snapshotFlow(ApiUserSession::class.java)

    suspend fun saveUser(
        uid: String?,
        firebaseToken: String?,
        account: GoogleSignInAccount? = null,
        firebaseUser: FirebaseUser? = null
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
                email = account?.email ?: firebaseUser?.email ?: "",
                auth_type = if (account != null) LOGIN_TYPE_GOOGLE else if (firebaseUser != null) LOGIN_TYPE_APPLE else 0,
                first_name = account?.givenName ?: firebaseUser?.displayName ?: "",
                last_name = account?.familyName ?: "",
                provider_firebase_id_token = firebaseToken,
                profile_image = account?.photoUrl?.toString() ?: firebaseUser?.photoUrl?.toString()
                    ?: ""
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

    suspend fun updateBatteryPct(userId: String, batteryPct: Float) {
        userRef.document(userId)
            .update(mapOf("battery_pct" to batteryPct, "updated_at" to System.currentTimeMillis()))
            .await()
    }

    suspend fun updateSessionState(id: String, state: Int) {
        userRef.document(id)
            .update(mapOf("state" to state, "updated_at" to System.currentTimeMillis())).await()
    }

    suspend fun getUserSession(userId: String): ApiUserSession? {
        return sessionRef(userId).whereEqualTo("session_active", true)
            .get().await().documents.firstOrNull()?.toObject(ApiUserSession::class.java)
    }

    suspend fun getUserNetworkStatus(
        userId: String,
        lastUpdatedTime: Long,
        onStatusChecked: (ApiUser?) -> Unit
    ) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastUpdatedTime < NETWORK_STATUS_CHECK_INTERVAL) {
            Timber.d("Network status check called too soon. Skipping call for $userId.")
            onStatusChecked(null)
            return
        }

        withContext(Dispatchers.IO) {
            val data = hashMapOf("userId" to userId)
            try {
                functions.getHttpsCallable("networkStatusCheck").call(data).addOnSuccessListener {
                    val user = runBlocking { getUser(userId) }
                    onStatusChecked(user)
                }.addOnFailureListener {
                    Timber.e(it, "Failed to check network status")
                    onStatusChecked(null)
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to check network status")
                onStatusChecked(null)
            }
        }
    }

    suspend fun updatePowerSaveModeStatus(currentUserId: String, powerSavingEnabled: Boolean) {
        val activeSession = getUserSession(currentUserId)
        try {
            if (activeSession != null) {
                val updatedSession = activeSession.copy(power_save_mode_enabled = powerSavingEnabled)
                sessionRef(currentUserId).document(activeSession.id).set(updatedSession).await()
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to update power-saving mode status for user $currentUserId")
        }
    }
}
