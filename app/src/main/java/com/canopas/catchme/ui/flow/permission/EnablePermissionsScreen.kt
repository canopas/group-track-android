package com.canopas.catchme.ui.flow.permission

import android.Manifest
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.canopas.catchme.R
import com.canopas.catchme.ui.component.PrimaryButton
import com.canopas.catchme.ui.theme.AppTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun EnablePermissionsScreen() {
    val viewModel = hiltViewModel<EnablePermissionViewModel>()
    val locationPermissionStates = rememberMultiplePermissionsState(
        listOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
    )

    val notificationPermissionStates = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
    } else {
        null
    }

    var showNotificationRational by remember {
        mutableStateOf(false)
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(AppTheme.colorScheme.surface)
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))
        Text(
            text = stringResource(R.string.enable_permission_title),
            style = AppTheme.appTypography.header1,

            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp)
        )
        Text(
            text = stringResource(R.string.enable_permission_subtitle),
            style = AppTheme.appTypography.label1.copy(color = AppTheme.colorScheme.textSecondary),

            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp)
                .padding(top = 2.dp)
        )

        Spacer(modifier = Modifier.height(40.dp))

        PermissionContent(
            title = stringResource(R.string.enable_permission_location_access_title),
            description = stringResource(R.string.enable_permission_location_access_desc),
            isGranted = locationPermissionStates.allPermissionsGranted,
            onClick = {
                if (!locationPermissionStates.allPermissionsGranted) {
                    locationPermissionStates.launchMultiplePermissionRequest()
                }
            }
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            PermissionContent(
                title = stringResource(R.string.enable_permission_notification_access_title),
                description = stringResource(R.string.enable_permission_notification_access_desc),
                isGranted = notificationPermissionStates?.status == PermissionStatus.Granted,
                onClick = {
                    if (notificationPermissionStates?.status != PermissionStatus.Granted) {
                        notificationPermissionStates?.launchPermissionRequest()
                    }
                }
            )
        }

        Spacer(modifier = Modifier.weight(1f))
        PrimaryButton(
            label = stringResource(id = R.string.common_btn_continue),
            onClick = {
                if (notificationPermissionStates?.status != null && notificationPermissionStates.status != PermissionStatus.Granted) {
                    showNotificationRational = true
                } else {
                    viewModel.navigationToHome()
                }
            },
            enabled = locationPermissionStates.allPermissionsGranted
        )

        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.enable_permission_footer),
            style = AppTheme.appTypography.label3,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp)
        )
    }

    if (showNotificationRational) {
        NotificationPermissionRationaleDialog(
            onSkip = {
                showNotificationRational = false
                viewModel.navigationToHome()
            },
            onContinue = {
                showNotificationRational = false
                notificationPermissionStates?.launchPermissionRequest()
            }
        )
    }
}

@Composable
fun NotificationPermissionRationaleDialog(
    onSkip: () -> Unit,
    onContinue: () -> Unit
) {
    AlertDialog(onDismissRequest = onSkip, title = {
        Text(text = stringResource(id = R.string.enable_permission_rational_title))
    }, text = {
            Text(text = stringResource(id = R.string.enable_permission_rational_msg))
        }, confirmButton = {
            TextButton(onClick = onContinue) {
                Text(stringResource(id = R.string.enable_permission_btn_enable))
            }
        }, dismissButton = {
            TextButton(onClick = onSkip) {
                Text(stringResource(id = R.string.common_btn_skip))
            }
        }, properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false))
}

@Composable
private fun PermissionContent(
    title: String,
    description: String,
    isGranted: Boolean = false,
    onClick: () -> Unit = {}
) {
    Row(modifier = Modifier.padding(horizontal = 28.dp, vertical = 10.dp)) {
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .size(40.dp)
                .background(if (isGranted) AppTheme.colorScheme.primary else AppTheme.colorScheme.surface)
                .border(1.dp, AppTheme.colorScheme.primary, CircleShape)
                .clickable(enabled = !isGranted) {
                    onClick()
                },
            contentAlignment = Alignment.Center
        ) {
            if (isGranted) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "",
                    tint = AppTheme.colorScheme.onPrimary
                )
            }
        }

        Column {
            Text(
                text = title.uppercase(),
                style = AppTheme.appTypography.body1.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = AppTheme.appTypography.body3.copy(color = AppTheme.colorScheme.textSecondary),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp)
            )
        }
    }
}
