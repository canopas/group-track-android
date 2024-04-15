package com.canopas.yourspace.domain.fcm

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.RemoteInput
import com.canopas.yourspace.R
import com.canopas.yourspace.ui.MainActivity

fun findActiveNotification(
    context: Context,
    notificationId: Int
): Notification? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        NotificationManagerCompat.from(context)
            .activeNotifications
            .find { it.id == notificationId }
            ?.notification

    } else {
        null
    }
}

fun Context.messageNotificationBuilder(
    notificationId: Int,
    threadId: String?,
    title: String,
    body: String,
    style: NotificationCompat.MessagingStyle
): NotificationCompat.Builder {
    val icon = android.R.drawable.ic_dialog_alert
    val replyActionButtonTitle = getString(R.string.common_reply)
    val remoteInput =
        RemoteInput.Builder(KEY_NOTIFICATION_REPLY)
            .setLabel(replyActionButtonTitle)
            .build()


    val replyAction = NotificationCompat.Action.Builder(
        icon,
        replyActionButtonTitle,
        replyActionPendingIntent(this, notificationId, threadId ?: "")
    ).addRemoteInput(remoteInput).build()



    return NotificationCompat.Builder(this, CHANNEL_YOURSPACE)
        .setSmallIcon(R.drawable.app_logo)
        .setContentTitle(title)
        .setContentText(body)
        .setAutoCancel(true)
        .setOnlyAlertOnce(true)
        .setCategory(NotificationCompat.CATEGORY_MESSAGE)
        .setContentIntent(clickActionPendingIntent(this,threadId))
        .addAction(replyAction)
        .setStyle(style)

}

private fun clickActionPendingIntent(context: Context, threadId: String?): PendingIntent {
    val intent = Intent(context, MainActivity::class.java).apply {
        putExtra(NotificationDataConst.KEY_THREAD_ID, threadId)
        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val pendingIntentFlag =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else PendingIntent.FLAG_UPDATE_CURRENT
    return PendingIntent.getActivity(
        context,
        System.currentTimeMillis().toInt(),
        intent,
        pendingIntentFlag
    )
}

private fun replyActionPendingIntent(
    context: Context,
    notificationId: Int,
    threadId: String
): PendingIntent {
    val activityActionIntent = Intent(context, ReplyActionReceiver::class.java).apply {
        putExtra(KEY_NOTIFICATION_ID, notificationId)
        putExtra(KEY_THREAD_ID, threadId)
    }

    val pendingIntentFlag =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else PendingIntent.FLAG_UPDATE_CURRENT
    return PendingIntent.getBroadcast(
        context,
        System.currentTimeMillis().toInt(),
        activityActionIntent,
        pendingIntentFlag
    )
}
