package com.canopas.yourspace.ui.flow.intro

import com.canopas.yourspace.MainCoroutineRule
import com.canopas.yourspace.data.storage.UserPreferences
import com.canopas.yourspace.ui.navigation.AppNavigator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@Ignore
@ExperimentalCoroutinesApi
class IntroViewModelTest {
    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private lateinit var viewModel: IntroViewModel

    private val userPreferences = mock<UserPreferences>()
    private val navigator = mock<AppNavigator>()

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
