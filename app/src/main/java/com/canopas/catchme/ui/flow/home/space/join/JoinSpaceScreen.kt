package com.canopas.catchme.ui.flow.home.space.join

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.canopas.catchme.R
import com.canopas.catchme.data.models.space.ApiSpace
import com.canopas.catchme.ui.component.AppBanner
import com.canopas.catchme.ui.component.OtpInputField
import com.canopas.catchme.ui.component.PrimaryButton
import com.canopas.catchme.ui.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinSpaceScreen() {
    val viewModel = hiltViewModel<JoinSpaceViewModel>()

    Scaffold(topBar = {
        TopAppBar(
            colors = TopAppBarDefaults.topAppBarColors(containerColor = AppTheme.colorScheme.surface),
            title = {
                Text(
                    text = stringResource(id = R.string.join_space_title),
                    style = AppTheme.appTypography.header3
                )
            },
            navigationIcon = {
                IconButton(onClick = { viewModel.popBackStack() }) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = ""
                    )
                }
            }
        )
    }) {
        JoinSpaceContent(modifier = Modifier.padding(it))
    }
}

@Composable
private fun JoinSpaceContent(modifier: Modifier) {
    val viewModel = hiltViewModel<JoinSpaceViewModel>()
    val state by viewModel.state.collectAsState()
    Column(
        modifier
            .fillMaxWidth()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.join_space_title_enter_code),
            style = AppTheme.appTypography.header2,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(40.dp))
        OtpInputField(pinText = state.inviteCode, onPinTextChange = {
            viewModel.onCodeChanged(it)
        })
        Spacer(modifier = Modifier.height(40.dp))
        Text(
            text = stringResource(R.string.onboard_space_join_subtitle),
            style = AppTheme.appTypography.body1,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()

        )
        Spacer(modifier = Modifier.height(30.dp))
        PrimaryButton(
            label = stringResource(id = R.string.common_btn_join_space),
            onClick = { viewModel.verifyAndJoinSpace() },
            enabled = state.inviteCode.length == 6,
            showLoader = state.verifying
        )
    }

    if (state.error != null) {
        AppBanner(msg = state.error!!) {
            viewModel.resetErrorState()
        }
    }

    if (state.errorInvalidInviteCode) {
        AppBanner(msg = stringResource(id = R.string.onboard_space_invalid_invite_code)) {
            viewModel.resetErrorState()
        }
    }

    if (state.joinedSpace != null) {
        JoinedSpacePopup(state.joinedSpace!!) { viewModel.popBackStack() }
    }
}

@Composable
fun JoinedSpacePopup(space: ApiSpace, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = {
            Text(text = stringResource(id = R.string.common_label_congratulations))
        },
        text = {
            Column(
                modifier = Modifier.padding(8.dp)
            ) {
                Text(
                    text = stringResource(
                        id = R.string.join_space_success_popup_message,
                        space.name
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onDismiss()
                }
            ) {
                Text(stringResource(id = R.string.common_btn_ok))
            }
        }
    )
}
