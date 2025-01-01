package com.canopas.yourspace.domain.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.canopas.yourspace.R
import com.canopas.yourspace.ui.MainActivity
import timber.log.Timber

const val YOURSPACE_CHANNEL_POWER_SAVING = "your_space_notification_channel_power_saving"
const val NOTIFICATION_ID_POWER_SAVING = 102

fun sendPowerSavingNotification(context: Context) {
    val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }
    val pendingIntent = PendingIntent.getActivity(
        context,
        0,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val notificationBuilder = NotificationCompat.Builder(context, YOURSPACE_CHANNEL_POWER_SAVING)
        .setSmallIcon(R.drawable.app_logo)
        .setContentTitle(context.getString(R.string.battery_saving_notification_title))
        .setContentText(context.getString(R.string.battery_saving_notification_description))
        .setAutoCancel(true)
        .setContentIntent(pendingIntent)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            YOURSPACE_CHANNEL_POWER_SAVING,
            context.getString(R.string.notification_channel_power_saving),
            NotificationManager.IMPORTANCE_HIGH
        )
        notificationManager.createNotificationChannel(channel)
    }

    Timber.e("Power saving notification sent")
    notificationManager.notify(NOTIFICATION_ID_POWER_SAVING, notificationBuilder.build())
}

fun cancelPowerSavingNotification(context: Context) {
    val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    notificationManager.cancel(NOTIFICATION_ID_POWER_SAVING)
    Timber.e("Power saving notification canceled")
}
