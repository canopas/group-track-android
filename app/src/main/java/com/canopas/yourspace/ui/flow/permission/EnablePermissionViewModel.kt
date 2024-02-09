package com.canopas.yourspace.ui.flow.permission

import androidx.lifecycle.ViewModel
import com.canopas.yourspace.ui.navigation.AppNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class EnablePermissionViewModel @Inject constructor(
    private val appNavigator: AppNavigator
) : ViewModel() {

    fun popBack() {
        appNavigator.navigateBack()
    }
}
