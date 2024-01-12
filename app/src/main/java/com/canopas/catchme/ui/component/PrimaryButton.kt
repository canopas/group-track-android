package com.canopas.catchme.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import com.canopas.catchme.ui.theme.AppTheme

@Composable
fun PrimaryButton(
    modifier: Modifier = Modifier,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth(fraction = 0.9f),
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(
            containerColor = AppTheme.colorScheme.primary
        ), enabled = enabled
    ) {
        Text(
            text = label,
            style = AppTheme.appTypography.subTitle2.copy(color = AppTheme.colorScheme.onPrimary),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 6.dp)
        )
    }
}

@Composable
fun PrimaryTextButton(
    modifier: Modifier = Modifier,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    TextButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth(fraction = 0.9f),
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(
            containerColor = AppTheme.colorScheme.surface,
            contentColor = AppTheme.colorScheme.primary
        ), enabled = enabled
    ) {
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
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth(fraction = 0.9f),
        border = BorderStroke(color = outlineColor, width = 1.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
        ), enabled = enabled
    ) {
        Text(
            text = label,
            style = AppTheme.appTypography.subTitle2.copy(color = contentColor),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 6.dp)
        )
    }
}