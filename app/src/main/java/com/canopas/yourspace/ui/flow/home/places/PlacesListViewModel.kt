package com.canopas.yourspace.ui.flow.home.places

import androidx.lifecycle.ViewModel
import com.canopas.yourspace.ui.navigation.AppDestinations
import com.canopas.yourspace.ui.navigation.AppNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PlacesListViewModel @Inject constructor(
    private val appNavigator: AppNavigator,
) : ViewModel() {

    fun navigateBack() {
        appNavigator.navigateBack()
    }

    fun navigateToAddPlace() {
        appNavigator.navigateTo(AppDestinations.locateOnMap.path)
    }


}