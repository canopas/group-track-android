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
import com.canopas.yourspace.data.models.location.toApiLocation
import com.canopas.yourspace.data.models.location.toLocation
import com.canopas.yourspace.data.repository.JourneyRepository
import com.canopas.yourspace.data.service.auth.AuthService
import com.canopas.yourspace.data.service.location.ApiLocationService
import com.canopas.yourspace.data.service.location.LocationJourneyService
import com.canopas.yourspace.data.service.location.LocationManager
import com.canopas.yourspace.data.storage.room.LocationTableDatabase
import com.canopas.yourspace.data.utils.LocationConverters
import com.google.android.gms.location.LocationResult
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Calendar
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
    lateinit var converters: LocationConverters

    @Inject
    lateinit var journeyRepository: JourneyRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        LocationResult.extractResult(intent)?.let { locationResult ->
            scope.launch {
                try {
                    val userId = authService.currentUser?.id ?: return@launch
                    locationResult.locations.forEach { extractedLocation ->

                        val locationData =
                            locationTableDatabase.locationTableDao().getLocationData(userId)

                        val lastLocation = locationData.getLastLocation(converters)
                        checkAndUpdateLastFiveMinLocations(locationData, extractedLocation)

                        lastLocation?.let {
                            Timber.d("XXX distance ${distanceBetween(extractedLocation, lastLocation!!.toLocation())} " +
                                    "last location ${Date(lastLocation!!.created_at!!)}")
                        }
                        val userState =
                            locationData.getUserState(converters, extractedLocation, lastLocation)

                        locationService.saveCurrentLocation(
                            userId,
                            extractedLocation.latitude,
                            extractedLocation.longitude,
                            Date().time,
                            userState = userState ?: UserState.STEADY.value
                        )

                        Timber.d("XXX user state $userState")
                        saveLocationJourney(
                            userState,
                            extractedLocation,
                            lastLocation,
                            userId
                        )

                    }

                } catch (e: Exception) {
                    Timber.e(e, "Error while saving location")
                }
            }
        }
    }

    /**
     * Save location journey based on user state
     * */
    private suspend fun saveLocationJourney(
        userState: Int?,
        extractedLocation: Location,
        lastLocation: ApiLocation?,
        userId: String
    ) {
        try {
            val locationData = userId.getLocationData(locationTableDatabase)
            val lastSteadyLocation = getLastSteadyLocation(locationData)
            val lastMovingLocation = getLastMovingLocation(locationData)
            val lastJourneyLocation = getLastJourneyLocation(locationData)

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
                            recordedAt = Date().time,
                            persistentLocationDate = lastSteadyLocation.created_at
                        )
                        return
                    }
                }
            }

            if (lastJourneyLocation == null || userState == null) {
                Timber.d("XXX last journey & location is null")
                locationJourneyService.saveJourneyIfNullLastLocation(
                    userId,
                    extractedLocation,
                    lastLocation,
                    lastJourneyLocation
                )
            } else {
                when (userState) {
                    UserState.STEADY.value -> {
                        Timber.d("XXX steady user")
                        locationJourneyService.saveJourneyForSteadyUser(
                            userId,
                            extractedLocation,
                            lastJourneyLocation,
                            lastSteadyLocation
                        )
                    }

                    UserState.MOVING.value -> {
                        Timber.d("XXX moving user")
                        locationJourneyService.saveJourneyForMovingUser(
                            userId,
                            extractedLocation,
                            lastLocation,
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
            locationData?.copy(lastMovingLocation = converters.journeyToString(lastMovingLocation))
                ?.let {
                    locationTableDatabase.locationTableDao().updateLocationTable(it)
                }
            return lastMovingLocation
        }
    }

    private suspend fun getLastJourneyLocation(locationData: LocationTable?): LocationJourney? {
        return locationData?.lastLocationJourney?.let {
            return converters.journeyFromString(it)
        } ?: run {
            val lastJourneyLocation =
                locationJourneyService.getLastJourneyLocation(authService.currentUser?.id ?: "")
            locationData?.copy(lastLocationJourney = converters.journeyToString(lastJourneyLocation))
                ?.let {
                    locationTableDatabase.locationTableDao().updateLocationTable(it)
                }
            return lastJourneyLocation
        }
    }

    private suspend fun checkAndUpdateLastFiveMinLocations(
        locationData: LocationTable?,
        extractedLocation: Location
    ) {
        val locations =
            locationData?.lastFiveMinutesLocations?.let { converters.locationListFromString(it) }
        val userId = authService.currentUser?.id ?: ""

        if (locations.isNullOrEmpty()) {
            val lastFiveMinLocations = locationService.getLastFiveMinuteLocations(userId)
            val locationList = lastFiveMinLocations.toList().flatten().toMutableList()
            updateLocationData(locationData, locationList)
        } else {
            val firstLocationFromList = locations.lastOrNull()
            firstLocationFromList?.let {
                if (extractedLocation.time - it.created_at!! > 5 * 60 * 1000) {
                    val updatedLocationList = locations.toMutableList().apply { remove(it) }
                    updatedLocationList.add(extractedLocation.toApiLocation(userId))
                    updateLocationData(locationData, updatedLocationList)
                }
            }
        }
    }

    private suspend fun updateLocationData(
        locationData: LocationTable?,
        updatedLocations: MutableList<ApiLocation>
    ) {
        locationData?.copy(
            lastFiveMinutesLocations = converters.locationListToString(updatedLocations)
        )?.let {
            locationTableDatabase.locationTableDao().updateLocationTable(it)
        }
    }
}
