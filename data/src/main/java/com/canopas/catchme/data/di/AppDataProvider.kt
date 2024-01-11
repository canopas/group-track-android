package com.canopas.catchme.data.di

import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class AppDataProvider {
    @Provides
    @Singleton
    fun provideFirebaseDb(): FirebaseFirestore =
        Firebase.firestore
}
