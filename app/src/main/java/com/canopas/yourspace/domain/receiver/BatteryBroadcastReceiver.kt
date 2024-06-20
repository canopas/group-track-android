package com.canopas.yourspace.domain.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import com.canopas.yourspace.data.service.auth.AuthService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class BatteryBroadcastReceiver @Inject constructor(
    private val authService: AuthService
) : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
                    val batteryPct = level.toFloat()
                    authService.updateBatteryStatus(batteryPct)
                } catch (e: Exception) {
                    Timber.e(e, "Failed to update battery status")
                }
            }
        }
    }
}
