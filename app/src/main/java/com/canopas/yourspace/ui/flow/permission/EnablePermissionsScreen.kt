package com.canopas.yourspace.ui.flow.permission

import android.Manifest
import android.app.Activity
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.canopas.yourspace.R
import com.canopas.yourspace.data.utils.openAppSettings
import com.canopas.yourspace.ui.component.AppBanner
import com.canopas.yourspace.ui.component.ShowBackgroundLocationRequestDialog
import com.canopas.yourspace.ui.theme.AppTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale

@Composable
fun EnablePermissionsScreen() {
    Scaffold(topBar = { EnablePermissionsAppBar() }) {
        EnablePermissionsContent(Modifier.padding(it))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EnablePermissionsAppBar() {
    val viewModel = hiltViewModel<EnablePermissionViewModel>()

    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = AppTheme.colorScheme.surface
        ),
        title = {
            Text(
                text = stringResource(id = R.string.enable_permission_title),
                style = AppTheme.appTypography.subTitle1
            )
        },
        navigationIcon = {
            IconButton(
                onClick = {
                    viewModel.popBack()
                },
                modifier = Modifier
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_nav_back_arrow_icon),
                    contentDescription = null,
                    tint = AppTheme.colorScheme.textSecondary
                )
            }
        }
    )
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun EnablePermissionsContent(modifier: Modifier) {
    val viewModel = hiltViewModel<EnablePermissionViewModel>()
    val context = LocalContext.current
    val locationPermissionStates = rememberMultiplePermissionsState(
        listOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)
    )

    val bgLocationPermissionStates =
        rememberPermissionState(Manifest.permission.ACCESS_BACKGROUND_LOCATION)

    val notificationPermissionStates = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
    } else {
        null
    }

    var showNotificationRational by remember {
        mutableStateOf(false)
    }

    var showRequiredLocationAccessBanner by remember {
        mutableStateOf(false)
    }

    var showBgLocationRational by remember {
        mutableStateOf(false)
    }

    var shouldAskedForLocation by remember {
        mutableStateOf(false)
    }

    var showLocationRational by remember {
        mutableStateOf(false)
    }

    val scrollState = rememberScrollState()
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .background(AppTheme.colorScheme.surface)
            .padding(bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.enable_permission_subtitle),
            style = AppTheme.appTypography.body1.copy(color = AppTheme.colorScheme.textDisabled),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 14.dp)
        )

        Spacer(modifier = Modifier.height(28.dp))

        PermissionContent(
            title = stringResource(R.string.enable_permission_location_access_title),
            description = stringResource(R.string.enable_permission_location_access_desc),
            isGranted = locationPermissionStates.allPermissionsGranted,
            onClick = {
                if (!locationPermissionStates.allPermissionsGranted &&
                    !locationPermissionStates.shouldShowRationale && shouldAskedForLocation
                ) {
                    showLocationRational = true
                } else if (!locationPermissionStates.allPermissionsGranted) {
                    shouldAskedForLocation = true
                    locationPermissionStates.launchMultiplePermissionRequest()
                }
            }
        )

        PermissionContent(
            title = stringResource(R.string.enable_permission_background_location_access_title),
            description = stringResource(R.string.enable_permission_background_location_access_desc),
            isGranted = bgLocationPermissionStates.status == PermissionStatus.Granted,
            onClick = {
                if (locationPermissionStates.allPermissionsGranted) {
                    if (bgLocationPermissionStates.status != PermissionStatus.Granted) {
                        bgLocationPermissionStates.launchPermissionRequest()
                    } else if (bgLocationPermissionStates.status is PermissionStatus.Denied &&
                        !bgLocationPermissionStates.status.shouldShowRationale
                    ) {
                        showBgLocationRational = true
                    }
                } else {
                    showRequiredLocationAccessBanner = true
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
        Text(
            text = stringResource(R.string.enable_permission_footer),
            style = AppTheme.appTypography.caption.copy(color = AppTheme.colorScheme.textDisabled),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        )
    }

    if (showNotificationRational) {
        NotificationPermissionRationaleDialog(
            onSkip = {
                showNotificationRational = false
                viewModel.popBack()
            },
            onContinue = {
                showNotificationRational = false
                if (notificationPermissionStates?.status is PermissionStatus.Denied &&
                    !notificationPermissionStates.status.shouldShowRationale
                ) {
                    (context as Activity).openAppSettings()
                } else {
                    notificationPermissionStates?.launchPermissionRequest()
                }
            }
        )
    }

    if (showRequiredLocationAccessBanner) {
        AppBanner(msg = stringResource(id = R.string.enable_permission_background_location_access_required_location_permisison_msg)) {
            showRequiredLocationAccessBanner = false
        }
    }

    if (showBgLocationRational) {
        ShowBackgroundLocationRequestDialog(
            locationPermissionStates = bgLocationPermissionStates,
            onDismiss = {
                showBgLocationRational = false
            },
            goToSettings = {
                if (bgLocationPermissionStates.status.shouldShowRationale) {
                    (context as Activity).openAppSettings()
                } else {
                    bgLocationPermissionStates.launchPermissionRequest()
                }
                showBgLocationRational = false
            }
        )
    }

    if (showLocationRational) {
        ShowBackgroundLocationRequestDialog(
            locationPermissionStates = locationPermissionStates.permissions.first(),
            onDismiss = {
                showLocationRational = false
            },
            goToSettings = {
                (context as Activity).openAppSettings()
                showLocationRational = false
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
    Row(
        modifier = Modifier
            .clickable {
                ripple()
                onClick()
            }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .size(24.dp)
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
                    tint = AppTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Text(
                text = title,
                style = AppTheme.appTypography.subTitle2.copy(color = AppTheme.colorScheme.textPrimary)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = description,
                style = AppTheme.appTypography.body2.copy(color = AppTheme.colorScheme.textDisabled)
            )
        }
    }
}
