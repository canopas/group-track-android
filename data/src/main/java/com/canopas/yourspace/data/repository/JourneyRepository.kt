package com.canopas.yourspace.data.repository

import android.location.Location
import com.canopas.yourspace.data.models.location.ApiLocation
import com.canopas.yourspace.data.models.location.LocationJourney
import com.canopas.yourspace.data.models.location.LocationTable
import com.canopas.yourspace.data.models.location.UserState
import com.canopas.yourspace.data.models.location.isSteadyLocation
import com.canopas.yourspace.data.models.location.toApiLocation
import com.canopas.yourspace.data.models.location.toLocation
import com.canopas.yourspace.data.models.location.toLocationFromSteadyJourney
import com.canopas.yourspace.data.service.location.ApiLocationService
import com.canopas.yourspace.data.service.location.LocationJourneyService
import com.canopas.yourspace.data.storage.room.LocationTableDatabase
import com.canopas.yourspace.data.utils.Config
import com.canopas.yourspace.data.utils.LocationConverters
import com.canopas.yourspace.data.utils.getLocationData
import kotlinx.coroutines.flow.toList
import timber.log.Timber
import java.util.Calendar
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JourneyRepository @Inject constructor(
    private val locationTableDatabase: LocationTableDatabase,
    private val locationJourneyService: LocationJourneyService,
    private val locationService: ApiLocationService,
    private val converters: LocationConverters

) {

    suspend fun saveLocationJourney(
        userState: Int?,
        extractedLocation: Location,
        userId: String
    ) {
        try {
            val locationData = userId.getLocationData(locationTableDatabase)

            val lastLocation = getLastLocation(locationData)

            val lastSteadyLocation = getLastSteadyLocation(userId, locationData)
            val lastMovingLocation = getLastMovingLocation(userId, locationData)
            val lastJourneyLocation = getLastJourneyLocation(userId, locationData)

            if (lastJourneyLocation?.isSteadyLocation() == true) {
                lastSteadyLocation?.let {
                    val calendar1 = Calendar.getInstance().apply {
                        timeInMillis = lastSteadyLocation.created_at!!
                    }
                    val calendar2 = Calendar.getInstance().apply {
                        timeInMillis = extractedLocation.time
                    }
                    val calendar3 = Calendar.getInstance().apply {
                        timeInMillis = lastSteadyLocation.persistent_location_date ?: Date().time
                    }
                    // Check if day is changed
                    if ((calendar1.get(Calendar.DAY_OF_MONTH) != calendar2.get(Calendar.DAY_OF_MONTH)) ||
                        (calendar2.get(Calendar.DAY_OF_MONTH) != calendar3.get(Calendar.DAY_OF_MONTH))
                    ) {
                        Timber.d("XXX day changed")
                        locationJourneyService.saveCurrentJourney(
                            userId = userId,
                            fromLatitude = lastSteadyLocation.from_latitude,
                            fromLongitude = lastSteadyLocation.from_longitude,
                            currentLocationDuration = extractedLocation.time - lastSteadyLocation.created_at!!,
                            persistentLocationDate = lastSteadyLocation.created_at
                        )
                        return
                    }
                }
            }


            if (lastJourneyLocation == null || userState == null) {
                Timber.d("XXX has last journey ${lastJourneyLocation != null} or userState is  $userState")
                saveJourneyIfNullLastLocation(
                    userId,
                    extractedLocation,
                    lastLocation,
                    lastJourneyLocation
                )
            } else {
                when (userState) {
                    UserState.STEADY.value -> {
                        Timber.d("XXX steady user")
                        saveJourneyForSteadyUser(
                            userId,
                            extractedLocation,
                            lastJourneyLocation,
                            lastSteadyLocation
                        )
                    }

                    UserState.MOVING.value -> {
                        Timber.d("XXX moving user")
                        saveJourneyForMovingUser(
                            userId,
                            extractedLocation,
                            lastJourneyLocation,
                            lastSteadyLocation,
                            lastMovingLocation
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error while saving location journey")
        }
    }

    private suspend fun getLastSteadyLocation(
        userId: String,
        locationData: LocationTable?
    ): LocationJourney? {
        locationData?.lastSteadyLocation?.let {
            return converters.journeyFromString(it)
        } ?: run {
            val lastSteadyLocation =
                locationJourneyService.getLastSteadyLocation(userId)
            locationData?.copy(lastSteadyLocation = converters.journeyToString(lastSteadyLocation))
                ?.let {
                    locationTableDatabase.locationTableDao().updateLocationTable(it)
                }
            return lastSteadyLocation
        }
    }

    private suspend fun getLastMovingLocation(
        userId: String,
        locationData: LocationTable?
    ): LocationJourney? {
        locationData?.lastMovingLocation?.let {
            return converters.journeyFromString(it)
        } ?: run {
            val lastMovingLocation =
                locationJourneyService.getLastMovingLocation(userId)
            locationData?.copy(lastMovingLocation = converters.journeyToString(lastMovingLocation))
                ?.let {
                    locationTableDatabase.locationTableDao().updateLocationTable(it)
                }
            return lastMovingLocation
        }
    }

    private suspend fun getLastJourneyLocation(
        userId: String,
        locationData: LocationTable?
    ): LocationJourney? {
        return locationData?.lastLocationJourney?.let {
            return converters.journeyFromString(it)
        } ?: run {
            val lastJourneyLocation =
                locationJourneyService.getLastJourneyLocation(userId)
            locationData?.copy(lastLocationJourney = converters.journeyToString(lastJourneyLocation))
                ?.let {
                    locationTableDatabase.locationTableDao().updateLocationTable(it)
                }
            return lastJourneyLocation
        }
    }

    /**
     * Get last five minute locations from local database ~ 1 location per minute
     * */
    private fun getLastFiveMinuteLocations(
        locationData: LocationTable?
    ): List<ApiLocation> {
        return locationData?.lastFiveMinutesLocations?.let {
            converters.locationListFromString(it)
        } ?: emptyList()
    }

    private fun getLastLocation(locationData: LocationTable?): ApiLocation? {
        return locationData?.latestLocation?.let {
            converters.locationFromString(it)
        }
    }


    private suspend fun checkAndUpdateLastFiveMinLocations(
        userId: String,
        locationData: LocationTable,
        extractedLocation: Location
    ) {
        val locations =
            locationData.lastFiveMinutesLocations?.let { converters.locationListFromString(it) }
        Timber.d("XXX local 5min location ${locations?.size}")
        if (locations.isNullOrEmpty()) {
            val lastFiveMinLocations =
                locationService.getLastFiveMinuteLocations(userId).toList().flatten()
            Timber.d("XXX getLastFiveMinuteLocations from remote ${lastFiveMinLocations.size}")
            updateLocationData(locationData, lastFiveMinLocations)
        } else {
            updateLocationData(locationData, locations)

            val latest = locations.maxByOrNull { it.created_at!! }
            if (latest == null || latest.created_at!! < System.currentTimeMillis() - 60000) {
                Timber.d("XXX add new location ${Date(extractedLocation.time)}")
                val updated = locations.toMutableList()
                updated.removeAll { extractedLocation.time - it.created_at!! > Config.FIVE_MINUTES }
                updated.add(extractedLocation.toApiLocation(userId))
                updateLocationData(locationData, updated)

            }
        }
    }

    private suspend fun updateLocationData(
        locationData: LocationTable,
        updatedLocations: List<ApiLocation>
    ) {

        val updatedData = locationData.copy(
            lastFiveMinutesLocations = converters.locationListToString(updatedLocations)
        )
        locationTableDatabase.locationTableDao().updateLocationTable(updatedData)

    }

    suspend fun getUserState(userId: String, extractedLocation: Location): Int? {
        var locationData =
            locationTableDatabase.locationTableDao().getLocationData(userId)

        val lastLocation = getLastLocation(locationData)


        locationData?.let { checkAndUpdateLastFiveMinLocations(userId, it, extractedLocation) }

        locationData =
            locationTableDatabase.locationTableDao().getLocationData(userId)

        val userState = getUserState(locationData, extractedLocation, lastLocation)

        return userState

    }

    private fun getUserState(
        locationData: LocationTable?,
        extractedLocation: Location,
        lastLocation: ApiLocation?
    ): Int? {
        val lastFiveMinuteLocations = getLastFiveMinuteLocations(locationData)

        if (lastFiveMinuteLocations.isEmpty()) return null
        if (lastFiveMinuteLocations.isMoving(extractedLocation)) return UserState.MOVING.value
        if (lastLocation?.user_state != UserState.STEADY.value) return UserState.STEADY.value
        return null
    }

    private suspend fun saveJourneyIfNullLastLocation(
        currentUserId: String,
        extractedLocation: Location,
        lastLocation: ApiLocation?,
        lastJourneyLocation: LocationJourney?
    ) {
        if (lastJourneyLocation == null) {
            locationJourneyService.saveCurrentJourney(
                userId = currentUserId,
                fromLatitude = extractedLocation.latitude,
                fromLongitude = extractedLocation.longitude,
                currentLocationDuration = extractedLocation.time - (lastLocation?.created_at ?: 0L)
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
                Timber.d(
                    "XXX Sudden location change detected distance $distance time ${timeDifference > 5 * 60 * 1000} " +
                            "date newJourney ${Date(lastJourneyLocation.created_at!!)}"
                )
                locationJourneyService.saveCurrentJourney(
                    userId = currentUserId,
                    fromLatitude = extractedLocation.latitude,
                    fromLongitude = extractedLocation.longitude,
                    currentLocationDuration = extractedLocation.time - (lastLocation?.created_at
                        ?: 0L)
                )
            }
        }
    }

    /**
     * Save journey for moving user
     * */
    private suspend fun saveJourneyForMovingUser(
        currentUserId: String,
        extractedLocation: Location,
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

        if (lastMovingLocation != null && !lastJourneyLocation.isSteadyLocation()) {
            newJourney = newJourney.copy(id = lastMovingLocation.id)
            Timber.d("XXX Updating last location journey ${newJourney.id}")
            locationJourneyService.updateLastLocationJourney(currentUserId, newJourney)
        } else {
            Timber.d("XXX Saving new location journey")
            locationJourneyService.saveCurrentJourney(
                userId = newJourney.user_id,
                fromLatitude = newJourney.from_latitude,
                fromLongitude = newJourney.from_longitude,
                toLatitude = newJourney.to_latitude,
                toLongitude = newJourney.to_longitude,
                routeDistance = newJourney.route_distance,
                routeDuration = newJourney.route_duration,
                currentLocationDuration = newJourney.current_location_duration
            )
        }
    }

    /**
     * Save journey as steady when user state is steady
     * */
    private suspend fun saveJourneyForSteadyUser(
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
        locationJourneyService.saveCurrentJourney(
            userId = currentUserId,
            fromLatitude = extractedLocation.latitude,
            fromLongitude = extractedLocation.longitude,
            currentLocationDuration = extractedLocation.time - lastJourneyLocation.created_at
        )
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

}

/**
 * Check if user is moving or steady
 * */
fun List<ApiLocation>.isMoving(currentLocation: Location): Boolean {
    return any {
        val distance = currentLocation.distanceTo(it.toLocation()).toDouble()
        distance > Config.RADIUS_TO_CHECK_USER_STATE
    }
}
