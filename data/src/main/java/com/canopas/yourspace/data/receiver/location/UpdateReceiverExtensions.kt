package com.canopas.yourspace.data.receiver.location

import android.location.Location
import com.canopas.yourspace.data.models.location.ApiLocation
import com.canopas.yourspace.data.models.location.LocationJourney
import com.canopas.yourspace.data.models.location.LocationTable
import com.canopas.yourspace.data.models.location.UserState
import com.canopas.yourspace.data.models.location.isSteadyLocation
import com.canopas.yourspace.data.models.location.toLocation
import com.canopas.yourspace.data.models.location.toLocationFromSteadyJourney
import com.canopas.yourspace.data.service.location.LocationJourneyService
import com.canopas.yourspace.data.storage.room.LocationTableDatabase
import com.canopas.yourspace.data.utils.Config
import com.canopas.yourspace.data.utils.LocationConverters
import timber.log.Timber
import java.util.Date

/**
 * Calculate distance between two locations
 * */
fun distanceBetween(location1: Location, location2: Location): Float {
    val distance = FloatArray(1)
    Location.distanceBetween(
        location1.latitude,
        location1.longitude,
        location2.latitude,
        location2.longitude,
        distance
    )
    return distance[0]
}

/**
 * Check if user is moving or steady
 * */
fun List<ApiLocation>.isMoving(currentLocation: Location): Boolean {
    val latestLocation =
        this.filter { it.created_at != null }
            .minByOrNull { it.created_at!! }
    latestLocation?.let {
        val distance = distanceBetween(
            currentLocation,
            latestLocation?.toLocation()!!
        )
        Timber.d("XXX check isMoving --- Distance between $distance min location ${Date(latestLocation.created_at!!)}")
    }
    return latestLocation != null && distanceBetween(
        currentLocation,
        latestLocation.toLocation()
    ) > Config.RADIUS_TO_CHECK_USER_STATE
}


/**
 * Get last five minute locations from local database ~ 1 location per minute
 * */
fun LocationTable?.getLastFiveMinuteLocations(
    converters: LocationConverters
): List<ApiLocation> {
    return this?.lastFiveMinutesLocations?.let {
        converters.locationListFromString(it)
    } ?: emptyList()
}

/**
 * Get user state based on last five minute locations.
 *
 * Check [UserState] for more details
 * */
fun LocationTable?.getUserState(
    converters: LocationConverters,
    extractedLocation: Location,
    lastLocation: ApiLocation?
): Int? {
    val lastFiveMinuteLocations = this.getLastFiveMinuteLocations(converters)

    if (lastFiveMinuteLocations.isEmpty()) return null
    if (lastFiveMinuteLocations.isMoving(extractedLocation)) return UserState.MOVING.value
    if (lastLocation?.user_state != UserState.STEADY.value) return UserState.STEADY.value
    return null
}

/**
 * Get location data from local database
 * */
fun String.getLocationData(locationTableDatabase: LocationTableDatabase): LocationTable? {
    return locationTableDatabase.locationTableDao().getLocationData(this)
}

/**
 * Get last location from local database
 * */
fun LocationTable?.getLastLocation(converters: LocationConverters): ApiLocation? {
    return this?.latestLocation?.let {
        converters.locationFromString(it)
    }
}

/**
 * Save journey as steady if last location is null or
 *
 * sudden location change is detected(happens when location is turned off and user is moving)
 * */
suspend fun LocationJourneyService.saveJourneyIfNullLastLocation(
    currentUserId: String,
    extractedLocation: Location,
    lastLocation: ApiLocation?,
    lastJourneyLocation: LocationJourney?
) {
    if (lastJourneyLocation == null) {
        saveCurrentJourney(
            userId = currentUserId,
            fromLatitude = extractedLocation.latitude,
            fromLongitude = extractedLocation.longitude,
            currentLocationDuration = extractedLocation.time - (lastLocation?.created_at ?: 0L),
            recordedAt = Date().time
        )
    } else {
        val timeDifference = extractedLocation.time - lastJourneyLocation.created_at!!
        val distance = distanceBetween(
            extractedLocation,
            lastJourneyLocation.toLocationFromSteadyJourney()
        ).toDouble()


        if ((timeDifference > 5 * 60 * 1000 && !lastJourneyLocation.isSteadyLocation()) ||
            distance > Config.DISTANCE_TO_CHECK_SUDDEN_LOCATION_CHANGE
        ) {
            Timber.d("XXX Sudden location change detected distance $distance time ${timeDifference > 5 * 60 * 1000} " +
                    "date newJourney ${Date(lastJourneyLocation.created_at!!)}")
            saveCurrentJourney(
                userId = currentUserId,
                fromLatitude = extractedLocation.latitude,
                fromLongitude = extractedLocation.longitude,
                currentLocationDuration = extractedLocation.time - (lastLocation?.created_at ?: 0L),
                recordedAt = Date().time
            )
        }
    }
}

