package com.canopas.yourspace.data.models.location

import com.google.errorprone.annotations.Keep

@Keep
data class LocationJourney(
    val id: String,
    val userId: String,
    val fromLatitude: Double,
    val fromLongitude: Double,
    var toLatitude: Double? = null,
    var toLongitude: Double? = null,
    val routeDistance: Double? = null,
    val routeDuration: String? = null,
    val currentLocationDuration: String? = null,
    val isSticky: Boolean,
    val createdAt: Long?
)
