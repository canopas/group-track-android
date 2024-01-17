package com.canopas.catchme.ui.flow.home

import androidx.lifecycle.ViewModel
import com.canopas.catchme.ui.navigation.HomeNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class HomeScreenViewModel @Inject constructor(
    private val navigator: HomeNavigator
) : ViewModel() {

    val navActions = navigator.navigationChannel


}