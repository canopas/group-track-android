package com.canopas.yourspace.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.canopas.yourspace.ui.theme.AppTheme

@Composable
fun PrimaryButton(
    modifier: Modifier = Modifier,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    showLoader: Boolean = false,
    containerColor: Color = AppTheme.colorScheme.primary,
    contentColor: Color = AppTheme.colorScheme.onPrimary
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth(fraction = 0.9f),
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor
        ),
        enabled = enabled
    ) {
        if (showLoader) {
            AppProgressIndicator(color = contentColor)
            Spacer(modifier = Modifier.width(4.dp))
        }

        Text(
            text = label,
            style = AppTheme.appTypography.button.copy(color = contentColor),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 6.dp)
        )
    }
}

@Composable
fun PrimaryTextButton(
    modifier: Modifier = Modifier,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    showLoader: Boolean = false,
    icon: @Composable (() -> Unit)? = null,
    contentColor: Color = AppTheme.colorScheme.primary,
    containerColor: Color = AppTheme.colorScheme.surface
) {
    TextButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth(fraction = 0.9f),
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        enabled = enabled
    ) {
        if (showLoader) {
            AppProgressIndicator(color = contentColor)
            Spacer(modifier = Modifier.width(4.dp))
        }
        if (icon != null && !showLoader) {
            icon()
            Spacer(modifier = Modifier.width(4.dp))
        }

        Text(
            text = label,
            style = AppTheme.appTypography.subTitle2,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 6.dp)
        )
    }
}

@Composable
fun PrimaryOutlinedButton(
    modifier: Modifier = Modifier,
    label: String,
    onClick: () -> Unit,
    containerColor: Color = AppTheme.colorScheme.surface,
    contentColor: Color = AppTheme.colorScheme.primary,
    outlineColor: Color = AppTheme.colorScheme.primary,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth(fraction = 0.9f),
        border = BorderStroke(color = outlineColor, width = 1.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor
        ),
        enabled = enabled
    ) {
        Text(
            text = label,
            style = AppTheme.appTypography.subTitle2.copy(color = contentColor),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 6.dp)
        )
    }
}
