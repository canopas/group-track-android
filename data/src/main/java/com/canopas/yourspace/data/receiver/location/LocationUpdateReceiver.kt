package com.canopas.yourspace.data.receiver.location

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.Location
import com.canopas.yourspace.data.models.location.ApiLocation
import com.canopas.yourspace.data.models.location.UserState
import com.canopas.yourspace.data.models.location.toLocation
import com.canopas.yourspace.data.service.auth.AuthService
import com.canopas.yourspace.data.service.location.ApiLocationService
import com.canopas.yourspace.data.service.location.LocationManager
import com.google.android.gms.location.LocationResult
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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
    lateinit var locationManager: LocationManager

    @Inject
    lateinit var authService: AuthService
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        LocationResult.extractResult(intent)?.let { locationResult ->
            scope.launch {
                try {
                    locationResult.locations.map { extractedLocation ->

                        val lastFiveMinuteLocations = getLastFiveMinuteLocations()
                        val lastLocation = lastLocation()

                        var userState =
                            if (lastFiveMinuteLocations.isNotEmpty() && lastFiveMinuteLocations.isMoving(
                                    extractedLocation
                                )
                            ) {
                                UserState.MOVING.value
                            } else if (lastFiveMinuteLocations.isEmpty()) {
                                UserState.STEADY.value
                            } else {
                                lastLocation?.let {
                                    if (it.user_state == UserState.MOVING.value) {
                                        UserState.REST_POINT.value
                                    } else {
                                        UserState.STEADY.value
                                    }
                                }
                            }
                        lastLocation?.let {
                            if (it.user_state == null) {
                                userState = UserState.REST_POINT.value
                            }
                        }
                        locationService.saveCurrentLocation(
                            authService.currentUser?.id ?: "",
                            extractedLocation.latitude,
                            extractedLocation.longitude,
                            Date().time,
                            userState
                        )
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Error while saving location")
                }
            }
        }
    }

    private suspend fun lastLocation(): ApiLocation? {
        return locationService.getLastLocation(authService.currentUser?.id ?: "")
    }

    private suspend fun getLastFiveMinuteLocations(): List<ApiLocation> {
        val lastFiveMinuteLocations =
            locationService.getLastFiveMinuteLocations(authService.currentUser?.id ?: "")
        val locationsList = mutableListOf<ApiLocation>()
        lastFiveMinuteLocations.collectLatest { locations ->
            locationsList.addAll(locations)
        }
        return locationsList
    }

    private fun List<ApiLocation>.isMoving(currentLocation: Location): Boolean {
        return any {
            val distance = currentLocation.distanceTo(it.toLocation()).toDouble()
            distance > 100
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
