package com.canopas.catchme.ui.flow.home.space.create

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.canopas.catchme.ui.navigation.AppDestinations.SpaceInvitation.KEY_INVITE_CODE
import com.canopas.catchme.ui.navigation.AppDestinations.SpaceInvitation.KEY_SPACE_NAME
import com.canopas.catchme.ui.navigation.HomeNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SpaceInviteCodeViewModel @Inject constructor(
    private val appNavigator: HomeNavigator,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    val spaceInviteCode = savedStateHandle.get<String>(KEY_INVITE_CODE) ?: ""
    val spaceName = savedStateHandle.get<String>(KEY_SPACE_NAME) ?: ""

    fun popBackStack() {
        appNavigator.navigateBack()
    }
}
