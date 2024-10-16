package com.canopas.yourspace.data.service.location

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import com.canopas.yourspace.data.receiver.location.ACTION_LOCATION_UPDATE
import com.canopas.yourspace.data.receiver.location.LocationUpdateReceiver
import com.canopas.yourspace.data.utils.hasCoarseLocationPermission
import com.canopas.yourspace.data.utils.hasFineLocationPermission
import com.canopas.yourspace.data.utils.isBackgroundLocationPermissionGranted
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

private const val LOCATION_UPDATE_INTERVAL = 10000L
private const val MINIMUM_DISTANCE = 10f

@Singleton
class LocationManager @Inject constructor(@ApplicationContext private val context: Context) {

    private lateinit var request: LocationRequest
    private var locationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    init {
        createRequest()
    }

    @SuppressLint("MissingPermission")
    suspend fun getLastLocation(): Location? {
        if (!context.hasCoarseLocationPermission) return null
        return locationClient.lastLocation.await()
    }

    private val locationUpdatePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, LocationUpdateReceiver::class.java)
        intent.action = ACTION_LOCATION_UPDATE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
        } else {
            PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        }
    }

    private fun createRequest() {
        request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, LOCATION_UPDATE_INTERVAL)
            .apply {
                setGranularity(Granularity.GRANULARITY_PERMISSION_LEVEL)
                setMinUpdateIntervalMillis(LOCATION_UPDATE_INTERVAL)
                setWaitForAccurateLocation(true)
            }.build()
    }

    private fun createRequestWithMinimumDistance() {
        request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, LOCATION_UPDATE_INTERVAL)
            .apply {
                setGranularity(Granularity.GRANULARITY_PERMISSION_LEVEL)
                setMinUpdateIntervalMillis(LOCATION_UPDATE_INTERVAL)
                setMinUpdateDistanceMeters(MINIMUM_DISTANCE)
                setWaitForAccurateLocation(true)
            }.build()
    }

    @SuppressLint("MissingPermission")
    internal fun startLocationTracking() {
        if (context.hasFineLocationPermission) {
            locationClient.requestLocationUpdates(request, locationUpdatePendingIntent)
        }
    }

    internal fun stopLocationTracking() {
        if (!context.isBackgroundLocationPermissionGranted) {
            locationClient.flushLocations()
            locationClient.removeLocationUpdates(locationUpdatePendingIntent)
        }
    }

    fun startService() {
        context.startService(Intent(context, BackgroundLocationService::class.java))
    }

    fun stopService() {
        context.stopService(Intent(context, BackgroundLocationService::class.java))
    }

    /**
     * Function to stop time request and start distance request so that the location updates are
     * triggered based on the distance between the current location and the last location to avoid
     * unnecessary location updates.
     * */
    internal fun stopTimeRequestAndStartDistanceRequest() {
        // Return if existing request is already distance request
        if (request.minUpdateDistanceMeters == MINIMUM_DISTANCE) return
        stopLocationTracking()
        createRequestWithMinimumDistance()
        startLocationTracking()
    }

    /**
     * Function to stop distance request and start time request so that the location updates are
     * triggered based on the time interval to get the location updates at regular intervals.
     * */
    internal fun stopDistanceRequestAndStartTimeRequest() {
        // Return if existing request is already time request
        if (request.minUpdateDistanceMeters == 0f) return
        stopLocationTracking()
        createRequest()
        startLocationTracking()
    }
}
