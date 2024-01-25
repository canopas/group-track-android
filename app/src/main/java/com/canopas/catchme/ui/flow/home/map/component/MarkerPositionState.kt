package com.canopas.catchme.ui.flow.home.map.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.MarkerState

@Composable
fun rememberMarkerState(
    position: LatLng = LatLng(0.0, 0.0)
): MarkerState {
    var state by remember {
        mutableStateOf(MarkerState(position))
    }

    val animatable = remember { Animatable(0f) }

    LaunchedEffect(key1 = position) {
        animatable.snapTo(0f)
        animatable.animateTo(1f, animationSpec = tween(1000))
        state = MarkerState(
            position = LatLng(
                position.latitude,
                position.longitude
            )
        )
    }

    val animatedLatLong = animatedLatLng(
        animatable.value,
        state.position,
        LatLng(position.latitude, position.longitude)
    )

    return MarkerState(
        position = LatLng(
            animatedLatLong.latitude,
            animatedLatLong.longitude
        )
    )
}

private fun animatedLatLng(fraction: Float, a: LatLng, b: LatLng): LatLng {
    val lat = (b.latitude - a.latitude) * fraction + a.latitude
    var lngDelta = b.longitude - a.longitude

    // Take the shortest path across the 180th meridian.
    if (Math.abs(lngDelta) > 180) {
        lngDelta -= Math.signum(lngDelta) * 360
    }
    val lng = lngDelta * fraction + a.longitude
    return LatLng(lat, lng)
}
