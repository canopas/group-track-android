package com.canopas.yourspace.data.storage.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.canopas.yourspace.data.models.location.LocationTable
import com.canopas.yourspace.data.models.location.LogEntry

@Database(entities = [LocationTable::class, LogEntry::class], version = 3, exportSchema = false)
abstract class LocationTableDatabase : RoomDatabase() {

    abstract fun locationTableDao(): LocationTableDao
    abstract fun logDao(): LogDao

    companion object {

        @Volatile
        private var INSTANCE: LocationTableDatabase? = null

        fun getInstance(context: Context): LocationTableDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LocationTableDatabase::class.java,
                    "location_table_database"
                ).addMigrations(MIGRATION_2_3).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
            CREATE TABLE IF NOT EXISTS `logs` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `timestamp` TEXT NOT NULL,
                `priority` TEXT NOT NULL,
                `tag` TEXT,
                `message` TEXT NOT NULL
            )
                    """.trimIndent()
                )
            }
        }
    }
}
