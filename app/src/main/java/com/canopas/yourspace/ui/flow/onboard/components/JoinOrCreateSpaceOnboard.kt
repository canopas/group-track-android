package com.canopas.yourspace.ui.flow.onboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.canopas.yourspace.R
import com.canopas.yourspace.ui.component.AppBanner
import com.canopas.yourspace.ui.component.OtpInputField
import com.canopas.yourspace.ui.component.PrimaryButton
import com.canopas.yourspace.ui.flow.home.space.join.JoinedSpacePopup
import com.canopas.yourspace.ui.flow.onboard.OnboardViewModel
import com.canopas.yourspace.ui.theme.AppTheme

@Composable
fun JoinOrCreateSpaceOnboard() {
    val viewModel = hiltViewModel<OnboardViewModel>()
    val state by viewModel.state.collectAsState()

    Column(
        Modifier
            .fillMaxSize()
            .background(AppTheme.colorScheme.surface)
    ) {
        JoinSpaceComponent(
            state.spaceInviteCode ?: "",
            state.verifyingInviteCode,
            onCodeChanged = { viewModel.onInviteCodeChanged(it) },
            onJoin = { viewModel.submitInviteCode() },
            onCreate = { viewModel.navigateToCreateSpace() }
        )
    }

    if (state.errorInvalidInviteCode) {
        AppBanner(
            msg = stringResource(id = R.string.onboard_space_invalid_invite_code),
            containerColor = AppTheme.colorScheme.alertColor
        ) {
            viewModel.resetErrorState()
        }
    }

    if (state.error != null) {
        AppBanner(msg = state.error!!) {
            viewModel.resetErrorState()
        }
    }

    if (state.joinedSpace != null) {
        JoinedSpacePopup(state.joinedSpace!!) { viewModel.navigateToHome() }
    }
}

@Composable
private fun JoinSpaceComponent(
    code: String,
    verifyingInviteCode: Boolean,
    onCodeChanged: (String) -> Unit,
    onJoin: () -> Unit,
    onCreate: () -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))
        Text(
            text = stringResource(R.string.onboard_space_join_title),
            style = AppTheme.appTypography.header3,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.onboard_space_join_subtitle),
            style = AppTheme.appTypography.subTitle1,
            color = AppTheme.colorScheme.textDisabled,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(40.dp))
        OtpInputField(pinText = code, onPinTextChange = {
            onCodeChanged(it)
        })

        Spacer(modifier = Modifier.weight(1f))

        PrimaryButton(
            modifier = Modifier.fillMaxWidth(),
            label = stringResource(id = R.string.common_btn_join_space),
            onClick = onJoin,
            enabled = code.length == 6,
            showLoader = verifyingInviteCode
        )

        Spacer(modifier = Modifier.height(16.dp))

        PrimaryButton(
            modifier = Modifier.fillMaxWidth(),
            label = stringResource(id = R.string.onboard_space_btn_create_new),
            onClick = onCreate
        )
        Spacer(modifier = Modifier.height(24.dp))
    }
}
