package com.canopas.catchme.ui.flow.onboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.canopas.catchme.R
import com.canopas.catchme.ui.component.CreateSpace
import com.canopas.catchme.ui.flow.onboard.OnboardViewModel
import com.canopas.catchme.ui.theme.AppTheme

@Composable
fun CreateSpaceOnboard() {
    val viewModel = hiltViewModel<OnboardViewModel>()
    val state by viewModel.state.collectAsState()

    val initialName = if (state.lastName.isEmpty()) {
        ""
    } else {
        stringResource(
            id = R.string.onboard_create_space_initial_name,
            state.lastName
        )
    }
    var spaceName by remember { mutableStateOf(initialName) }

    CreateSpace(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colorScheme.surface)
            .padding(top = 80.dp),
        spaceName = spaceName,
        showLoader = state.creatingSpace,
        onSpaceNameChanged = { spaceName = it }
    ) {
        viewModel.createSpace(spaceName)
    }
}
