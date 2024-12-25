package com.canopas.yourspace.ui.flow.permission

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.canopas.yourspace.data.storage.UserPreferences
import com.canopas.yourspace.data.utils.AppDispatcher
import com.canopas.yourspace.ui.navigation.AppNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EnablePermissionViewModel @Inject constructor(
    private val appNavigator: AppNavigator,
    private val userPreferences: UserPreferences,
    private val appDispatcher: AppDispatcher
) : ViewModel() {

    private val _state = MutableStateFlow(EnablePermissionState())
    val state = _state.asStateFlow()

    fun refreshBatteryOptimizationState() {
        viewModelScope.launch(appDispatcher.IO) {
            _state.emit(
                state.value.copy(
                    isBatteryOptimized = userPreferences.isBatteryOptimizationEnabled
                )
            )
        }
    }

    fun changeBatteryOptimizationValue(value: Boolean) {
        viewModelScope.launch(appDispatcher.IO) {
            _state.value = _state.value.copy(isBatteryOptimized = value)
            userPreferences.isBatteryOptimizationEnabled = value
        }
    }

    fun popBack() {
        appNavigator.navigateBack()
    }
}

data class EnablePermissionState(
    var isBatteryOptimized: Boolean = false
)
