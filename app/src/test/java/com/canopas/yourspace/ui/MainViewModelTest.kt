package com.canopas.yourspace.ui

import com.canopas.yourspace.MainCoroutineRule
import com.canopas.yourspace.data.models.user.ApiUserSession
import com.canopas.yourspace.data.repository.SpaceRepository
import com.canopas.yourspace.data.service.auth.AuthService
import com.canopas.yourspace.data.service.user.ApiUserService
import com.canopas.yourspace.data.storage.UserPreferences
import com.canopas.yourspace.data.utils.AppDispatcher
import com.canopas.yourspace.ui.navigation.AppNavigator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class MainViewModelTest {
    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private lateinit var viewModel: MainViewModel

    private val userPreferences = mock<UserPreferences>()
    private val navigator = mock<AppNavigator>()
    private val apiUserService = mock<ApiUserService>()
    private val authService = mock<AuthService>()
    private val spaceRepository = mock<SpaceRepository>()
    private val testDispatcher = AppDispatcher(IO = UnconfinedTestDispatcher())

    private fun setup() {
        viewModel = MainViewModel(
            userPreferences,
            navigator,
            testDispatcher,
            apiUserService,
            authService,
            spaceRepository
        )
    }

    @Test
    fun `should update initialRoute state to sign-in if user is null`() = runTest {
        whenever(userPreferences.isIntroShown()).thenReturn(true)
        whenever(userPreferences.currentUser).thenReturn(null)
        whenever(userPreferences.currentUserSessionState).thenReturn(flowOf())
        setup()
        assert(viewModel.state.value.initialRoute == "sign-in")
    }

    @Test
    fun `should update initialRoute state to onboard if onboard is not shown`() = runTest {
        whenever(userPreferences.currentUser).thenReturn(mock())
        whenever(userPreferences.isIntroShown()).thenReturn(true)
        whenever(userPreferences.isOnboardShown()).thenReturn(false)
        whenever(userPreferences.currentUserSessionState).thenReturn(flowOf())
        setup()
        assert(viewModel.state.value.initialRoute == "onboard")
    }

    @Test
    fun `should not update initialRoute state to onboard if onboard is shown`() = runTest {
        whenever(userPreferences.currentUser).thenReturn(mock())
        whenever(userPreferences.isIntroShown()).thenReturn(true)
        whenever(userPreferences.isOnboardShown()).thenReturn(true)
        whenever(userPreferences.currentUserSessionState).thenReturn(flowOf())
        setup()
        assert(viewModel.state.value.initialRoute != "onboard")
    }

    @Test
    fun `listenUserSession should emit sessionExpiredState when session is not active`() = runTest {
        val session = ApiUserSession(user_id = "1", id = "1", session_active = true)
        val updatedSession = ApiUserSession(user_id = "1", id = "1", session_active = false)

        whenever(userPreferences.currentUserSessionState).thenReturn(flowOf(session))
        whenever(apiUserService.getUserSessionFlow("1", "1")).thenReturn(flowOf(updatedSession))
        whenever(userPreferences.isIntroShown()).thenReturn(true)

        setup()
        verify(apiUserService).getUserSessionFlow("1", "1")
        assert(viewModel.state.value.isSessionExpired)
    }

    @Test
    fun `listenUserSession should not emit sessionExpiredState when session is active`() = runTest {
        val session = ApiUserSession(user_id = "1", id = "1", session_active = true)

        whenever(userPreferences.currentUserSessionState).thenReturn(flowOf(session))
        whenever(apiUserService.getUserSessionFlow("1", "1")).thenReturn(flowOf(session))
        whenever(userPreferences.isIntroShown()).thenReturn(true)

        setup()
        verify(apiUserService).getUserSessionFlow("1", "1")
        assert(!viewModel.state.value.isSessionExpired)
    }

    @Test
    fun `listenUserSession should not emit sessionExpiredState when session is null`() = runTest {
        whenever(userPreferences.currentUserSessionState).thenReturn(flowOf(null))
        whenever(userPreferences.isIntroShown()).thenReturn(true)

        setup()
        assert(!viewModel.state.value.isSessionExpired)
    }

    @Test
    fun `signOut should call authService signOut`() = runTest {
        whenever(userPreferences.isIntroShown()).thenReturn(true)
        whenever(userPreferences.currentUserSessionState).thenReturn(flowOf(null))

        setup()
        viewModel.signOut()
        verify(authService).signOut()
    }

    @Test
    fun `signOut should navigate to sign-in screen`() = runTest {
        whenever(userPreferences.isIntroShown()).thenReturn(true)
        whenever(userPreferences.currentUserSessionState).thenReturn(flowOf(null))

        setup()
        viewModel.signOut()
        verify(navigator).navigateTo(
            "sign-in",
            clearStack = true
        )
    }

    @Test
    fun `signOut should emit sessionExpiredState false`() = runTest {
        whenever(userPreferences.isIntroShown()).thenReturn(true)
        whenever(userPreferences.currentUserSessionState).thenReturn(flowOf(null))

        setup()
        viewModel.signOut()
        assert(!viewModel.state.value.isSessionExpired)
    }
}
