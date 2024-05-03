package com.canopas.yourspace.data.service.place

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.canopas.yourspace.data.models.place.ApiPlace
import com.canopas.yourspace.data.receiver.geofence.GeofenceBroadcastReceiver
import com.canopas.yourspace.data.utils.isLocationPermissionGranted
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.Geofence.GEOFENCE_TRANSITION_ENTER
import com.google.android.gms.location.Geofence.GEOFENCE_TRANSITION_EXIT
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeoFenceService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val client: GeofencingClient
) {
    private val geofenceList = mutableMapOf<String, Geofence>()

    private val geofencingPendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        PendingIntent.getBroadcast(
            context,
            1001,
            intent,
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                PendingIntent.FLAG_CANCEL_CURRENT
            } else {
                PendingIntent.FLAG_MUTABLE
            }
        )
    }

    fun addGeofence(places: List<ApiPlace>) {
        if (!context.isLocationPermissionGranted) return

        places
            .filterNot { apiPlace -> apiPlace.latitude == 0.0 || apiPlace.longitude == 0.0 || apiPlace.radius == 0.0 }
            .forEach { apiPlace ->
                val key = apiPlace.id
                val geofence = createGeofence(key, apiPlace)
                geofenceList[key] = geofence
            }

        registerGeofence()
    }

    private fun registerGeofence() {
        if (geofenceList.isEmpty()) return

        val request = GeofencingRequest.Builder().also { request ->
            request.setInitialTrigger(0)
            request.addGeofences(geofenceList.values.toList())
        }.build()

        client.addGeofences(request, geofencingPendingIntent)
            .addOnSuccessListener {
                Timber.d("RegisterGeofence: Success")
            }.addOnFailureListener { exception ->
                Timber.e(exception, "RegisterGeofence: Failed")
            }
    }

    suspend fun deregisterGeofence() = kotlin.runCatching {
        client.removeGeofences(geofenceList.keys.toList()).await()
        geofenceList.clear()
    }

    private fun createGeofence(
        key: String,
        place: ApiPlace
    ): Geofence {
        return Geofence.Builder()
            .setRequestId(key)
            .setCircularRegion(place.latitude, place.longitude, place.radius.toFloat())
            .setTransitionTypes(GEOFENCE_TRANSITION_ENTER or GEOFENCE_TRANSITION_EXIT)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .build()
    }
}
