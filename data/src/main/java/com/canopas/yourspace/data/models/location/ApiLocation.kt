package com.canopas.yourspace.data.models.location

import androidx.annotation.Keep
import java.util.UUID

enum class UserState(val value: Int) {
    REST_POINT(0),
    STEADY(1),
    MOVING(2)
}

@Keep
data class ApiLocation(
    val id: String = UUID.randomUUID().toString(),
    val user_id: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val created_at: Long? = System.currentTimeMillis(),
    val user_state: Int? = UserState.MOVING.value
)

fun ApiLocation.toLocation() = android.location.Location("").apply {
    latitude = this@toLocation.latitude
    longitude = this@toLocation.longitude
}

fun android.location.Location.toApiLocation(userId: String) = ApiLocation(
    user_id = userId,
    latitude = latitude,
    longitude = longitude
)
