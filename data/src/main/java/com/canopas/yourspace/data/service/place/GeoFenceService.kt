package com.canopas.yourspace.data.service.place

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.canopas.yourspace.data.models.place.ApiPlace
import com.canopas.yourspace.data.receiver.geofence.GeofenceBroadcastReceiver
import com.canopas.yourspace.data.utils.hasFineLocationPermission
import com.canopas.yourspace.data.utils.isLocationPermissionGranted
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.Geofence.GEOFENCE_TRANSITION_ENTER
import com.google.android.gms.location.Geofence.GEOFENCE_TRANSITION_EXIT
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.lang.Exception
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeoFenceService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val client: GeofencingClient,
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

    fun addGeofence(
        key: String,
        apiPlace: ApiPlace,
    ) {
        if (apiPlace.latitude == 0.0 || apiPlace.longitude == 0.0 || apiPlace.radius == 0.0) return
        geofenceList[key] = createGeofence(key, apiPlace)
    }

    fun removeGeofence(key: String) {
        geofenceList.remove(key)
    }

    fun registerGeofence() {
        if (geofenceList.isEmpty() || !context.isLocationPermissionGranted) return
        client.addGeofences(createGeofencingRequest(), geofencingPendingIntent)
            .addOnSuccessListener {
                Timber.d("registerGeofence: Success")
            }.addOnFailureListener { exception ->
                Timber.e(exception, "registerGeofence: Failed")
            }
    }

    suspend fun deregisterGeofence() = kotlin.runCatching {
        client.removeGeofences(geofencingPendingIntent).await()
        geofenceList.clear()
    }

    private fun createGeofencingRequest(): GeofencingRequest {
        return GeofencingRequest.Builder().apply {
            setInitialTrigger(GEOFENCE_TRANSITION_ENTER or GEOFENCE_TRANSITION_EXIT)
            addGeofences(geofenceList.values.toList())
        }.build()
    }

    private fun createGeofence(
        key: String,
        place: ApiPlace
    ): Geofence {

        return Geofence.Builder()
            .setRequestId(key)
            .setCircularRegion(place.latitude, place.longitude, place.radius.toFloat())
            .setTransitionTypes(GEOFENCE_TRANSITION_ENTER or GEOFENCE_TRANSITION_EXIT)
            .build()
    }

}