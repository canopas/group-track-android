package com.canopas.yourspace.data.utils

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Named

class Device @Inject constructor(
    @ApplicationContext var context: Context,
    @Named("app_version_code") val versionCode: Long
) {
    @SuppressLint("HardwareIds")
    fun getId(): String {
        return Settings.Secure
            .getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    }

    fun getDeviceOsVersion(): String {
        return Build.VERSION.RELEASE
    }

    fun getAppVersionCode(): Long {
        return versionCode
    }

    fun deviceModel(): String {
        return Build.MODEL
    }

    fun deviceName(): String {
        return Build.DEVICE
    }

    fun timeZone(): String {
        return TimeZone.getDefault().id
    }
}
