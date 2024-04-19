package com.canopas.yourspace.ui.flow.geofence.addplace.locate

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.canopas.yourspace.R
import com.canopas.yourspace.ui.flow.home.map.DEFAULT_CAMERA_ZOOM
import com.canopas.yourspace.ui.flow.home.map.component.MapControlBtn
import com.canopas.yourspace.ui.flow.home.map.distanceTo
import com.canopas.yourspace.ui.theme.AppTheme
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocateOnMapScreen() {
    val viewModel = hiltViewModel<LocateOnMapViewModel>()
    val state by viewModel.state.collectAsState()

    val userLocation = remember(state.defaultLocation) {
        val location = state.defaultLocation
        LatLng(location?.latitude ?: 0.0, location?.longitude ?: 0.0)
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(userLocation, DEFAULT_CAMERA_ZOOM)
    }

    LaunchedEffect(userLocation) {
        cameraPositionState.animate(
            CameraUpdateFactory.newLatLngZoom(
                userLocation,
                DEFAULT_CAMERA_ZOOM
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppTheme.colorScheme.surface),
                title = {
                    Text(
                        text = stringResource(id = R.string.locate_on_map_title),
                        style = AppTheme.appTypography.header3
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.popBackStack() }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = ""
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            viewModel.onNextClick(
                                cameraPositionState.position.target.latitude,
                                cameraPositionState.position.target.longitude
                            )
                        },
                        enabled = !cameraPositionState.isMoving,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = AppTheme.colorScheme.primary,
                            disabledContentColor = AppTheme.colorScheme.textDisabled
                        )
                    ) {
                        Text(
                            text = stringResource(id = R.string.common_btn_next),
                            style = AppTheme.appTypography.subTitle1
                        )
                    }
                }
            )
        },
        contentColor = AppTheme.colorScheme.textPrimary,
        containerColor = AppTheme.colorScheme.surface
    ) {
        LocateOnMapContent(modifier = Modifier.padding(it), cameraPositionState, userLocation)
    }
}

@Composable
fun LocateOnMapContent(
    modifier: Modifier,
    cameraPositionState: CameraPositionState,
    userLocation: LatLng
) {
    val scope = rememberCoroutineScope()
    val relocate by remember {
        derivedStateOf {
            val distance = cameraPositionState.position.target.distanceTo(userLocation)
            distance > 100
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        MapView(cameraPositionState)

        MapControlBtn(
            modifier = Modifier
                .padding(bottom = 10.dp, end = 10.dp)
                .align(Alignment.BottomEnd),
            icon = R.drawable.ic_relocate,
            show = relocate
        ) {
            scope.launch {
                cameraPositionState.animate(
                    CameraUpdateFactory.newLatLngZoom(
                        userLocation,
                        DEFAULT_CAMERA_ZOOM
                    )
                )
            }
        }
    }
}

@Composable
private fun BoxScope.MapView(
    cameraPositionState: CameraPositionState
) {
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

    GoogleMap(
        cameraPositionState = cameraPositionState,
        properties = mapProperties,
        uiSettings = MapUiSettings(
            zoomControlsEnabled = false,
            tiltGesturesEnabled = false,
            myLocationButtonEnabled = false,
            compassEnabled = false,
            mapToolbarEnabled = false
        )
    )

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
            modifier = Modifier.size(24.dp)
        )
    }
}
