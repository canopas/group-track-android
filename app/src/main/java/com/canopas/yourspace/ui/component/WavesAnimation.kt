package com.canopas.yourspace.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment.Companion.TopEnd
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.canopas.yourspace.ui.theme.AppTheme
import kotlinx.coroutines.delay

@Composable
fun WavesAnimation(content: @Composable () -> Unit) {
    val waves = listOf(
        remember { Animatable(0f) },
        remember { Animatable(0f) },
        remember { Animatable(0f) },
        remember { Animatable(0f) }
    )

    val animationSpec = infiniteRepeatable<Float>(
        animation = tween(3000, easing = FastOutLinearInEasing),
        repeatMode = RepeatMode.Restart
    )

    waves.forEachIndexed { index, animatable ->
        LaunchedEffect(animatable) {
            delay(index * 1000L)
            animatable.animateTo(
                targetValue = 1f,
                animationSpec = animationSpec
            )
        }
    }

    val dys = waves.map { it.value }

    Box(
        modifier = Modifier.fillMaxSize().offset(y = 100.dp, x = -(20).dp),
        contentAlignment = TopEnd
    ) {
        dys.forEach { dy ->
            Box(
                Modifier
                    .size(50.dp)
                    .align(TopEnd)
                    .graphicsLayer {
                        scaleX = dy * 4 + 1
                        scaleY = dy * 4 + 1
                        alpha = 1 - dy
                    }
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(color = AppTheme.colorScheme.containerLow, shape = CircleShape)
                )
            }
        }

        Box(
            Modifier
                .wrapContentSize()
                .align(TopEnd)
                .background(color = AppTheme.colorScheme.containerLow, shape = CircleShape)
        ) {
            content()
        }
    }
}
