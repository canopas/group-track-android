package com.canopas.yourspace.ui.flow.geofence.edit

import android.graphics.Point
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.canopas.yourspace.R
import com.canopas.yourspace.data.models.place.ApiPlace
import com.canopas.yourspace.data.models.place.ApiPlaceMemberSetting
import com.canopas.yourspace.data.models.user.ApiUser
import com.canopas.yourspace.data.models.user.UserInfo
import com.canopas.yourspace.ui.component.AppAlertDialog
import com.canopas.yourspace.ui.component.AppBanner
import com.canopas.yourspace.ui.component.AppProgressIndicator
import com.canopas.yourspace.ui.component.NoInternetScreen
import com.canopas.yourspace.ui.component.PrimaryButton
import com.canopas.yourspace.ui.component.UserProfile
import com.canopas.yourspace.ui.component.gesturesDisabled
import com.canopas.yourspace.ui.flow.geofence.component.PlaceNameContent
import com.canopas.yourspace.ui.flow.home.map.DEFAULT_CAMERA_ZOOM
import com.canopas.yourspace.ui.theme.AppTheme
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.Projection
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.SphericalUtil
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sqrt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPlaceScreen() {
    val viewModel = hiltViewModel<EditPlaceViewModel>()
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppTheme.colorScheme.surface),
                title = {
                    Text(
                        text = stringResource(id = R.string.edit_place_title),
                        style = AppTheme.appTypography.subTitle1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.popBackStack() }) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_nav_back_arrow_icon),
                            contentDescription = ""
                        )
                    }
                },
                actions = {
                    if (state.isInternetAvailable) {
                        TextButton(
                            onClick = viewModel::savePlace,
                            enabled = state.enableSave,
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = AppTheme.colorScheme.primary,
                                disabledContentColor = AppTheme.colorScheme.textDisabled
                            )
                        ) {
                            if (state.saving) {
                                AppProgressIndicator()
                            } else {
                                Text(
                                    text = stringResource(id = R.string.common_save),
                                    style = AppTheme.appTypography.button
                                )
                            }
                        }
                    }
                }
            )
        },
        contentColor = AppTheme.colorScheme.textPrimary,
        containerColor = AppTheme.colorScheme.surface
    ) {
        if (state.isInternetAvailable) {
            Box(
                modifier = Modifier
                    .padding(it)
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (state.isLoading) {
                    AppProgressIndicator()
                } else {
                    EditPlaceContent()
                }
            }
        } else {
            NoInternetScreen(viewModel::checkInternetConnection)
        }
    }

    if (state.showDeletePlaceConfirmation) {
        DeletePlaceConfirmation(viewModel)
    }

    if (state.error != null) {
        AppBanner(msg = state.error!!) {
            viewModel.resetErrorState()
        }
    }
}

@Composable
fun DeletePlaceConfirmation(viewModel: EditPlaceViewModel) {
    AppAlertDialog(
        subTitle = stringResource(id = R.string.places_list_delete_dialogue_message_text),
        confirmBtnText = stringResource(id = R.string.common_btn_delete),
        dismissBtnText = stringResource(id = R.string.common_btn_cancel),
        isConfirmDestructive = true,
        onConfirmClick = viewModel::onDeletePlace,
        onDismissClick = viewModel::dismissDeletePlaceConfirmation,
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    )
}

