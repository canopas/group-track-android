package com.canopas.yourspace.ui.flow.settings.profile

import androidx.lifecycle.ViewModel
import com.canopas.yourspace.ui.navigation.AppNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import javax.inject.Inject

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val navigator: AppNavigator
) : ViewModel() {

    private val _state = MutableStateFlow(EditProfileState())
    val state = _state.asStateFlow()

    init {
        getUser()
    }

    private fun getUser() {

    }

    fun popBackStack() {
        navigator.navigateBack()
    }

    fun saveUser() {


    }

    fun resetErrorState() {
        _state.value = _state.value.copy(error = null)
    }

    fun onProfileImageChanged(profileUrl: File?) {

    }

    fun showProfileChooser(show: Boolean = true) {
        _state.value = _state.value.copy(showProfileChooser = show)
    }
}


data class EditProfileState(
    val saving: Boolean = false,
    val loading: Boolean = false,
    val allowSave: Boolean = false,
    val enablePhone: Boolean = false,
    val enableEmail: Boolean = false,
    val showProfileChooser: Boolean = false,
    val error: String? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val profileUrl: String? = null
)