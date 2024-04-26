package com.canopas.yourspace.data.repository

import com.canopas.yourspace.data.service.place.ApiPlaceService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

class GeofenceRepository @Inject constructor(
    private val apiPlaceService: ApiPlaceService,
    private val spaceRepository: SpaceRepository,
) {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {

        scope.launch {
        }
    }

}