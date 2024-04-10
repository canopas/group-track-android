package com.canopas.yourspace

import android.app.Application
import com.google.firebase.crashlytics.FirebaseCrashlytics
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class YourSpaceApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG || BuildConfig.VERSION_NAME == "1.0.0") {
            Timber.plant(Timber.DebugTree())
        } else {
            FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
        }
    }
}
