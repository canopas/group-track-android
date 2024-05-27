package com.canopas.yourspace.ui.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.canopas.yourspace.ui.theme.AppTheme

@Composable
fun ActionIconButton(
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int,
    iconSize: Dp = 24.dp,
    showLoader: Boolean = false,
    enabled: Boolean = true,
    containerColor: Color = AppTheme.colorScheme.containerNormalOnSurface,
    contentColor: Color = AppTheme.colorScheme.textPrimary,
    shape: RoundedCornerShape = RoundedCornerShape(30.dp),
    onClick: () -> Unit
) {
    IconButton(
        modifier = modifier
            .clip(shape)
            .wrapContentSize(),
        enabled = enabled,
        onClick = { if (enabled) onClick() },
        colors = IconButtonDefaults.iconButtonColors(
            containerColor = containerColor,
            contentColor = contentColor
        )
    ) {
        if (showLoader) {
            AppProgressIndicator(strokeWidth = 2.dp)
        } else {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = "",
                modifier = Modifier.size(iconSize)
            )
        }
    }
}
