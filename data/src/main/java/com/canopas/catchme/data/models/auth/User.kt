package com.canopas.catchme.data.models.auth

import java.util.Date

const val LOGIN_TYPE_GOOGLE = 1
const val LOGIN_TYPE_PHONE = 2
const val LOGIN_DEVICE_TYPE_ANDROID = 1

data class ApiUser(
    val phone: String?,
    val email: String?,
    val auth_type: Int,
    val first_name: String?,
    val last_name: String?,
    val profile_image: String?,
    val location_enabled: Boolean = true,
    val created_at: Date?,
)

data class ApiUserSession(
    val user_id: String?,
    val device_id: String?,
    val fcm_token: String?,
    val device_name: String?,
    val platform: Int = LOGIN_DEVICE_TYPE_ANDROID,
    val is_active: Boolean = true,
    val app_version: String?,
    val battery_status: String?,
    val created_at: Date?,
)
