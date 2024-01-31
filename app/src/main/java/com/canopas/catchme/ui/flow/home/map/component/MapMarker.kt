package com.canopas.catchme.ui.flow.home.map.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import com.canopas.catchme.data.models.location.ApiLocation
import com.canopas.catchme.data.models.user.ApiUser
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Marker

@Composable
fun MapMarker(
    user: ApiUser,
    location: ApiLocation,
    onClick: () -> Unit
) {
    val iconState = rememberMarkerIconState(user)
    val markerState = rememberMarkerState(
        user = user,
        position = LatLng(
            location.latitude,
            location.longitude
        )
    )

    Marker(
        state = markerState,
        title = user.fullName,
        icon = iconState,
        anchor = Offset(0.0f, 1f),
        onClick = {
            onClick()
            false
        }
    )
}
