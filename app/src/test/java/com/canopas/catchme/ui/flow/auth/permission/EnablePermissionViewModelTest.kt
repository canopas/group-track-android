package com.canopas.catchme.ui.flow.auth.permission

import com.canopas.catchme.ui.flow.permission.EnablePermissionViewModel
import com.canopas.catchme.ui.navigation.HomeNavigator
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class EnablePermissionViewModelTest {

    private val appNavigator = mock<HomeNavigator>()

    private val viewModel = EnablePermissionViewModel(appNavigator)

    @Test
    fun `popBack should call navigate back`() {
        viewModel.popBack()

        verify(appNavigator).navigateBack()
    }
}
