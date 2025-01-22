package com.canopas.yourspace.data.repository

import android.location.Location
import com.canopas.yourspace.data.models.location.JourneyResult
import com.canopas.yourspace.data.models.location.JourneyType
import com.canopas.yourspace.data.models.location.LocationJourney
import com.canopas.yourspace.data.models.location.isSteady
import com.canopas.yourspace.data.models.location.toLocationFromMovingJourney
import com.canopas.yourspace.data.models.location.toLocationFromSteadyJourney
import com.canopas.yourspace.data.models.location.toRoute
import timber.log.Timber
import java.util.Calendar
import kotlin.math.sqrt

private const val MIN_DISTANCE = 100.0 // 100 meters
private const val MIN_TIME_DIFFERENCE = 5 * 60 * 1000L // 5 minutes
private const val MIN_DISTANCE_FOR_MOVING = 10.0 // 10 meters
private const val MIN_UPDATE_INTERVAL_MINUTE = 30000L // 30 seconds

/**
 * Function to generate a new journey or update an existing one.
 *
 * @param userId ID of current user
 * @param newLocation The newly received location
 * @param lastKnownJourney The last known journey (could be null)
 * @param lastLocations A list of previous location fixes used for e.g. geometric median
 * @return A [JourneyResult] holding either updatedJourney or newJourney or both, or null if no changes.
 */
fun getJourney(
    userId: String,
    newLocation: Location,
    lastKnownJourney: LocationJourney?,
    lastLocations: List<Location>
): JourneyResult? {
    // 1. If there is no previous journey, create a new STEADY journey
    if (lastKnownJourney == null) {
        Timber.tag("XXX").e("No previous journey")
        val newSteadyJourney = LocationJourney(
            user_id = userId,
            from_latitude = newLocation.latitude,
            from_longitude = newLocation.longitude,
            created_at = System.currentTimeMillis(),
            updated_at = System.currentTimeMillis(),
            type = JourneyType.STEADY
        )
        return JourneyResult(null, newSteadyJourney)
    }

    // 2. Calculate a geometric median if needed (optional if lastLocations is empty or 1)
    val geometricMedian = if (lastLocations.isNotEmpty()) {
        geometricMedianCalculation(lastLocations)
    } else {
        null
    }

    // 3. Determine how far the newLocation is from our reference point
    val distance = if (lastKnownJourney.isSteady()) {
        // Compare newLocation with "from" location
        Timber.tag("xxx").e("LastKnownJourney = isSteady")
        distanceBetween(
            geometricMedian ?: newLocation,
            lastKnownJourney.toLocationFromSteadyJourney()
        )
    } else {
        // Compare newLocation with "to" location
        Timber.tag("xxx").e("LastKnownJourney = isMoving")
        distanceBetween(
            geometricMedian ?: newLocation,
            lastKnownJourney.toLocationFromMovingJourney()
        )
    }

    // 4. Calculate time difference
    val timeDifference = newLocation.time - lastKnownJourney.updated_at

    // 5. Check if the day changed
    val dayChanged = isDayChanged(newLocation, lastKnownJourney)

    // 6. If lastKnownJourney is STEADY, distance < MIN_DISTANCE, but the day changed -> update the existing journey
    if (lastKnownJourney.isSteady() && distance < MIN_DISTANCE && dayChanged) {
        val updatedJourney = lastKnownJourney.copy(
            from_latitude = newLocation.latitude,
            from_longitude = newLocation.longitude,
            updated_at = System.currentTimeMillis()
        )
        return JourneyResult(updatedJourney, null)
    }

    // -----------------------------------------------------------------
    // Manage Journey
    // 1. lastKnownJourney is null, create a new journey
    // 2. If user is stationary
    //   a. update the journey with the last location and update the updated_at
    //   b. If distance > 150,
    //     - update the journey with the last location and update the updated_at
    //     - create a new moving journey
    // 3. If user is moving
    //   a. If distance > 150, update the last location, route and updated_at
    //   b. If distance < 150 and time diff between two location updates > 5 mins,
    //      - update the journey with the last location and update the updated_at, and stop the journey
    //      - create a new stationary journey
    // -----------------------------------------------------------------
    if (lastKnownJourney.isSteady()) {
        // STEADY journey (to_latitude/to_longitude == null)

        // If distance > MIN_DISTANCE => user started moving
        if (distance > MIN_DISTANCE) {
            // 1. Update last STEADY journey with new "from" lat/lng
            Timber.tag("xxx").e("lastKnownJourney = isSteady && Distance > MIN_DISTANCE")
            val updatedJourney = lastKnownJourney.copy(
                updated_at = System.currentTimeMillis()
            )

            // 2. Create NEW MOVING journey
            val newMovingJourney = LocationJourney(
                user_id = userId,
                from_latitude = lastKnownJourney.from_latitude,
                from_longitude = lastKnownJourney.from_longitude,
                to_latitude = newLocation.latitude,
                to_longitude = newLocation.longitude,
                routes = lastLocations.map { it.toRoute() },
                route_distance = distance,
                route_duration = timeDifference,
                created_at = System.currentTimeMillis(),
                updated_at = System.currentTimeMillis(),
                type = JourneyType.MOVING
            )
            return JourneyResult(updatedJourney, newMovingJourney)
        }
        // If distance < MIN_DISTANCE && timeDifference > MIN_UPDATE_INTERVAL_MINUTE => just update STEADY
        else if (distance < MIN_DISTANCE && timeDifference > MIN_UPDATE_INTERVAL_MINUTE) {
            val updatedJourney = lastKnownJourney.copy(
                from_latitude = newLocation.latitude,
                from_longitude = newLocation.longitude,
                updated_at = System.currentTimeMillis()
            )
            return JourneyResult(updatedJourney, null)
        }
    } else {
        // MOVING journey (to_latitude/to_longitude != null)

        // If the user likely stopped moving => (timeDifference > MIN_TIME_DIFFERENCE)
        if (timeDifference > MIN_TIME_DIFFERENCE) {
            // 1. Update last moving journey
            Timber.tag("xxx").e("timeDifference > MIN_TIME_DIFFERENCE")
            val updatedJourney = lastKnownJourney.copy(
                to_latitude = newLocation.latitude,
                to_longitude = newLocation.longitude,
                route_distance = (lastKnownJourney.route_distance ?: 0.0) + distance,
                route_duration = lastKnownJourney.updated_at -
                    lastKnownJourney.created_at,
                routes = lastKnownJourney.routes + newLocation.toRoute()
            )

            // 2. Create NEW STEADY journey
            val newSteadyJourney = LocationJourney(
                user_id = userId,
                from_latitude = newLocation.latitude,
                from_longitude = newLocation.longitude,
                created_at = lastKnownJourney.updated_at,
                updated_at = System.currentTimeMillis(),
                type = JourneyType.STEADY
            )
            Timber.e("XXX newSteadyJourney = $newSteadyJourney")
            return JourneyResult(updatedJourney, newSteadyJourney)
        }
        // If user is still moving => distance > MIN_DISTANCE_FOR_MOVING, timeDifference > MIN_UPDATE_INTERVAL_MINUTE => update route
        else if (distance > MIN_DISTANCE_FOR_MOVING && timeDifference > MIN_UPDATE_INTERVAL_MINUTE) {
            Timber.e("XXX distance > MIN_DISTANCE_FOR_MOVING && timeDifference > MIN_UPDATE_INTERVAL_MINUTE")
            val updatedJourney = lastKnownJourney.copy(
                to_latitude = newLocation.latitude,
                to_longitude = newLocation.longitude,
                // Add new distance to previous distance, if you want cumulative
                route_distance = (lastKnownJourney.route_distance ?: 0.0) + distance,
                route_duration = lastKnownJourney.updated_at -
                    lastKnownJourney.created_at,
                routes = lastKnownJourney.routes + newLocation.toRoute(),
                updated_at = System.currentTimeMillis()
            )
            return JourneyResult(updatedJourney, null)
        }
    }

    // If none of the conditions are satisfied, return null
    return null
}

