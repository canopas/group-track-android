package com.canopas.yourspace.data.receiver.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.Location
import com.canopas.yourspace.data.repository.SpaceRepository
import com.canopas.yourspace.data.service.auth.AuthService
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import timber.log.Timber
import javax.inject.Inject

object GeofenceBroadcastReceiverConst {
    const val GEOFENCE_EXTRA_KEY_PLACE_NAME = "geofence_place_name"
    const val GEOFENCE_EXTRA_KEY_SPACE_ID = "geofence_space_id"
    const val GEOFENCE_EXTRA_KEY_CREATED_BY = "geofence_created_by"
}

@AndroidEntryPoint
class GeofenceBroadcastReceiver : BroadcastReceiver() {

    @Inject
    lateinit var authService: AuthService

    @Inject
    lateinit var spaceRepository: SpaceRepository
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onReceive(context: Context, intent: Intent) {
        if (authService.currentUser == null) return
        Timber.d("XXX Geofence Alert recieved  - extra ${intent.extras}")

        val placeCreatedBy =
            intent.getStringExtra(GeofenceBroadcastReceiverConst.GEOFENCE_EXTRA_KEY_CREATED_BY)
                ?: return

        val placeName =
            intent.getStringExtra(GeofenceBroadcastReceiverConst.GEOFENCE_EXTRA_KEY_PLACE_NAME)
                ?: return

        val spaceId =
            intent.getStringExtra(GeofenceBroadcastReceiverConst.GEOFENCE_EXTRA_KEY_SPACE_ID)
                ?: return

        if (placeCreatedBy == authService.currentUser?.id) {
            Timber.d("XXX Alert for current user. Ignored: $placeName")
            return
        }

        if (spaceId != spaceRepository.currentSpaceId) {
            Timber.d("XXX Alert for diff space. Ignored: $placeName")
            return
        }

        val geofencingEvent = GeofencingEvent.fromIntent(intent) ?: return

        if (geofencingEvent.hasError()) {
            val errorMessage = GeofenceStatusCodes
                .getStatusCodeString(geofencingEvent.errorCode)
            Timber.e("XXX Geofence error: $errorMessage")
            return
        }

        // spaceRepository.getSpace(spaceId)

        val geofenceTransition = geofencingEvent.geofenceTransition
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER ||
            geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT
        ) {
            val triggeringGeofences = geofencingEvent.triggeringGeofences

            triggeringGeofences?.forEach { geofence ->
                Timber.d("XXX Geofence Alert: ${geofence.requestId}")
                val lat = geofence.latitude
                val lng = geofence.longitude
                val radius = geofence.radius
                val circle = Location("circle").apply { latitude = lat; longitude = lng }
            }

            // sendNotification(geofenceTransitionDetails)
        } else {
            Timber.e("XXX Geofence transition error: $geofenceTransition")
        }
    }

    private fun isWithInGeofence(
        circle: Location,
        memberLocation: Location,
        radius: Double
    ): Boolean {
        val distance = circle.distanceTo(memberLocation)
        return distance <= radius
    }
}
