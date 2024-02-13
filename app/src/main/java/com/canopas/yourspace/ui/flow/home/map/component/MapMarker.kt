package com.canopas.yourspace.ui.flow.home.map.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.canopas.yourspace.data.models.location.ApiLocation
import com.canopas.yourspace.data.models.user.ApiUser
import com.canopas.yourspace.ui.component.UserProfile
import com.canopas.yourspace.ui.theme.AppTheme
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.MarkerComposable

@Composable
fun MapMarker(
    user: ApiUser,
    location: ApiLocation,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    // val iconState = rememberMarkerIconState(user)
    val markerState = rememberMarkerState(
        user = user,
        position = LatLng(
            location.latitude,
            location.longitude
        )
    )
    var painter: AsyncImagePainter? = null

    if (!user.profile_image.isNullOrEmpty()) {
        painter = rememberAsyncImagePainter(
            ImageRequest.Builder(LocalContext.current).data(
                user.profile_image
            ).allowHardware(false)
                .build()
        )
    }

    MarkerComposable(
        keys = arrayOf(user.id, isSelected, painter?.state ?: 0),
        state = markerState,
        title = user.fullName,
        zIndex = if (isSelected) 1f else 0f,
        anchor = Offset(0.0f, 1f),
        onClick = {
            onClick()
            true
        }
    ) {
        MarkerContent(user = user, isSelected, painter = painter)
    }
}

@Composable
fun MarkerContent(user: ApiUser, isSelected: Boolean, painter: AsyncImagePainter?) {
    val shape = RoundedCornerShape(16.dp, 16.dp, 16.dp, 3.dp)

    Box(
        modifier = Modifier
            .size(64.dp)
            .background(
                if (isSelected) AppTheme.colorScheme.secondary else AppTheme.colorScheme.surface,
                shape = shape
            )
            .border(1.5.dp, AppTheme.colorScheme.containerHigh, shape = shape)
            .padding(5.dp),
        contentAlignment = Alignment.Center
    ) {
        UserProfile(
            modifier = Modifier
                .fillMaxSize(),
            user = user,
            imagePainter = painter
        )
    }
}
