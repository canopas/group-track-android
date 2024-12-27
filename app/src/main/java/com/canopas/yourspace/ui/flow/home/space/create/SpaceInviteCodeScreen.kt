package com.canopas.yourspace.ui.flow.home.space.create

import android.content.Context
import android.content.Intent
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.canopas.yourspace.R
import com.canopas.yourspace.domain.utils.ConnectivityObserver
import com.canopas.yourspace.ui.component.AppBanner
import com.canopas.yourspace.ui.component.PrimaryButton
import com.canopas.yourspace.ui.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpaceInvite() {
    val viewModel = hiltViewModel<SpaceInviteCodeViewModel>()
    val state by viewModel.state.collectAsState()

    if (state.error != null) {
        AppBanner(msg = state.error!!) {
            viewModel.resetErrorState()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.onStart()
    }

    Scaffold(topBar = {
        TopAppBar(
            colors = TopAppBarDefaults.topAppBarColors(containerColor = AppTheme.colorScheme.surface),
            title = {
                Text(
                    text = stringResource(id = R.string.space_inviate_code_title),
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
            },
            actions = {
                if (state.isUserAdmin) {
                    IconButton(
                        onClick = {
                            if (state.connectivityStatus == ConnectivityObserver.Status.Available) {
                                viewModel.regenerateInviteCode()
                            } else {
                                viewModel.setErrorState(Exception())
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = ""
                        )
                    }
                }
            }
        )
    }) {
        SpaceInviteContent(spaceInviteCode = state.inviteCode, modifier = Modifier.padding(it))
    }
}

@Composable
private fun SpaceInviteContent(spaceInviteCode: String, modifier: Modifier) {
    val viewModel = hiltViewModel<SpaceInviteCodeViewModel>()
    val context = LocalContext.current
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .background(AppTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.space_invite_title, viewModel.spaceName),
            style = AppTheme.appTypography.header4,
            color = AppTheme.colorScheme.textPrimary,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.space_invite_subtitle),
            style = AppTheme.appTypography.body1.copy(AppTheme.colorScheme.textDisabled),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(40.dp))

        InvitationCodeComponent(spaceInviteCode)

        Spacer(modifier = Modifier.weight(1f))

        PrimaryButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            label = stringResource(R.string.onboard_share_space_code_btn),
            onClick = { shareInvitationCode(context, spaceInviteCode) }
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
            .background(
                color = AppTheme.colorScheme.containerLow,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = spaceCode,
            style = AppTheme.appTypography.header1.copy(
                color = AppTheme.colorScheme.primary,
                letterSpacing = 0.8.sp,
                fontWeight = FontWeight.ExtraBold
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.onboard_share_code_expiry_desc),
            style = AppTheme.appTypography.subTitle1.copy(
                color = AppTheme.colorScheme.textSecondary
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
