package com.canopas.catchme.ui.flow.home.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.canopas.catchme.data.models.user.UserInfo
import com.canopas.catchme.data.repository.SpaceRepository
import com.canopas.catchme.data.service.auth.AuthService
import com.canopas.catchme.data.utils.AppDispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    private val spaceRepository: SpaceRepository,
    private val authService: AuthService,
    private val appDispatcher: AppDispatcher
) : ViewModel() {

    private val _state = MutableStateFlow(MapScreenState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch((appDispatcher.IO)) {
            spaceRepository.listenMemberWithLocation()
        }
        viewModelScope.launch {
            spaceRepository.members.collectLatest {
                // Timber.d("XXX member location update: $it")
                _state.emit(_state.value.copy(members = it))
            }
        }
    }
}

data class MapScreenState(
    val members: List<UserInfo> = emptyList()

)
