package com.canopas.catchme.ui.flow.home.map

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.canopas.catchme.R
import com.canopas.catchme.ui.flow.home.map.component.MapMarker
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

@Composable
fun MapScreen() {
    val scope = rememberCoroutineScope()
    val tempLatLong = remember {
        LatLng(21.231809060338193, 72.83629238605499)
    }
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(tempLatLong, 16f)
    }

    val relocate by remember {
        derivedStateOf {
            cameraPositionState.position.target != tempLatLong
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        MapView(cameraPositionState)

        AnimatedVisibility(
            visible = relocate,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 10.dp, end = 10.dp)

        ) {
            RelocateBtn(icon = R.drawable.ic_relocate) {
                scope.launch {
                    cameraPositionState.animate(
                        CameraUpdateFactory.newLatLngZoom(
                            tempLatLong,
                            16f
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun MapView(cameraPositionState: CameraPositionState) {
    val viewModel = hiltViewModel<MapViewModel>()
    val state by viewModel.state.collectAsState()

    GoogleMap(
        cameraPositionState = cameraPositionState,
        properties = MapProperties(),
        uiSettings = MapUiSettings(zoomControlsEnabled = false, tiltGesturesEnabled = false)
    ) {
        state.members.filter { it.location != null }.forEach {
            MapMarker(user = it.user, location = it.location!!) {}
        }
    }
}

@Composable
private fun RelocateBtn(
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int,
    onClick: () -> Unit
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
