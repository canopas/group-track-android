package com.canopas.yourspace.data.receiver.location

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.Location
import com.canopas.yourspace.data.models.location.ApiLocation
import com.canopas.yourspace.data.models.location.LocationJourney
import com.canopas.yourspace.data.models.location.LocationTable
import com.canopas.yourspace.data.models.location.UserState
import com.canopas.yourspace.data.models.location.isSteadyLocation
import com.canopas.yourspace.data.models.location.toLocation
import com.canopas.yourspace.data.service.auth.AuthService
import com.canopas.yourspace.data.service.location.ApiLocationService
import com.canopas.yourspace.data.service.location.LocationJourneyService
import com.canopas.yourspace.data.service.location.LocationManager
import com.canopas.yourspace.data.storage.room.LocationTableDatabase
import com.canopas.yourspace.data.utils.Config.DISTANCE_TO_CHECK_SUDDEN_LOCATION_CHANGE
import com.canopas.yourspace.data.utils.Config.RADIUS_TO_CHECK_USER_STATE
import com.canopas.yourspace.data.utils.Converters
import com.google.android.gms.location.LocationResult
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Date
import javax.inject.Inject

const val ACTION_LOCATION_UPDATE = "action.LOCATION_UPDATE"

@AndroidEntryPoint
class LocationUpdateReceiver : BroadcastReceiver() {

    @Inject
    lateinit var locationService: ApiLocationService

    @Inject
    lateinit var locationJourneyService: LocationJourneyService

    @Inject
    lateinit var locationManager: LocationManager

    @Inject
    lateinit var locationTableDatabase: LocationTableDatabase

    @Inject
    lateinit var authService: AuthService

