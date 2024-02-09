package com.canopas.yourspace.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.canopas.yourspace.data.storage.UserPreferences
import com.canopas.yourspace.ui.navigation.AppDestinations
import com.canopas.yourspace.ui.navigation.AppNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val userPreferences: UserPreferences,
    navigator: AppNavigator
) : ViewModel() {

    val navActions = navigator.navigationChannel

    init {
        viewModelScope.launch {
            if (userPreferences.isIntroShown()) {
                if (userPreferences.currentUser == null) {
                    navigator.navigateTo(
                        AppDestinations.signIn.path,
                        popUpToRoute = AppDestinations.intro.path,
                        inclusive = true
                    )
                } else if (!userPreferences.isOnboardShown()) {
                    navigator.navigateTo(
                        AppDestinations.onboard.path,
                        popUpToRoute = AppDestinations.intro.path,
                        inclusive = true
                    )
                } else {
                    navigator.navigateTo(
                        AppDestinations.home.path,
                        popUpToRoute = AppDestinations.intro.path,
                        inclusive = true
                    )
                }
            }
        }
    }
}
