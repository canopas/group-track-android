package com.canopas.catchme.ui.flow.home.map.component

import android.content.Context
import android.location.Geocoder
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.canopas.catchme.R
import com.canopas.catchme.data.models.user.ApiUser
import com.canopas.catchme.data.models.user.UserInfo
import com.canopas.catchme.ui.theme.AppTheme
import com.canopas.catchme.ui.theme.InterFontFamily
import com.google.android.gms.maps.model.LatLng
import timber.log.Timber
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.abs

@Composable
fun MapUserItem(
    userInfo: UserInfo,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val user = userInfo.user
    val location = remember(userInfo) {
        val latLng =
            LatLng(userInfo.location?.latitude ?: 0.0, userInfo.location?.longitude ?: 0.0)
        getAddress(context, latLng) ?: context.getString(R.string.map_user_item_location_unknown)
    }
    val lastUpdated = remember(userInfo) {
        getTimeAgoString(context, userInfo.location?.created_at ?: 0L)
    }

    Card(
        modifier = Modifier
            .height(100.dp)
            .aspectRatio(2.8f)
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
                    text = buildAnnotatedString {
                        withStyle(
                            style = SpanStyle(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                                fontFamily = InterFontFamily
                            )
                        ) {
                            append(user.fullName)
                        }
                        withStyle(
                            style = SpanStyle(
                                fontWeight = FontWeight.W400,
                                fontSize = 10.sp,
                                fontFamily = InterFontFamily
                            )
                        ) {
                            if (userInfo.isLocationEnable) append(" • $lastUpdated")
                        }
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (!userInfo.isLocationEnable) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.Info,
                            contentDescription = "",
                            tint = Color.Red,
                            modifier = Modifier
                                .size(14.dp)

                        )
                        Text(
                            text = stringResource(id = R.string.map_user_item_location_off),
                            style = AppTheme.appTypography.label1.copy(
                                color = Color.Red,
                                fontWeight = FontWeight.Normal
                            ),
                            modifier = Modifier
                                .padding(start = 4.dp)
                        )
                    }
                } else {
                    Text(
                        text = location,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        style = AppTheme.appTypography.label2.copy(color = AppTheme.colorScheme.textSecondary)
                    )
                }
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
                    fontWeight = FontWeight.Normal,
                    fontSize = 28.sp
                )
            )
        }
    }
}

private fun getTimeAgoString(context: Context, timestamp: Long): String {
    val now = System.currentTimeMillis()
    val duration = abs(timestamp - now)
    val days = TimeUnit.MILLISECONDS.toDays(duration)
    val hours = TimeUnit.MILLISECONDS.toHours(duration)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(duration)

    return when {
        minutes < 2 -> context.getString(R.string.map_user_item_location_updated_now)
        hours < 1 -> context.getString(
            R.string.map_user_item_location_updated_minutes_ago,
            minutes.toString()
        )

        days < 1 -> context.getString(
            R.string.map_user_item_location_updated_hours_ago,
            hours.toString()
        )

        else -> {
            val output = SimpleDateFormat("dd MMM")
            val since = output.format(Date(timestamp))
            context.getString(R.string.map_user_item_location_updated_since_days, since)
        }
    }
}

private fun getAddress(context: Context, latLng: LatLng): String? {
    try {
        val geocoder = Geocoder(context, Locale.getDefault())
        val addressList = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
        addressList?.map {
        }
        if (addressList != null && addressList.size > 0) {
            val address = addressList[0]
            return address.getAddressLine(0)
        }
    } catch (e: IOException) {
        Timber.e(e)
    }
    return null
}
