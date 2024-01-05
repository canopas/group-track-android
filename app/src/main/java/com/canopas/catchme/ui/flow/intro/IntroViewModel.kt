package com.canopas.catchme.ui.flow.intro

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.canopas.catchme.data.storage.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class IntroViewModel @Inject constructor(
    private val userPreferences: UserPreferences
) : ViewModel() {

    val showIntroView = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            showIntroView.value = !userPreferences.isIntroShown()
        }
    }

    fun completeIntroScreen() = viewModelScope.launch {
        showIntroView.value = false
        userPreferences.setIntroShown(true)
    }

}