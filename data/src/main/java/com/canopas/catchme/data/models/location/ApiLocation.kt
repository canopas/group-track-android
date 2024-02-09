package com.canopas.catchme.data.models.location

import androidx.annotation.Keep
import java.util.UUID

@Keep
data class ApiLocation(
    val id: String = UUID.randomUUID().toString(),
    val user_id: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val created_at: Long? = System.currentTimeMillis()
)

fun ApiLocation.toLocation() = android.location.Location("").apply {
    latitude = this@toLocation.latitude
    longitude = this@toLocation.longitude
}
