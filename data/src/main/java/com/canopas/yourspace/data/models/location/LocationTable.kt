package com.canopas.yourspace.data.models.location

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
@Entity(tableName = "location_table")
data class LocationTable(

    @PrimaryKey
    @ColumnInfo(name = "user_id")
    val userId: String = "",

    @ColumnInfo(name = "last_five_minutes_locations")
    val lastFiveMinutesLocations: String? = null, // last 5 min ApiLocations

    @ColumnInfo(name = "last_location_journey")
    val lastLocationJourney: String? = null // last journey
)

@Entity(tableName = "logs")
data class LogEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestamp: String,
    val priority: String,
    val tag: String?,
    val message: String
)
