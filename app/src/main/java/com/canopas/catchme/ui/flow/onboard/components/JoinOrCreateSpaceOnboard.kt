package com.canopas.catchme.ui.flow.onboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
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
import com.canopas.catchme.ui.component.AppBanner
import com.canopas.catchme.ui.component.OtpInputField
import com.canopas.catchme.ui.component.PrimaryButton
import com.canopas.catchme.ui.flow.home.space.join.JoinedSpacePopup
import com.canopas.catchme.ui.flow.onboard.OnboardViewModel
import com.canopas.catchme.ui.theme.AppTheme

@Composable
fun JoinOrCreateSpaceOnboard() {
    val viewModel = hiltViewModel<OnboardViewModel>()
    val state by viewModel.state.collectAsState()

    val scrollState = rememberScrollState()
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .background(AppTheme.colorScheme.surface)
    ) {
        JoinSpaceComponent(
            state.spaceInviteCode ?: "",
            state.verifyingInviteCode,
            onCodeChanged = {
                viewModel.onInviteCodeChanged(it)
            }
        ) {
            viewModel.submitInviteCode()
        }

        Spacer(modifier = Modifier.height(80.dp))

        CreateSpaceComponent {
            viewModel.navigateToCreateSpace()
        }
    }

    if (state.errorInvalidInviteCode) {
        AppBanner(msg = stringResource(id = R.string.onboard_space_invalid_invite_code)) {
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
    onJoin: () -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))
        Text(
            text = stringResource(R.string.onboard_space_join_title),
            style = AppTheme.appTypography.header2,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(40.dp))
        OtpInputField(pinText = code, onPinTextChange = {
            onCodeChanged(it)
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
            label = stringResource(id = R.string.common_btn_verify),
            onClick = onJoin,
            enabled = code.length == 6,
            showLoader = verifyingInviteCode
        )
    }
}

@Composable
private fun CreateSpaceComponent(onCreateNewSpace: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(top = 25.dp)
                .padding(bottom = 30.dp)
        ) {
            Divider(
                thickness = 1.dp,
                color = AppTheme.colorScheme.textSecondary.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(60.dp))
            Text(
                text = stringResource(R.string.onboard_space_create_title),
                style = AppTheme.appTypography.header2.copy(color = AppTheme.colorScheme.textPrimary),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = stringResource(R.string.onboard_space_create_subtitle),
                style = AppTheme.appTypography.body1.copy(color = AppTheme.colorScheme.textSecondary),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 30.dp)

            )
            Spacer(modifier = Modifier.height(20.dp))
            PrimaryButton(
                label = stringResource(id = R.string.onboard_space_btn_create_new),
                onClick = onCreateNewSpace,
                enabled = true
            )
        }
        Box(
            modifier = Modifier
                .height(50.dp)
                .width(60.dp)
                .background(color = AppTheme.colorScheme.surface, shape = RoundedCornerShape(40.dp))
                .border(
                    width = 1.dp,
                    color = AppTheme.colorScheme.textSecondary.copy(alpha = 0.5f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.common_label_or).uppercase(),
                style = AppTheme.appTypography.subTitle2.copy(
                    color = AppTheme.colorScheme.textSecondary.copy(
                        alpha = 0.5f
                    )
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier
            )
        }
    }
}
