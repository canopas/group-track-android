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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import com.canopas.yourspace.ui.component.PrimaryButton
import com.canopas.yourspace.ui.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpaceInvite() {
    val viewModel = hiltViewModel<SpaceInviteCodeViewModel>()

    Scaffold(topBar = {
        TopAppBar(
            colors = TopAppBarDefaults.topAppBarColors(containerColor = AppTheme.colorScheme.surface),
            title = {
                Text(
                    text = stringResource(id = R.string.space_inviate_code_title),
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
        SpaceInviteContent(modifier = Modifier.padding(it))
    }
}

@Composable
private fun SpaceInviteContent(modifier: Modifier) {
    val viewModel = hiltViewModel<SpaceInviteCodeViewModel>()
    val context = LocalContext.current
    Column(
        modifier
            .fillMaxSize()
            .background(AppTheme.colorScheme.surface)
            .padding(horizontal = 16.dp)
            .padding(top = 40.dp, bottom = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.space_invite_title, viewModel.spaceName),
            style = AppTheme.appTypography.header1,
            modifier = Modifier
                .fillMaxWidth()
        )
        Text(
            text = stringResource(R.string.space_invite_subtitle),
            style = AppTheme.appTypography.subTitle1.copy(AppTheme.colorScheme.textSecondary),
            modifier = Modifier
                .fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(40.dp))
        InvitationCodeComponent(viewModel.spaceInviteCode)
        Spacer(modifier = Modifier.height(40.dp))
        PrimaryButton(
            label = stringResource(R.string.onboard_share_space_code_btn),
            onClick = { shareInvitationCode(context, viewModel.spaceInviteCode) }
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
            .background(color = AppTheme.colorScheme.primary.copy(alpha = 0.1f), shape = RoundedCornerShape(20.dp))
            .padding(vertical = 24.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = spaceCode,
            style = AppTheme.appTypography.header1.copy(
                color = AppTheme.colorScheme.primary,
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
                color = AppTheme.colorScheme.textPrimary.copy(
                    alpha = 0.8f
                )
            ),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()

        )
    }
}
