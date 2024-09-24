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

    @ColumnInfo(name = "last_five_locations")
    val lastFiveLocations: String? = null, // last 5 extracted locations

    @ColumnInfo(name = "last_location_journey")
    val lastLocationJourney: String? = null // last journey
)
