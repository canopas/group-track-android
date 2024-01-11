package com.canopas.catchme.ui.flow.intro

import com.canopas.catchme.MainCoroutineRule
import com.canopas.catchme.data.storage.UserPreferences
import com.canopas.catchme.ui.navigation.AppNavigator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@ExperimentalCoroutinesApi
class IntroViewModelTest {
    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private lateinit var viewModel: IntroViewModel

    private val userPreferences = mock<UserPreferences>()
    private val navigator = mock<AppNavigator>()

    @Before
    fun setup() {
        viewModel = IntroViewModel(userPreferences, navigator)
    }

    @Test
    fun `completedIntro should set IntroShown to true`() = runTest {
        viewModel.completedIntro()
        verify(userPreferences).setIntroShown(true)
    }

    @Test
    fun `completedIntro should navigate to signIn screen`() = runTest {
        viewModel.completedIntro()
        verify(navigator).navigateTo(
            "sign-in",
            popUpToRoute = "intro",
            inclusive = true
        )
    }
}
