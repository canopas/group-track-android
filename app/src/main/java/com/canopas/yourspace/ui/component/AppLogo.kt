package com.canopas.yourspace.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.canopas.yourspace.R
import com.canopas.yourspace.ui.theme.AppTheme
import com.canopas.yourspace.ui.theme.AppTheme.colorScheme

@Composable
fun AppLogo(colorTint: Color = colorScheme.primary) {
    Image(
        painter = painterResource(id = R.drawable.app_logo),
        contentDescription = "app_log",
        modifier = Modifier.size(50.dp)
    )

    Spacer(modifier = Modifier.size(16.dp))

    Text(
        text = stringResource(id = R.string.app_name),
        textAlign = TextAlign.Center,
        style = AppTheme.appTypography.logo
            .copy(
                color = colorTint
            )
    )
}
