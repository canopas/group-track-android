package com.canopas.catchme.ui
import com.canopas.catchme.MainCoroutineRule
import com.canopas.catchme.data.storage.UserPreferences
import com.canopas.catchme.ui.navigation.AppNavigator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class MainViewModelTest {
    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private lateinit var viewModel: MainViewModel

    private val userPreferences = mock<UserPreferences>()
    private val navigator = mock<AppNavigator>()

    @Test
    fun `should navigate to sign-in screen if intro is shown and current user is null`() = runTest {
        whenever(userPreferences.isIntroShown()).thenReturn(true)
        whenever(userPreferences.currentUser).thenReturn(null)
        viewModel = MainViewModel(userPreferences, navigator)
        verify(navigator).navigateTo(
            "sign-in",
            popUpToRoute = "intro",
            inclusive = true
        )
    }

    @Test
    fun `should navigate to home screen if intro is shown`() = runTest {
        whenever(userPreferences.isIntroShown()).thenReturn(true)
        whenever(userPreferences.currentUser).thenReturn(mock())
        whenever(userPreferences.isOnboardShown()).thenReturn(true)

        viewModel = MainViewModel(userPreferences, navigator)
        verify(navigator).navigateTo(
            "home",
            popUpToRoute = "intro",
            inclusive = true
        )
    }

    @Test
    fun `should navigate to onboard screen if onboard is not shown`() = runTest {
        whenever(userPreferences.currentUser).thenReturn(mock())
        whenever(userPreferences.isIntroShown()).thenReturn(true)
        whenever(userPreferences.isOnboardShown()).thenReturn(false)
        viewModel = MainViewModel(userPreferences, navigator)
        verify(navigator).navigateTo(
            "onboard",
            popUpToRoute = "intro",
            inclusive = true
        )
    }

    @Test
    fun `should not navigate to onboard screen if onboard is shown`() = runTest {
        whenever(userPreferences.currentUser).thenReturn(mock())
        whenever(userPreferences.isIntroShown()).thenReturn(true)
        whenever(userPreferences.isOnboardShown()).thenReturn(true)

        viewModel = MainViewModel(userPreferences, navigator)
        verify(navigator, times(0)).navigateTo(
            "onboard",
            popUpToRoute = "intro",
            inclusive = true
        )
    }
}
