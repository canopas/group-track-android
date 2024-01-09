package com.canopas.catchme.ui.flow.auth.methods

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.canopas.catchme.ui.navigation.AppDestinations
import com.canopas.catchme.ui.navigation.AppNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignInMethodViewModel @Inject constructor(
    private val navigator: AppNavigator
) : ViewModel() {

    fun signInWithPhone() = viewModelScope.launch {
        navigator.navigateTo(AppDestinations.phoneSignIn.path)
    }

    fun signInWithGoogle() {

    }

}