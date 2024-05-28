package com.canopas.yourspace.ui.flow.settings

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.canopas.yourspace.BuildConfig
import com.canopas.yourspace.R
import com.canopas.yourspace.data.models.space.ApiSpace
import com.canopas.yourspace.data.models.user.ApiUser
import com.canopas.yourspace.data.utils.Config
import com.canopas.yourspace.ui.component.AppAlertDialog
import com.canopas.yourspace.ui.component.AppProgressIndicator
import com.canopas.yourspace.ui.component.motionClickEvent
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
                        style = AppTheme.appTypography.subTitle1
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
            style = AppTheme.appTypography.subTitle1,
            color = AppTheme.colorScheme.textDisabled,
            textAlign = TextAlign.Start,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )
        state.user?.let {
            ProfileView(user = it) { viewModel.editProfile() }
            Divider(
                modifier = Modifier
                    .padding(bottom = 24.dp)
                    .fillMaxWidth(),
                color = AppTheme.colorScheme.outline
            )
        }

        SpaceSettingsContent(state.spaces, state.loadingSpaces) {
            viewModel.navigateToSpaceSettings(it)
        }

        Spacer(modifier = Modifier.height(24.dp))

        OtherSettingsContent(viewModel)

        Spacer(modifier = Modifier.height(30.dp))

        Text(
            text = stringResource(id = R.string.setting_app_version, BuildConfig.VERSION_NAME),
            style = AppTheme.appTypography.caption,
            color = AppTheme.colorScheme.textDisabled
        )

        if (state.openSignOutDialog) {
            ShowSignOutDialog(viewModel)
        }
    }
}

@Composable
private fun OtherSettingsContent(viewModel: SettingsViewModel) {
    val context = LocalContext.current
    Text(
        text = stringResource(id = R.string.setting_other),
        style = AppTheme.appTypography.subTitle1,
        color = AppTheme.colorScheme.textDisabled,
        textAlign = TextAlign.Start,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp)
    )

    SettingsItem(
        label = stringResource(id = R.string.setting_contact_support),
        icon = R.drawable.ic_setting_contact_support_icon,
        onClick = {
            viewModel.showContactSupport()
        }
    )

    SettingsItem(
        label = stringResource(id = R.string.setting_privacy),
        icon = R.drawable.ic_setting_privacy_icon,
        onClick = {
            openUrl(context as Activity, Config.PRIVACY_POLICY_URL)
        }
    )

    SettingsItem(
        label = stringResource(id = R.string.setting_about_us),
        icon = R.drawable.ic_setting_about_us_icon,
        onClick = {
            openUrl(context as Activity, Config.PRIVACY_POLICY_URL)
        }
    )

    SettingsItem(
        label = stringResource(id = R.string.setting_btn_sign_out),
        icon = R.drawable.ic_setting_sign_out_icon,
        onClick = {
            viewModel.showSignOutConfirmation(true)
        }
    )
}

private fun openUrl(context: Activity, url: String) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        data = Uri.parse(url)
    }
    context.startActivity(intent)
}

@Composable
private fun SpaceSettingsContent(
    spaces: List<ApiSpace>,
    loading: Boolean,
    openSpaceSettings: (String) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(id = R.string.setting_spaces),
            style = AppTheme.appTypography.subTitle1,
            color = AppTheme.colorScheme.textDisabled,
            textAlign = TextAlign.Start,
            modifier = Modifier.padding(start = 16.dp)
        )
        if (loading) {
            AppProgressIndicator()
        }
    }

    spaces.forEach { space ->
        val isLastItem = spaces.last().id == space.id
        SettingsItem(
            label = space.name,
            char = space.name.firstOrNull().toString(),
            showDivider = !isLastItem,
            onClick = { openSpaceSettings(space.id) }
        )
    }
    HorizontalDivider(
        modifier = Modifier
            .padding(top = 16.dp)
            .fillMaxWidth(),
        color = AppTheme.colorScheme.outline
    )
}

@Composable
private fun SettingsItem(
    label: String,
    char: String? = null,
    icon: Int? = null,
    showDivider: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .motionClickEvent { onClick() }
            .padding(start = 16.dp, end = 16.dp, top = 16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(
                    color = AppTheme.colorScheme.containerLow,
                    shape = RoundedCornerShape(8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (char != null) {
                Text(
                    text = char,
                    style = AppTheme.appTypography.subTitle1,
                    color = AppTheme.colorScheme.textPrimary
                )
            }
            if (icon != null) {
                Image(
                    painter = painterResource(id = icon),
                    contentDescription = "",
                    colorFilter = ColorFilter.tint(AppTheme.colorScheme.textPrimary)
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            style = AppTheme.appTypography.subTitle2,
            color = AppTheme.colorScheme.textPrimary,
            modifier = Modifier.weight(1f)
        )

        Icon(
            painter = painterResource(id = R.drawable.ic_right_arrow_icon),
            contentDescription = "",
            tint = AppTheme.colorScheme.textSecondary
        )
    }
    if (showDivider) {
        HorizontalDivider(
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, top = 16.dp)
                .fillMaxWidth(),
            color = AppTheme.colorScheme.outline
        )
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
fun ProfileView(user: ApiUser, onClick: () -> Unit) {
    val userName = user.fullName
    val profileImageUrl = user.profile_image ?: ""

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .motionClickEvent { onClick() }
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
            .background(
                color = AppTheme.colorScheme.containerLow,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(vertical = 16.dp)
    ) {
        ProfileImageView(
            data = profileImageUrl,
            modifier = Modifier
                .padding(start = 16.dp)
                .size(64.dp)
                .border(1.dp, AppTheme.colorScheme.textDisabled, CircleShape),
            char = user.fullName.firstOrNull().toString()
        )

        Column(
            modifier = Modifier
                .padding(start = 16.dp)
                .weight(1f)
        ) {
            Text(
                text = userName ?: "",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = AppTheme.appTypography.subTitle2,
                color = AppTheme.colorScheme.textPrimary
            )
        }
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
