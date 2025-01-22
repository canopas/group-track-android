package com.canopas.yourspace.ui.flow.settings.space

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.canopas.yourspace.R
import com.canopas.yourspace.data.models.user.UserInfo
import com.canopas.yourspace.domain.utils.ConnectivityObserver
import com.canopas.yourspace.ui.component.AppAlertDialog
import com.canopas.yourspace.ui.component.AppBanner
import com.canopas.yourspace.ui.component.AppProgressIndicator
import com.canopas.yourspace.ui.component.NoInternetScreen
import com.canopas.yourspace.ui.component.PrimaryTextButton
import com.canopas.yourspace.ui.component.UserProfile
import com.canopas.yourspace.ui.theme.AppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun SpaceProfileScreen() {
    val viewModel = hiltViewModel<SpaceProfileViewModel>()
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            viewModel.fetchSpaceDetail()
        }
    }

    Scaffold(
        topBar = {
            SpaceProfileToolbar()
        }
    ) {
        Box(
            modifier = Modifier
                .padding(it)
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (state.connectivityStatus == ConnectivityObserver.Status.Available) {
                SpaceProfileContent()
                if (state.isLoading) {
                    AppProgressIndicator()
                }
            } else {
                NoInternetScreen(viewModel::checkInternetConnection)
            }
        }
    }
    if (state.error != null) {
        AppBanner(msg = state.error!!) {
            viewModel.resetErrorState()
        }
    }

    if (state.showDeleteSpaceConfirmation) {
        AppAlertDialog(
            title = stringResource(id = R.string.space_settings_btn_delete_space),
            subTitle = stringResource(id = R.string.space_settings_delete_confrim_message),
            confirmBtnText = stringResource(id = R.string.space_settings_delete_confrim_btn),
            dismissBtnText = stringResource(id = R.string.common_btn_cancel),
            isConfirmDestructive = true,
            onDismissClick = {
                viewModel.showDeleteSpaceConfirmation(false)
            },
            onConfirmClick = {
                viewModel.deleteSpace()
            }
        )
    }

    if (state.showChangeAdminDialog) {
        AppAlertDialog(
            title = stringResource(R.string.space_setting_change_admin_title),
            subTitle = stringResource(R.string.space_setting_change_admin_description),
            confirmBtnText = stringResource(R.string.change_admin_button),
            dismissBtnText = stringResource(id = R.string.common_btn_cancel),
            isConfirmDestructive = true,
            onConfirmClick = { viewModel.onChangeAdminClicked() },
            onDismissClick = { viewModel.showChangeAdminDialog(false) }
        )
    }

    if (state.showLeaveSpaceConfirmation) {
        AppAlertDialog(
            title = stringResource(id = R.string.space_settings_btn_leave_space),
            subTitle = stringResource(id = R.string.space_settings_leave_confrim_message),
            confirmBtnText = stringResource(id = R.string.space_settings_leave_confrim_btn),
            dismissBtnText = stringResource(id = R.string.common_btn_cancel),
            isConfirmDestructive = true,
            onDismissClick = {
                viewModel.showLeaveSpaceConfirmation(false)
            },
            onConfirmClick = {
                viewModel.leaveSpace()
            }
        )
    }

    if (state.showRemoveMemberConfirmation) {
        AppAlertDialog(
            title = stringResource(id = R.string.remove_member_title),
            subTitle = stringResource(id = R.string.remove_member_confirmation_message),
            confirmBtnText = stringResource(id = R.string.remove_member_confirm_btn),
            dismissBtnText = stringResource(id = R.string.common_btn_cancel),
            isConfirmDestructive = true,
            onDismissClick = {
                viewModel.showRemoveMemberConfirmationWithId(false, "")
            },
            onConfirmClick = {
                state.memberToRemove?.let { memberId ->
                    viewModel.removeMember(memberId)
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpaceProfileToolbar() {
    val viewModel = hiltViewModel<SpaceProfileViewModel>()
    val state by viewModel.state.collectAsState()
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(containerColor = AppTheme.colorScheme.surface),
        title = {
            Text(
                text = stringResource(
                    id = R.string.setting_space_settings,
                    state.spaceInfo?.space?.name ?: ""
                ),
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
            if (state.isAdmin && state.spaceMemberCount > 1) {
                IconButton(
                    onClick = { viewModel.onAdminMenuExpanded(true) }
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = ""
                    )
                }

                DropdownMenu(
                    expanded = state.isMenuExpanded,
                    onDismissRequest = { viewModel.onAdminMenuExpanded(false) }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(id = R.string.space_setting_change_admin_title)) },
                        onClick = {
                            viewModel.onAdminMenuExpanded(false)
                            viewModel.navigateToChangeAdminScreen(state.spaceInfo)
                        }
                    )
                }
            }
        }
    )
}

