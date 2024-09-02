package com.canopas.yourspace.data.repository

import android.location.Location
import com.canopas.yourspace.data.models.location.ApiLocation
import com.canopas.yourspace.data.models.location.LocationJourney
import com.canopas.yourspace.data.models.location.LocationTable
import com.canopas.yourspace.data.models.location.UserState
import com.canopas.yourspace.data.models.location.isSteadyLocation
import com.canopas.yourspace.data.models.location.toApiLocation
import com.canopas.yourspace.data.models.location.toLocation
import com.canopas.yourspace.data.models.location.toLocationFromMovingJourney
import com.canopas.yourspace.data.models.location.toLocationFromSteadyJourney
import com.canopas.yourspace.data.models.location.toRoute
import com.canopas.yourspace.data.service.location.ApiJourneyService
import com.canopas.yourspace.data.service.location.ApiLocationService
import com.canopas.yourspace.data.storage.room.LocationTableDatabase
import com.canopas.yourspace.data.utils.LocationConverters
import kotlinx.coroutines.flow.toList
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

const val MIN_DISTANCE = 100.0
const val MIN_TIME_DIFFERENCE = 5 * 60 * 1000

@Singleton
class JourneyRepository @Inject constructor(
    private val locationTableDatabase: LocationTableDatabase,
    private val journeyService: ApiJourneyService,
    private val locationService: ApiLocationService,
    private val converters: LocationConverters
) {

    suspend fun saveLocationJourney(
        userState: Int,
        extractedLocation: Location,
        userId: String
    ) {
        Timber.e("save location journey")
        try {
            val locationData = getLocationData(userId)
            val lastJourney = getLastJourneyLocation(userId, locationData)
            Timber.e("last location from local: $lastJourney")

            when {
                lastJourney == null -> {
                    Timber.e("last journey is null")
                    journeyService.saveCurrentJourney(
                        userId = userId,
                        fromLatitude = extractedLocation.latitude,
                        fromLongitude = extractedLocation.longitude
                    )
                }

                userState == UserState.STEADY.value -> {
                    Timber.e("user state is steady")
                    saveJourneyForSteadyUser(
                        currentUserId = userId,
                        extractedLocation = extractedLocation,
                        lastKnownJourney = lastJourney
                    )
                }

                userState == UserState.MOVING.value -> {
                    Timber.e("user state is moving")
                    saveJourneyForMovingUser(
                        currentUserId = userId,
                        extractedLocation = extractedLocation,
                        lastKnownJourney = lastJourney
                    )
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error while saving location journey")
        }
        Timber.e("Exit from save location journey------------")
    }

    private fun getLocationData(
        userid: String
    ): LocationTable? {
        Timber.e("get location data from local")
        return locationTableDatabase.locationTableDao().getLocationData(userid)
    }

    private suspend fun getLastJourneyLocation(
        userId: String,
        locationData: LocationTable?
    ): LocationJourney? {
        Timber.e("get last location data from local")
        return locationData?.lastLocationJourney?.let {
            return converters.journeyFromString(it)
        } ?: run {
            val lastJourneyLocation = journeyService.getLastJourneyLocation(userId)
            locationData?.copy(lastLocationJourney = converters.journeyToString(lastJourneyLocation))
                ?.let {
                    locationTableDatabase.locationTableDao().updateLocationTable(it)
                }
            return lastJourneyLocation
        }
    }

    suspend fun getUserState(userId: String, extractedLocation: Location): Int {
        var locationData = getLocationData(userId)
        if (locationData != null) {
            checkAndUpdateLastFiveMinLocations(userId, locationData, extractedLocation)
        } else {
            insertLocationData(userId, extractedLocation)
        }

        locationData = getLocationData(userId)
        val userState = getUserState(locationData, extractedLocation)
        return userState
    }

    private suspend fun insertLocationData(
        userId: String,
        extractedLocation: Location
    ): LocationTable {
        val location = ApiLocation(
            user_id = userId,
            latitude = extractedLocation.latitude,
            longitude = extractedLocation.longitude,
            created_at = System.currentTimeMillis(),
            user_state = UserState.STEADY.value
        )
        Timber.e("insert location data when local is empty: $location")
        val locationTable = LocationTable(
            userId = userId,
            lastFiveMinutesLocations = converters.locationListToString(listOf(location))
        )
        locationTableDatabase.locationTableDao().insertLocationData(locationTable)
        return locationTable
    }

    private suspend fun checkAndUpdateLastFiveMinLocations(
        userId: String,
        locationData: LocationTable,
        extractedLocation: Location
    ) {
        Timber.e("check and update last five min locations")
        val locations =
            locationData.lastFiveMinutesLocations?.let { converters.locationListFromString(it) }

        if (locations.isNullOrEmpty()) {
            Timber.e("last five min locations is empty get from firestore")
            val lastFiveMinLocations =
                locationService.getLastFiveMinuteLocations(userId).toList().flatten()
            updateLocationData(locationData, lastFiveMinLocations)
        } else {
            Timber.e("last five min locations is not empty")
            val latest = locations.maxByOrNull { it.created_at!! }
            if (latest == null || latest.created_at!! < System.currentTimeMillis() - 60000) {
                val updated = locations.toMutableList()
                updated.removeAll { extractedLocation.time - it.created_at!! > MIN_TIME_DIFFERENCE }
                updated.add(extractedLocation.toApiLocation(userId))
                updateLocationData(locationData, updated)
            }
        }
    }

    private suspend fun updateLocationData(
        locationData: LocationTable,
        updatedLocations: List<ApiLocation>
    ) {
        Timber.e("update last five minutes location data into the local")
        val updatedData = locationData.copy(
            lastFiveMinutesLocations = converters.locationListToString(updatedLocations)
        )
        locationTableDatabase.locationTableDao().updateLocationTable(updatedData)
    }

    private fun getUserState(
        locationData: LocationTable?,
        extractedLocation: Location
    ): Int {
        val lastFiveMinuteLocations = getLastFiveMinuteLocations(locationData)
        val medianLocation = geometricMedian(lastFiveMinuteLocations.map { it.toLocation() })
        val distance = medianLocation.distanceTo(extractedLocation).toDouble()
        Timber.e("median location:$medianLocation")
        Timber.e("distance between last location:$distance")
        return if (distance > MIN_DISTANCE) {
            Timber.e(" user state: Moving")
            UserState.MOVING.value
        } else {
            Timber.e(" user state: Steady")
            UserState.STEADY.value
        }
    }

    /**
     * Get last five minute locations from local database ~ 1 location per minute
     * */
    private fun getLastFiveMinuteLocations(locationData: LocationTable?): List<ApiLocation> {
        return locationData?.lastFiveMinutesLocations?.let {
            converters.locationListFromString(it)
        } ?: emptyList()
    }

    /**
     * Save journey for moving user
     * */
    private suspend fun saveJourneyForMovingUser(
        currentUserId: String,
        extractedLocation: Location,
        lastKnownJourney: LocationJourney
    ) {
        Timber.e("in moving function")
        if (lastKnownJourney.isSteadyLocation()) {
            val updatedRoutes = lastKnownJourney.routes.toMutableList()
            updatedRoutes.add(extractedLocation.toRoute())
            journeyService.saveCurrentJourney(
                userId = currentUserId,
                fromLatitude = lastKnownJourney.from_latitude,
                fromLongitude = lastKnownJourney.from_longitude,
                toLatitude = extractedLocation.latitude,
                toLongitude = extractedLocation.longitude,
                routeDistance = distanceBetween(
                    extractedLocation,
                    lastKnownJourney.toLocationFromSteadyJourney()
                ).toDouble(),
                routeDuration = extractedLocation.time - lastKnownJourney.update_at!!
            )
        } else {
            val distance = lastKnownJourney.toLocationFromMovingJourney().distanceTo(
                extractedLocation
            ).toDouble()
            val updatedRoutes = lastKnownJourney.routes.toMutableList()
            updatedRoutes.add(extractedLocation.toRoute())
            Timber.e("moving distance: $distance")
            journeyService.updateLastLocationJourney(
                userId = currentUserId,
                lastKnownJourney.copy(
                    to_latitude = extractedLocation.latitude,
                    to_longitude = extractedLocation.longitude,
                    route_distance = (lastKnownJourney.route_distance ?: 0.0) + distance,
                    route_duration = extractedLocation.time - lastKnownJourney.created_at!!,
                    routes = updatedRoutes,
                    update_at = System.currentTimeMillis()
                )
            )
        }
        Timber.e("Exit from moving function")
    }

    /**
     * Save journey as steady when user state is steady
     * */
    private suspend fun saveJourneyForSteadyUser(
        currentUserId: String,
        extractedLocation: Location,
        lastKnownJourney: LocationJourney
    ) {
        val lastLatLong =
            if (lastKnownJourney.isSteadyLocation()) {
                lastKnownJourney.toLocationFromSteadyJourney()
            } else {
                lastKnownJourney.toLocationFromMovingJourney()
            }

        val distance = distanceBetween(extractedLocation, lastLatLong)
        val timeDifference = extractedLocation.time - lastKnownJourney.created_at!!

        Timber.e("in steady function new location: $extractedLocation last location: $lastLatLong")
        Timber.e("in steady function distance: $distance timeDifference: $timeDifference")
        when {
            timeDifference > MIN_TIME_DIFFERENCE && distance > MIN_DISTANCE -> {
                Timber.e("time and distance in more then minimum")
                if (lastKnownJourney.isSteadyLocation()) {
                    journeyService.updateLastLocationJourney(
                        userId = currentUserId,
                        lastKnownJourney.copy(update_at = System.currentTimeMillis())
                    )
                } else {
                    journeyService.saveCurrentJourney(
                        currentUserId,
                        fromLatitude = lastKnownJourney.to_latitude!!,
                        fromLongitude = lastKnownJourney.to_longitude!!,
                        createdAt = lastKnownJourney.update_at
                    )
                }

                journeyService.saveCurrentJourney(
                    userId = currentUserId,
                    fromLatitude = lastKnownJourney.to_latitude
                        ?: lastKnownJourney.from_latitude,
                    fromLongitude = lastKnownJourney.to_longitude
                        ?: lastKnownJourney.from_longitude,
                    toLatitude = extractedLocation.latitude,
                    toLongitude = extractedLocation.longitude,
                    routeDistance = distance.toDouble(),
                    routeDuration = extractedLocation.time - lastKnownJourney.update_at!!,
                    createdAt = lastKnownJourney.update_at,
                    updateAt = System.currentTimeMillis()
                )
            }

            timeDifference < MIN_TIME_DIFFERENCE && distance > MIN_DISTANCE -> {
                Timber.e("time less then minimum distance and distance more then min distance")
                val updatedRoutes = lastKnownJourney.routes.toMutableList()
                updatedRoutes.add(extractedLocation.toRoute())
                journeyService.updateLastLocationJourney(
                    userId = currentUserId,
                    journey = lastKnownJourney.copy(
                        to_longitude = extractedLocation.longitude,
                        to_latitude = extractedLocation.latitude,
                        route_distance = lastKnownJourney.toLocationFromSteadyJourney()
                            .distanceTo(
                                extractedLocation
                            ).toDouble(),
                        routes = updatedRoutes,
                        route_duration = extractedLocation.time - lastKnownJourney.created_at,
                        update_at = System.currentTimeMillis()
                    )
                )

            }

            timeDifference > MIN_TIME_DIFFERENCE && distance < MIN_DISTANCE -> {
                Timber.e("time more then minimum distance and distance less then min distance")
                if (lastKnownJourney.isSteadyLocation()) {
                    journeyService.updateLastLocationJourney(
                        userId = currentUserId,
                        lastKnownJourney.copy(
                            update_at = System.currentTimeMillis()
                        )
                    )
                } else {
                    journeyService.saveCurrentJourney(
                        currentUserId,
                        fromLatitude = extractedLocation.latitude,
                        fromLongitude = extractedLocation.longitude,
                        createdAt = System.currentTimeMillis()
                    )
                }
            }

            timeDifference < MIN_TIME_DIFFERENCE && distance < MIN_DISTANCE -> {
                Timber.e("time and distance is less then minimum")
                journeyService.updateLastLocationJourney(
                    userId = currentUserId,
                    lastKnownJourney.copy(update_at = System.currentTimeMillis())
                )
            }
        }
    }

    /**
     * Calculate distance between two locations
     * */
    private fun distanceBetween(location1: Location, location2: Location): Float {
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

    private fun geometricMedian(locations: List<Location>): Location {
        return locations.minByOrNull { candidate ->
            locations.sumOf { location -> candidate.distanceTo(location).toDouble() }
        } ?: throw IllegalArgumentException("Location list is empty")
    }
}
