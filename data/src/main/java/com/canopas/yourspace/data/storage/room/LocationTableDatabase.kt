package com.canopas.yourspace.data.storage.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.canopas.yourspace.data.models.location.LocationTable

@Database(entities = [LocationTable::class], version = 2, exportSchema = false)
abstract class LocationTableDatabase : RoomDatabase() {

    abstract fun locationTableDao(): LocationTableDao

    companion object {

        @Volatile
        private var INSTANCE: LocationTableDatabase? = null

        fun getInstance(context: Context): LocationTableDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LocationTableDatabase::class.java,
                    "location_table_database"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
