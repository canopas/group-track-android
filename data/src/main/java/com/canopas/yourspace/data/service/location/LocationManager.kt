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

private const val LOCATION_UPDATE_INTERVAL = 10000L // 10 seconds
private const val STEADY_UPDATE_INTERVAL = 30000L // 30 seconds
private const val MOVING_DISTANCE = 10f // 10 meters
private const val STEADY_DISTANCE = 50f // 50 meters

@SuppressLint("MissingPermission")
@Singleton
class LocationManager @Inject constructor(@ApplicationContext private val context: Context) {

    private var request: LocationRequest
    private var locationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
    private var currentMovingState: Boolean = true

    init {
        request = createRequest()
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

    private fun createRequest(): LocationRequest {
        return LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            if (currentMovingState) LOCATION_UPDATE_INTERVAL else STEADY_UPDATE_INTERVAL
        )
            .apply {
                setGranularity(Granularity.GRANULARITY_PERMISSION_LEVEL)
                setMinUpdateIntervalMillis(if (currentMovingState) LOCATION_UPDATE_INTERVAL else STEADY_UPDATE_INTERVAL)
                setWaitForAccurateLocation(true)
                setMinUpdateDistanceMeters(if (currentMovingState) 0f else 10f)
            }
            .build()
    }

    internal fun updateRequestBasedOnState(isMoving: Boolean) {
        if (currentMovingState == isMoving) return
        currentMovingState = isMoving
        request = createRequest()
        startLocationTracking()
    }

    fun startLocationTracking() {
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
}
