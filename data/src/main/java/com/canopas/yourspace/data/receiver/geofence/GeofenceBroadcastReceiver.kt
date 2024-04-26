package com.canopas.yourspace.data.receiver.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class GeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent) ?: return
        if (geofencingEvent.hasError()) {
            val errorMessage = GeofenceStatusCodes
                .getStatusCodeString(geofencingEvent.errorCode)
            Timber.e("Geofence error: $errorMessage")
            return
        }
        val geofenceTransition = geofencingEvent.geofenceTransition
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER ||
            geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT
        ) {

            val triggeringGeofences = geofencingEvent.triggeringGeofences

            val alertString = "Geofence Alert :" +
                    " Trigger ${geofencingEvent.triggeringGeofences}" +
                    " Transition ${geofencingEvent.geofenceTransition}"

            Timber.d("XXX Geofence Alert: $alertString")

            // sendNotification(geofenceTransitionDetails)

        } else {
            Timber.e("Geofence transition error: $geofenceTransition")
        }
    }

}

