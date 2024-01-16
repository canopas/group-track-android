package com.canopas.catchme.ui.flow.auth.permission

import com.canopas.catchme.ui.navigation.AppNavigator
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class EnablePermissionViewModelTest {

    private val appNavigator = mock<AppNavigator>()

    private val viewModel = EnablePermissionViewModel(appNavigator)

    @Test
    fun `navigationToHome should navigate to home screen`() {
        viewModel.navigationToHome()

        verify(appNavigator).navigateTo("home", "enable-permissions", true)
    }
}
