package com.canopas.yourspace.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.canopas.yourspace.R
import com.canopas.yourspace.data.models.user.ApiUser
import com.canopas.yourspace.ui.theme.AppTheme

@Composable
fun MarkerUserProfile(
    modifier: Modifier,
    user: ApiUser?,
    shape: RoundedCornerShape = RoundedCornerShape(16.dp),
    placeholderSize: Dp = 24.dp,
    imagePainter: AsyncImagePainter? = null
) {
    UserProfile(modifier = modifier, user = user, shape, placeholderSize, imagePainter)
}

@Composable
fun UserProfile(
    modifier: Modifier,
    user: ApiUser?,
    shape: RoundedCornerShape = CircleShape,
    placeholderSize: Dp = 24.dp,
    imagePainter: AsyncImagePainter? = null
) {
    val profileUrl = user?.profile_image

    Box(
        modifier = modifier
            .background(
                AppTheme.colorScheme.surface,
                shape = shape
            )
            .background(
                AppTheme.colorScheme.primary,
                shape = shape
            )
            .clip(shape),
        contentAlignment = Alignment.Center
    ) {
        if (!profileUrl.isNullOrEmpty()) {
            BuildUserProfileImage(profileUrl, imagePainter, placeholderSize)
        } else if (!user?.fullName.isNullOrEmpty()) {
            Text(
                text = user?.fullName?.take(1)?.uppercase() ?: "?",
                style = AppTheme.appTypography.header4.copy(color = AppTheme.colorScheme.textInversePrimary)
            )
        } else {
            Icon(
                painter = painterResource(id = R.drawable.ic_user_profile_placeholder),
                contentDescription = null,
                tint = AppTheme.colorScheme.onPrimary,
                modifier = Modifier.size(placeholderSize)
            )
        }
    }
}

@Composable
fun BuildUserProfileImage(profileUrl: String, imagePainter: AsyncImagePainter?, placeholderSize: Dp) {
    Box(contentAlignment = Alignment.Center) {
        val painter = imagePainter ?: rememberAsyncImagePainter(
            ImageRequest.Builder(LocalContext.current).data(profileUrl).build()
        )
        Image(
            painter = painter,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            contentDescription = "ProfileImage"
        )
        if (painter.state !is AsyncImagePainter.State.Success) {
            Icon(
                painter = painterResource(id = R.drawable.ic_user_profile_placeholder),
                contentDescription = null,
                tint = AppTheme.colorScheme.onPrimary,
                modifier = Modifier.size(placeholderSize)
            )
        }
    }
}
