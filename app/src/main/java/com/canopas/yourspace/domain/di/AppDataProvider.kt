package com.canopas.yourspace.domain.di

import com.canopas.yourspace.BuildConfig
import com.canopas.yourspace.data.utils.AppDispatcher
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
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
}
