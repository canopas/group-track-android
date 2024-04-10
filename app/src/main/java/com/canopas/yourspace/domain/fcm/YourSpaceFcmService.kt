package com.canopas.yourspace.domain.fcm

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.canopas.yourspace.R
import com.canopas.yourspace.data.storage.UserPreferences
import com.canopas.yourspace.ui.MainActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

const val CHANNEL_YOURSPACE = "notification_channel_your_space"
const val NOTIFICATION_ID = 101


@AndroidEntryPoint
class YourSpaceFcmService : FirebaseMessagingService() {
    @Inject
    lateinit var userPreferences: UserPreferences

    @Inject
    lateinit var notificationManager: NotificationManager

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        userPreferences.isFCMRegistered = false
        if (userPreferences.currentUser != null) {
            FcmRegisterWorker.startService(applicationContext)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Timber.e("XXX: onMessageReceived: ${message} dat ${message.data} message ${message.notification}")
        val notification = message.notification
        notification?.let {
            val title = notification.title
            val body = notification.body
            val profile = message.data["senderProfileUrl"]
            if (title != null && body != null) {
                sendNotification(this, title, body, profile)
            }
        }
    }

    private fun sendNotification(
        context: Context,
        title: String,
        body: String,
        profile: String?,
    ) {

        val user: Person = Person.Builder().setIcon(userIcon).setName(userName).build()

        val activityActionIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntentFlag =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else PendingIntent.FLAG_UPDATE_CURRENT
        val activityActionPendingIntent: PendingIntent = PendingIntent.getActivity(
            context,
            0,
            activityActionIntent,
            pendingIntentFlag
        )

        val nBuilder: NotificationCompat.Builder =
            NotificationCompat.Builder(context, CHANNEL_YOURSPACE)
                .setSmallIcon(R.drawable.app_logo)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setContentIntent(activityActionPendingIntent)

        scope.launch {
            val bitmap = getTrackBitmapFromUrl(profile ?: "")
            bitmap?.let {
                nBuilder.setLargeIcon(it)
                notificationManager.notify(NOTIFICATION_ID, nBuilder.build())
            }
        }

        notificationManager.notify(NOTIFICATION_ID, nBuilder.build())
    }

    private suspend fun getTrackBitmapFromUrl(
        url: String,
    ): Bitmap? {
        try {
            val size = resources.getDimensionPixelSize(R.dimen.size_notification_large)

            val loader = ImageLoader(this)
            val request = ImageRequest.Builder(this)
                .data(url)
                .allowHardware(false)
                .size(size, size)
                .build()

            val result = (loader.execute(request) as? SuccessResult)?.drawable
            val bitmap = (result as? BitmapDrawable)?.bitmap
            return bitmap
        } catch (throwable: Throwable) {
            return null
        }
    }
}