/**
 * Save journey as steady when user state is steady
 * */
suspend fun LocationJourneyService.saveJourneyForSteadyUser(
    currentUserId: String,
    extractedLocation: Location,
    lastJourneyLocation: LocationJourney,
    lastSteadyLocation: LocationJourney?
) {
    val distance = lastSteadyLocation?.toLocationFromSteadyJourney()?.let { location ->
        distanceBetween(extractedLocation, location)
    }?.toDouble() ?: 0.0
    val timeDifference = extractedLocation.time - lastJourneyLocation.created_at!!
    Timber.d(
        "XXX SteadyUser distance $distance time $timeDifference  journey ${
            Date(
                lastJourneyLocation.created_at
            )
        }"
    )
    if ((timeDifference < Config.FIVE_MINUTES && distance < Config.DISTANCE_TO_CHECK_SUDDEN_LOCATION_CHANGE) ||
        (lastJourneyLocation.isSteadyLocation() && distance < Config.DISTANCE_TO_CHECK_SUDDEN_LOCATION_CHANGE)
    ) {
        return
    }
    Timber.d("XXX Saving steady location journey")
    saveCurrentJourney(
        userId = currentUserId,
        fromLatitude = extractedLocation.latitude,
        fromLongitude = extractedLocation.longitude,
        currentLocationDuration = extractedLocation.time - lastJourneyLocation.created_at,
        recordedAt = Date().time
    )
}

/**
 * Save journey for moving user
 * */
suspend fun LocationJourneyService.saveJourneyForMovingUser(
    currentUserId: String,
    extractedLocation: Location,
    lastLocation: ApiLocation?,
    lastJourneyLocation: LocationJourney,
    lastSteadyLocation: LocationJourney?,
    lastMovingLocation: LocationJourney?
) {
    var newJourney = LocationJourney(
        user_id = currentUserId,
        from_latitude = lastSteadyLocation?.from_latitude ?: extractedLocation.latitude,
        from_longitude = lastSteadyLocation?.from_longitude ?: extractedLocation.longitude,
        to_latitude = extractedLocation.latitude,
        to_longitude = extractedLocation.longitude,
        route_distance = lastSteadyLocation?.toLocationFromSteadyJourney()?.let { location ->
            distanceBetween(extractedLocation, location).toDouble()
        },
        route_duration = extractedLocation.time - (
                (
                        lastMovingLocation?.created_at
                            ?: lastSteadyLocation?.created_at
                        ) ?: 0L
                ),
        current_location_duration = extractedLocation.time - (
                (
                        lastMovingLocation?.created_at
                            ?: lastSteadyLocation?.created_at
                        ) ?: 0L
                ),
        created_at = lastMovingLocation?.created_at ?: System.currentTimeMillis()
    )

    val distance = distanceBetween(
        extractedLocation,
        lastJourneyLocation.toLocationFromSteadyJourney()
    ).toDouble()

    val timeDifference = Date().time - newJourney.created_at!!
//
//    if (distance > Config.DISTANCE_TO_CHECK_SUDDEN_LOCATION_CHANGE || timeDifference > 5 * 60 * 1000) {
//        Timber.d("XXX Sudden location change detected distance $distance time ${timeDifference > 5 * 60 * 1000}" +
//                "date  newJourney ${Date(newJourney.created_at!!)}")
//        saveCurrentJourney(
//            userId = currentUserId,
//            fromLatitude = extractedLocation.latitude,
//            fromLongitude = extractedLocation.longitude,
//            currentLocationDuration = extractedLocation.time - (lastLocation?.created_at ?: 0L),
//            recordedAt = Date().time
//        )
//        return
//    }

    if (lastMovingLocation != null && !lastJourneyLocation.isSteadyLocation()) {
        newJourney = newJourney.copy(id = lastMovingLocation.id)
        Timber.d("XXX Updating last location journey")
        updateLastLocationJourney(currentUserId, newJourney)
    } else {
        Timber.d("XXX Saving new location journey")
        saveCurrentJourney(
            userId = newJourney.user_id,
            fromLatitude = newJourney.from_latitude,
            fromLongitude = newJourney.from_longitude,
            toLatitude = newJourney.to_latitude,
            toLongitude = newJourney.to_longitude,
            routeDistance = newJourney.route_distance,
            routeDuration = newJourney.route_duration,
            currentLocationDuration = newJourney.current_location_duration,
            recordedAt = newJourney.created_at ?: 0L
        )
    }
}
