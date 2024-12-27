package com.canopas.yourspace.data.storage.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [SenderKeyEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun senderKeyDao(): SenderKeyDao
}
