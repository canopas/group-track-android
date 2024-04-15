package com.canopas.yourspace.domain.fcm

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import com.canopas.yourspace.data.service.auth.AuthService
import com.canopas.yourspace.data.service.messages.ApiMessagesService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

const val KEY_NOTIFICATION_ID = "extra_notification_id"
const val KEY_THREAD_ID = "extra_thread_id"
const val KEY_NOTIFICATION_REPLY = "extra_notification_reply"

@AndroidEntryPoint
class ReplyActionReceiver : BroadcastReceiver() {
    @Inject
    lateinit var authService: AuthService

    @Inject
    lateinit var apiMessagesService: ApiMessagesService

    @Inject
    lateinit var notificationManager: NotificationManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        Timber.e("onReceive reply data ${intent.data}")
        val inputtedText =
            RemoteInput.getResultsFromIntent(intent)?.getCharSequence(KEY_NOTIFICATION_REPLY)
                ?: return

        val notificationId = intent.getIntExtra(KEY_NOTIFICATION_ID, 0)
        val threadId = intent.getStringExtra(KEY_THREAD_ID) ?: return

        val sender = authService.currentUser ?: return
        val senderId = sender.id
        Timber.e(" reply $inputtedText notificationId $notificationId threadId $threadId senderId $senderId")

        scope.launch {
            apiMessagesService.sendMessage(threadId, senderId, inputtedText.toString())
            Timber.e(" reply sent $threadId")
        }

        val activeNotification = findActiveNotification(context, notificationId) ?: return

        val activeStyle =
            NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(
                activeNotification
            ) ?: return

        val user = Person.Builder().apply {
            setKey(senderId)
            setName("You")
        }.build()

        activeStyle.addMessage(
            inputtedText.toString(),
            System.currentTimeMillis(),
            user
        )

        Timber.e("Reply recieiver")
        val nBuilder =
            context.messageNotificationBuilder(
                notificationId,
                threadId,
                sender.first_name ?: "",
                inputtedText.toString(),
                activeStyle
            )

        notificationManager.notify(notificationId, nBuilder.build())
        Timber.e("notification added")
    }
}
