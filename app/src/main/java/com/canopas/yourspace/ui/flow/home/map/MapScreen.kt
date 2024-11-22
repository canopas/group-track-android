package com.canopas.yourspace.ui.flow.home.map

import android.Manifest
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.canopas.yourspace.R
import com.canopas.yourspace.data.models.location.toApiLocation
import com.canopas.yourspace.data.utils.hasAllPermission
import com.canopas.yourspace.data.utils.hasFineLocationPermission
import com.canopas.yourspace.data.utils.hasNotificationPermission
import com.canopas.yourspace.data.utils.isLocationPermissionGranted
import com.canopas.yourspace.domain.utils.isLocationServiceEnabled
import com.canopas.yourspace.domain.utils.openLocationSettings
import com.canopas.yourspace.ui.component.ShowEnableLocationDialog
import com.canopas.yourspace.ui.flow.home.map.component.AddMemberBtn
import com.canopas.yourspace.ui.flow.home.map.component.MapCircles
import com.canopas.yourspace.ui.flow.home.map.component.MapControlBtn
import com.canopas.yourspace.ui.flow.home.map.component.MapMarker
import com.canopas.yourspace.ui.flow.home.map.component.MapStyleBottomSheet
import com.canopas.yourspace.ui.flow.home.map.component.MapUserItem
import com.canopas.yourspace.ui.flow.home.map.component.SelectedUserDetail
import com.canopas.yourspace.ui.theme.AppTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch

const val DEFAULT_CAMERA_ZOOM = 15f
private const val DEFAULT_CAMERA_ZOOM_FOR_SELECTED_USER = 17f

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MapScreen() {
    val viewModel = hiltViewModel<MapViewModel>()
    val state by viewModel.state.collectAsState()
    val scope = rememberCoroutineScope()

    val userLocation = remember(state.defaultCameraPosition) {
        val location = state.defaultCameraPosition
        LatLng(location?.latitude ?: 0.0, location?.longitude ?: 0.0)
    }

    val defaultCameraZoom =
        if (userLocation.latitude == 0.0 && userLocation.longitude == 0.0) 0f else DEFAULT_CAMERA_ZOOM

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(userLocation, defaultCameraZoom)
    }

    val relocate by remember {
        derivedStateOf {
            val distance = cameraPositionState.position.target.distanceTo(userLocation)
            distance > 100
        }
    }

    val permissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    LaunchedEffect(permissionState) {
        snapshotFlow { permissionState.status == PermissionStatus.Granted && state.defaultCameraPosition == null }
            .collect {
                if (it) {
                    viewModel.startLocationTracking()
                }
            }
    }

    LaunchedEffect(userLocation, state.isMapLoaded) {
        if (state.isMapLoaded) {
            val location = state.selectedUser?.location
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(
                    userLocation,
                    if (location != null) DEFAULT_CAMERA_ZOOM_FOR_SELECTED_USER else defaultCameraZoom
                )
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        MapView(cameraPositionState)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomEnd)
        ) {
            Column(modifier = Modifier.align(Alignment.End)) {
                MapControlBtn(icon = R.drawable.ic_map_type) {
                    viewModel.toggleStyleSheetVisibility(true)
                }

                MapControlBtn(
                    icon = R.drawable.ic_relocate,
                    show = relocate
                ) {
                    scope.launch {
                        if (state.isMapLoaded) {
                            cameraPositionState.animate(
                                CameraUpdateFactory.newLatLngZoom(
                                    userLocation,
                                    DEFAULT_CAMERA_ZOOM
                                )
                            )
                        }
                    }
                }
                if (state.enabledAddPlaces) {
                    MapControlBtn(
                        icon = R.drawable.ic_geofence,
                        containerColor = AppTheme.colorScheme.primary,
                        contentColor = AppTheme.colorScheme.onPrimary
                    ) {
                        viewModel.navigateToPlaces()
                    }
                }
            }
            LazyColumn(modifier = Modifier.align(Alignment.CenterHorizontally)) {
                item {
                    val selectedUser = state.selectedUser
                    AnimatedVisibility(
                        visible = state.showUserDetails,
                        enter = slideInVertically { it } + scaleIn() + expandVertically(expandFrom = Alignment.Top),
                        exit = scaleOut() + slideOutVertically(targetOffsetY = { it }) + shrinkVertically(
                            shrinkTowards = Alignment.Bottom
                        )
                    ) {
                        SelectedUserDetail(
                            userInfo = selectedUser,
                            onDismiss = { viewModel.dismissMemberDetail() },
                            onTapTimeline = { viewModel.showJourneyTimeline() }
                        )
                    }
                }
                item {
                    if (state.members.isNotEmpty()) {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                                .widthIn(max = 600.dp)
                                .wrapContentSize()
                                .background(
                                    AppTheme.colorScheme.surface,
                                    shape = RoundedCornerShape(50.dp)
                                )
                                .padding(horizontal = 24.dp, vertical = 8.dp)
                                .align(Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item {
                                AddMemberBtn(state.loadingInviteCode) { viewModel.addMember() }
                            }
                            items(state.members) {
                                MapUserItem(it) {
                                    viewModel.showMemberDetail(it)
                                }
                            }
                        }
                    }
                }

                item {
                    val context = LocalContext.current
                    AnimatedVisibility(
                        visible = !context.hasAllPermission || !context.isLocationServiceEnabled() || !context.hasNotificationPermission,
                        enter = slideInVertically(tween(100)) { it },
                        exit = slideOutVertically(tween(100)) { it }
                    ) {
                        PermissionFooter {
                            viewModel.navigateToPermissionScreen()
                        }
                    }
                }
            }
        }
        if (state.isStyleSheetVisible) {
            Column(modifier = Modifier.align(Alignment.BottomEnd)) {
                MapStyleBottomSheet(
                    onStyleSelected = { style ->
                        viewModel.updateMapStyle(style)
                    }
                )
            }
        }
    }
}

