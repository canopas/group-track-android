package com.canopas.yourspace.data.models.location

import android.location.Location
import androidx.annotation.Keep
import com.google.android.gms.maps.model.LatLng
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
    val routes: List<JourneyRoute> = emptyList(),
    val created_at: Long = System.currentTimeMillis(),
    val updated_at: Long = System.currentTimeMillis(),
    val type: JourneyType = if (to_latitude != null && to_longitude != null) JourneyType.MOVING else JourneyType.STEADY,
    val key_id: String = ""
)

@Keep
@JsonClass(generateAdapter = true)
data class EncryptedLocationJourney(
    val id: String = UUID.randomUUID().toString(),
    val user_id: String = "",
    val from_latitude: String = "", // Encrypted latitude - from
    val from_longitude: String = "", // Encrypted longitude - from
    val to_latitude: String? = null, // Encrypted latitude - to
    val to_longitude: String? = null, // Encrypted longitude - to
    val route_distance: Double? = null,
    val route_duration: Long? = null,
    val routes: List<EncryptedJourneyRoute> = emptyList(), // Encrypted journey routes
    val created_at: Long = System.currentTimeMillis(),
    val updated_at: Long = System.currentTimeMillis(),
    val type: JourneyType = if (to_latitude != null && to_longitude != null) JourneyType.MOVING else JourneyType.STEADY,
    val key_id: String = ""
)

@Keep
data class JourneyRoute(val latitude: Double = 0.0, val longitude: Double = 0.0)

@Keep
@JsonClass(generateAdapter = true)
data class EncryptedJourneyRoute(
    val latitude: String = "", // Encrypted latitude
    val longitude: String = "" // Encrypted longitude
)

/**
 * Data class to hold the result of the journey generation.
 */
data class JourneyResult(
    val updatedJourney: LocationJourney?,
    val newJourney: LocationJourney?
)

fun Location.toRoute(): JourneyRoute {
    return JourneyRoute(latitude, longitude)
}

fun JourneyRoute.toLatLng() = LatLng(latitude, longitude)
fun LocationJourney.toRoute(): List<LatLng> {
    if (isSteady()) {
        return emptyList()
    } else if (isMoving()) {
        val result = listOf(
            LatLng(
                from_latitude,
                from_longitude
            )
        ) + routes.map { it.toLatLng() } + listOf(
            LatLng(to_latitude ?: 0.0, to_longitude ?: 0.0)
        )
        return result
    } else {
        return emptyList()
    }
}

fun LocationJourney.isSteady() = type == JourneyType.STEADY

fun LocationJourney.isMoving() = type == JourneyType.MOVING

fun LocationJourney.toLocationFromSteadyJourney() = Location("").apply {
    latitude = this@toLocationFromSteadyJourney.from_latitude
    longitude = this@toLocationFromSteadyJourney.from_longitude
}

fun LocationJourney.toLocationFromMovingJourney() = Location("").apply {
    latitude = this@toLocationFromMovingJourney.to_latitude ?: 0.0
    longitude = this@toLocationFromMovingJourney.to_longitude ?: 0.0
}

enum class JourneyType {
    MOVING,
    STEADY
}
