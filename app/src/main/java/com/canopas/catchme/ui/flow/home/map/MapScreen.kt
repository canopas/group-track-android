package com.canopas.catchme.ui.flow.home.map

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.canopas.catchme.R
import com.canopas.catchme.ui.flow.home.map.component.MapMarker
import com.canopas.catchme.ui.flow.home.map.component.MapUserItem
import com.canopas.catchme.ui.flow.home.map.component.MemberDetailBottomSheetContent
import com.canopas.catchme.ui.theme.AppTheme
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen() {
    val viewModel = hiltViewModel<MapViewModel>()
    val state by viewModel.state.collectAsState()

    BottomSheetScaffold(sheetContent = {
        if (state.selectedUser != null && state.showUserDetails) {
            MemberDetailBottomSheetContent(state.selectedUser!!)
        }
    }) {
        MapScreenContent(modifier = Modifier)

    }
}

@Composable
fun MapScreenContent(modifier: Modifier) {
    val scope = rememberCoroutineScope()
    val viewModel = hiltViewModel<MapViewModel>()
    val state by viewModel.state.collectAsState()

    val userLocation = remember(state.currentCameraPosition) {
        val location = state.currentCameraPosition
        LatLng(location?.latitude ?: 0.0, location?.longitude ?: 0.0)
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(userLocation, 16f)
    }

    val relocate by remember {
        derivedStateOf {
            val distance = cameraPositionState.position.target.distanceTo(userLocation)
            distance > 100
        }
    }

    LaunchedEffect(userLocation) {
        cameraPositionState.animate(
            CameraUpdateFactory.newLatLngZoom(
                userLocation,
                16f
            )
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        MapView(cameraPositionState)

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 10.dp),
            horizontalAlignment = Alignment.End
        ) {
            RelocateBtn(
                icon = R.drawable.ic_relocate,
                show = relocate
            ) {
                scope.launch {
                    cameraPositionState.animate(
                        CameraUpdateFactory.newLatLngZoom(
                            userLocation,
                            16f
                        )
                    )
                }
            }

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.members) {
                    MapUserItem(it) {
                        viewModel.showMemberDetail(it)
                    }
                }
            }
        }
    }
}

@Composable
private fun MapView(
    cameraPositionState: CameraPositionState,
) {
    val viewModel = hiltViewModel<MapViewModel>()
    val state by viewModel.state.collectAsState()

    GoogleMap(
        cameraPositionState = cameraPositionState,
        properties = MapProperties(),
        uiSettings = MapUiSettings(
            zoomControlsEnabled = false,
            tiltGesturesEnabled = false,
            myLocationButtonEnabled = false,
            compassEnabled = false,
            mapToolbarEnabled = false
        )
    ) {
        state.members.filter { it.location != null && it.isLocationEnable }.forEach {
            MapMarker(
                user = it.user,
                location = it.location!!,
                isSelected = it.user.id == state.selectedUser?.user?.id
            ) {
                viewModel.showMemberDetail(it)
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
        modifier = Modifier
            .padding(bottom = 10.dp, end = 10.dp)
    ) {
        SmallFloatingActionButton(
            modifier = modifier,
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
