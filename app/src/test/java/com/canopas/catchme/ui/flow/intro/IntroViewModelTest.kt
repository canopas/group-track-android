package com.canopas.catchme.ui.flow.intro

import com.canopas.catchme.MainCoroutineRule
import com.canopas.catchme.data.storage.UserPreferences
import com.canopas.catchme.ui.navigation.MainNavigator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class IntroViewModelTest {
    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private lateinit var viewModel: IntroViewModel

    private val userPreferences = mock<UserPreferences>()
    private val navigator = mock<MainNavigator>()

    @Test
    fun `should navigate to sign-in screen if intro is shown and current user is null`() = runTest {
        whenever(userPreferences.isIntroShown()).thenReturn(true)
        whenever(userPreferences.currentUser).thenReturn(null)
        viewModel = IntroViewModel(userPreferences, navigator)
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

        viewModel = IntroViewModel(userPreferences, navigator)
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
        viewModel = IntroViewModel(userPreferences, navigator)
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

        viewModel = IntroViewModel(userPreferences, navigator)
        verify(navigator, times(0)).navigateTo(
            "onboard",
            popUpToRoute = "intro",
            inclusive = true
        )
    }

    @Test
    fun `completedIntro should set IntroShown to true`() = runTest {
        whenever(userPreferences.currentUser).thenReturn(mock())
        whenever(userPreferences.isIntroShown()).thenReturn(true)
        whenever(userPreferences.isOnboardShown()).thenReturn(true)
        viewModel = IntroViewModel(userPreferences, navigator)
        viewModel.completedIntro()
        verify(userPreferences).setIntroShown(true)
    }

    @Test
    fun `completedIntro should navigate to signIn screen`() = runTest {
        whenever(userPreferences.isIntroShown()).thenReturn(false)
        viewModel = IntroViewModel(userPreferences, navigator)
        viewModel.completedIntro()
        verify(navigator).navigateTo(
            "sign-in",
            popUpToRoute = "intro",
            inclusive = true
        )
    }
}
