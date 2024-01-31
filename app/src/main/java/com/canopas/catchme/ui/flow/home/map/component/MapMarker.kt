package com.canopas.catchme.ui.flow.home.map.component

import android.util.Log
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
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
                if (isSelected) AppTheme.colorScheme.tertiary else AppTheme.colorScheme.surface,
                shape = shape
            )
            .border(1.5.dp, AppTheme.colorScheme.containerHigh, shape = shape)
            .padding(5.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    AppTheme.colorScheme.surface,
                    shape = RoundedCornerShape(16.dp)
                )
                .background(
                    AppTheme.colorScheme.primary.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(16.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (!profileUrl.isNullOrEmpty()) {
                Image(
                    painter = rememberAsyncImagePainter(
                        ImageRequest.Builder(LocalContext.current).data(
                            profileUrl
                        ).build()
                    ),
                    contentScale = ContentScale.Crop,
                    contentDescription = "ProfileImage"
                )
            } else {
                Text(
                    text = user.fullName.take(1).uppercase(),
                    style = TextStyle(
                        color = Color.White,
                        fontWeight = FontWeight.Normal,
                        fontSize = 28.sp
                    )
                )
            }
        }
    }
}