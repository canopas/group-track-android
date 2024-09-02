package com.canopas.yourspace.data.storage.room

import androidx.annotation.Keep
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.canopas.yourspace.data.models.location.LocationTable
import com.canopas.yourspace.data.models.location.LogEntry

@Keep
@Dao
interface LocationTableDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocationData(location: LocationTable)

    @Update
    suspend fun updateLocationTable(location: LocationTable)

    @Query("SELECT * FROM location_table WHERE user_id = :userId")
    fun getLocationData(userId: String): LocationTable?

    @Query("DELETE FROM location_table")
    fun deleteLocationData()
}

@Dao
interface LogDao {
    @Insert
    suspend fun insert(logEntry: LogEntry)

    @Query("SELECT * FROM logs")
    suspend fun getAllLogs(): List<LogEntry>
}
