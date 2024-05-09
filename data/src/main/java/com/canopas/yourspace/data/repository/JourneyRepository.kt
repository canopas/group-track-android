package com.canopas.yourspace.data.repository

import android.location.Location
import com.canopas.yourspace.data.models.location.ApiLocation
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JourneyRepository @Inject constructor() {
    fun saveLocationJourney(
        userState: Int?,
        extractedLocation: Location?,
        lastLocation: ApiLocation?,
        userId: String
    ) {

    }


}