/**
 * Checks if the day of [newLocation]'s time differs from the day of [lastKnownJourney]'s updated_at.
 */
private fun isDayChanged(
    newLocation: Location,
    lastKnownJourney: LocationJourney
): Boolean {
    val lastMillis = lastKnownJourney.updated_at
    val lastCal = Calendar.getInstance().apply { timeInMillis = lastMillis }
    val lastDay = lastCal.get(Calendar.DAY_OF_YEAR)

    val newCal = Calendar.getInstance().apply { timeInMillis = newLocation.time }
    val newDay = newCal.get(Calendar.DAY_OF_YEAR)

    return lastDay != newDay
}

/**
 * Computes distance in meters between two [Location] objects.
 */
private fun distanceBetween(loc1: Location, loc2: Location): Double {
    return loc1.distanceTo(loc2).toDouble()
}

/**
 * Rough "geometric median" among a list of [Location] objects by scanning
 * which point yields smallest sum of distances to all others.
 */
private fun geometricMedianCalculation(locations: List<Location>): Location {
    val result = locations.minByOrNull { candidate ->
        locations.sumOf { location ->
            val distance = distance(candidate, location)
            distance
        }
    } ?: throw IllegalArgumentException("Location list is empty")
    return result
}

private fun distance(loc1: Location, loc2: Location): Double {
    val latDiff = loc1.latitude - loc2.latitude
    val lonDiff = loc1.longitude - loc2.longitude
    return sqrt(latDiff * latDiff + lonDiff * lonDiff)
}
