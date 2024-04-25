package com.canopas.yourspace

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.Configuration
import com.canopas.yourspace.data.service.auth.AuthService
import com.canopas.yourspace.data.service.auth.AuthStateChangeListener
import com.canopas.yourspace.data.storage.UserPreferences
import com.canopas.yourspace.domain.fcm.CHANNEL_YOURSPACE
import com.canopas.yourspace.domain.fcm.FcmRegisterWorker
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class YourSpaceApplication :
    Application(),
    DefaultLifecycleObserver,
    Configuration.Provider,
    AuthStateChangeListener {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var userPreferences: UserPreferences

    @Inject
    lateinit var authService: AuthService

    @Inject
    lateinit var notificationManager: NotificationManager

    override fun onCreate() {
        super<Application>.onCreate()
        Timber.plant(Timber.DebugTree())
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        authService.addListener(this)
        setNotificationChannel()
    }

    private fun setNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val reminderChannel = NotificationChannel(
                CHANNEL_YOURSPACE,
                getString(R.string.title_notification_channel),
                NotificationManager.IMPORTANCE_HIGH
            )

            reminderChannel.description =
                getString(R.string.description_notification_channel)
            reminderChannel.enableLights(true)
            notificationManager.createNotificationChannel(reminderChannel)
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)

        if (userPreferences.currentUser != null && !userPreferences.isFCMRegistered) {
            FcmRegisterWorker.startService(this)
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onAuthStateChanged() {
        if (userPreferences.currentUser != null && !userPreferences.isFCMRegistered) {
            FcmRegisterWorker.startService(this)
        }
    }
}
