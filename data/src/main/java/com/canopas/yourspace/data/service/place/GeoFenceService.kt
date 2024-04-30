package com.canopas.yourspace.data.service.place

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.canopas.yourspace.data.models.place.ApiPlace
import com.canopas.yourspace.data.receiver.geofence.GeofenceBroadcastReceiver
import com.canopas.yourspace.data.receiver.geofence.GeofenceBroadcastReceiverConst
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

    fun addGeofence(apiPlace: ApiPlace) {
        if (!context.isLocationPermissionGranted) return
        if (geofenceList.containsKey(apiPlace.id)) return
        if (apiPlace.latitude == 0.0 || apiPlace.longitude == 0.0 || apiPlace.radius == 0.0) return

        Timber.d("XXX addGeofence: ${apiPlace.name}")

        registerGeofence(apiPlace)
    }

    fun removeGeofence(key: String) {
        geofenceList.remove(key)
    }

    private fun registerGeofence(apiPlace: ApiPlace) {
        val key = apiPlace.id
        val geofence = createGeofence(key, apiPlace)
        geofenceList[key] = geofence

        val request = GeofencingRequest.Builder().also { request ->
            request.setInitialTrigger(0)
            request.addGeofence(geofence)
        }.build()

        val intent = Intent(context, GeofenceBroadcastReceiver::class.java).apply {
            putExtra(GeofenceBroadcastReceiverConst.GEOFENCE_EXTRA_KEY_PLACE_NAME, apiPlace.name)
            putExtra(GeofenceBroadcastReceiverConst.GEOFENCE_EXTRA_KEY_SPACE_ID, apiPlace.space_id)
            putExtra(
                GeofenceBroadcastReceiverConst.GEOFENCE_EXTRA_KEY_CREATED_BY,
                apiPlace.created_by
            )
        }

        val pendingIntentFlag =
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                PendingIntent.FLAG_CANCEL_CURRENT
            } else {
                PendingIntent.FLAG_MUTABLE
            }

        val geofencingPendingIntent = PendingIntent.getBroadcast(
            context,
            System.currentTimeMillis().toInt(),
            intent,
            pendingIntentFlag
        )

        client.addGeofences(request, geofencingPendingIntent)
            .addOnSuccessListener {
                Timber.d("XXX registerGeofence: Success for ${apiPlace.name}")
            }.addOnFailureListener { exception ->
                Timber.e(exception, "XXX registerGeofence: Failed for ${apiPlace.name}")
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
