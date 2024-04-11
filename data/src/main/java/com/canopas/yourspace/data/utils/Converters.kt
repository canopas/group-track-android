package com.canopas.yourspace.data.utils

import androidx.room.TypeConverter
import com.canopas.yourspace.data.models.location.ApiLocation
import com.canopas.yourspace.data.models.location.LocationJourney
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import javax.inject.Inject

/**
 * Converters for Room database, used for converting complex data types to Room supported data types.
 *
 * For example, converting List<ApiLocation> to String and vice versa.
 *
 * Similarly, converting ApiLocation to String and vice versa
 * */
class Converters @Inject constructor() {

    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val type = Types.newParameterizedType(List::class.java, ApiLocation::class.java)
    private val listJsonAdapter = moshi.adapter<List<ApiLocation>>(type)
    private val apiLocationAdapter = moshi.adapter(ApiLocation::class.java)
    private val journeyAdapter = moshi.adapter(LocationJourney::class.java)

    /**
     * Convert String to List<ApiLocation>
     * */
    @TypeConverter
    fun locationListFromString(value: String): List<ApiLocation>? {
        return listJsonAdapter.fromJson(value)
    }

    /**
     * Convert List<ApiLocation> to String
     * */
    @TypeConverter
    fun locationListToString(list: List<ApiLocation>?): String {
        return listJsonAdapter.toJson(list)
    }

    /**
     * Convert String to ApiLocation
     * */
    @TypeConverter
    fun locationFromString(value: String): ApiLocation? {
        return apiLocationAdapter.fromJson(value)
    }

    /**
     * Convert ApiLocation to String
     * */
    @TypeConverter
    fun locationToString(apiLocation: ApiLocation?): String {
        return apiLocationAdapter.toJson(apiLocation)
    }

    /**
     * Convert String to LocationJourney
     * */
    @TypeConverter
    fun journeyFromString(value: String): LocationJourney? {
        return journeyAdapter.fromJson(value)
    }

    /**
     * Convert LocationJourney to String
     * */
    @TypeConverter
    fun journeyToString(journey: LocationJourney?): String {
        return journeyAdapter.toJson(journey)
    }
}
