package com.canopas.yourspace.ui.flow.onboard.components

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.canopas.yourspace.R
import com.canopas.yourspace.domain.utils.ConnectivityObserver
import com.canopas.yourspace.ui.component.PrimaryButton
import com.canopas.yourspace.ui.component.PrimaryTextButton
import com.canopas.yourspace.ui.flow.onboard.OnboardViewModel
import com.canopas.yourspace.ui.theme.AppTheme

@Composable
fun ShareSpaceCodeOnboard() {
    val viewModel = hiltViewModel<OnboardViewModel>()
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(AppTheme.colorScheme.surface)
            .padding(horizontal = 16.dp)
            .padding(top = 40.dp, bottom = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))
        Text(
            text = stringResource(R.string.onboard_share_space_code_title),
            style = AppTheme.appTypography.header1,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
        )
        Text(
            text = stringResource(R.string.onboard_share_space_code_subtitle),
            style = AppTheme.appTypography.header4.copy(fontWeight = FontWeight.W500),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        )
        Spacer(modifier = Modifier.height(40.dp))
        InvitationCodeComponent(state.spaceInviteCode ?: "")
        Spacer(modifier = Modifier.height(40.dp))
        Text(
            text = stringResource(R.string.onboard_share_space_code_tips),
            style = AppTheme.appTypography.body1,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()

        )
        Spacer(modifier = Modifier.weight(1f))
        PrimaryButton(
            label = stringResource(R.string.onboard_share_space_code_btn),
            onClick = {
                when (state.connectivityStatus) {
                    ConnectivityObserver.Status.Available -> {
                        shareInvitationCode(context, state.spaceInviteCode ?: "")
                    }

                    else -> {
                        Toast.makeText(
                            context,
                            R.string.common_internet_error_toast,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(10.dp))
        PrimaryTextButton(
            label = stringResource(R.string.common_btn_skip),
            onClick = { viewModel.navigateToHome() },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private fun shareInvitationCode(context: Context, code: String) {
    val sendIntent: Intent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(
            Intent.EXTRA_TEXT,
            context.getString(R.string.common_share_invite_code_message, code)
        )
        type = "text/plain"
    }

    val shareIntent = Intent.createChooser(sendIntent, null)
    context.startActivity(shareIntent)
}

@Composable
fun InvitationCodeComponent(spaceCode: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(color = AppTheme.colorScheme.tertiary, shape = RoundedCornerShape(20.dp))
            .padding(vertical = 24.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = spaceCode,
            style = AppTheme.appTypography.header1.copy(
                color = AppTheme.colorScheme.textInversePrimary,
                fontSize = 34.sp,
                letterSpacing = 0.8.sp,
                fontWeight = FontWeight.ExtraBold
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.onboard_share_code_expiry_desc),
            style = AppTheme.appTypography.body1.copy(
                color = AppTheme.colorScheme.textInversePrimary.copy(
                    alpha = 0.8f
                )
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()

        )
    }
}
