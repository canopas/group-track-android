package com.canopas.catchme.ui.flow.home.map

import com.canopas.catchme.data.service.location.ApiLocationService
import com.canopas.catchme.data.service.space.SpaceService
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    private val spaceService: SpaceService,
    private val locationService: ApiLocationService,
) {



}