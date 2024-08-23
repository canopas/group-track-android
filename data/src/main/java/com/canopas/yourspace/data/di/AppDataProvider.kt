package com.canopas.yourspace.data.di

import android.content.Context
import com.canopas.yourspace.data.storage.room.LocationTableDatabase
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.LocationServices
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.storage.FirebaseStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class AppDataProvider {
    @Provides
    @Singleton
    fun provideFirebaseDb(): FirebaseFirestore =
        Firebase.firestore

    @Provides
    @Singleton
    fun provideFirebaseStorage(): FirebaseStorage =
        FirebaseStorage.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseFunctions(): FirebaseFunctions =
        FirebaseFunctions.getInstance(FirebaseApp.getInstance(), "asia-south1")

    @Provides
    @Singleton
    fun provideLocationTableDatabase(@ApplicationContext context: Context): LocationTableDatabase =
        LocationTableDatabase.getInstance(context)

    @Provides
    @Singleton
    fun provideGeoFencingClient(@ApplicationContext context: Context): GeofencingClient =
        LocationServices.getGeofencingClient(context)

    @Provides
    @Singleton
    fun providePlaceClient(@ApplicationContext context: Context): PlacesClient =
        Places.createClient(context)
}
