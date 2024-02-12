package com.canopas.yourspace.ui.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.unit.dp

const val FLAG_IGNORE_GLOBAL_SETTINGS = 2
const val FEEDBACK_CONSTANT = 3

interface MultipleEventsCutter {
    fun processEvent(event: () -> Unit)

    companion object
}

internal fun MultipleEventsCutter.Companion.get(): MultipleEventsCutter =
    MultipleEventsCutterImpl()

class MultipleEventsCutterImpl : MultipleEventsCutter {
    private val now: Long
        get() = System.currentTimeMillis()

    private var lastEventTimeMs: Long = 0

    override fun processEvent(event: () -> Unit) {
        if (now - lastEventTimeMs >= 300L) {
            event.invoke()
        }
        lastEventTimeMs = now
    }
}

fun Modifier.motionClickEvent(
    onClick: () -> Unit
) = composed(
    inspectorInfo = debugInspectorInfo {
        name = "motionClickEvent"
    }
) {
    val multipleEventsCutter = remember { MultipleEventsCutter.get() }
    val interactionSource = remember { MutableInteractionSource() }
    var selected by remember { mutableStateOf(false) }
    val scale = animateFloatAsState(if (selected) 0.96f else 1f)
    this
        .scale(scale.value)
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = {
                multipleEventsCutter.processEvent { onClick() }
            }
        )
        .pointerInput(selected) {
            awaitPointerEventScope {
                selected = if (selected) {
                    waitForUpOrCancellation()
                    false
                } else {
                    awaitFirstDown(false)
                    true
                }
            }
        }
}

@OptIn(ExperimentalFoundationApi::class)
fun Modifier.combinedMotionClickEvent(onClick: () -> Unit, onLongClick: () -> Unit) = composed(
    inspectorInfo = debugInspectorInfo {
        name = "combinedMotionClickEvent"
    }
) {
    val multipleEventsCutter = remember { MultipleEventsCutter.get() }
    val view = LocalView.current
    val interactionSource = remember { MutableInteractionSource() }
    var selected by remember { mutableStateOf(false) }
    val scale = animateFloatAsState(if (selected) 0.96f else 1f)
    this
        .scale(scale.value)
        .combinedClickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = {
                multipleEventsCutter.processEvent { onClick() }
            },
            onLongClick = {
                view.performHapticFeedback(FEEDBACK_CONSTANT, FLAG_IGNORE_GLOBAL_SETTINGS)
                onLongClick()
            }
        )
        .pointerInput(selected) {
            awaitPointerEventScope {
                selected = if (selected) {
                    waitForUpOrCancellation()
                    false
                } else {
                    awaitFirstDown(false)
                    true
                }
            }
        }
}

// user this event modifier only for icon button
fun Modifier.rippleClickEvent(
    onClick: () -> Unit
) = composed {
    this.clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = rememberRipple(bounded = false, radius = 20.dp),
        onClick = { onClick() }
    )
}
