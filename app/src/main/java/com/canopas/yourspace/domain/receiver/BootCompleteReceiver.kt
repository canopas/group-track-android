package com.canopas.yourspace.domain.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.canopas.yourspace.data.service.location.LocationManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class BootCompleteReceiver : BroadcastReceiver() {

    @Inject
    lateinit var locationManager: LocationManager

    override fun onReceive(context: Context, intent: Intent) {
        if (Intent.ACTION_BOOT_COMPLETED == intent.action) {
            locationManager.startService()
        }
    }
}
