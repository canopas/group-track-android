package com.canopas.yourspace.data.storage

import android.location.Location
import android.util.LruCache
import com.canopas.yourspace.data.models.location.LocationJourney
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocationCache @Inject constructor() {
    private val lastJourneyCache = LruCache<String, LocationJourney>(25)
    private val lastFiveLocationsCache = LruCache<String, List<Location>>(25)
    private val lastMovingJourney = LruCache<String, LocationJourney>(25)

    fun putLastJourney(journey: LocationJourney, userId: String) {
        lastJourneyCache.put(userId, journey)
    }

    fun getLastJourney(userId: String): LocationJourney? {
        return lastJourneyCache.get(userId) ?: null
    }

    fun putLastFiveLocations(locations: List<Location>, userId: String) {
        lastFiveLocationsCache.put(userId, locations)
    }

    fun getLastFiveLocations(userId: String): List<Location>? {
        return lastFiveLocationsCache.get(userId) ?: null
    }

    fun putLastMovingJourney(lastMovingJourney: LocationJourney, userId: String) {
        this.lastMovingJourney.put(userId, lastMovingJourney)
    }

    fun getLastMovingJourney(userId: String): LocationJourney? {
        return lastMovingJourney.get(userId) ?: null
    }

    fun clear() {
        lastJourneyCache.evictAll()
        lastFiveLocationsCache.evictAll()
    }
}
