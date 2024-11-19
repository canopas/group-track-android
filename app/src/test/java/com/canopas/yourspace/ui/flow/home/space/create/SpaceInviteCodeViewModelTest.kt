package com.canopas.yourspace.ui.flow.home.space.create

import androidx.lifecycle.SavedStateHandle
import com.canopas.yourspace.MainCoroutineRule
import com.canopas.yourspace.data.repository.SpaceRepository
import com.canopas.yourspace.data.service.auth.AuthService
import com.canopas.yourspace.data.service.space.SpaceInvitationService
import com.canopas.yourspace.data.utils.AppDispatcher
import com.canopas.yourspace.ui.navigation.AppDestinations
import com.canopas.yourspace.ui.navigation.AppDestinations.SpaceInvitation.KEY_INVITE_CODE
import com.canopas.yourspace.ui.navigation.AppNavigator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class SpaceInviteCodeViewModelTest {

    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    private val savedStateHandle = mock<SavedStateHandle>()
    private val appNavigator = mock<AppNavigator>()
    private val appDispatcher = mock<AppDispatcher>()
    private val spaceRepository = mock<SpaceRepository>()
    private val spaceInvitationService = mock<SpaceInvitationService>()
    private val authService = mock<AuthService>()

    private lateinit var viewModel: SpaceInviteCodeViewModel

    @Before
    fun setUp() {
        viewModel = SpaceInviteCodeViewModel(
            appNavigator = appNavigator,
            savedStateHandle = savedStateHandle,
            appDispatcher = appDispatcher,
            spaceRepository = spaceRepository,
            spaceInvitationService = spaceInvitationService,
            authService = authService
        )
    }

    @Test
    fun `popBackStack should call navigateBack on appNavigator`() {
        viewModel.popBackStack()
        verify(appNavigator).navigateBack()
    }

    @Test
    fun `spaceInviteCode should return value from savedStateHandle`() {
        whenever(savedStateHandle.get<String>(KEY_INVITE_CODE)).thenReturn("123456")
        assert(viewModel.spaceInviteCode.value == "123456")
    }

    @Test
    fun `spaceName should return value from savedStateHandle`() {
        whenever(savedStateHandle.get<String>(AppDestinations.SpaceInvitation.KEY_SPACE_NAME)).thenReturn("space_name")
        assert(viewModel.spaceName == "space_name")
    }
}
