package com.canopas.yourspace.ui.flow.journey.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import com.canopas.yourspace.R
import com.canopas.yourspace.data.models.location.LocationJourney
import com.canopas.yourspace.data.models.location.toRoute
import com.canopas.yourspace.ui.theme.AppTheme
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMapOptions
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
import kotlinx.coroutines.launch

@Composable
fun JourneyMap(
    modifier: Modifier,
    location: LocationJourney?,
    gestureEnable: Boolean,
    fromMarkerContent: @Composable () -> Unit,
    toMarkerContent: @Composable () -> Unit,
    polyLineWidth: Float = 5f,
    anchor: Offset = Offset(0.5f, 1.0f),
    onMapTap: (() -> Unit) = { }
) {
    val fromLatLang = LatLng(location?.from_latitude ?: 0.0, location?.from_longitude ?: 0.0)
    val toLatLang = LatLng(location?.to_latitude ?: 0.0, location?.to_longitude ?: 0.0)

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

    val cameraPositionState = rememberCameraPositionState()
    val scope = rememberCoroutineScope()

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
            scope.launch {
                if (location == null) return@launch
                try {
                    val boundsBuilder = LatLngBounds.builder()
                        .apply {
                            include(fromLatLang)
                            location.toRoute().forEach { latLng ->
                                include(latLng)
                            }
                            include(toLatLang)
                        }.build()
                    val update = CameraUpdateFactory.newLatLngBounds(boundsBuilder, 50)
                    cameraPositionState.move(update)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    ) {
        location?.let {
            LocationMarker(fromLatLang, anchor, fromMarkerContent)

            LocationMarker(toLatLang, anchor, toMarkerContent)

            Polyline(
                points = location.toRoute(),
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
