package com.canopas.yourspace.domain.fcm

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.graphics.drawable.IconCompat
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.canopas.yourspace.R
import com.canopas.yourspace.data.storage.UserPreferences
import com.canopas.yourspace.domain.fcm.NotificationDataConst.KEY_IS_GROUP
import com.canopas.yourspace.domain.fcm.NotificationDataConst.KEY_SENDER_ID
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

const val CHANNEL_YOURSPACE = "notification_channel_your_space"
const val NOTIFICATION_ID = 101

private const val NOTIFICATION_TYPE_CHAT = "chat"

object NotificationDataConst {
    const val KEY_PROFILE_URL = "senderProfileUrl"
    const val KEY_NOTIFICATION_TYPE = "type"
    const val KEY_IS_GROUP = "isGroup"
    const val KEY_GROUP_NAME = "groupName"
    const val KEY_SENDER_ID = "senderId"
    const val KEY_THREAD_ID = "threadId"
}

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
        val notification = message.notification
        notification?.let {
            val title = notification.title
            val body = notification.body
            val profile = message.data[NotificationDataConst.KEY_PROFILE_URL]
            val type = message.data[NotificationDataConst.KEY_NOTIFICATION_TYPE]

            if (title != null && body != null && type == NOTIFICATION_TYPE_CHAT) {
                scope.launch {
                    val bitmap =
                        if (profile.isNullOrEmpty()) null else getTrackBitmapFromUrl(profile)
                    sendNotification(this@YourSpaceFcmService, title, body, message.data, bitmap)
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun sendNotification(
        context: Context,
        title: String,
        body: String,
        data: MutableMap<String, String>,
        profile: Bitmap?
    ) {
        val isGroup = data[NotificationDataConst.KEY_IS_GROUP].toBoolean()
        val groupName = data[NotificationDataConst.KEY_GROUP_NAME]
        val senderId = data[NotificationDataConst.KEY_SENDER_ID]
        val threadId = data[NotificationDataConst.KEY_THREAD_ID]
        val notificationId = threadId?.hashCode() ?: NOTIFICATION_ID

        val user = Person.Builder().apply {
            setKey(senderId)
            setName(title)
            profile?.let { setIcon(IconCompat.createWithAdaptiveBitmap(it)) }
        }.build()

        val style =
            findActiveNotification(this, notificationId)
                ?.let { NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(it) }
                ?: NotificationCompat.MessagingStyle(user)

        style.also {
            if (isGroup) {
                it.setGroupConversation(true)
                it.setConversationTitle(groupName)
            }
            it.addMessage(body, System.currentTimeMillis(), user)
        }

        val nBuilder =
            context.messageNotificationBuilder(notificationId, threadId, title, body, style)
        notificationManager.notify(notificationId, nBuilder.build())
    }

    private suspend fun getTrackBitmapFromUrl(
        url: String
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