@Composable
private fun SpaceProfileContent() {
    val viewModel = hiltViewModel<SpaceProfileViewModel>()
    val state by viewModel.state.collectAsState()
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    val focusManager = LocalFocusManager.current
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val outlineColor =
        if (isFocused) AppTheme.colorScheme.primary else AppTheme.colorScheme.outline

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(bottom = 80.dp)
        ) {
            Text(
                text = stringResource(id = R.string.space_setting_hint_space_name),
                color = if (isFocused) AppTheme.colorScheme.primary else AppTheme.colorScheme.textDisabled,
                style = AppTheme.appTypography.caption,
                modifier = Modifier.padding(start = 16.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                BasicTextField(
                    value = state.spaceName ?: "",
                    onValueChange = { viewModel.onNameChanged(it.trimStart()) },
                    enabled = state.isAdmin,
                    maxLines = 1,
                    interactionSource = interactionSource,
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = 8.dp),
                    singleLine = true,
                    textStyle = AppTheme.appTypography.subTitle2.copy(color = AppTheme.colorScheme.textPrimary),
                    keyboardActions = KeyboardActions(onDone = {
                        focusManager.clearFocus()
                    }),
                    cursorBrush = SolidColor(AppTheme.colorScheme.primary)
                )
                if (state.allowSave) {
                    if (state.isNameChanging) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    } else {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "",
                            tint = outlineColor,
                            modifier = Modifier
                                .padding(horizontal = 8.dp)
                                .clickable {
                                    ripple(true)
                                    viewModel.saveSpace()
                                    focusManager.clearFocus()
                                }
                        )
                    }
                }
            }

            HorizontalDivider(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                color = outlineColor
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.space_invite_code_title),
                style = AppTheme.appTypography.body2,
                color = AppTheme.colorScheme.textDisabled,
                modifier = Modifier.padding(start = 16.dp)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Text(
                    text = state.inviteCode,
                    modifier = Modifier.weight(1f),
                    style = AppTheme.appTypography.header4
                )

                if (state.isAdmin) {
                    if (state.isCodeLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        IconButton(onClick = { viewModel.regenerateInviteCode() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "")
                        }
                    }
                }

                IconButton(
                    onClick = { shareInvitationCode(context = context, code = state.inviteCode) }
                ) {
                    Icon(Icons.Default.Share, contentDescription = "")
                }
            }
            Text(
                text = stringResource(R.string.space_invite_code_expire_text, state.codeExpireTime),
                style = AppTheme.appTypography.body2,
                color = AppTheme.colorScheme.textDisabled,
                modifier = Modifier.padding(start = 16.dp)
            )

            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                color = AppTheme.colorScheme.outline
            )

            Header(title = stringResource(id = R.string.space_setting_title_your_location))

            state.spaceInfo?.members?.firstOrNull { it.user.id == state.currentUserId }
                ?.let { user ->
                    UserItem(
                        userInfo = user,
                        isChecked = state.locationEnabled,
                        enable = true,
                        isAdmin = state.isAdmin,
                        currentUser = state.currentUserId!!,
                        isAdminUser = state.spaceInfo?.space?.admin_id == user.user.id,
                        onCheckedChange = { isChecked ->
                            viewModel.onLocationEnabledChanged(isChecked)
                        },
                        onMemberRemove = {
                            viewModel.showRemoveMemberConfirmationWithId(true, "")
                        }
                    )
                }

            HorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                color = AppTheme.colorScheme.outline
            )

            Header(title = stringResource(id = R.string.space_setting_title_members_location_status))

            val others =
                state.spaceInfo?.members?.filter { it.user.id != state.currentUserId }
                    ?: emptyList()

            if (others.isNotEmpty()) {
                others.forEach { user ->
                    UserItem(
                        userInfo = user,
                        isChecked = user.isLocationEnable,
                        enable = state.isAdmin,
                        isAdmin = state.isAdmin,
                        currentUser = state.currentUserId!!,
                        isAdminUser = state.spaceInfo?.space?.admin_id == user.user.id,
                        onCheckedChange = { isChecked ->
                            viewModel.updateMemberLocation(user.user.id, isChecked)
                        },
                        onMemberRemove = {
                            viewModel.showRemoveMemberConfirmationWithId(true, user.user.id)
                        }
                    )
                }
            } else {
                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = stringResource(
                        id = R.string.space_setting_no_members,
                        state.spaceInfo?.space?.name ?: ""
                    ),
                    style = AppTheme.appTypography.body1,
                    color = AppTheme.colorScheme.textPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                )
            }
        }
        if (state.spaceInfo != null && state.spaceMemberCount == 1) {
            FooterButton(
                title = stringResource(id = R.string.space_settings_btn_delete_space),
                onClick = {
                    viewModel.showDeleteSpaceConfirmation(true)
                },
                showLoader = state.deletingSpace,
                icon = Icons.Default.Delete
            )
        }

        if (state.spaceInfo != null && state.spaceMemberCount > 1) {
            FooterButton(
                title = stringResource(id = R.string.space_settings_btn_leave_space),
                onClick = {
                    if (state.isAdmin) {
                        viewModel.showChangeAdminDialog(true)
                    } else {
                        viewModel.showLeaveSpaceConfirmation(true)
                    }
                },
                showLoader = state.leavingSpace,
                icon = Icons.AutoMirrored.Filled.ExitToApp
            )
        }
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
private fun BoxScope.FooterButton(
    title: String,
    icon: ImageVector,
    showLoader: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        AppTheme.colorScheme.surface.copy(alpha = 0.1f),
                        AppTheme.colorScheme.surface.copy(alpha = 0.9f),
                        AppTheme.colorScheme.surface,
                        AppTheme.colorScheme.surface
                    )
                )
            )
            .padding(bottom = 16.dp, top = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        PrimaryTextButton(
            label = title,
            onClick = onClick,
            containerColor = AppTheme.colorScheme.containerLow,
            contentColor = AppTheme.colorScheme.alertColor,
            showLoader = showLoader,
            icon = {
                Icon(
                    imageVector = icon,
                    contentDescription = "",
                    modifier = Modifier.size(20.dp)
                )
            }
        )
    }
}

