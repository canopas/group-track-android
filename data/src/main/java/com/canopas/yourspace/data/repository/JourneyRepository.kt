package com.canopas.yourspace.data.repository

import android.location.Location
import com.canopas.yourspace.data.models.location.LocationJourney
import com.canopas.yourspace.data.models.location.isSteadyLocation
import com.canopas.yourspace.data.models.location.toLocationFromMovingJourney
import com.canopas.yourspace.data.models.location.toLocationFromSteadyJourney
import com.canopas.yourspace.data.models.location.toLocationJourney
import com.canopas.yourspace.data.models.location.toRoute
import com.canopas.yourspace.data.service.location.ApiJourneyService
import com.canopas.yourspace.data.storage.LocationCache
import timber.log.Timber
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

const val MIN_DISTANCE = 150.0 // 150 meters
const val MIN_TIME_DIFFERENCE = 5 * 60 * 1000 // 5 minutes

@Singleton
class JourneyRepository @Inject constructor(
    private val journeyService: ApiJourneyService,
    private val locationCache: LocationCache
) {

    suspend fun saveLocationJourney(
        extractedLocation: Location,
        userId: String
    ) {
        try {
            val lastKnownJourney = getLastKnownLocation(userId, extractedLocation)

            val isDayChanged = isDayChanged(extractedLocation, lastKnownJourney)

            if (isDayChanged) {
                // Day is changed between last known journey and current location
                // Just save again the last known journey in remote database with updated day i.e., current time
                saveJourneyOnDayChanged(userId, lastKnownJourney)
                return
            }

            // Check add add extracted location to last five locations to calculate geometric median
            checkAndSaveLastFiveLocations(extractedLocation, userId)

            // Check and save location journey based on user state i.e., steady or moving
            checkAndSaveLocationJourney(userId, extractedLocation, lastKnownJourney)
        } catch (e: Exception) {
            Timber.e(e, "Error while saving location journey")
        }
    }

    /**
     * Compare last known journey with extracted location and check if day is changed
     * */
    private fun isDayChanged(
        extractedLocation: Location,
        lastKnownJourney: LocationJourney
    ): Boolean {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = lastKnownJourney.update_at!!
        val lastKnownDay = calendar.get(Calendar.DAY_OF_MONTH)
        calendar.timeInMillis = extractedLocation.time
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
        return lastKnownDay != currentDay
    }

    /**
     * Save last known journey with updated day
     * */
    private suspend fun saveJourneyOnDayChanged(
        userId: String,
        lastKnownJourney: LocationJourney
    ) {
        var newJourneyId = ""
        // Removed createdAt and updatedAt from saveCurrentJourney so that we can just create a new journey
        // Because it was creating issue while checking current user state...
        // Changes: https://github.com/canopas/your-space-android/pull/86/commits/b4ee9717148217a72c23ba139520bc308d75f887
        journeyService.saveCurrentJourney(
            userId = userId,
            fromLatitude = lastKnownJourney.from_latitude,
            fromLongitude = lastKnownJourney.from_longitude,
            toLatitude = lastKnownJourney.to_latitude,
            toLongitude = lastKnownJourney.to_longitude
        ) {
            newJourneyId = it
        }
        val newJourney = lastKnownJourney.copy(
            id = newJourneyId,
            created_at = System.currentTimeMillis(),
            update_at = System.currentTimeMillis()
        )
        locationCache.putLastJourney(newJourney, userId)
    }

    /**
     * Get last known location journey from cache
     * If not available, fetch from remote database and save it to cache
     * If not available in remote database as well, save extracted location as new location journey
     * with steady state in cache as well as remote database
     * */
    private suspend fun getLastKnownLocation(
        userid: String,
        extractedLocation: Location
    ): LocationJourney {
        // Return last location journey if available from cache
        return locationCache.getLastJourney(userid) ?: kotlin.run {
            // Here, means no location journey available in cache
            // Fetch last location journey from remote database and save it to cache
            val lastJourney = journeyService.getLastJourneyLocation(userid)
            lastJourney?.let {
                locationCache.putLastJourney(it, userid)
                return lastJourney
            } ?: run {
                // Here, means no location journey available in remote database as well
                // Possibly user is new or no location journey available
                // Save extracted location as new location journey with steady state in cache
                // as well as remote database and return it
                var newJourneyId = ""
                journeyService.saveCurrentJourney(
                    userId = userid,
                    fromLatitude = extractedLocation.latitude,
                    fromLongitude = extractedLocation.longitude,
                    createdAt = extractedLocation.time
                ) {
                    newJourneyId = it
                }
                val locationJourney = extractedLocation.toLocationJourney(userid, newJourneyId)
                locationCache.putLastJourney(locationJourney, userid)
                return locationJourney
            }
        }
    }

    /**
     * Figure out the state of user i.e., steady or moving and save location journey accordingly.
     * */
    private suspend fun checkAndSaveLocationJourney(
        userId: String,
        extractedLocation: Location,
        lastKnownJourney: LocationJourney
    ) {
        val geometricMedian = locationCache.getLastFiveLocations(userId)?.let {
            geometricMedian(it)
        }
        val distance =
            if (lastKnownJourney.isSteadyLocation()) {
                distanceBetween(
                    geometricMedian ?: extractedLocation,
                    lastKnownJourney.toLocationFromSteadyJourney()
                )
            } else {
                distanceBetween(
                    geometricMedian ?: extractedLocation,
                    lastKnownJourney.toLocationFromMovingJourney()
                )
            }

        val timeDifference = (geometricMedian?.time ?: extractedLocation.time) - lastKnownJourney.update_at!!

        if (lastKnownJourney.isSteadyLocation()) {
            // Handle steady user
            if (distance > MIN_DISTANCE) {
                // Here, means last known journey is steady and and now user has started moving
                // Save journey for moving user and update cache as well:
                saveJourneyWhenUserStartsMoving(
                    userId,
                    extractedLocation,
                    lastKnownJourney
                )
            }
        } else {
            // Handle moving user
            if (distance > MIN_DISTANCE) {
                // Here, means last known journey is moving and user is still moving
                // Save journey for moving user and update last known journey.
                // Note: Need to use lastKnownJourney.id as journey id because we are updating the journey
                updateJourneyForContinuedMovingUser(
                    userId,
                    extractedLocation,
                    lastKnownJourney
                )
            } else if (distance < MIN_DISTANCE && timeDifference > MIN_TIME_DIFFERENCE) {
                // Here, means last known journey is moving and user has stopped moving
                // Save journey for steady user and update last known journey:
                saveJourneyOnJourneyStopped(
                    userId,
                    extractedLocation,
                    lastKnownJourney
                )
            }
        }
    }

    /**
     * Save journey when user starts moving i.e., state changes from steady to moving
     * */
    private suspend fun saveJourneyWhenUserStartsMoving(
        userId: String,
        extractedLocation: Location,
        lastKnownJourney: LocationJourney
    ) {
        var newJourneyId = ""
        val journey = LocationJourney(
            user_id = userId,
            from_latitude = lastKnownJourney.from_latitude,
            from_longitude = lastKnownJourney.from_longitude,
            to_latitude = extractedLocation.latitude,
            to_longitude = extractedLocation.longitude,
            routes = listOf(
                lastKnownJourney.toLocationFromSteadyJourney().toRoute(),
                extractedLocation.toRoute()
            )
        )
        journeyService.saveCurrentJourney(
            userId = userId,
            fromLatitude = lastKnownJourney.from_latitude,
            fromLongitude = lastKnownJourney.from_longitude,
            toLatitude = extractedLocation.latitude,
            toLongitude = extractedLocation.longitude
        ) {
            newJourneyId = it
        }
        locationCache.putLastJourney(journey.copy(id = newJourneyId), userId)
    }

    /**
     * Update journey for continued moving user i.e., state is moving and user is still moving
     * */
    private suspend fun updateJourneyForContinuedMovingUser(
        userId: String,
        extractedLocation: Location,
        lastKnownJourney: LocationJourney
    ) {
        val journey = LocationJourney(
            id = lastKnownJourney.id,
            user_id = userId,
            from_latitude = lastKnownJourney.from_latitude,
            from_longitude = lastKnownJourney.from_longitude,
            to_latitude = extractedLocation.latitude,
            to_longitude = extractedLocation.longitude,
            routes = lastKnownJourney.routes + listOf(extractedLocation.toRoute()),
            created_at = lastKnownJourney.created_at
        )
        journeyService.updateLastLocationJourney(
            userId = userId,
            journey = journey
        )
        locationCache.putLastJourney(journey, userId)
    }

    /**
     * Save journey when user stops moving i.e., state changes from moving to steady
     * */
    private suspend fun saveJourneyOnJourneyStopped(
        userId: String,
        extractedLocation: Location,
        lastKnownJourney: LocationJourney
    ) {
        val movingJourney = LocationJourney(
            id = lastKnownJourney.id,
            user_id = userId,
            from_latitude = lastKnownJourney.from_latitude,
            from_longitude = lastKnownJourney.from_longitude,
            to_latitude = extractedLocation.latitude,
            to_longitude = extractedLocation.longitude,
            routes = lastKnownJourney.routes + listOf(extractedLocation.toRoute()),
            created_at = lastKnownJourney.created_at,
            update_at = lastKnownJourney.update_at
        )
        journeyService.updateLastLocationJourney(
            userId = userId,
            journey = movingJourney
        )

        // Save journey for steady user and update cache as well:
        var newJourneyId = ""
        journeyService.saveCurrentJourney(
            userId = userId,
            fromLatitude = extractedLocation.latitude,
            fromLongitude = extractedLocation.longitude,
            createdAt = lastKnownJourney.update_at
        ) {
            newJourneyId = it
        }
        val steadyJourney = LocationJourney(
            id = newJourneyId,
            user_id = userId,
            from_latitude = extractedLocation.latitude,
            from_longitude = extractedLocation.longitude
        )
        locationCache.putLastJourney(steadyJourney, userId)
    }

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
     * Check and add local journey to remote database
     * Can happen when user switches space or joins/creates new space
     * */
    suspend fun checkAndAddLocalJourneyToRemoteDatabase(
        userId: String,
        from: Long? = null,
        to: Long? = null
    ): LocationJourney? {
        val lastJourney = locationCache.getLastJourney(userId)

        val journeyFromRemote = journeyService.getLastJourneyLocation(userId)
        lastJourney?.let { journey ->
            if (from != null && to != null) {
                val calendar = Calendar.getInstance()
                calendar.timeInMillis = journey.created_at!!
                val lastKnownDay = calendar.get(Calendar.DAY_OF_MONTH)
                calendar.timeInMillis = from
                val fromDay = calendar.get(Calendar.DAY_OF_MONTH)
                calendar.timeInMillis = to
                val toDay = calendar.get(Calendar.DAY_OF_MONTH)
                if (lastKnownDay in fromDay..toDay) {
                    // If last journey and journey from remote database is same then
                    // no need to add it to remote database
                    if (lastJourney == journeyFromRemote) {
                        return lastJourney
                    }
                    journeyService.saveCurrentJourney(
                        userId = userId,
                        fromLatitude = journey.from_latitude,
                        fromLongitude = journey.from_longitude
                    ) {
                        Timber.d("Local journey added to remote database with steady state")
                    }
                    return lastJourney
                }
            }
        }
        return null
    }

    private fun distance(loc1: Location, loc2: Location): Double {
        val latDiff = loc1.latitude - loc2.latitude
        val lonDiff = loc1.longitude - loc2.longitude
        return sqrt(latDiff * latDiff + lonDiff * lonDiff)
    }

    private fun geometricMedian(locations: List<Location>): Location {
        val result = locations.minByOrNull { candidate ->
            locations.sumOf { location ->
                val distance = distance(candidate, location)
                distance
            }
        } ?: throw IllegalArgumentException("Location list is empty")
        return result
    }

    private fun checkAndSaveLastFiveLocations(extractedLocation: Location, userId: String) {
        val lastFiveLocations = locationCache.getLastFiveLocations(userId) ?: emptyList()
        val lastFiveLocationsWithNewLocation = lastFiveLocations.toMutableList()
        if (lastFiveLocations.isEmpty()) {
            lastFiveLocationsWithNewLocation.add(extractedLocation)
        } else if (lastFiveLocations.size < 5) {
            lastFiveLocationsWithNewLocation.add(extractedLocation)
        } else {
            lastFiveLocationsWithNewLocation.removeAt(0)
            lastFiveLocationsWithNewLocation.add(extractedLocation)
        }
        locationCache.putLastFiveLocations(lastFiveLocationsWithNewLocation, userId)
    }
}
