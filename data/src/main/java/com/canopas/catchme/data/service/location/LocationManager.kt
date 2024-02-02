package com.canopas.catchme.data.service.location

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import com.canopas.catchme.data.receiver.location.ACTION_LOCATION_UPDATE
import com.canopas.catchme.data.receiver.location.LocationUpdateReceiver
import com.canopas.catchme.data.utils.hasFineLocationPermission
import com.canopas.catchme.data.utils.isBackgroundLocationPermissionGranted
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
private const val LOCATION_UPDATE_DISTANCE = 10f

@Singleton
class LocationManager @Inject constructor(@ApplicationContext private val context: Context) {

    private var request: LocationRequest
    private var locationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    init {
        request = createRequest()
    }

    suspend fun getLastLocation(): Location? = locationClient.lastLocation.await()

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

    private fun createRequest(): LocationRequest =
        LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 0)
            .apply {
                setGranularity(Granularity.GRANULARITY_PERMISSION_LEVEL)
                setMinUpdateDistanceMeters(LOCATION_UPDATE_DISTANCE)
                setWaitForAccurateLocation(true)
            }.build()

    fun startLocationTracking() {
        if (context.hasFineLocationPermission) {
            locationClient.requestLocationUpdates(request, locationUpdatePendingIntent)
        }
    }

    fun stopLocationTracking() {
        if (!context.isBackgroundLocationPermissionGranted) {
            locationClient.flushLocations()
            locationClient.removeLocationUpdates(locationUpdatePendingIntent)
        }
    }

    fun startService() {
        context.startService(Intent(context, BackgroundLocationService::class.java))
    }
}