@Composable
private fun UserItem(
    userInfo: UserInfo,
    isChecked: Boolean,
    enable: Boolean,
    isAdmin: Boolean,
    currentUser: String,
    isAdminUser: Boolean = false,
    onCheckedChange: (Boolean) -> Unit,
    onMemberRemove: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        UserProfile(modifier = Modifier.size(40.dp), user = userInfo.user)
        Spacer(modifier = Modifier.width(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = userInfo.user.fullName,
                style = AppTheme.appTypography.subTitle2,
                color = AppTheme.colorScheme.textPrimary,
                textAlign = TextAlign.Start
            )

            if (isAdminUser) {
                Text(
                    text = stringResource(R.string.space_profile_screen_admin_text),
                    style = AppTheme.appTypography.subTitle3,
                    color = AppTheme.colorScheme.textDisabled,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }

        Switch(
            checked = isChecked,
            enabled = enable,
            colors = SwitchDefaults.colors(
                checkedThumbColor = AppTheme.colorScheme.textInversePrimary,
                uncheckedThumbColor = AppTheme.colorScheme.onPrimary,
                uncheckedTrackColor = AppTheme.colorScheme.containerHigh,
                disabledCheckedTrackColor = AppTheme.colorScheme.containerHigh
            ),
            onCheckedChange = { isChecked ->
                onCheckedChange(isChecked)
            },
            modifier = Modifier.padding(end = 8.dp)
        )

        if (isAdmin && userInfo.user.id != currentUser) {
            Icon(
                painter = painterResource(R.drawable.ic_remove),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier
                    .padding(8.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = false)
                    ) {
                        onMemberRemove()
                    }
            )
        }
    }
}

@Composable
private fun Header(title: String) {
    Text(
        text = title,
        style = AppTheme.appTypography.subTitle1,
        color = AppTheme.colorScheme.textDisabled,
        textAlign = TextAlign.Start,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, bottom = 8.dp)
    )
}
