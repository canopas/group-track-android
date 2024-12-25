package com.canopas.yourspace.domain.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
import com.canopas.yourspace.data.service.auth.AuthService
import com.canopas.yourspace.domain.utils.cancelPowerSavingNotification
import com.canopas.yourspace.domain.utils.sendPowerSavingNotification
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class PowerSavingModeObserver @Inject constructor(
    private val authService: AuthService
) : BroadcastReceiver() {

    private var context: Context? = null

    fun initReceiver(context: Context) {
        this.context = context
        val filter = IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
        context.registerReceiver(this, filter)
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        val powerManager = context?.getSystemService(Context.POWER_SERVICE) as PowerManager
        val isPowerSaving = powerManager.isPowerSaveMode

        if (isPowerSaving) {
            sendPowerSavingNotification(context)
        } else {
            cancelPowerSavingNotification(context)
        }

        if (intent?.action == PowerManager.ACTION_POWER_SAVE_MODE_CHANGED) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    authService.updatePowerSaveModeStatus(isPowerSaving)
                } catch (e: Exception) {
                    Timber.e(e, "Failed to update battery status")
                }
            }
        }
    }

    fun unregisterReceiver() {
        context?.unregisterReceiver(this)
    }
}
