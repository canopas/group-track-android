package com.canopas.yourspace.ui.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.canopas.yourspace.ui.theme.AppTheme

@Composable
fun AppAlertDialog(
    title: String? = null,
    subTitle: String,
    confirmBtnText: String? = null,
    dismissBtnText: String? = null,
    onConfirmClick: (() -> Unit)? = null,
    onDismissClick: (() -> Unit)? = null,
    isConfirmDestructive: Boolean = false,
    properties: DialogProperties? = null
) {
    AlertDialog(
        onDismissRequest = {},
        properties = properties ?: DialogProperties(),
        containerColor = AppTheme.colorScheme.containerNormalOnSurface,
        tonalElevation = 20.dp,
        title = {
            if (title != null) {
                Text(
                    text = title,
                    style = AppTheme.appTypography.header3
                )
            }
        },
        text = {
            Text(
                text = subTitle,
                style = AppTheme.appTypography.body1
            )
        },
        confirmButton = {
            if (confirmBtnText != null && onConfirmClick != null) {
                TextButton(
                    onClick = (onConfirmClick)
                ) {
                    Text(
                        text = confirmBtnText,
                        style = AppTheme.appTypography.subTitle2,
                        color = if (isConfirmDestructive) AppTheme.colorScheme.alertColor else AppTheme.colorScheme.primary
                    )
                }
            }
        },
        dismissButton = {
            if (dismissBtnText != null && onDismissClick != null) {
                TextButton(onClick = (onDismissClick)) {
                    Text(
                        text = dismissBtnText,
                        style = AppTheme.appTypography.subTitle2,
                        color = if (isConfirmDestructive) AppTheme.colorScheme.textSecondary else AppTheme.colorScheme.primary
                    )
                }
            }
        }
    )
}
