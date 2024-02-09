package com.canopas.yourspace.ui.flow.home.space.create

import androidx.lifecycle.SavedStateHandle
import com.canopas.yourspace.ui.navigation.AppDestinations
import com.canopas.yourspace.ui.navigation.AppDestinations.SpaceInvitation.KEY_INVITE_CODE
import com.canopas.yourspace.ui.navigation.AppNavigator
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class SpaceInviteCodeViewModelTest {

    private val savedStateHandle = mock<SavedStateHandle>()
    private val appNavigator = mock<AppNavigator>()

    @Test
    fun `popBackStack should call navigateBack on appNavigator`() {
        val viewModel = SpaceInviteCodeViewModel(appNavigator, savedStateHandle)
        viewModel.popBackStack()
        verify(appNavigator).navigateBack()
    }

    @Test
    fun `spaceInviteCode should return value from savedStateHandle`() {
        whenever(savedStateHandle.get<String>(KEY_INVITE_CODE)).thenReturn("123456")
        val viewModel = SpaceInviteCodeViewModel(appNavigator, savedStateHandle)
        assert(viewModel.spaceInviteCode == "123456")
    }

    @Test
    fun `spaceName should return value from savedStateHandle`() {
        whenever(savedStateHandle.get<String>(AppDestinations.SpaceInvitation.KEY_SPACE_NAME)).thenReturn("space_name")
        val viewModel = SpaceInviteCodeViewModel(appNavigator, savedStateHandle)
        assert(viewModel.spaceName == "space_name")
    }
}
