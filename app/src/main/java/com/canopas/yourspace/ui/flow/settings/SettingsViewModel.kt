package com.canopas.yourspace.ui.flow.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.canopas.yourspace.data.models.space.ApiSpace
import com.canopas.yourspace.data.models.user.ApiUser
import com.canopas.yourspace.data.repository.SpaceRepository
import com.canopas.yourspace.data.service.auth.AuthService
import com.canopas.yourspace.data.utils.AppDispatcher
import com.canopas.yourspace.ui.navigation.AppDestinations
import com.canopas.yourspace.ui.navigation.AppNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val navigator: AppNavigator,
    private val authService: AuthService,
    private val appDispatcher: AppDispatcher,
    private val spaceRepository: SpaceRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsScreenState())
    val state = _state.asStateFlow()

    init {
        getUser()
        getCurrentSpace()
    }

    private fun getUser() = viewModelScope.launch(appDispatcher.IO) {
        authService.getUserFlow().collectLatest { user ->
            _state.emit(_state.value.copy(user = user))
        }
    }

    private fun getCurrentSpace() = viewModelScope.launch(appDispatcher.IO) {
        try {
            val space = spaceRepository.getCurrentSpace()
            _state.emit(_state.value.copy(selectedSpace = space))
        } catch (e: Exception) {
            Timber.d(e, "Failed to get current space")
        }
    }

    fun popBackStack() {
        navigator.navigateBack()
    }

    fun showSignOutConfirmation(show: Boolean) {
        _state.value = _state.value.copy(openSignOutDialog = show)
    }

    fun showDeleteAccountConfirmation(show: Boolean) {
        _state.value = _state.value.copy(openDeleteAccountDialog = show)
    }

    fun signOutUser() = viewModelScope.launch(appDispatcher.IO) {
        _state.emit(_state.value.copy(signingOut = true, openSignOutDialog = false))
        authService.signOut()
        navigator.navigateTo(
            AppDestinations.signIn.path,
            AppDestinations.home.path,
            true
        )
        _state.emit(_state.value.copy(signingOut = false))
    }

    fun editProfile() {
        navigator.navigateTo(AppDestinations.editProfile.path)
    }
}

data class SettingsScreenState(
    val user: ApiUser? = null,
    val selectedSpace: ApiSpace? = null,
    var openSignOutDialog: Boolean = false,
    var openDeleteAccountDialog: Boolean = false,
    var deletingAccount: Boolean = false,
    var signingOut: Boolean = false
)
