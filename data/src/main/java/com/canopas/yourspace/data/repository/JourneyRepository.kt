package com.canopas.yourspace.data.repository

import android.location.Location
import com.canopas.yourspace.data.models.location.LocationJourney
import com.canopas.yourspace.data.service.location.ApiJourneyService
import com.canopas.yourspace.data.storage.LocationCache
import com.canopas.yourspace.data.storage.UserPreferences
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JourneyRepository @Inject constructor(
    private val journeyService: ApiJourneyService,
    private val locationCache: LocationCache,
    private val userPreferences: UserPreferences
) {
    suspend fun saveLocationJourney(
        extractedLocation: Location,
        userId: String
    ) {
        try {
            val userPreferences = userPreferences.currentUser?.space_ids ?: emptyList()
            userPreferences.forEach { spaceId ->
                cacheLocations(extractedLocation, userId)
                val lastKnownJourney = getLastKnownLocation(userId, spaceId)

                val result = getJourney(
                    userId = userId,
                    newLocation = extractedLocation,
                    lastKnownJourney = lastKnownJourney,
                    lastLocations = locationCache.getLastFiveLocations(userId, spaceId)
                        ?: emptyList()
                )

                result?.updatedJourney?.let { journey ->
                    locationCache.putLastJourney(journey, userId, spaceId)
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
                    locationCache.putLastJourney(currentJourney, userId, spaceId)
                }
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
        userid: String,
        spaceId: String
    ): LocationJourney? {
        // Return last location journey if available from cache
        return locationCache.getLastJourney(userid, spaceId) ?: run {
            // Here, means no location journey available in cache
            // Fetch last location journey from remote database and save it to cache
            val lastJourney = journeyService.getLastJourneyLocation(userid, spaceId)
            if (lastJourney != null) {
                Timber.tag("XXX").e("lastJourney for $spaceId is ${lastJourney.id} ")
            }
            return lastJourney?.let {
                locationCache.putLastJourney(it, userid, spaceId)
                lastJourney
            }
        }
    }

    private fun cacheLocations(extractedLocation: Location, userId: String) {
        userPreferences.currentUser?.space_ids?.forEach { spaceId ->
            val lastFiveLocations =
                (locationCache.getLastFiveLocations(userId, spaceId) ?: emptyList()).toMutableList()
            Timber.tag("xxx").e("LastFiveLocations :- $lastFiveLocations for spaceId $spaceId")
            if (lastFiveLocations.size >= 5) {
                lastFiveLocations.removeAt(0)
            }
            lastFiveLocations.add(extractedLocation)
            locationCache.putLastFiveLocations(lastFiveLocations, userId, spaceId)
        }
    }
}
