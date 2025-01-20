package com.canopas.yourspace.data.models.user

import androidx.annotation.Keep
import com.google.firebase.firestore.Blob
import com.google.firebase.firestore.Exclude
import com.squareup.moshi.JsonClass
import java.util.UUID

const val LOGIN_TYPE_GOOGLE = 1
const val LOGIN_TYPE_APPLE = 2
const val LOGIN_DEVICE_TYPE_ANDROID = 1

@Keep
data class ApiUser(
    val id: String = UUID.randomUUID().toString(),
    val email: String? = null,
    val auth_type: Int? = null,
    val first_name: String? = "",
    val last_name: String? = "",
    val profile_image: String? = "",
    val location_enabled: Boolean = true,
    val space_ids: List<String>? = emptyList(),
    val provider_firebase_id_token: String? = null,
    val fcm_token: String? = "",
    val state: Int = USER_STATE_UNKNOWN,
    val battery_pct: Float? = 0f,
    val created_at: Long? = System.currentTimeMillis(),
    val updated_at: Long? = System.currentTimeMillis(),
    val identity_key_public: Blob? = Blob.fromBytes(ByteArray(0)),
    val identity_key_private: Blob? = Blob.fromBytes(ByteArray(0)),
    val identity_key_salt: Blob? = Blob.fromBytes(ByteArray(0)) // Salt for key derivation
) {
    @get:Exclude
    val fullName: String get() = "$first_name $last_name"

    @get:Exclude
    val firstChar: String get() = fullName.trim().firstOrNull()?.toString() ?: "?"

    @get:Exclude
    val noNetwork: Boolean get() = state == USER_STATE_NO_NETWORK_OR_PHONE_OFF

    @get:Exclude
    val locationPermissionDenied: Boolean get() = state == USER_STATE_LOCATION_PERMISSION_DENIED
}

@Keep
@JsonClass(generateAdapter = true)
data class ApiUserSession(
    val id: String = UUID.randomUUID().toString(),
    val user_id: String = "",
    val device_id: String? = "",
    val device_name: String? = "",
    val platform: Int = LOGIN_DEVICE_TYPE_ANDROID,
    val session_active: Boolean = true,
    val app_version: Long? = 0,
    val created_at: Long? = System.currentTimeMillis()
) {
    @get:Exclude
    val loggedOut: Boolean get() = !session_active
}

const val USER_STATE_UNKNOWN = 0
const val USER_STATE_NO_NETWORK_OR_PHONE_OFF = 1
const val USER_STATE_LOCATION_PERMISSION_DENIED = 2
