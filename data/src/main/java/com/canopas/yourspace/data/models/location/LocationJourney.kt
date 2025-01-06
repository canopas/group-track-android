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
    val created_at: Long? = System.currentTimeMillis(),
    val update_at: Long? = System.currentTimeMillis(),
    val type: JourneyType? = null
)

@Keep
data class JourneyRoute(val latitude: Double = 0.0, val longitude: Double = 0.0)

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

fun LocationJourney.isSteady(): Boolean {
    if (type != null) {
        return type == JourneyType.STEADY
    }
    return to_latitude == null || to_longitude == null
}

fun LocationJourney.isMoving(): Boolean {
    if (type != null) {
        return type == JourneyType.MOVING
    }
    return to_latitude != null && to_longitude != null
}

fun LocationJourney.toLocationFromSteadyJourney() = Location("").apply {
    latitude = this@toLocationFromSteadyJourney.from_latitude
    longitude = this@toLocationFromSteadyJourney.from_longitude
}

fun LocationJourney.toLocationFromMovingJourney() = Location("").apply {
    latitude = this@toLocationFromMovingJourney.to_latitude ?: 0.0
    longitude = this@toLocationFromMovingJourney.to_longitude ?: 0.0
}

fun Location.toLocationJourney(userId: String, journeyId: String) = LocationJourney(
    id = journeyId,
    user_id = userId,
    from_latitude = latitude,
    from_longitude = longitude
)

enum class JourneyType {
    MOVING,
    STEADY
}
