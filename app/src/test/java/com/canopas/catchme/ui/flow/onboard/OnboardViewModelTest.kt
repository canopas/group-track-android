package com.canopas.catchme.ui.flow.onboard

import com.canopas.catchme.MainCoroutineRule
import com.canopas.catchme.data.models.auth.ApiUser
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
            navigator
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
        assert(viewModel.state.value.spaceCode == "invitationCode")
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
    fun `navigateToJoinSpace should set currentStep to JoinSpace`() {
        viewModel.navigateToJoinSpace("code")
        assert(viewModel.state.value.currentStep == OnboardItems.JoinSpace)
    }

    @Test
    fun `navigateToJoinSpace should set spaceCode`() {
        viewModel.navigateToJoinSpace("code")
        assert(viewModel.state.value.spaceCode == "code")
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
}
