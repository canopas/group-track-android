package com.canopas.catchme.ui.flow.auth.permission

import androidx.lifecycle.ViewModel
import com.canopas.catchme.ui.navigation.AppDestinations
import com.canopas.catchme.ui.navigation.MainNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class EnablePermissionViewModel @Inject constructor(
    private val appNavigator: MainNavigator
) : ViewModel() {

    fun navigationToHome() {
        appNavigator.navigateTo(
            AppDestinations.home.path,
            popUpToRoute = AppDestinations.enablePermissions.path,
            inclusive = true
        )
    }
}
