package com.canopas.catchme.ui.component

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.canopas.catchme.R
import com.canopas.catchme.ui.theme.AppTheme
import com.canopas.catchme.ui.theme.AppTheme.colorScheme
import com.canopas.catchme.ui.theme.KalamBoldFont

@Composable
fun AppLogo(colorTint: Color = colorScheme.primary) {
    Icon(
        painter = painterResource(id = R.drawable.app_logo_white_outlined),
        contentDescription = "app_log",
        modifier = Modifier.size(50.dp),
        tint = colorTint
    )

    Text(
        text = "CatchMe",
        textAlign = TextAlign.Center,
        style = AppTheme.appTypography.logo
            .copy(
                color = colorTint,
                fontFamily = KalamBoldFont,
            ),
    )
}