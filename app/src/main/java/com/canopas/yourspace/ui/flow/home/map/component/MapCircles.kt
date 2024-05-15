package com.canopas.yourspace.ui.flow.home.map.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.canopas.yourspace.R
import com.canopas.yourspace.data.models.place.ApiPlace
import com.canopas.yourspace.ui.theme.AppTheme
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.MarkerComposable
import com.google.maps.android.compose.MarkerState

@Composable
fun MapCircles(place: ApiPlace, onClick: () -> Unit) {
    val markerState = MarkerState(
        position = LatLng(place.latitude, place.longitude)
    )

    Circle(
        center = LatLng(place.latitude, place.longitude),
        radius = place.radius,
        fillColor = AppTheme.colorScheme.primary.copy(alpha = 0.4f),
        strokeColor = AppTheme.colorScheme.primary,
        strokeWidth = 1f
    )

    MarkerComposable(
        keys = arrayOf(place.id),
        state = markerState,
        title = place.name,
        anchor = Offset(0.5f, 0.5f),
        onClick = {
            onClick()
            true
        }
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(AppTheme.colorScheme.onPrimary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_tab_places_filled),
                contentDescription = "",
                tint = AppTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
