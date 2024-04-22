package com.canopas.yourspace.ui.flow.geofence.addplace.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.canopas.yourspace.R
import com.canopas.yourspace.ui.component.PrimaryButton
import com.canopas.yourspace.ui.flow.home.map.DEFAULT_CAMERA_ZOOM
import com.canopas.yourspace.ui.theme.AppTheme
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaceAddedPopup(
    lat: Double,
    lng: Double,
    placeName: String,
    onDismiss: (() -> Unit)
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            modifier = Modifier,
            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 10.dp),
            shape = RoundedCornerShape(10.dp)
        ) {
            MapView(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.77f),
                lat,
                lng
            )
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(id = R.string.place_added_popup_title, placeName),
                style = AppTheme.appTypography.header1,
                color = AppTheme.colorScheme.textPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(horizontal = 10.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = stringResource(id = R.string.place_added_popup_description),
                style = AppTheme.appTypography.body1,
                color = AppTheme.colorScheme.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(horizontal = 10.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))
            PrimaryButton(
                label = stringResource(id = R.string.common_btn_got_it),
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun MapView(
    modifier: Modifier,
    lat: Double,
    long: Double
) {
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(lat, long), DEFAULT_CAMERA_ZOOM)
    }

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
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = mapProperties,
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false,
                tiltGesturesEnabled = false,
                myLocationButtonEnabled = false,
                compassEnabled = false,
                mapToolbarEnabled = false,
                scrollGesturesEnabled = false,
                rotationGesturesEnabled = false
            )
        ) {
        }

        PlaceMarker()
    }
}

@Composable
private fun PlaceMarker() {
    Box(
        modifier = Modifier
            .size(70.dp)
            .background(
                AppTheme.colorScheme.primary.copy(alpha = 0.6f),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        SmallFloatingActionButton(
            modifier = Modifier.align(Alignment.Center),
            onClick = { },
            containerColor = AppTheme.colorScheme.surface,
            contentColor = AppTheme.colorScheme.primary,
            shape = CircleShape
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_tab_places_filled),
                contentDescription = "",
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
