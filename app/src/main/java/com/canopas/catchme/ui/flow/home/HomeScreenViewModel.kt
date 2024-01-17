package com.canopas.catchme.ui.flow.home

import androidx.lifecycle.ViewModel
import com.canopas.catchme.ui.navigation.HomeNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class HomeScreenViewModel @Inject constructor(
    private val navigator: HomeNavigator
) : ViewModel() {

    val navActions = navigator.navigationChannel

    private val _state = MutableStateFlow(HomeScreenState())
    val state: StateFlow<HomeScreenState> = _state

    fun onTabChange(index: Int) {
        _state.value = _state.value.copy(currentTab = index)
    }
}

data class HomeScreenState(
    val currentTab: Int = 0
)
