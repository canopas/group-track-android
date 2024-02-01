package com.canopas.catchme.ui.flow.home.map.component

import android.util.Log
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.canopas.catchme.data.models.location.ApiLocation
import com.canopas.catchme.data.models.user.ApiUser
import com.canopas.catchme.ui.component.UserProfile
import com.canopas.catchme.ui.theme.AppTheme
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

    MarkerComposable(
        keys = arrayOf(user.id, isSelected),
        state = markerState,
        title = user.fullName,
        anchor = Offset(0.0f, 1f),
        onClick = {
            onClick()
            true
        }
    ) {
        MarkerContent(user = user, isSelected)
    }
}

@Composable
fun MarkerContent(user: ApiUser, isSelected: Boolean) {
    val profileUrl = user.profile_image
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
            user = user
        )
    }
}