@Composable
private fun EditPlaceContent() {
    val viewModel = hiltViewModel<EditPlaceViewModel>()
    val state by viewModel.state.collectAsState()
    val scrollState = rememberScrollState()

    val cameraPositionState = rememberCameraPositionState {
        val placeLocation =
            LatLng(state.updatePlace?.latitude ?: 0.0, state.updatePlace?.longitude ?: 0.0)
        position = CameraPosition.fromLatLngZoom(placeLocation, DEFAULT_CAMERA_ZOOM)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState, enabled = !cameraPositionState.isMoving)
            .padding(bottom = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        state.updatePlace?.let { place ->
            MapView(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.85f),
                place.radius,
                cameraPositionState,
                enabled = state.isAdmin,
                viewModel,
                viewModel::onPlaceLocationChanged
            )

            if (state.isAdmin) {
                RadiusSlider(place.radius, viewModel::onPlaceRadiusChanged)
            }

            PlaceDetails(
                place,
                state.isAdmin,
                cameraPositionState,
                viewModel::onPlaceNameChanged
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        state.updatedSetting?.let {
            PlaceSettings(
                it,
                state.spaceMembers,
                viewModel::toggleArrives,
                viewModel::toggleLeaves
            )
        }

        Spacer(modifier = Modifier.height(30.dp))
        DeletePlaceFooter(
            state.isAdmin,
            state.deleting,
            viewModel::showDeletePlaceConfirmation
        )
    }
}

@Composable
fun DeletePlaceFooter(admin: Boolean, deleting: Boolean, showDeletePlaceConfirmation: () -> Unit) {
    if (admin) {
        PrimaryButton(
            label = stringResource(id = R.string.edit_place_admin_btn_delete),
            onClick = showDeletePlaceConfirmation,
            showLoader = deleting,
            containerColor = AppTheme.colorScheme.containerLow,
            contentColor = AppTheme.colorScheme.alertColor
        )
    } else {
        Text(
            text = stringResource(id = R.string.edit_place_not_admin_cannot_delete),
            style = AppTheme.appTypography.label1,
            color = AppTheme.colorScheme.textSecondary,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
fun PlaceSettings(
    setting: ApiPlaceMemberSetting,
    members: List<UserInfo>,
    toggleArrives: (String, Boolean) -> Unit,
    toggleLeaves: (String, Boolean) -> Unit
) {
    Header(title = stringResource(id = R.string.edit_place_title_get_notified))

    members.forEach { member ->
        val isLastItem = members.indexOf(member) == members.lastIndex
        val enableArrives = setting.arrival_alert_for.contains(member.user.id)
        val enableLeaves = setting.leave_alert_for.contains(member.user.id)
        MemberItem(
            member.user,
            enableArrives,
            enableLeaves,
            { toggleArrives(member.user.id, it) },
            { toggleLeaves(member.user.id, it) }
        )

        if (!isLastItem) {
            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                thickness = 1.dp,
                color = AppTheme.colorScheme.outline
            )
        }
    }
}

@Composable
private fun MemberItem(
    user: ApiUser,
    enableArrives: Boolean,
    enableLeaves: Boolean,
    onArrives: (Boolean) -> Unit,
    onLeaves: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UserProfile(modifier = Modifier.size(40.dp), user = user)
            Text(
                text = user.fullName,
                style = AppTheme.appTypography.subTitle2,
                color = AppTheme.colorScheme.textPrimary,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp)
            )
        }

        Column {
            PlaceSwitchView(
                label = stringResource(id = R.string.edit_place_notified_when_arrives),
                enableSwitch = enableArrives,
                toggleSwitch = onArrives
            )
            Spacer(modifier = Modifier.height(8.dp))
            PlaceSwitchView(
                label = stringResource(id = R.string.edit_place_notified_when_leaves),
                enableSwitch = enableLeaves,
                toggleSwitch = onLeaves
            )
        }
    }
}

@Composable
private fun PlaceSwitchView(
    label: String,
    enableSwitch: Boolean,
    toggleSwitch: (Boolean) -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = AppTheme.appTypography.body2,
            color = AppTheme.colorScheme.textDisabled
        )
        Spacer(modifier = Modifier.width(16.dp))

        Switch(
            checked = enableSwitch,
            onCheckedChange = toggleSwitch,
            colors = SwitchDefaults.colors(
                checkedThumbColor = AppTheme.colorScheme.onPrimary
            )
        )
    }
}

@Composable
private fun PlaceDetails(
    place: ApiPlace,
    isAdmin: Boolean,
    cameraPositionState: CameraPositionState,
    onPlaceNameChanged: (String) -> Unit
) {
    Header(title = stringResource(id = R.string.edit_place_title_place_details))
    PlaceNameContent(
        place.name,
        cameraPositionState,
        LatLng(place.latitude, place.longitude),
        enable = isAdmin,
        onPlaceNameChanged
    )
}

@Composable
private fun Header(title: String) {
    Text(
        text = title,
        style = AppTheme.appTypography.subTitle1,
        color = AppTheme.colorScheme.textDisabled,
        textAlign = TextAlign.Start,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 24.dp)
    )
}

