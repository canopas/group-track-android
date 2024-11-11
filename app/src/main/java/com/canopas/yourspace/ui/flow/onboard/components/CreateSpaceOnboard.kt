package com.canopas.yourspace.ui.flow.onboard.components

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.canopas.yourspace.R
import com.canopas.yourspace.domain.utils.ConnectivityObserver
import com.canopas.yourspace.ui.component.AppBanner
import com.canopas.yourspace.ui.component.CreateSpace
import com.canopas.yourspace.ui.flow.onboard.OnboardItems
import com.canopas.yourspace.ui.flow.onboard.OnboardViewModel
import com.canopas.yourspace.ui.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateSpaceOnboard() {
    val viewModel = hiltViewModel<OnboardViewModel>()
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    val initialName = if (state.lastName.isEmpty()) {
        ""
    } else {
        stringResource(
            id = R.string.onboard_create_space_initial_name,
            state.lastName
        )
    }
    var spaceName by rememberSaveable { mutableStateOf(initialName) }

    BackHandler {
        viewModel.popTo(OnboardItems.JoinOrCreateSpace)
    }
    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppTheme.colorScheme.surface),
                title = {
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.popTo(OnboardItems.JoinOrCreateSpace) }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = ""
                        )
                    }
                }
            )
        }
    ) {
        CreateSpace(
            modifier = Modifier
                .padding(it),
            spaceName = spaceName,
            showLoader = state.creatingSpace,
            onSpaceNameChanged = { spaceName = it }
        ) {
            if (state.connectivityStatus == ConnectivityObserver.Status.Available) {
                viewModel.createSpace(spaceName)
            } else {
                Toast.makeText(context, R.string.common_internet_error_toast, Toast.LENGTH_SHORT).show()
            }
        }
    }

    if (state.error != null) {
        AppBanner(msg = state.error!!) {
            viewModel.resetErrorState()
        }
    }
}
