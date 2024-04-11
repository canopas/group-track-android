package com.canopas.yourspace.data.models.location

import androidx.annotation.Keep
import com.squareup.moshi.JsonClass
import java.util.UUID

/**
 * User state that can be either [UserState.STEADY] or [UserState.MOVING]
 * */
enum class UserState(val value: Int) {
    STEADY(0),
    MOVING(1)
}

@Keep
@JsonClass(generateAdapter = true)
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
