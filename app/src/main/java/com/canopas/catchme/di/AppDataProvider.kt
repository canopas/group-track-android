package com.canopas.catchme.di

import com.canopas.catchme.BuildConfig
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
}