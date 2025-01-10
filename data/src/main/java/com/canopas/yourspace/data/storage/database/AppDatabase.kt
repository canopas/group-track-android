package com.canopas.yourspace.data.storage.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [SenderKeyEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun senderKeyDao(): SenderKeyDao
}

val MIGRATION_1_1 = object : Migration(1, 1) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // No changes required for now.
    }
}
