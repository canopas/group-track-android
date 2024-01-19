package com.canopas.catchme.data.service.location

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Looper
import com.canopas.catchme.data.receiver.location.ACTION_LOCATION_UPDATE
import com.canopas.catchme.data.receiver.location.LocationUpdateReceiver
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.LocationAvailability
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

private const val LOCATION_UPDATE_INTERVAL = 10000L

@Singleton
class LocationManager @Inject constructor(@ApplicationContext private val context: Context) {

    private var request: LocationRequest
    private var locationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    init {
        request = createRequest()
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

    private fun createRequest(): LocationRequest =
        LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, LOCATION_UPDATE_INTERVAL).apply {
            setGranularity(Granularity.GRANULARITY_PERMISSION_LEVEL)
            setWaitForAccurateLocation(true)
        }.build()


    fun startLocationTracking() {
        locationClient.requestLocationUpdates(request, locationUpdatePendingIntent)
    }

    fun stopLocationTracking() {
        locationClient.flushLocations()
        locationClient.removeLocationUpdates(locationUpdatePendingIntent)
    }

    fun startService() {
        context.startService(Intent(context, LocationService::class.java))
    }
}