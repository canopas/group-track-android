package com.canopas.catchme.data.models.auth

const val LOGIN_TYPE_GOOGLE = 1
const val LOGIN_TYPE_PHONE = 2
const val LOGIN_DEVICE_TYPE_ANDROID = 1

data class ApiLoginRequest(
    val platform: Int = LOGIN_DEVICE_TYPE_ANDROID,
    val auth_type: Int,
    val device_id: String?,
    val app_version: String?,
    val device_name: String?,
    val phone: String?,
    val email: String?,
    val firebase_id_token: String?,
    val fcm_token: String?
)


