package com.canopas.yourspace.data.di

import android.content.Context
import androidx.room.Room
import com.canopas.yourspace.data.models.user.ApiUserSession
import com.canopas.yourspace.data.storage.database.AppDatabase
import com.canopas.yourspace.data.storage.database.MIGRATION_1_1
import com.canopas.yourspace.data.storage.database.SenderKeyDao
import com.canopas.yourspace.data.utils.Config
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
import javax.inject.Named
import javax.inject.Singleton

private const val DATABASE_NAME = "sender_keys_db"

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
        FirebaseFunctions.getInstance(FirebaseApp.getInstance(), Config.FIREBASE_REGION)

    @Provides
    @Singleton
    fun provideGeoFencingClient(@ApplicationContext context: Context): GeofencingClient =
        LocationServices.getGeofencingClient(context)

    @Provides
    @Singleton
    fun providePlaceClient(@ApplicationContext context: Context): PlacesClient =
        Places.createClient(context)

    @Provides
    @Singleton
    fun providedApiUserSession(): ApiUserSession = ApiUserSession()

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext appContext: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            appContext,
            AppDatabase::class.java,
            DATABASE_NAME
        )
            .addMigrations(MIGRATION_1_1)
            .build()
    }

    @Provides
    @Singleton
    @Named("sender_key_dao")
    fun provideSenderKeyDao(appDatabase: AppDatabase): SenderKeyDao {
        return appDatabase.senderKeyDao()
    }
}
