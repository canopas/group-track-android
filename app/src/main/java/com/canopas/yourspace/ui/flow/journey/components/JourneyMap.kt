package com.canopas.yourspace.ui.flow.journey.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import com.canopas.yourspace.R
import com.canopas.yourspace.data.models.location.LocationJourney
import com.canopas.yourspace.data.models.location.toRoute
import com.canopas.yourspace.ui.theme.AppTheme
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMapOptions
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.Dash
import com.google.android.gms.maps.model.Gap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.MarkerComposable
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState

@Composable
fun JourneyMap(
    modifier: Modifier,
    location: LocationJourney?,
    gestureEnable: Boolean,
    fromMarkerContent: @Composable () -> Unit,
    toMarkerContent: @Composable () -> Unit,
    polyLineWidth: Float = 5f,
    anchor: Offset = Offset(0.5f, 1.0f),
    shouldAnimate: Boolean = false,
    onMapTap: (() -> Unit) = { }
) {
    val fromLatLang = LatLng(location?.from_latitude ?: 0.0, location?.from_longitude ?: 0.0)
    val toLatLang = LatLng(location?.to_latitude ?: 0.0, location?.to_longitude ?: 0.0)
    var isMapLoaded by remember {
        mutableStateOf(false)
    }
    val animatedProgress = remember { Animatable(0f) }

    val isDarkMode = isSystemInDarkTheme()
    val context = LocalContext.current
    val mapProperties = remember(isDarkMode) {
        MapProperties(
            mapStyleOptions = if (isDarkMode) {
                MapStyleOptions.loadRawResourceStyle(context, R.raw.map_theme_night)
            } else {
                null
            }
        )
    }

    // Calculate the route points
    val routePoints = location?.toRoute() ?: listOf()
    val animatedPoints = remember(routePoints, animatedProgress.value) {
        routePoints.take((animatedProgress.value * routePoints.size).toInt())
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(fromLatLang, 15f)
    }

    LaunchedEffect(key1 = location, isMapLoaded) {
        if (isMapLoaded) {
            try {
                val boundsBuilder = LatLngBounds.builder()
                    .apply {
                        include(fromLatLang)
                        include(toLatLang)
                        routePoints.forEach { latLng ->
                            this.include(latLng)
                        }
                    }.build()
                val update = CameraUpdateFactory.newLatLngBounds(boundsBuilder, 50)
                cameraPositionState.move(update)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        animatedProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 4000) // Set your desired duration
        )
    }

    GoogleMap(
        mergeDescendants = false,
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        properties = mapProperties,
        onMapClick = { onMapTap() },
        googleMapOptionsFactory = {
            GoogleMapOptions()
        },
        uiSettings = MapUiSettings(
            zoomControlsEnabled = false,
            tiltGesturesEnabled = false,
            myLocationButtonEnabled = false,
            compassEnabled = false,
            mapToolbarEnabled = false,
            rotationGesturesEnabled = gestureEnable,
            scrollGesturesEnabled = gestureEnable,
            zoomGesturesEnabled = gestureEnable,
            scrollGesturesEnabledDuringRotateOrZoom = gestureEnable,
            indoorLevelPickerEnabled = gestureEnable
        ),
        onMapLoaded = {
            isMapLoaded = true
        }
    ) {
        location?.let {
            LocationMarker(fromLatLang, anchor, fromMarkerContent)

            LocationMarker(toLatLang, anchor, toMarkerContent)

            Polyline(
                points = if (shouldAnimate) animatedPoints else routePoints,
                color = AppTheme.colorScheme.primary,
                width = polyLineWidth,
                pattern = listOf(Gap(8F), Dash(12F))
            )
        }
    }
}

@Composable
private fun LocationMarker(
    latLang: LatLng,
    anchor: Offset,
    markerContent: @Composable () -> Unit
) {
    MarkerComposable(
        state = rememberMarkerState(position = latLang),
        content = markerContent,
        anchor = anchor
    )
}
