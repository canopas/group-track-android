package com.canopas.catchme.ui.flow.onboard

import com.canopas.catchme.MainCoroutineRule
import com.canopas.catchme.data.models.auth.ApiUser
import com.canopas.catchme.data.models.space.ApiSpace
import com.canopas.catchme.data.models.space.ApiSpaceInvitation
import com.canopas.catchme.data.service.space.SpaceInvitationService
import com.canopas.catchme.data.service.space.SpaceService
import com.canopas.catchme.data.service.user.UserService
import com.canopas.catchme.data.storage.UserPreferences
import com.canopas.catchme.data.utils.AppDispatcher
import com.canopas.catchme.ui.navigation.AppNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class OnboardViewModelTest {
    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private lateinit var viewModel: OnboardViewModel

    private val userService = mock<UserService>()
    private val userPreferences = mock<UserPreferences>()
    private val spaceService = mock<SpaceService>()
    private val navigator = mock<AppNavigator>()
    private val invitationService = mock<SpaceInvitationService>()

    private val testDispatcher = AppDispatcher(IO = UnconfinedTestDispatcher())
    private val currentUser = ApiUser(first_name = "first", last_name = "last")

    @Before
    fun setup() {
        whenever(userService.currentUser).thenReturn(currentUser)
        viewModel = OnboardViewModel(
            userService,
            testDispatcher,
            spaceService,
            userPreferences,
            navigator,
            invitationService
        )
    }

    @Test
    fun `on init should set first and last name`() {
        assert(viewModel.state.value.firstName == "first")
        assert(viewModel.state.value.lastName == "last")
    }

    @Test
    fun `onFirstNameChange should update first name`() {
        viewModel.onFirstNameChange("new first")
        assert(viewModel.state.value.firstName == "new first")
    }

    @Test
    fun `onLastNameChange should update last name`() {
        viewModel.onLastNameChange("new last")
        assert(viewModel.state.value.lastName == "new last")
    }

    @Test
    fun `navigateToSpaceInfo should not call updateUser if user detail is not updated`() = runTest {
        val user = ApiUser(first_name = "first", last_name = "last")
        assert(viewModel.state.value.firstName == "first")
        assert(viewModel.state.value.lastName == "last")

        viewModel.navigateToSpaceInfo()

        verify(userService, times(0)).updateUser(user)
    }

    @Test
    fun `navigateToSpaceInfo should call updateUser if user detail is updated`() = runTest {
        viewModel.onFirstNameChange("new first")
        viewModel.onLastNameChange("new last")
        val user = currentUser.copy(first_name = "new first", last_name = "new last")
        viewModel.navigateToSpaceInfo()

        verify(userService).updateUser(user)
    }

    @Test
    fun `navigateToSpaceInfo should set currentStep to SpaceIntro`() = runTest {
        viewModel.navigateToSpaceInfo()
        assert(viewModel.state.value.currentStep == OnboardItems.SpaceIntro)
    }

    @Test
    fun `navigateToSpaceInfo should set updatingUserName to false`() = runTest {
        viewModel.navigateToSpaceInfo()
        assert(!viewModel.state.value.updatingUserName)
    }

    @Test
    fun `navigateToJoinOrCreateSpace should set currentStep to JoinOrCreateSpace`() {
        viewModel.navigateToJoinOrCreateSpace()
        assert(viewModel.state.value.currentStep == OnboardItems.JoinOrCreateSpace)
    }

    @Test
    fun `navigateToCreateSpace should set currentStep to CreateSpace`() {
        viewModel.navigateToCreateSpace()
        assert(viewModel.state.value.currentStep == OnboardItems.CreateSpace)
    }

    @Test
    fun `createSpace should set creatingSpace state to true`() = runTest {
        whenever(spaceService.createSpace("space")).doSuspendableAnswer {
            withContext(Dispatchers.IO) { delay(1000) }
            return@doSuspendableAnswer "invitationCode"
        }
        viewModel.createSpace("space")
        assert(viewModel.state.value.creatingSpace)
    }

    @Test
    fun `createSpace should set spaceName`() = runTest {
        viewModel.createSpace("space")
        assert(viewModel.state.value.spaceName == "space")
    }

    @Test
    fun `createSpace should call createSpace`() = runTest {
        viewModel.createSpace("space")
        verify(spaceService).createSpace("space")
    }

    @Test
    fun `createSpace should set spaceCode`() = runTest {
        whenever(spaceService.createSpace("space")).thenReturn("invitationCode")
        viewModel.createSpace("space")
        assert(viewModel.state.value.spaceInviteCode == "invitationCode")
    }

    @Test
    fun `createSpace should set creatingSpace to false after space created`() = runTest {
        whenever(spaceService.createSpace("space")).thenReturn("invitationCode")
        viewModel.createSpace("space")
        assert(!viewModel.state.value.creatingSpace)
    }

    @Test
    fun `createSpace should set currentStep to ShareSpaceCodeOnboard after space created`() =
        runTest {
            whenever(spaceService.createSpace("space")).thenReturn("invitationCode")
            viewModel.createSpace("space")
            assert(viewModel.state.value.currentStep == OnboardItems.ShareSpaceCodeOnboard)
        }

    @Test
    fun `navigateToPermission should set OnboardShown to true`() = runTest {
        viewModel.navigateToPermission()
        verify(userPreferences).setOnboardShown(true)
    }

    @Test
    fun `navigateToPermission should navigate to home screen`() = runTest {
        viewModel.navigateToPermission()
        verify(navigator).navigateTo("home", "onboard", true)
    }

    @Test
    fun `onInviteCodeChange should update invite code`() {
        viewModel.onInviteCodeChanged("inviteCode")
        assert(viewModel.state.value.spaceInviteCode == "inviteCode")
    }

    @Test
    fun `onInviteCodeChange should call submitInviteCode when invite code length is 6`() = runTest {
        val invitation = mock<ApiSpaceInvitation>()
        whenever(invitationService.getInvitation("123456")).thenReturn(invitation)

        viewModel.onInviteCodeChanged("123456")

        verify(invitationService).getInvitation("123456")
    }

    @Test
    fun `submitInviteCode should set verifyingInviteCode to true`() = runTest {
        viewModel.onInviteCodeChanged("inviteCode")

        whenever(invitationService.getInvitation("inviteCode")).doSuspendableAnswer {
            withContext(Dispatchers.IO) { delay(1000) }
            return@doSuspendableAnswer null
        }
        viewModel.submitInviteCode()
        assert(viewModel.state.value.verifyingInviteCode)
    }

    @Test
    fun `submitInviteCode should call getInvitation`() = runTest {
        val invitation = mock<ApiSpaceInvitation>()
        viewModel.onInviteCodeChanged("inviteCode")

        whenever(invitationService.getInvitation("inviteCode")).thenReturn(invitation)
        viewModel.submitInviteCode()
        verify(invitationService).getInvitation("inviteCode")
    }

    @Test
    fun `submitInviteCode should set errorInvalidInviteCode state to true if invitation is null`() =
        runTest {
            viewModel.onInviteCodeChanged("inviteCode")

            whenever(invitationService.getInvitation("inviteCode")).thenReturn(null)
            viewModel.submitInviteCode()
            assert(viewModel.state.value.errorInvalidInviteCode)
        }

    @Test
    fun `submitInviteCode should not call getSpace if invitation is null`() = runTest {
        viewModel.onInviteCodeChanged("inviteCode")

        whenever(invitationService.getInvitation("inviteCode")).thenReturn(null)
        viewModel.submitInviteCode()
        verifyNoInteractions(spaceService)
    }

    @Test
    fun `submitInviteCode should set spaceName and spaceId`() = runTest {
        viewModel.onInviteCodeChanged("inviteCode")

        val invitation = mock<ApiSpaceInvitation>()
        whenever(invitation.space_id).thenReturn("spaceId")

        val space = mock<ApiSpace>()
        whenever(space.name).thenReturn("spaceName")

        whenever(invitationService.getInvitation("inviteCode")).thenReturn(invitation)
        whenever(spaceService.getSpace("spaceId")).thenReturn(space)
        viewModel.submitInviteCode()

        assert(viewModel.state.value.spaceName == space.name)
        assert(viewModel.state.value.spaceId == invitation.space_id)
    }

    @Test
    fun `submitInviteCode should set verifyingInviteCode and errorInvalidInviteCode to false`() =
        runTest {
            viewModel.onInviteCodeChanged("inviteCode")

            val invitation = mock<ApiSpaceInvitation>()
            whenever(invitation.space_id).thenReturn("spaceId")

            val space = mock<ApiSpace>()
            whenever(space.name).thenReturn("spaceName")

            whenever(invitationService.getInvitation("inviteCode")).thenReturn(invitation)
            whenever(spaceService.getSpace("spaceId")).thenReturn(space)
            viewModel.submitInviteCode()

            assert(!viewModel.state.value.verifyingInviteCode)
            assert(!viewModel.state.value.errorInvalidInviteCode)
        }

    @Test
    fun `submitInviteCode should set currentStep to JoinSpace`() = runTest {
        viewModel.onInviteCodeChanged("inviteCode")

        val invitation = mock<ApiSpaceInvitation>()
        whenever(invitation.space_id).thenReturn("spaceId")

        val space = mock<ApiSpace>()
        whenever(space.name).thenReturn("spaceName")

        whenever(invitationService.getInvitation("inviteCode")).thenReturn(invitation)
        whenever(spaceService.getSpace("spaceId")).thenReturn(space)
        viewModel.submitInviteCode()

        assert(viewModel.state.value.currentStep == OnboardItems.JoinSpace)
    }

    @Test
    fun `submitInviteCode should set error state if getInvitation throws exception`() = runTest {
        viewModel.onInviteCodeChanged("inviteCode")

        whenever(invitationService.getInvitation("inviteCode")).thenThrow(RuntimeException("error"))
        viewModel.submitInviteCode()
        assert(viewModel.state.value.error == "error")
    }

    @Test
    fun `joinSpace should set joiningSpace to true`() = runTest {
        val invitation = mock<ApiSpaceInvitation>()

        whenever(invitation.space_id).thenReturn("spaceId")

        val space = mock<ApiSpace>()
        whenever(space.name).thenReturn("spaceName")

        whenever(invitationService.getInvitation("inviteCode")).thenReturn(invitation)
        whenever(spaceService.getSpace("spaceId")).thenReturn(space)

        whenever(spaceService.joinSpace("spaceId")).doSuspendableAnswer {
            withContext(Dispatchers.IO) { delay(5000) }
            return@doSuspendableAnswer null
        }

        viewModel.onInviteCodeChanged("inviteCode")
        viewModel.submitInviteCode()
        viewModel.joinSpace()
        assert(viewModel.state.value.joiningSpace)
    }

    @Test
    fun `joinSpace should call joinSpace`() = runTest {
        val invitation = mock<ApiSpaceInvitation>()

        whenever(invitation.space_id).thenReturn("spaceId")

        val space = mock<ApiSpace>()
        whenever(space.name).thenReturn("spaceName")

        whenever(invitationService.getInvitation("inviteCode")).thenReturn(invitation)
        whenever(spaceService.getSpace("spaceId")).thenReturn(space)

        viewModel.onInviteCodeChanged("inviteCode")
        viewModel.submitInviteCode()

        viewModel.joinSpace()
        verify(spaceService).joinSpace("spaceId")
    }

    @Test
    fun `joinSpace should set joiningSpace to false after space joined`() = runTest {
        val invitation = mock<ApiSpaceInvitation>()

        whenever(invitation.space_id).thenReturn("spaceId")

        val space = mock<ApiSpace>()
        whenever(space.name).thenReturn("spaceName")

        whenever(invitationService.getInvitation("inviteCode")).thenReturn(invitation)
        whenever(spaceService.getSpace("spaceId")).thenReturn(space)

        viewModel.onInviteCodeChanged("inviteCode")
        viewModel.submitInviteCode()
        viewModel.joinSpace()

        assert(!viewModel.state.value.joiningSpace)
    }

    @Test
    fun `joinSpace should navigate to permission screen after space joined`() = runTest {
        val invitation = mock<ApiSpaceInvitation>()

        whenever(invitation.space_id).thenReturn("spaceId")

        val space = mock<ApiSpace>()
        whenever(space.name).thenReturn("spaceName")

        whenever(invitationService.getInvitation("inviteCode")).thenReturn(invitation)
        whenever(spaceService.getSpace("spaceId")).thenReturn(space)

        viewModel.onInviteCodeChanged("inviteCode")
        viewModel.submitInviteCode()
        viewModel.joinSpace()

        verify(navigator).navigateTo("home", "onboard", true)
    }

    @Test
    fun `joinSpace should set OnboardShown to true after space joined`() = runTest {
        val invitation = mock<ApiSpaceInvitation>()

        whenever(invitation.space_id).thenReturn("spaceId")

        val space = mock<ApiSpace>()
        whenever(space.name).thenReturn("spaceName")

        whenever(invitationService.getInvitation("inviteCode")).thenReturn(invitation)
        whenever(spaceService.getSpace("spaceId")).thenReturn(space)

        viewModel.onInviteCodeChanged("inviteCode")
        viewModel.submitInviteCode()
        viewModel.joinSpace()

        verify(userPreferences).setOnboardShown(true)
    }

    @Test
    fun `joinSpace should set error if joinSpace throw error`() = runTest {
        val invitation = mock<ApiSpaceInvitation>()

        whenever(invitation.space_id).thenReturn("spaceId")

        val space = mock<ApiSpace>()
        whenever(space.name).thenReturn("spaceName")

        whenever(invitationService.getInvitation("inviteCode")).thenReturn(invitation)
        whenever(spaceService.getSpace("spaceId")).thenReturn(space)
        whenever(spaceService.joinSpace("spaceId")).thenThrow(RuntimeException("error"))

        viewModel.onInviteCodeChanged("inviteCode")
        viewModel.submitInviteCode()
        viewModel.joinSpace()

        assert(viewModel.state.value.error == "error")
    }

    @Test
    fun `resetErrorState should reset error state`() {
        viewModel.resetErrorState()
        assert(viewModel.state.value.error == null)
        assert(!viewModel.state.value.errorInvalidInviteCode)
    }
}
