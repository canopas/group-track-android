package com.canopas.yourspace.data.receiver.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.canopas.yourspace.data.R
import com.canopas.yourspace.data.service.auth.AuthService
import com.canopas.yourspace.data.service.place.ApiPlaceService
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import com.google.firebase.functions.FirebaseFunctions
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class GeofenceBroadcastReceiver : BroadcastReceiver() {

    @Inject
    lateinit var authService: AuthService

    @Inject
    lateinit var functions: FirebaseFunctions

    @Inject
    lateinit var placeService: ApiPlaceService

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onReceive(context: Context, intent: Intent) {
        if (authService.currentUser == null) return

        val geofencingEvent = GeofencingEvent.fromIntent(intent) ?: return
        if (geofencingEvent.hasError()) {
            val errorMessage = GeofenceStatusCodes
                .getStatusCodeString(geofencingEvent.errorCode)
            Timber.e("Geofence error: $errorMessage")
            return
        }

        try {
            val eventBy = authService.currentUser!!

            val geofenceTransition = geofencingEvent.geofenceTransition

            if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER ||
                geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT
            ) {
                val triggeringGeofences = geofencingEvent.triggeringGeofences

                Timber.d("Geofence Alert received")
                triggeringGeofences?.forEach { geofence ->
                    val placeId = geofence.requestId

                    scope.launch {
                        val place = placeService.getPlace(placeId) ?: return@launch
                        val placeName = place.name
                        val spaceId = place.space_id

                        val message =
                            if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
                                context.getString(
                                    R.string.geofence_alert_event_entered,
                                    eventBy.first_name,
                                    placeName
                                )
                            } else {
                                context.getString(
                                    R.string.geofence_alert_event_exited,
                                    eventBy.first_name,
                                    placeName
                                )
                            }

                        val data = mapOf(
                            "placeId" to placeId,
                            "spaceId" to spaceId,
                            "eventBy" to eventBy.id,
                            "message" to message
                        )

                        functions.getHttpsCallable("sendGeoFenceNotification").call(data)
                            .await()
                    }
                }
            } else {
                Timber.e("Geofence transition error: $geofenceTransition")
            }
        } catch (e: Exception) {
            Timber.e(e, "Error while processing geofence alert")
        }
    }
}
