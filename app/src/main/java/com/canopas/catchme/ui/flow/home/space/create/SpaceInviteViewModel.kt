package com.canopas.catchme.ui.flow.home.space.create

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.canopas.catchme.ui.navigation.HomeNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SpaceInviteViewModel @Inject constructor(
    private val appNavigator: HomeNavigator,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    val spaceInviteCode = savedStateHandle.get<String>("spaceInviteCode")?:""

    fun popBackStack() {
        appNavigator.navigateBack()
    }
}

