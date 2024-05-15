package com.canopas.yourspace.ui.flow.home.map.member.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.canopas.yourspace.R
import com.canopas.yourspace.data.models.location.LocationJourney
import com.canopas.yourspace.ui.component.gesturesDisabled
import com.canopas.yourspace.ui.theme.AppTheme
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.Cap
import com.google.android.gms.maps.model.Dash
import com.google.android.gms.maps.model.Dot
import com.google.android.gms.maps.model.Gap
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.PatternItem
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerComposable
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState
import com.google.maps.android.ktx.model.cameraPosition

@Composable
fun JourneyMap(
    location: LocationJourney, onTap: () -> Unit
) {
    val fromLatLang = LatLng(location.from_latitude, location.from_longitude)
    val toLatLang = LatLng(location.to_latitude!!, location.to_longitude!!)

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

    LaunchedEffect(key1 = Unit) {
        val boundsBuilder = LatLngBounds.builder()
            .include(fromLatLang)
            .include(toLatLang)
            .build()
        cameraPositionState.move(CameraUpdateFactory.newLatLngBounds(boundsBuilder, 150))
    }

    GoogleMap(
        mergeDescendants = true,
        modifier = Modifier
            .padding(end = 16.dp)
            .height(125.dp)
            .clip(shape = RoundedCornerShape(8.dp))
            .gesturesDisabled(true),
        cameraPositionState = cameraPositionState,
        properties = mapProperties,
        uiSettings = MapUiSettings(
            zoomControlsEnabled = false,
            tiltGesturesEnabled = false,
            myLocationButtonEnabled = false,
            compassEnabled = false,
            mapToolbarEnabled = false
        )
    ) {
        FromLocationMarker(fromLatLang)
        ToLocationMarker(toLatLang)

        Polyline(
            points = listOf(fromLatLang, toLatLang), color = AppTheme.colorScheme.primary,
            width = 5f,
            pattern = listOf(Gap(8F), Dash(12F))
        )
    }

}

@Composable
fun ToLocationMarker(toLatLang: LatLng) {
    MarkerComposable(
        state = rememberMarkerState(position = toLatLang)
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_journey_destination),
            contentDescription = null,
            tint = AppTheme.colorScheme.alertColor,
            modifier = Modifier.size(28.dp)
        )
    }
}

@Composable
fun FromLocationMarker(fromLatLang: LatLng) {

    MarkerComposable(
        state = rememberMarkerState(position = fromLatLang)
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_tab_places_filled),
            contentDescription = null,
            tint = AppTheme.colorScheme.primary,
            modifier = Modifier.size(30.dp)
        )
    }
}
