package com.canopas.yourspace.domain.di

import android.app.NotificationManager
import android.content.Context
import com.canopas.yourspace.BuildConfig
import com.canopas.yourspace.data.utils.AppDispatcher
import com.canopas.yourspace.domain.utils.NetworkUtils
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class AppDataProvider {

    @Provides
    @Singleton
    @Named("app_version_code")
    fun provideAppVersionCode(): Long {
        return BuildConfig.VERSION_CODE.toLong()
    }

    @Provides
    @Singleton
    fun provideAppDispatcher() = AppDispatcher()

    @Provides
    @Singleton
    fun provideNotificationManager(@ApplicationContext context: Context): NotificationManager {
        return context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    @Provides
    @Singleton
    fun provideNetworkUtils(@ApplicationContext context: Context): NetworkUtils {
        return NetworkUtils(context)
    }
}