@Composable
fun PermissionFooter(onClick: () -> Unit) {
    val context = LocalContext.current
    val hasLocationPermission = context.hasFineLocationPermission

    val locationEnabled = if (context.isLocationPermissionGranted) {
        context.isLocationServiceEnabled()
    } else {
        true
    }

    val title =
        if (!context.isLocationPermissionGranted) {
            stringResource(id = R.string.home_permission_footer_missing_location_permission_title)
        } else if (!context.hasNotificationPermission) {
            stringResource(id = R.string.home_permission_footer_title)
        } else {
            stringResource(id = R.string.home_permission_footer_missing_location_permission_title)
        }

    val subTitle =
        if (!locationEnabled) {
            stringResource(id = R.string.home_permission_footer_location_off_subtitle)
        } else if (!hasLocationPermission) {
            stringResource(id = R.string.home_permission_footer_subtitle)
        } else {
            stringResource(id = R.string.home_permission_footer_missing_location_permission_subtitle)
        }

    var showTurnOnLocationPopup by remember {
        mutableStateOf(false)
    }

    Row(
        modifier = Modifier
            .clickable {
                if (!locationEnabled) {
                    showTurnOnLocationPopup = true
                } else {
                    onClick()
                }
            }
            .background(color = if (!locationEnabled) AppTheme.colorScheme.alertColor else AppTheme.colorScheme.secondary)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                style = AppTheme.appTypography.label1.copy(
                    color = AppTheme.colorScheme.textInversePrimary,
                    fontWeight = FontWeight.W600
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subTitle,
                style = AppTheme.appTypography.body3.copy(color = AppTheme.colorScheme.textInversePrimary)
            )
        }
        Icon(
            painter = painterResource(id = R.drawable.ic_right_arrow_icon),
            contentDescription = "",
            tint = AppTheme.colorScheme.textInversePrimary
        )
    }

    if (showTurnOnLocationPopup) {
        ShowEnableLocationDialog(
            onDismiss = {
                showTurnOnLocationPopup = false
            },
            goToSettings = {
                showTurnOnLocationPopup = false
                context.openLocationSettings()
            }
        )
    }
}

@Composable
private fun MapView(
    cameraPositionState: CameraPositionState
) {
    val viewModel = hiltViewModel<MapViewModel>()
    val state by viewModel.state.collectAsState()
    val terrainStyle = stringResource(R.string.map_style_terrain)
    val satelliteStyle = stringResource(R.string.map_style_satellite)

    val mapStyleOptions = remember(state.selectedMapStyle) {
        when (state.selectedMapStyle) {
            terrainStyle -> MapType.TERRAIN
            satelliteStyle -> MapType.SATELLITE
            else -> MapType.NORMAL
        }
    }

    val isDarkMode = isSystemInDarkTheme()
    val context = LocalContext.current
    val mapProperties = MapProperties(
        mapStyleOptions = if (isDarkMode) {
            MapStyleOptions.loadRawResourceStyle(context, R.raw.map_theme_night)
        } else {
            null
        },
        mapType = mapStyleOptions
    )

    GoogleMap(
        cameraPositionState = cameraPositionState,
        properties = mapProperties,
        uiSettings = MapUiSettings(
            zoomControlsEnabled = false,
            tiltGesturesEnabled = false,
            myLocationButtonEnabled = false,
            compassEnabled = false,
            mapToolbarEnabled = false
        ),
        onMapLoaded = {
            viewModel.onMapLoaded()
        }
    ) {
        if (state.members.isNotEmpty()) {
            state.members.filter { it.location != null && it.isLocationEnable }.forEach {
                MapMarker(
                    user = it.user,
                    location = it.location!!,
                    isSelected = it.user.id == state.selectedUser?.user?.id
                ) {
                    viewModel.showMemberDetail(it)
                }
            }
        } else {
            val location = state.defaultCameraPosition
            val currentUser = state.currentUser
            if (location != null && currentUser != null) {
                MapMarker(
                    user = currentUser,
                    location = location.toApiLocation(currentUser.id),
                    isSelected = currentUser.id == state.selectedUser?.user?.id
                ) {}
            }
        }

        state.places.forEach {
            MapCircles(place = it) {}
        }
    }
}

fun LatLng.distanceTo(other: LatLng): Double {
    val result = FloatArray(3)
    android.location.Location.distanceBetween(
        latitude,
        longitude,
        other.latitude,
        other.longitude,
        result
    )
    return result[0].toDouble()
}
