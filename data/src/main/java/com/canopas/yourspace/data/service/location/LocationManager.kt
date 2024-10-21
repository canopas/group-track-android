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

private const val LOCATION_UPDATE_INTERVAL = 60000L // 1 minute
private const val LOCATION_UPDATE_DISTANCE = 10f // 10 meters
private const val LOCATION_UPDATE_FASTEST_INTERVAL = 5000L // 5 seconds

@SuppressLint("MissingPermission")
@Singleton
class LocationManager @Inject constructor(@ApplicationContext private val context: Context) {

    private var timeBasedLocationResult: LocationRequest
    private var distanceBasedLocationResult: LocationRequest
    private var locationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    init {
        timeBasedLocationResult = createTimeBasedRequest()
        distanceBasedLocationResult = createDistanceBasedRequest()
    }

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

    private fun createTimeBasedRequest(): LocationRequest =
        LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, LOCATION_UPDATE_INTERVAL)
            .apply {
                setGranularity(Granularity.GRANULARITY_PERMISSION_LEVEL)
                setMinUpdateIntervalMillis(LOCATION_UPDATE_INTERVAL)
                setWaitForAccurateLocation(true)
            }.build()

    private fun createDistanceBasedRequest(): LocationRequest =
        LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, LOCATION_UPDATE_FASTEST_INTERVAL)
            .apply {
                setGranularity(Granularity.GRANULARITY_PERMISSION_LEVEL)
                setMinUpdateIntervalMillis(LOCATION_UPDATE_FASTEST_INTERVAL)
                setWaitForAccurateLocation(true)
                setMinUpdateDistanceMeters(LOCATION_UPDATE_DISTANCE)
            }.build()

    internal fun startLocationTracking() {
        if (context.hasFineLocationPermission) {
            // Request location updates for time-based location requests
            locationClient.requestLocationUpdates(
                timeBasedLocationResult,
                locationUpdatePendingIntent
            )

            // Request location updates for distance-based location requests
            locationClient.requestLocationUpdates(
                distanceBasedLocationResult,
                locationUpdatePendingIntent
            )
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
}
