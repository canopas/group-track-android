package com.canopas.yourspace

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.Configuration
import com.canopas.yourspace.data.service.auth.AuthService
import com.canopas.yourspace.data.service.auth.AuthState
import com.canopas.yourspace.data.service.auth.AuthStateChangeListener
import com.canopas.yourspace.data.storage.UserPreferences
import com.canopas.yourspace.domain.fcm.FcmRegisterWorker
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class YourSpaceApplication : Application(), DefaultLifecycleObserver,
    Configuration.Provider,
    AuthStateChangeListener {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var userPreferences: UserPreferences

    @Inject
    lateinit var authService: AuthService

    override fun onCreate() {
        super<Application>.onCreate()
        Timber.plant(Timber.DebugTree())
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        authService.addListener(this)
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
