package com.canopas.yourspace.ui.flow.home.map

import android.Manifest
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalConfiguration
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
import com.canopas.yourspace.data.utils.isLocationPermissionGranted
import com.canopas.yourspace.domain.utils.isLocationServiceEnabled
import com.canopas.yourspace.domain.utils.openLocationSettings
import com.canopas.yourspace.ui.component.ShowEnableLocationDialog
import com.canopas.yourspace.ui.flow.home.map.component.AddMemberBtn
import com.canopas.yourspace.ui.flow.home.map.component.MapMarker
import com.canopas.yourspace.ui.flow.home.map.component.MapUserItem
import com.canopas.yourspace.ui.flow.home.map.member.MemberDetailBottomSheetContent
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
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch

private const val DEFAULT_CAMERA_ZOOM = 15f
private const val DEFAULT_CAMERA_ZOOM_FOR_SELECTED_USER = 17f

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun MapScreen() {
    val viewModel = hiltViewModel<MapViewModel>()
    val state by viewModel.state.collectAsState()
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp

    val permissionState = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)
    LaunchedEffect(permissionState) {
        snapshotFlow { permissionState.status == PermissionStatus.Granted && state.defaultCameraPosition == null }
            .collect {
                if (it) {
                    viewModel.startLocationTracking()
                }
            }
    }

    val bottomSheetState = rememberStandardBottomSheetState(
        initialValue = SheetValue.Hidden,
        skipHiddenState = false
    )
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = bottomSheetState
    )

    LaunchedEffect(bottomSheetState) {
        snapshotFlow { bottomSheetState.currentValue }
            .collect {
                if (it == SheetValue.Hidden) {
                    viewModel.dismissMemberDetail()
                }
            }
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetContainerColor = AppTheme.colorScheme.surface,
        sheetPeekHeight = if (state.showUserDetails) (screenHeight / 3).dp else 0.dp,
        sheetContent = {
            state.selectedUser?.let { MemberDetailBottomSheetContent(state.selectedUser!!) }
        }
    ) {
        MapScreenContent(modifier = Modifier)
    }
}

@Composable
fun MapScreenContent(modifier: Modifier) {
    val scope = rememberCoroutineScope()
    val viewModel = hiltViewModel<MapViewModel>()
    val state by viewModel.state.collectAsState()

    val userLocation = remember(state.defaultCameraPosition) {
        val location = state.defaultCameraPosition
        LatLng(location?.latitude ?: 0.0, location?.longitude ?: 0.0)
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(userLocation, DEFAULT_CAMERA_ZOOM)
    }

    val relocate by remember {
        derivedStateOf {
            val distance = cameraPositionState.position.target.distanceTo(userLocation)
            distance > 100
        }
    }

    LaunchedEffect(userLocation) {
        val location = state.selectedUser?.location
        cameraPositionState.animate(
            CameraUpdateFactory.newLatLngZoom(
                userLocation,
                if (location != null) DEFAULT_CAMERA_ZOOM_FOR_SELECTED_USER else DEFAULT_CAMERA_ZOOM
            )
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        MapView(cameraPositionState)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomEnd)

        ) {
            RelocateBtn(
                modifier = Modifier.align(Alignment.End),
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

            if (state.members.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 10.dp)
                        .widthIn(max = 600.dp)
                        .fillMaxWidth()
                        .shadow(10.dp, shape = RoundedCornerShape(6.dp))
                        .background(AppTheme.colorScheme.surface, shape = RoundedCornerShape(6.dp))
                        .align(Alignment.CenterHorizontally),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
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

            val context = LocalContext.current
            AnimatedVisibility(
                visible = !context.hasAllPermission || !context.isLocationServiceEnabled(),
                enter = slideInVertically(tween(100)) { it },
                exit = slideOutVertically(tween(100)) { it }
            ) {
                PermissionFooter() {
                    viewModel.navigateToPermissionScreen()
                }
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
        stringResource(id = R.string.home_permission_footer_missing_location_permission_title)

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
            Icons.Default.KeyboardArrowRight,
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
    val isDarkMode = isSystemInDarkTheme()
    val context = LocalContext.current
    val mapProperties = MapProperties(
        mapStyleOptions = if (isDarkMode) {
            MapStyleOptions.loadRawResourceStyle(context, R.raw.map_theme_night)
        } else {
            null
        }
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
        )
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
    }
}

@Composable
private fun RelocateBtn(
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int,
    show: Boolean = true,
    onClick: () -> Unit
) {
    AnimatedVisibility(
        visible = show,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
            .padding(bottom = 10.dp, end = 10.dp)
    ) {
        SmallFloatingActionButton(
            onClick = { onClick() },
            containerColor = AppTheme.colorScheme.surface,
            contentColor = AppTheme.colorScheme.primary
        ) {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = "",
                modifier = Modifier.size(24.dp)
            )
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
