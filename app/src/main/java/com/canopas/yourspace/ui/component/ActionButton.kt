package com.canopas.yourspace.ui.component

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.canopas.yourspace.ui.theme.AppTheme

@Composable
fun ActionButton(
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int,
    iconSize: Dp = 24.dp,
    visible: Boolean = true,
    showLoader: Boolean = false,
    enabled: Boolean = true,
    containerColor: Color = AppTheme.colorScheme.containerNormalOnSurface,
    contentColor: Color = AppTheme.colorScheme.textPrimary,
    shape: RoundedCornerShape = RoundedCornerShape(30.dp),
    onClick: () -> Unit
) {
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        tween(durationMillis = 100),
        label = ""
    )

    IconButton(
        modifier = modifier
            .clip(shape)
            .wrapContentSize()
            .graphicsLayer(alpha = alpha),
        enabled = enabled,
        onClick = { if (visible) onClick() },
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
