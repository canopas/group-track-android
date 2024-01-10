package com.canopas.catchme.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.canopas.catchme.data.storage.UserPreferences
import com.canopas.catchme.ui.navigation.AppDestinations
import com.canopas.catchme.ui.navigation.AppNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val userPreferences: UserPreferences,
    appNavigator: AppNavigator
) : ViewModel() {

    val navActions = appNavigator.navigationChannel
    private val _appState = MutableStateFlow(AppState())
    val appState: StateFlow<AppState> = _appState

    init {
        viewModelScope.launch {
            if (!userPreferences.isIntroShown()) {
                appNavigator.navigateTo(
                    AppDestinations.intro.path,
                    popUpToRoute = AppDestinations.home.path,
                    inclusive = true
                )
            }
        }
    }
}

data class AppState(val showIntro: Boolean = false)