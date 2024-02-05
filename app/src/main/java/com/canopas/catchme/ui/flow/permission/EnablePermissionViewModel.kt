package com.canopas.catchme.ui.flow.permission

import androidx.lifecycle.ViewModel
import com.canopas.catchme.ui.navigation.HomeNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class EnablePermissionViewModel @Inject constructor(
    private val appNavigator: HomeNavigator
) : ViewModel() {

    fun popBack() {
        appNavigator.navigateBack()
    }
}
