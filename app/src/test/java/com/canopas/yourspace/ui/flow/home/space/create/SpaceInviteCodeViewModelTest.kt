package com.canopas.yourspace.ui.flow.home.space.create

import androidx.lifecycle.SavedStateHandle
import com.canopas.yourspace.MainCoroutineRule
import com.canopas.yourspace.data.repository.SpaceRepository
import com.canopas.yourspace.data.service.auth.AuthService
import com.canopas.yourspace.data.service.space.SpaceInvitationService
import com.canopas.yourspace.data.utils.AppDispatcher
import com.canopas.yourspace.ui.navigation.AppDestinations.SpaceInvitation.KEY_INVITE_CODE
import com.canopas.yourspace.ui.navigation.AppDestinations.SpaceInvitation.KEY_SPACE_NAME
import com.canopas.yourspace.ui.navigation.AppNavigator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class SpaceInviteCodeViewModelTest {

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    private val savedStateHandle = mock<SavedStateHandle>()
    private val appNavigator = mock<AppNavigator>()
    private val testDispatcher = AppDispatcher(IO = UnconfinedTestDispatcher())
    private val spaceRepository = mock<SpaceRepository>()
    private val spaceInvitationService = mock<SpaceInvitationService>()
    private val authService = mock<AuthService>()

    private lateinit var viewModel: SpaceInviteCodeViewModel

    @Before
    fun setViewModel() {
        whenever(savedStateHandle.get<String>(KEY_INVITE_CODE)).thenReturn("inviteCode")
        whenever(savedStateHandle.get<String>(KEY_SPACE_NAME)).thenReturn("space1")

        viewModel = SpaceInviteCodeViewModel(
            appNavigator = appNavigator,
            savedStateHandle = savedStateHandle,
            appDispatcher = testDispatcher,
            spaceRepository = spaceRepository,
            spaceInvitationService = spaceInvitationService,
            authService = authService
        )
    }

    @Test
    fun `popBackStack should call navigateBack on appNavigator`() {
        setViewModel()
        viewModel.popBackStack()
        verify(appNavigator).navigateBack()
    }

    @Test
    fun `spaceInviteCode should return value from savedStateHandle`() {
        assert(viewModel.state.value.inviteCode == "inviteCode")
    }

    @Test
    fun `spaceName should return value from savedStateHandle`() {
        assert(viewModel.state.value.spaceName == "space1")
    }
}
