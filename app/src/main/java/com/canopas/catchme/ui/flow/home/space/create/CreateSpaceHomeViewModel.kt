package com.canopas.catchme.ui.flow.home.space.create

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.canopas.catchme.data.service.space.ApiSpaceService
import com.canopas.catchme.data.utils.AppDispatcher
import com.canopas.catchme.ui.navigation.AppDestinations
import com.canopas.catchme.ui.navigation.HomeNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class CreateSpaceHomeViewModel @Inject constructor(
    private val appNavigator: HomeNavigator,
    private val spaceService: ApiSpaceService,
    private val appDispatcher: AppDispatcher
) : ViewModel() {

    private val _state = MutableStateFlow(CreateSpaceHomeState())
    val state = _state.asStateFlow()

    fun navigateBack() {
        appNavigator.navigateBack()
    }

    fun onSpaceNameChange(spaceName: String) {
        _state.value = _state.value.copy(spaceName = spaceName)
    }

    fun createSpace() = viewModelScope.launch(appDispatcher.IO) {
        try {
            _state.emit(_state.value.copy(creatingSpace = true))
            val invitationCode = spaceService.createSpace(_state.value.spaceName)
            _state.emit(
                _state.value.copy(
                    creatingSpace = false,
                    spaceInviteCode = invitationCode
                )
            )
            appNavigator.navigateTo(
                AppDestinations.spaceInvite.path,
                AppDestinations.createSpace.path, inclusive = true
            )
        } catch (e: Exception) {
            Timber.e(e, "Unable to create space")
            _state.emit(_state.value.copy(error = e.localizedMessage))
        }
    }

}

data class CreateSpaceHomeState(
    val spaceName: String = "",
    val spaceInviteCode: String? = "",
    val creatingSpace: Boolean = false,
    val error: String? = null
)