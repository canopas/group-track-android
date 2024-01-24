package com.canopas.catchme.ui.flow.home.map.component

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.CornerPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.canopas.catchme.data.models.user.ApiUser
import com.canopas.catchme.data.models.location.ApiLocation
import com.canopas.catchme.ui.theme.AppTheme.colorScheme
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberMarkerState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import kotlin.math.roundToInt

@Composable
fun MapMarker(
    user: ApiUser,
    location: ApiLocation,
    onClick: () -> Unit
) {

    val iconState = rememberMarkerIconState(user)

    var state by remember {
        mutableStateOf(
            MarkerState(
                position = LatLng(
                    location.latitude,
                    location.longitude
                )
            )
        )
    }

    val animatable = remember { Animatable(0f) }

    LaunchedEffect(key1 = location, user) {
        animatable.snapTo(0f)
        animatable.animateTo(1f, animationSpec = tween(1000))
        Timber.d("XXX update state")
        state = MarkerState(
            position = LatLng(
                location.latitude,
                location.longitude
            )
        )
    }

    // Timber.d("XXX MapMarker: ${user.fullName} }")

    val interpolator = remember {
        LatLngInterpolator.Linear()
    }

    val animatedLatLong = interpolator.interpolate(
        animatable.value,
        state.position,
        LatLng(location.latitude, location.longitude)
    )

    Marker(
        state = MarkerState(
            position = LatLng(
                animatedLatLong.latitude,
                animatedLatLong.longitude
            )
        ),
        title = user.fullName,
        icon = iconState,
        onClick = {
            onClick()
            false
        }
    )
}



interface LatLngInterpolator {
    fun interpolate(fraction: Float, a: LatLng, b: LatLng): LatLng

    class Linear : LatLngInterpolator {
        override fun interpolate(fraction: Float, a: LatLng, b: LatLng): LatLng {
            val lat = (b.latitude - a.latitude) * fraction + a.latitude
            var lngDelta = b.longitude - a.longitude

            // Take the shortest path across the 180th meridian.
            if (Math.abs(lngDelta) > 180) {
                lngDelta -= Math.signum(lngDelta) * 360
            }
            val lng = lngDelta * fraction + a.longitude
            return LatLng(lat, lng)
        }
    }
}
