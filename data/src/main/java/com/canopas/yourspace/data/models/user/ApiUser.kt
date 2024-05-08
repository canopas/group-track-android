package com.canopas.yourspace.data.models.user

import androidx.annotation.Keep
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.ServerTimestamp
import com.squareup.moshi.JsonClass
import java.util.Date
import java.util.UUID

const val LOGIN_TYPE_GOOGLE = 1
const val LOGIN_TYPE_PHONE = 2
const val LOGIN_DEVICE_TYPE_ANDROID = 1

@Keep
data class ApiUser(
    val id: String = UUID.randomUUID().toString(),
    val phone: String? = null,
    val email: String? = null,
    val auth_type: Int? = null,
    val first_name: String? = "",
    val last_name: String? = "",
    val profile_image: String? = "",
    val location_enabled: Boolean = true,
    val space_ids: List<String>? = emptyList(),
    val provider_firebase_id_token: String? = null,
    val fcm_token: String? = null,
    val created_at: Long? = System.currentTimeMillis()
) {
    @get:Exclude
    val fullName: String get() = "$first_name $last_name"

    @get:Exclude
    val firstChar: String get() = fullName.trim().firstOrNull()?.toString() ?: "?"
}

@Keep
@JsonClass(generateAdapter = true)
data class ApiUserSession(
    val id: String = UUID.randomUUID().toString(),
    val user_id: String = "",
    val device_id: String? = "",
    val fcm_token: String? = "",
    val device_name: String? = "",
    val platform: Int = LOGIN_DEVICE_TYPE_ANDROID,
    val session_active: Boolean = true,
    val app_version: Long? = 0,
    val battery_pct: Float? = 0f,
    val created_at: Long? = System.currentTimeMillis()
)
