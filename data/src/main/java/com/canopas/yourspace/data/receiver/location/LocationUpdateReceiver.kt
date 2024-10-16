package com.canopas.yourspace.data.receiver.location

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.Location
import com.canopas.yourspace.data.repository.JourneyRepository
import com.canopas.yourspace.data.service.auth.AuthService
import com.canopas.yourspace.data.service.location.ApiJourneyService
import com.canopas.yourspace.data.service.location.ApiLocationService
import com.canopas.yourspace.data.service.location.LocationManager
import com.canopas.yourspace.data.storage.LocationCache
import com.google.android.gms.location.LocationResult
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

const val ACTION_LOCATION_UPDATE = "action.LOCATION_UPDATE"
private const val MINIMUM_DISTANCE = 10f

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

    @Inject
    lateinit var locationCache: LocationCache

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        LocationResult.extractResult(intent)?.let { locationResult ->
            scope.launch {
                try {
                    val userId = authService.currentUser?.id ?: return@launch
                    val lastLocation = locationCache.getLastExtractedLocation(userId)

                    locationResult.locations.forEach { extractedLocation ->
                        if (!extractedLocation.hasAccuracy()) return@forEach

                        val accuracy = extractedLocation.accuracy
                        if (accuracy >= 30.0f) {
                            Timber.e("Skipping location update due to low accuracy: $accuracy")
                            return@forEach
                        }

                        val distance = lastLocation?.distanceTo(extractedLocation) ?: Float.MAX_VALUE

                        if (distance < MINIMUM_DISTANCE) {
                            Timber.e("Distance is less than $MINIMUM_DISTANCE meters, skipping location update")
                        } else {
                            Timber.d("Updating location, distance=$distance, accuracy=$accuracy")
                            updateLocation(userId, extractedLocation)
                        }

                        journeyRepository.saveLocationJourney(extractedLocation, userId)
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Error while saving location")
                }
            }
        }
    }

    private suspend fun updateLocation(userId: String, extractedLocation: Location) {
        locationCache.putLastExtractedLocation(extractedLocation, userId)
        locationService.saveCurrentLocation(
            userId,
            extractedLocation.latitude,
            extractedLocation.longitude,
            System.currentTimeMillis()
        )
    }
}
