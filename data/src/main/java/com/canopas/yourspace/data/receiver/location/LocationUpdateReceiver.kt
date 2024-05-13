package com.canopas.yourspace.data.receiver.location

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.Location
import com.canopas.yourspace.data.models.location.ApiLocation
import com.canopas.yourspace.data.models.location.LocationJourney
import com.canopas.yourspace.data.models.location.LocationTable
import com.canopas.yourspace.data.models.location.UserState
import com.canopas.yourspace.data.models.location.isSteadyLocation
import com.canopas.yourspace.data.models.location.toApiLocation
import com.canopas.yourspace.data.repository.JourneyRepository
import com.canopas.yourspace.data.service.auth.AuthService
import com.canopas.yourspace.data.service.location.ApiLocationService
import com.canopas.yourspace.data.service.location.LocationJourneyService
import com.canopas.yourspace.data.service.location.LocationManager
import com.canopas.yourspace.data.storage.room.LocationTableDatabase
import com.canopas.yourspace.data.utils.Config.FIVE_MINUTES
import com.canopas.yourspace.data.utils.LocationConverters
import com.google.android.gms.location.LocationResult
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Calendar
import java.util.Date
import javax.inject.Inject

const val ACTION_LOCATION_UPDATE = "action.LOCATION_UPDATE"

@AndroidEntryPoint
class LocationUpdateReceiver : BroadcastReceiver() {

    @Inject
    lateinit var locationService: ApiLocationService

    @Inject
    lateinit var locationJourneyService: LocationJourneyService

    @Inject
    lateinit var locationManager: LocationManager

    @Inject
    lateinit var locationTableDatabase: LocationTableDatabase

    @Inject
    lateinit var authService: AuthService

    @Inject
    lateinit var converters: LocationConverters

    @Inject
    lateinit var journeyRepository: JourneyRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        LocationResult.extractResult(intent)?.let { locationResult ->
            Timber.d("XXXX --------------- ")

            scope.launch {
                try {
                    val userId = authService.currentUser?.id ?: return@launch
                    locationResult.locations.forEach { extractedLocation ->
                        val userState = journeyRepository.getUserState(userId, extractedLocation)
                        Timber.d("XXX user state ${userState} is moving ${userState == UserState.MOVING.value}")
                        locationService.saveCurrentLocation(
                            userId,
                            extractedLocation.latitude,
                            extractedLocation.longitude,
                            System.currentTimeMillis(),
                            userState = userState ?: UserState.STEADY.value
                        )
                        journeyRepository.saveLocationJourney(userState, extractedLocation, userId)
                    }

                } catch (e: Exception) {
                    Timber.e(e, "Error while saving location")
                }
            }
        }
    }

}