@Composable
private fun RadiusSlider(radius: Double, onPlaceRadiusChanged: (Double) -> Unit) {
    val radiusText = when {
        radius < 1609.34 -> "${radius.roundToInt()} m"
        else -> {
            val miles = (radius / 1609.34)
            val milesText = if (miles % 1 == 0.0) "${miles.toInt()}" else miles.format(1)
            "$milesText mi"
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Slider(
            value = radius.toFloat(),
            onValueChange = {
                onPlaceRadiusChanged(it.toDouble())
            },
            valueRange = 100f..3219f,
            steps = 0,
            modifier = Modifier
                .weight(1f)
                .padding(end = 4.dp)
        )

        Text(
            text = radiusText,
            modifier = Modifier.widthIn(min = 40.dp),
            textAlign = TextAlign.End,
            style = AppTheme.appTypography.caption,
            color = AppTheme.colorScheme.textSecondary
        )
    }
}

@Composable
private fun MapView(
    modifier: Modifier,
    placeRadius: Double,
    cameraPositionState: CameraPositionState,
    enabled: Boolean,
    viewModel: EditPlaceViewModel,
    onPlaceLocationChanged: (LatLng) -> Unit
) {
    LaunchedEffect(key1 = cameraPositionState.position.target) {
        onPlaceLocationChanged(cameraPositionState.position.target)
    }

    val state by viewModel.state.collectAsState()
    LaunchedEffect(placeRadius, state.isMapLoaded) {
        snapshotFlow { placeRadius }
            .distinctUntilChanged()
            .collect {
                delay(500)
                if (state.isMapLoaded) {
                    val newBound = toBounds(cameraPositionState.position.target, it)
                    cameraPositionState.animate(CameraUpdateFactory.newLatLngBounds(newBound, 50))
                }
            }
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

    Box(modifier = modifier.gesturesDisabled(!enabled)) {
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
        )
        PlaceMarker(placeRadius, cameraPositionState)
    }
}

@Composable
private fun PlaceMarker(radiusInMeters: Double, cameraPositionState: CameraPositionState) {
    val points =
        cameraPositionState.projection?.toScreenLocation(cameraPositionState.position.target)
            ?: Point(0, 0)
    val radius = convertZoneRadiusToPixels(
        cameraPositionState.position.target,
        radiusInMeters,
        cameraPositionState.projection
    )

    Box(
        modifier = Modifier
            .clipToBounds(),
        contentAlignment = Alignment.Center
    ) {
        val color = AppTheme.colorScheme.primary.copy(alpha = 0.6f)
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = color,
                radius = radius.toFloat(),
                center = Offset(points.x.toFloat(), points.y.toFloat())
            )
        }

        Box(
            modifier = Modifier
                .size(40.dp)
                .align(Alignment.Center)
                .background(AppTheme.colorScheme.surface, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_tab_places_filled),
                contentDescription = "",
                modifier = Modifier.size(20.dp),
                tint = AppTheme.colorScheme.primary
            )
        }
    }
}

fun Double.format(digits: Int) = "%.${digits}f".format(this)

private fun convertZoneRadiusToPixels(
    latLang: LatLng,
    radiusInMeters: Double,
    projection: Projection?
): Int {
    if (projection == null) return 0
    val EARTH_RADIUS = 6378100.0
    val lat1 = radiusInMeters / EARTH_RADIUS
    val lng1 = radiusInMeters / (EARTH_RADIUS * cos((Math.PI * latLang.latitude / 180)))

    val lat2 = latLang.latitude + lat1 * 180 / Math.PI
    val lng2 = latLang.longitude + lng1 * 180 / Math.PI

    val p1: Point = projection.toScreenLocation(LatLng(latLang.latitude, latLang.longitude))
    val p2: Point = projection.toScreenLocation(LatLng(lat2, lng2))
    return abs((p1.x - p2.x).toDouble()).toInt()
}

fun toBounds(center: LatLng?, radiusInMeters: Double): LatLngBounds {
    val distanceFromCenterToCorner = radiusInMeters * sqrt(2.0)
    val southwestCorner: LatLng =
        SphericalUtil.computeOffset(center, distanceFromCenterToCorner, 225.0)
    val northeastCorner: LatLng =
        SphericalUtil.computeOffset(center, distanceFromCenterToCorner, 45.0)
    return LatLngBounds(southwestCorner, northeastCorner)
}
