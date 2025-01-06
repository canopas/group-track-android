package com.canopas.yourspace.data.repository

import android.location.Location
import com.canopas.yourspace.data.models.location.LocationJourney
import com.canopas.yourspace.data.models.location.isSteady
import com.canopas.yourspace.data.service.location.ApiJourneyService
import com.canopas.yourspace.data.service.location.LocationManager
import com.canopas.yourspace.data.storage.LocationCache
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JourneyRepository @Inject constructor(
    private val journeyService: ApiJourneyService,
    private val locationCache: LocationCache,
    private val locationManager: LocationManager
) {
    suspend fun saveLocationJourney(
        extractedLocation: Location,
        userId: String
    ) {
        try {
            cacheLocations(extractedLocation, userId)

            val lastKnownJourney = getLastKnownLocation(userId)

            val result = getJourney(
                userId = userId,
                newLocation = extractedLocation,
                lastKnownJourney = lastKnownJourney,
                lastLocations = locationCache.getLastFiveLocations(userId) ?: emptyList()
            )

            result?.updatedJourney?.let { journey ->
                locationCache.putLastJourney(journey, userId)
                journeyService.updateJourney(
                    userId = userId,
                    journey = journey
                )
            }

            result?.newJourney?.let { journey ->
                val currentJourney = journeyService.addJourney(
                    userId = userId,
                    newJourney = journey
                )
                locationCache.putLastJourney(currentJourney, userId)
            }

            // Update location request based on state
            if (result?.updatedJourney != null && result.newJourney != null) {
                locationManager.updateRequestBasedOnState(
                    isMoving = !result.newJourney.isSteady()
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "Error while saving location journey")
        }
    }

    /**
     * Get last known location journey from cache
     * If not available, fetch from remote database and save it to cache
     * If not available in remote database as well, save extracted location as new location journey
     * with steady state in cache as well as remote database
     * */
    private suspend fun getLastKnownLocation(
        userid: String
    ): LocationJourney? {
        // Return last location journey if available from cache
        return locationCache.getLastJourney(userid) ?: run {
            // Here, means no location journey available in cache
            // Fetch last location journey from remote database and save it to cache
            val lastJourney = journeyService.getLastJourneyLocation(userid)
            return lastJourney?.let {
                locationCache.putLastJourney(it, userid)
                lastJourney
            }
        }
    }

    private fun cacheLocations(extractedLocation: Location, userId: String) {
        val lastFiveLocations = (locationCache.getLastFiveLocations(userId) ?: emptyList()).toMutableList()
        if (lastFiveLocations.size >= 5) {
            lastFiveLocations.removeAt(0)
        }
        lastFiveLocations.add(extractedLocation)
        locationCache.putLastFiveLocations(lastFiveLocations, userId)
    }
}