    @Inject
    lateinit var converters: Converters

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        LocationResult.extractResult(intent)?.let { locationResult ->
            scope.launch {
                try {
                    locationResult.locations.map { extractedLocation ->
                        async {
                            authService.currentUser?.id?.let {
                                val locationData = getLocationData()
                                val lastLocation = getLastLocation(locationData)
                                val userState =
                                    getUserState(extractedLocation, lastLocation, locationData)
                                locationService.saveCurrentLocation(
                                    it,
                                    extractedLocation.latitude,
                                    extractedLocation.longitude,
                                    Date().time,
                                    userState = userState ?: UserState.STEADY.value
                                )
                                saveLocationJourney(userState, extractedLocation, lastLocation)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Error while saving location")
                }
            }
        }
    }

    private fun getLocationData(): LocationTable? {
        return authService.currentUser?.id?.let { userId ->
            locationTableDatabase.locationTableDao().getLocationData(userId)
        }
    }

    private suspend fun getUserState(
        extractedLocation: Location,
        lastLocation: ApiLocation?,
        locationData: LocationTable?
    ): Int? {
        return scope.async {
            try {
                val lastFiveMinuteLocations = getLastFiveMinuteLocations(locationData)
                return@async if (lastFiveMinuteLocations.isNotEmpty() && lastFiveMinuteLocations.isMoving(
                        extractedLocation
                    )
                ) {
                    UserState.MOVING.value
                } else if (lastFiveMinuteLocations.isEmpty()) {
                    null
                } else {
                    if (lastLocation?.user_state != UserState.STEADY.value) {
                        UserState.STEADY.value
                    } else {
                        null
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error while fetching user state")
                null
            }
        }.await()
    }

    private fun saveLocationJourney(
        userState: Int?,
        extractedLocation: Location,
        lastLocation: ApiLocation?
    ) {
        try {
            scope.launch {
                val locationData = getLocationData()
                val lastSteadyLocation = getLastSteadyLocation(locationData)
                val lastMovingLocation = getLastMovingLocation(locationData)
                val lastJourneyLocation = getLastJourneyLocation(locationData)

                if (lastJourneyLocation == null) {
                    locationJourneyService.saveCurrentJourney(
                        userId = authService.currentUser?.id ?: "",
                        fromLatitude = extractedLocation.latitude,
                        fromLongitude = extractedLocation.longitude,
                        currentLocationDuration = extractedLocation.time - (
                            lastLocation?.created_at
                                ?: 0L
                            ),
                        recordedAt = Date().time
                    )
                    return@launch
                }

                if (userState != null) {
                    when (userState) {
                        UserState.STEADY.value -> {
                            val distance = lastSteadyLocation?.toLocation()?.let { location ->
                                distanceBetween(
                                    extractedLocation,
                                    location
                                ).toDouble()
                            } ?: 0.0
                            if (lastJourneyLocation.isSteadyLocation() || distance < DISTANCE_TO_CHECK_SUDDEN_LOCATION_CHANGE) {
                                return@launch
                            }
                            locationJourneyService.saveCurrentJourney(
                                userId = authService.currentUser?.id ?: "",
                                fromLatitude = extractedLocation.latitude,
                                fromLongitude = extractedLocation.longitude,
                                currentLocationDuration = extractedLocation.time - (
                                    lastLocation?.created_at
                                        ?: 0L
                                    ),
                                recordedAt = Date().time
                            )
                        }

                        UserState.MOVING.value -> {
                            var newJourney = LocationJourney(
                                user_id = authService.currentUser?.id ?: "",
                                from_latitude = lastSteadyLocation?.from_latitude
                                    ?: extractedLocation.latitude,
                                from_longitude = lastSteadyLocation?.from_longitude
                                    ?: extractedLocation.longitude,
                                to_latitude = extractedLocation.latitude,
                                to_longitude = extractedLocation.longitude,
                                route_distance = lastSteadyLocation?.toLocation()
                                    ?.let { location ->
                                        distanceBetween(
                                            extractedLocation,
                                            location
                                        ).toDouble()
                                    },
                                route_duration = extractedLocation.time - (
                                    lastSteadyLocation?.created_at
                                        ?: 0L
                                    ),
                                current_location_duration = extractedLocation.time - (
                                    lastSteadyLocation?.created_at
                                        ?: 0L
                                    ),
                                created_at = Date().time
                            )

                            if (lastMovingLocation != null && !lastJourneyLocation.isSteadyLocation()) {
                                newJourney = newJourney.copy(id = lastMovingLocation.id)
                                updateLastMovingLocation(newJourney)
                            } else {
                                locationJourneyService.saveCurrentJourney(
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
                    }
                } else {
                    lastMovingLocation?.let {
                        val timeDifference =
                            extractedLocation.time - lastMovingLocation.created_at!!

                        // If user is at the same location for more than 5 minutes, save the location as steady
                        if (timeDifference > 5 * 60 * 1000 && !lastJourneyLocation.isSteadyLocation()) {
                            locationJourneyService.saveCurrentJourney(
                                userId = authService.currentUser?.id ?: "",
                                fromLatitude = extractedLocation.latitude,
                                fromLongitude = extractedLocation.longitude,
                                currentLocationDuration = extractedLocation.time - (
                                    lastLocation?.created_at
                                        ?: 0L
                                    ),
                                recordedAt = Date().time
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Error while saving location journey")
        }
    }

    private suspend fun getLastLocation(locationData: LocationTable?): ApiLocation? {
        locationData?.let { location ->
            location.latestLocation?.let {
                return converters.locationFromString(it)
            }
        } ?: run {
            val lastLocation = locationService.getLastLocation(authService.currentUser?.id ?: "")
            lastLocation?.let {
                locationTableDatabase.locationTableDao().insertLocationData(
                    LocationTable(
                        userId = authService.currentUser?.id ?: "",
                        latestLocation = converters.locationToString(it)
                    )
                )
            }
            return lastLocation
        }
    }

    private suspend fun getLastJourneyLocation(locationData: LocationTable?): LocationJourney? {
        return locationData?.lastLocationJourney?.let {
            return converters.journeyFromString(it)
        } ?: run {
            val lastJourneyLocation =
                locationJourneyService.getLastJourneyLocation(authService.currentUser?.id ?: "")
            locationData?.copy(lastLocationJourney = converters.journeyToString(lastJourneyLocation))?.let {
                locationTableDatabase.locationTableDao().updateLocationTable(it)
            }
            return lastJourneyLocation
        }
    }

    private suspend fun getLastSteadyLocation(locationData: LocationTable?): LocationJourney? {
        locationData?.lastSteadyLocation?.let {
            return converters.journeyFromString(it)
        } ?: run {
            val lastSteadyLocation =
                locationJourneyService.getLastSteadyLocation(authService.currentUser?.id ?: "")
            locationData?.copy(lastSteadyLocation = converters.journeyToString(lastSteadyLocation))
                ?.let {
                    locationTableDatabase.locationTableDao().updateLocationTable(it)
                }
            return lastSteadyLocation
        }
    }

    private suspend fun getLastMovingLocation(locationData: LocationTable?): LocationJourney? {
        locationData?.lastMovingLocation?.let {
            return converters.journeyFromString(it)
        } ?: run {
            val lastMovingLocation =
                locationJourneyService.getLastMovingLocation(authService.currentUser?.id ?: "")
            locationData?.copy(lastMovingLocation = converters.journeyToString(lastMovingLocation))?.let {
                locationTableDatabase.locationTableDao().updateLocationTable(it)
            }
            return lastMovingLocation
        }
    }

    private fun updateLastMovingLocation(newJourney: LocationJourney) {
        locationJourneyService.updateLastLocationJourney(
            authService.currentUser?.id ?: "",
            newJourney
        )
    }

    private suspend fun getLastFiveMinuteLocations(
        locationData: LocationTable?
    ): List<ApiLocation> {
        val locationList = mutableListOf<ApiLocation>()
        return locationData?.lastFiveMinutesLocations?.let {
            converters.locationListFromString(it)
        }.takeIf {
            val lastLocationCreatedAt = it?.firstOrNull()?.created_at ?: 0L
            it?.isNotEmpty() == true && (Date().time > (lastLocationCreatedAt + 5 * 60 * 1000))
        } ?: run {
            val locations =
                locationService.getLastFiveMinuteLocations(
                    authService.currentUser?.id ?: ""
                )
            locations.collectLatest {
                locationList.addAll(it)
            }
            locationData?.copy(lastFiveMinutesLocations = converters.locationListToString(locationList))
                ?.let {
                    locationTableDatabase.locationTableDao().updateLocationTable(it)
                }
            locationList
        }
    }

    private fun List<ApiLocation>.isMoving(currentLocation: Location): Boolean {
        return any {
            val distance = currentLocation.distanceTo(it.toLocation()).toDouble()
            distance > RADIUS_TO_CHECK_USER_STATE
        }
    }

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
