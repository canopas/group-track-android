package com.canopas.yourspace.data.service.user

import com.canopas.yourspace.data.models.user.ApiUser
import com.canopas.yourspace.data.models.user.ApiUserSession
import com.canopas.yourspace.data.models.user.LOGIN_TYPE_APPLE
import com.canopas.yourspace.data.models.user.LOGIN_TYPE_GOOGLE
import com.canopas.yourspace.data.service.location.ApiLocationService
import com.canopas.yourspace.data.storage.UserPreferences
import com.canopas.yourspace.data.utils.Config
import com.canopas.yourspace.data.utils.Config.FIRESTORE_COLLECTION_USERS
import com.canopas.yourspace.data.utils.Device
import com.canopas.yourspace.data.utils.PrivateKeyUtils
import com.canopas.yourspace.data.utils.snapshotFlow
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.Blob
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.signal.libsignal.protocol.IdentityKey
import org.signal.libsignal.protocol.IdentityKeyPair
import org.signal.libsignal.protocol.ecc.Curve
import timber.log.Timber
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

const val NETWORK_STATUS_CHECK_INTERVAL = 3 * 60 * 1000

@Singleton
class ApiUserService @Inject constructor(
    db: FirebaseFirestore,
    private val device: Device,
    private val locationService: ApiLocationService,
    private val functions: FirebaseFunctions,
    private val userPreferences: UserPreferences
) {
    private val currentUser = userPreferences.currentUser
    private val userRef = db.collection(FIRESTORE_COLLECTION_USERS)
    private fun sessionRef(userId: String) =
        userRef.document(userId.takeIf { it.isNotBlank() } ?: "null")
            .collection(Config.FIRESTORE_COLLECTION_USER_SESSIONS)

    suspend fun getUser(userId: String): ApiUser? {
        return try {
            userRef.document(userId).get().await().toObject(ApiUser::class.java)?.let { user ->
                if (currentUser == null || user.id != currentUser.id) return user
                val decryptedPrivateKey = decryptPrivateKey(user) ?: return@let user
                userPreferences.storePrivateKey(decryptedPrivateKey)
                user
            }
        } catch (e: Exception) {
            Timber.e(e, "Error while getting user")
            null
        }
    }

    fun getUserFlow(userId: String) =
        userRef.document(userId).snapshotFlow(ApiUser::class.java).map { user ->
            user?.let { decryptPrivateKey(it) }?.let { decryptedKey ->
                user.copy(identity_key_private = Blob.fromBytes(decryptedKey))
            }
        }

    fun getUserSessionFlow(userId: String, sessionId: String) =
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

    suspend fun generateAndSaveUserKeys(user: ApiUser, passKey: String): ApiUser {
        val identityKeyPair = generateIdentityKeyPair()
        val salt = ByteArray(16).apply { SecureRandom().nextBytes(this) }
        val encryptedPrivateKey = PrivateKeyUtils.encryptPrivateKey(
            identityKeyPair.privateKey.serialize(),
            passkey = passKey,
            salt = salt
        )
        // Store passkey in preferences
        userPreferences.storePasskey(passKey)

        userRef.document(user.id).update(
            mapOf(
                "updated_at" to System.currentTimeMillis(),
                "identity_key_public" to Blob.fromBytes(identityKeyPair.publicKey.publicKey.serialize()),
                "identity_key_private" to Blob.fromBytes(encryptedPrivateKey),
                "identity_key_salt" to Blob.fromBytes(salt)
            )
        ).await()
        return user.copy(
            updated_at = System.currentTimeMillis(),
            identity_key_public = Blob.fromBytes(identityKeyPair.publicKey.publicKey.serialize()),
            identity_key_private = Blob.fromBytes(identityKeyPair.privateKey.serialize()),
            identity_key_salt = Blob.fromBytes(salt)
        )
    }

    /**
     * Generates a new IdentityKeyPair.
     * */
    private fun generateIdentityKeyPair(): IdentityKeyPair {
        val keyPair = Curve.generateKeyPair()
        val publicKey = IdentityKey(keyPair.publicKey)
        return IdentityKeyPair(publicKey, keyPair.privateKey)
    }

    suspend fun validatePasskey(user: ApiUser, passKey: String): ByteArray? {
        val decryptedPrivateKey = decryptPrivateKey(user, passKey)
        return if (decryptedPrivateKey != null) {
            decryptedPrivateKey
        } else {
            Timber.e("Failed to validate passkey for user ${user.id}")
            null
        }
    }

    /**
     * Decrypts the private key using the stored passkey/PIN and salt from ApiUser.
     * Returns the decrypted private key as ByteArray.
     */
    private suspend fun decryptPrivateKey(user: ApiUser, pin: String? = null): ByteArray? {
        val encryptedPrivateKey = user.identity_key_private?.toBytes() ?: return null
        val salt = user.identity_key_salt?.toBytes() ?: return null
        return try {
            val passkey = pin ?: userPreferences.getPasskey() ?: return null
            val decrypted = PrivateKeyUtils.decryptPrivateKey(encryptedPrivateKey, salt, passkey)
            decrypted
        } catch (e: Exception) {
            Timber.e(e, "Failed to decrypt private key for user ${user.id}")
            null
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
}
