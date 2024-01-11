package com.canopas.catchme.ui.component

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.canopas.catchme.ui.theme.AppTheme

@Composable
fun AppProgressIndicator(color: Color = AppTheme.colorScheme.primary) {
    CircularProgressIndicator(
        color = color,
        modifier = Modifier
            .height(20.dp)
            .width(20.dp)
    )
}
