package com.canopas.yourspace.data.receiver.location

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.canopas.yourspace.data.repository.JourneyRepository
import com.canopas.yourspace.data.service.auth.AuthService
import com.canopas.yourspace.data.service.location.ApiJourneyService
import com.canopas.yourspace.data.service.location.ApiLocationService
import com.canopas.yourspace.data.service.location.LocationManager
import com.google.android.gms.location.LocationResult
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

const val ACTION_LOCATION_UPDATE = "action.LOCATION_UPDATE"

@AndroidEntryPoint
class LocationUpdateReceiver : BroadcastReceiver() {

    @Inject
    lateinit var locationService: ApiLocationService

    @Inject
    lateinit var journeyService: ApiJourneyService

    @Inject
    lateinit var locationManager: LocationManager

    @Inject
    lateinit var authService: AuthService

    @Inject
    lateinit var journeyRepository: JourneyRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        LocationResult.extractResult(intent)?.let { locationResult ->
            scope.launch {
                try {
                    Timber.d("Location update received: $locationResult")

                    val userId = authService.currentUser?.id ?: return@launch

                    locationResult.locations.forEach { extractedLocation ->
                        if (extractedLocation.hasAccuracy() && extractedLocation.accuracy >= 30.0f) {
                            Timber.e("Skipping location update due to low accuracy: ${extractedLocation.accuracy}")
                            return@forEach
                        }

                        locationService.saveCurrentLocation(
                            userId,
                            extractedLocation.latitude,
                            extractedLocation.longitude,
                            System.currentTimeMillis()
                        )
                        journeyRepository.saveLocationJourney(extractedLocation, userId)
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Error while saving location")
                }
            }
        }
    }
}
