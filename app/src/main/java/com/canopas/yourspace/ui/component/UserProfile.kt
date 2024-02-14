package com.canopas.yourspace.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.canopas.yourspace.data.models.user.ApiUser
import com.canopas.yourspace.ui.theme.AppTheme

@Composable
fun UserProfile(
    modifier: Modifier,
    user: ApiUser,
    imagePainter: AsyncImagePainter? = null
) {
    val profileUrl = user.profile_image

    Box(
        modifier = modifier
            .background(
                AppTheme.colorScheme.surface,
                shape = RoundedCornerShape(16.dp)
            )
            .background(
                AppTheme.colorScheme.primary,
                shape = RoundedCornerShape(16.dp)
            )
            .clip(RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (!profileUrl.isNullOrEmpty()) {
            val painter = imagePainter ?: rememberAsyncImagePainter(
                ImageRequest.Builder(LocalContext.current).data(
                    profileUrl
                ).build()
            )
            Image(
                painter = painter,
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
