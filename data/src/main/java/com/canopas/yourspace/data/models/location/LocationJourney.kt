package com.canopas.yourspace.data.models.location

import android.location.Location
import androidx.annotation.Keep
import com.squareup.moshi.JsonClass
import java.util.UUID

@Keep
@JsonClass(generateAdapter = true)
data class LocationJourney(
    val id: String = UUID.randomUUID().toString(),
    val user_id: String = "",
    val from_latitude: Double = 0.0,
    val from_longitude: Double = 0.0,
    var to_latitude: Double? = null,
    var to_longitude: Double? = null,
    val route_distance: Double? = null,
    val route_duration: Long? = null,
    val current_location_duration: Long? = null,
    val created_at: Long? = System.currentTimeMillis()
)

fun LocationJourney.isSteadyLocation(): Boolean {
    return to_latitude == null && to_longitude == null
}

fun LocationJourney.toLocation() = Location("").apply {
    latitude = this@toLocation.from_latitude
    longitude = this@toLocation.from_longitude
}
