package com.canopas.catchme.ui.flow.home.map.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.canopas.catchme.data.models.user.ApiUser
import com.canopas.catchme.data.models.user.UserInfo
import com.canopas.catchme.ui.theme.AppTheme

@Composable
fun MapUserItem(
    userInfo: UserInfo,
    onClick: () -> Unit
) {
    val user = userInfo.user
    Card(
        modifier = Modifier
            .height(120.dp)
            .aspectRatio(2.4f)
            .clickable {
                onClick()
            },
        colors = CardDefaults.cardColors(
            containerColor = AppTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)

    ) {
        Row(modifier = Modifier.padding(10.dp)) {
            UserProfile(user = user)

            Column(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .weight(1f)
            ) {
                Text(
                    text = user.fullName,
                    style = AppTheme.appTypography.subTitle1.copy(
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Text(
                    text = "A-303, sarjan  vatika appartment,dabholi gam, surat",
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = AppTheme.appTypography.body2.copy(color = AppTheme.colorScheme.textSecondary),
                    modifier = Modifier.padding(top = 4.dp)
                )

                Text(
                    text = "Since 20:30",
                    style = AppTheme.appTypography.body2.copy(color = AppTheme.colorScheme.textSecondary),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun UserProfile(user: ApiUser) {
    val profileUrl = user.profile_image
    Box(
        modifier = Modifier
            .size(50.dp)
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
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp
                )
            )
        }
    }
}
