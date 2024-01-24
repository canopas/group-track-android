package com.canopas.catchme.data.receiver.location

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.canopas.catchme.data.service.auth.AuthService
import com.canopas.catchme.data.service.location.ApiLocationService
import com.google.android.gms.location.LocationResult
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Date
import javax.inject.Inject

const val ACTION_LOCATION_UPDATE = "action.LOCATION_UPDATE"

@AndroidEntryPoint
class LocationUpdateReceiver : BroadcastReceiver() {

    @Inject
    lateinit var locationService: ApiLocationService

    @Inject
    lateinit var authService: AuthService
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        LocationResult.extractResult(intent)?.let { locationResult ->
            Timber.d("XXX location update")
            scope.launch {
                locationResult.locations.map {
                    locationService.saveCurrentLocation(
                        authService.currentUser?.id ?: "",
                        it.latitude,
                        it.longitude,
                        Date().time
                    )
                }
            }
        }
    }
}
