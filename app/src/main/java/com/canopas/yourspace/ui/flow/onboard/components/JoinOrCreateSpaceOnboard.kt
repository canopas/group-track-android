package com.canopas.yourspace.ui.flow.onboard.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
        JoinSpaceComponent()
    }

    if (state.errorInvalidInviteCode) {
        AppBanner(
            msg = stringResource(if (state.isInternetAvailable) R.string.onboard_space_invalid_invite_code else R.string.common_internet_error_toast),
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
private fun JoinSpaceComponent() {
    val viewModel = hiltViewModel<OnboardViewModel>()
    val state by viewModel.state.collectAsState()

    val context = LocalContext.current

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
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
        OtpInputField(pinText = state.spaceInviteCode ?: "", onPinTextChange = {
            viewModel.onInviteCodeChanged(it)
        })

        Spacer(modifier = Modifier.weight(1f))

        PrimaryButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            label = stringResource(id = R.string.common_btn_join_space),
            onClick = { viewModel.submitInviteCode() },
            enabled = state.spaceInviteCode?.length == 6,
            showLoader = state.verifyingInviteCode
        )

        Spacer(modifier = Modifier.height(16.dp))

        PrimaryButton(
            modifier = Modifier.fillMaxWidth(),
            label = stringResource(id = R.string.onboard_space_btn_create_new),
            onClick = {
                if (state.isInternetAvailable) {
                    viewModel.navigateToCreateSpace()
                } else {
                    Toast.makeText(
                        context,
                        R.string.common_internet_error_toast,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )
        Spacer(modifier = Modifier.height(24.dp))
    }
}
