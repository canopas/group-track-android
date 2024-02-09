package com.canopas.yourspace.ui.flow.auth.permission

import com.canopas.yourspace.ui.flow.permission.EnablePermissionViewModel
import com.canopas.yourspace.ui.navigation.AppNavigator
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class EnablePermissionViewModelTest {

    private val appNavigator = mock<AppNavigator>()

    private val viewModel = EnablePermissionViewModel(appNavigator)

    @Test
    fun `popBack should call navigate back`() {
        viewModel.popBack()

        verify(appNavigator).navigateBack()
    }
}
