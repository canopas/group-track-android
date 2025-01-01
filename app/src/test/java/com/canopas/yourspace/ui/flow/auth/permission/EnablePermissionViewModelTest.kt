package com.canopas.yourspace.ui.flow.auth.permission

import com.canopas.yourspace.data.storage.UserPreferences
import com.canopas.yourspace.data.utils.AppDispatcher
import com.canopas.yourspace.ui.flow.permission.EnablePermissionViewModel
import com.canopas.yourspace.ui.navigation.AppNavigator
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class EnablePermissionViewModelTest {

    private val appNavigator = mock<AppNavigator>()
    private val userPreferences = mock<UserPreferences>()
    private val appDispatcher = mock<AppDispatcher>()

    private val viewModel = EnablePermissionViewModel(appNavigator, userPreferences, appDispatcher)

    @Test
    fun `popBack should call navigate back`() {
        viewModel.popBack()

        verify(appNavigator).navigateBack()
    }
}
