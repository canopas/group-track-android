package com.canopas.yourspace.domain.receiver

import android.annotation.TargetApi
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkInfo
import android.os.Build
import androidx.core.content.ContextCompat.getSystemService
import com.canopas.yourspace.data.models.user.USER_STATE_LOCATION_PERMISSION_DENIED
import com.canopas.yourspace.data.models.user.USER_STATE_NO_NETWORK_OR_PHONE_OFF
import com.canopas.yourspace.data.models.user.USER_STATE_UNKNOWN
import com.canopas.yourspace.data.repository.GeofenceRepository
import com.canopas.yourspace.data.service.auth.AuthService
import com.canopas.yourspace.data.service.location.LocationManager
import com.canopas.yourspace.data.utils.hasAllPermission
import com.canopas.yourspace.data.utils.isLocationPermissionGranted
import com.canopas.yourspace.domain.utils.isNetWorkConnected
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class NetworkConnectionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var authService: AuthService


    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onReceive(context: Context, intent: Intent) {

        if (authService.currentUser == null) return
        if (intent.action != ConnectivityManager.CONNECTIVITY_ACTION) return

        val connected = context.isNetWorkConnected()
        val hasLocationPermission = context.isLocationPermissionGranted
        val state = if (connected && hasLocationPermission) USER_STATE_UNKNOWN
        else if (!connected) USER_STATE_NO_NETWORK_OR_PHONE_OFF
        else USER_STATE_LOCATION_PERMISSION_DENIED

        scope.launch { authService.updateUserSessionState(state) }
    }


}
