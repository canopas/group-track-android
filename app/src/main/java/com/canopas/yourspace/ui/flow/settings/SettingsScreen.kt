package com.canopas.yourspace.ui.flow.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowRight
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.canopas.yourspace.R
import com.canopas.yourspace.data.models.user.ApiUser
import com.canopas.yourspace.ui.component.AppAlertDialog
import com.canopas.yourspace.ui.component.PrimaryButton
import com.canopas.yourspace.ui.component.PrimaryTextButton
import com.canopas.yourspace.ui.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val viewModel = hiltViewModel<SettingsViewModel>()

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppTheme.colorScheme.surface),
                title = {
                    Text(
                        text = stringResource(id = R.string.settings_title),
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
        }
    ) {
        SettingsContent(modifier = Modifier.padding(it))
    }
}

@Composable
private fun SettingsContent(modifier: Modifier) {
    val scrollState = rememberScrollState()
    val viewModel = hiltViewModel<SettingsViewModel>()
    val state by viewModel.state.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(bottom = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(id = R.string.setting_profile),
            style = AppTheme.appTypography.header3,
            color = AppTheme.colorScheme.textPrimary,
            textAlign = TextAlign.Start,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 12.dp)
        )
        state.user?.let { ProfileView(user = it) }

        Spacer(modifier = Modifier.weight(1f))
        PrimaryButton(
            label = stringResource(id = R.string.setting_btn_sign_out),
            onClick = {
                viewModel.showSignOutConfirmation(true)
            },
            contentColor = AppTheme.colorScheme.alertColor,
            containerColor = AppTheme.colorScheme.containerHigh,
            showLoader = state.signingOut
        )
        Spacer(modifier = Modifier.height(8.dp))
        PrimaryTextButton(
            label = stringResource(id = R.string.settings_btn_delete_account),
            onClick = {
                viewModel.showDeleteAccountConfirmation(true)
            },
            contentColor = AppTheme.colorScheme.alertColor,
            showLoader = state.deletingAccount
        )

        if (state.openSignOutDialog) {
            ShowSignOutDialog(viewModel)
        }

        if (state.openDeleteAccountDialog) {
            ShowDeleteAccountDialog(viewModel)
        }
    }
}

@Composable
fun ShowSignOutDialog(viewModel: SettingsViewModel) {
    AppAlertDialog(
        title = stringResource(R.string.setting_sign_out_dialogue_title_text),
        subTitle = stringResource(R.string.setting_sign_out_dialogue_message_text),
        confirmBtnText = stringResource(R.string.setting_sign_out_dialogue_confirm_btn_text),
        dismissBtnText = stringResource(R.string.common_btn_cancel),
        onConfirmClick = { viewModel.signOutUser() },
        onDismissClick = { viewModel.showSignOutConfirmation(false) },
        isConfirmDestructive = true
    )
}

@Composable
fun ShowDeleteAccountDialog(viewModel: SettingsViewModel) {
    AppAlertDialog(
        title = stringResource(R.string.setting_delete_account_dialogue_title_text),
        subTitle = stringResource(R.string.setting_delete_account_dialogue_message_text),
        confirmBtnText = stringResource(R.string.setting_delete_account_dialogue_confirm_btn_text),
        dismissBtnText = stringResource(R.string.common_btn_cancel),
        onConfirmClick = { viewModel.deleteAccount() },
        onDismissClick = { viewModel.showDeleteAccountConfirmation(false) },
        isConfirmDestructive = true
    )
}

@Composable
fun ProfileView(user: ApiUser) {
    val userName = user.fullName
    val profileImageUrl = user.profile_image ?: ""

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 16.dp)
    ) {
        ProfileImageView(
            data = profileImageUrl,
            modifier = Modifier
                .size(66.dp)
                .border(1.dp, AppTheme.colorScheme.textDisabled, CircleShape),
            char = user.fullName.first().toString()
        )

        Column(
            modifier = Modifier
                .padding(start = 16.dp)
                .weight(1f)
        ) {
            Text(
                text = userName ?: "",
                style = AppTheme.appTypography.subTitle2,
                color = AppTheme.colorScheme.textPrimary
            )
        }
        Icon(
            Icons.Default.KeyboardArrowRight,
            contentDescription = "Consulting Image",
            modifier = Modifier.padding(horizontal = 8.dp),
            tint = AppTheme.colorScheme.textSecondary
        )
    }
}

@Composable
fun ProfileImageView(
    data: String?,
    modifier: Modifier = Modifier,
    char: String,
    backgroundColor: Color = AppTheme.colorScheme.containerHigh,
    textColor: Color = AppTheme.colorScheme.textPrimary
) {
    if (!data.isNullOrEmpty()) {
        Image(
            painter = rememberAsyncImagePainter(
                ImageRequest.Builder(LocalContext.current).data(data = data).build()
            ),
            modifier = modifier.clip(CircleShape),
            contentScale = ContentScale.Crop,
            contentDescription = "ProfileImage"
        )
    } else {
        BoxWithConstraints(
            modifier = modifier
                .clip(CircleShape)
                .background(backgroundColor)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = char ?: "?",
                style = AppTheme.appTypography.subTitle1.copy(
                    fontSize = (maxWidth.value + maxHeight.value).times(0.18).sp
                ),
                color = textColor,
                textAlign = TextAlign.Center
            )
        }
    }
}
