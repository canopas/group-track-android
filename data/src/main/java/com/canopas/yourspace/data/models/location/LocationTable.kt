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

    @ColumnInfo(name = "latest_location")
    val latestLocation: String? = null,  // last ApiLocation

    @ColumnInfo(name = "last_five_minutes_locations")
    val lastFiveMinutesLocations: String? = null,  // last 5 min ApiLocations

    @ColumnInfo(name = "last_steady_location")
    val lastSteadyLocation: String? = null, // last steady journey

    @ColumnInfo(name = "last_moving_location")
    val lastMovingLocation: String? = null,  // last moving journey

    @ColumnInfo(name = "last_location_journey")
    val lastLocationJourney: String? = null   // last journey
)
