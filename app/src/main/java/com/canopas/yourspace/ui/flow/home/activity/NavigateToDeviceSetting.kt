package com.canopas.yourspace.ui.flow.home.activity

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.canopas.yourspace.ui.flow.permission.EnablePermissionViewModel
import timber.log.Timber

fun navigateToSettings(context: Context, viewModel: EnablePermissionViewModel) {
    val packageManager = context.packageManager
    val intent = Intent()

    when (Build.MANUFACTURER.lowercase()) {
        "xiaomi" -> {
            intent.component = ComponentName(
                "com.miui.powerkeeper",
                "com.miui.powerkeeper.ui.HiddenAppsConfigActivity"
            )
        }

        "huawei" -> {
            intent.component = ComponentName(
                "com.huawei.systemmanager",
                "com.huawei.systemmanager.optimize.process.ProtectActivity"
            )
        }

        "oppo" -> {
            intent.component = ComponentName(
                "com.coloros.oppoguardelf",
                "com.coloros.powermanager.fuelgaue.PowerUsageModelActivity"
            )
        }

        "vivo" -> {
            intent.component = ComponentName(
                "com.vivo.abe",
                "com.vivo.applicationbehaviorengine.ui.ExcessivePowerManagerActivity"
            )
        }

        "realme" -> {
            intent.component = ComponentName(
                "com.coloros.oppoguardelf",
                "com.coloros.powermanager.fuelgaue.PowerUsageModelActivity"
            )
        }

        "oneplus" -> {
            intent.component = ComponentName(
                "com.oneplus.security",
                "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity"
            )
        }

        else -> {
            val packageName = context.packageName
            intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            intent.data = Uri.parse("package:$packageName")
        }
    }

    Timber.e("Navigating to settings: ${intent.component}")

    try {
        val activities = packageManager.queryIntentActivities(intent, 0)
        if (activities.isNotEmpty()) {
            context.startActivity(intent)
        } else {
            Timber.e("Activity not found. Falling back to general settings.")
            context.startActivity(Intent(Settings.ACTION_BATTERY_SAVER_SETTINGS))
        }
    } catch (e: Exception) {
        Timber.e("Error launching settings: ${e.localizedMessage}")
        context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
    }
    viewModel.changeBatteryOptimizationValue(true)
}
