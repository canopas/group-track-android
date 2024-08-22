package com.canopas.yourspace.ui.flow.home.space.join

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.canopas.yourspace.R
import com.canopas.yourspace.data.models.space.ApiSpace
import com.canopas.yourspace.ui.component.AppBanner
import com.canopas.yourspace.ui.component.OtpInputField
import com.canopas.yourspace.ui.component.PrimaryButton
import com.canopas.yourspace.ui.theme.AppTheme

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
                    style = AppTheme.appTypography.subTitle1
                )
            },
            navigationIcon = {
                IconButton(onClick = { viewModel.popBackStack() }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_nav_back_arrow_icon),
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
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.join_space_title_enter_code),
            style = AppTheme.appTypography.header3,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(40.dp))
        OtpInputField(pinText = state.inviteCode, onPinTextChange = {
            viewModel.onCodeChanged(it)
        })
        Spacer(modifier = Modifier.height(40.dp))
        Text(
            text = stringResource(R.string.onboard_space_join_subtitle),
            style = AppTheme.appTypography.subTitle1.copy(color = AppTheme.colorScheme.textDisabled),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )
        Spacer(modifier = Modifier.weight(1f))
        PrimaryButton(
            modifier = Modifier.fillMaxWidth(),
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
        AppBanner(
            msg = stringResource(id = R.string.onboard_space_invalid_invite_code),
            containerColor = AppTheme.colorScheme.alertColor
        ) {
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
