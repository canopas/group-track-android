package com.canopas.catchme.ui

import androidx.lifecycle.ViewModel
import com.canopas.catchme.data.storage.UserPreferences
import com.canopas.catchme.ui.navigation.MainNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val userPreferences: UserPreferences,
    appNavigator: MainNavigator
) : ViewModel() {

    val navActions = appNavigator.navigationChannel
}
