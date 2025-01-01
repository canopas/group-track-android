package com.canopas.yourspace.ui.flow.home.map.component

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.canopas.yourspace.R
import com.canopas.yourspace.data.models.location.ApiLocation
import com.canopas.yourspace.data.models.user.ApiUser
import com.canopas.yourspace.data.models.user.UserInfo
import com.canopas.yourspace.domain.utils.getAddress
import com.canopas.yourspace.domain.utils.timeAgo
import com.canopas.yourspace.ui.component.ActionIconButton
import com.canopas.yourspace.ui.component.UserBatteryStatus
import com.canopas.yourspace.ui.flow.home.map.MapScreenState
import com.canopas.yourspace.ui.flow.home.map.MapViewModel
import com.canopas.yourspace.ui.theme.AppTheme
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun SelectedUserDetail(
    userInfo: UserInfo?,
    onDismiss: () -> Unit,
    onTapTimeline: () -> Unit,
    currentUser: ApiUser?
) {
    if (userInfo?.user == null) return
    val user = userInfo.user

    val isCurrentUser = user.id == (currentUser?.id ?: "")
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(color = AppTheme.colorScheme.surface)
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            MemberProfileView(user.profile_image, user.firstChar, userInfo.user)

            MemberInfoView(user = user, userInfo.location, isCurrentUser) { onTapTimeline() }
        }

        Row(
            modifier = Modifier
                .wrapContentSize()
                .clip(CircleShape)
                .background(color = AppTheme.colorScheme.surface)
                .clickable { onDismiss() }
                .align(Alignment.TopCenter)
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_drop_down_icon),
                contentDescription = "",
                tint = AppTheme.colorScheme.textDisabled
            )
        }
    }
}

@Composable
private fun MemberProfileView(profileUrl: String?, name: String, user: ApiUser?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(color = AppTheme.colorScheme.primary, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (profileUrl.isNullOrEmpty()) {
                Text(
                    name,
                    style = AppTheme.appTypography.header4.copy(color = AppTheme.colorScheme.textInversePrimary)
                )
            } else {
                val painter = rememberAsyncImagePainter(
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
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
        if (user != null) {
            Spacer(modifier = Modifier.padding(top = 2.dp))
            UserBatteryStatus(user = user)
        }
    }
}

@Composable
private fun MemberInfoView(
    user: ApiUser,
    location: ApiLocation?,
    isCurrentUser: Boolean,
    onTapTimeline: () -> Unit
) {
    val context = LocalContext.current
    val viewModel = hiltViewModel<MapViewModel>()
    val state by viewModel.state.collectAsState()

    var address by remember { mutableStateOf("") }
    val time = timeAgo(location?.created_at ?: 0)

    val userStateText = if (!state.batterySaveModeValue) {
        if (user.noNetwork) {
            stringResource(R.string.map_selected_user_item_no_network_state)
        } else if (user.locationPermissionDenied) {
            stringResource(R.string.map_selected_user_item_location_off_state)
        } else {
            stringResource(R.string.map_selected_user_item_online_state)
        }
    } else {
        stringResource(R.string.battery_saver_text)
    }
    val userStateTextColor = if (!state.batterySaveModeValue) {
        if (user.noNetwork) {
            AppTheme.colorScheme.textSecondary
        } else if (user.locationPermissionDenied) {
            AppTheme.colorScheme.alertColor
        } else {
            AppTheme.colorScheme.successColor
        }
    } else {
        AppTheme.colorScheme.alertColor
    }

    LaunchedEffect(location) {
        if (location == null) {
            address = ""; return@LaunchedEffect
        }

        withContext(Dispatchers.IO) {
            val latLng = LatLng(location.latitude, location.longitude)
            delay(500)
            address = latLng.getAddress(context) ?: ""
        }
    }

    Column(modifier = Modifier.padding(start = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(
                    text = user.fullName,
                    style = AppTheme.appTypography.subTitle1.copy(color = AppTheme.colorScheme.textPrimary),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = userStateText,
                    style = AppTheme.appTypography.caption.copy(color = userStateTextColor)
                )
            }

            Spacer(modifier = Modifier.weight(1f))
            ActionIconButton(
                modifier = Modifier,
                iconSize = 24.dp,
                icon = R.drawable.ic_timeline,
                onClick = onTapTimeline
            )
            ActionIconButton(
                modifier = Modifier,
                iconSize = 20.dp,
                icon = if (isCurrentUser) R.drawable.ic_share else R.drawable.ic_navigation,
                onClick = {
                    if (location == null) return@ActionIconButton
                    if (isCurrentUser) {
                        shareLocation(
                            context,
                            LatLng(location.latitude, location.longitude),
                            state
                        )
                    } else {
                        openNavigation(
                            context,
                            LatLng(location.latitude, location.longitude),
                            state
                        )
                    }
                }
            )
        }
        Text(
            text = address,
            style = AppTheme.appTypography.body2.copy(color = AppTheme.colorScheme.textPrimary),
            overflow = TextOverflow.Ellipsis,
            maxLines = 2,
            modifier = Modifier.padding(top = 12.dp)
        )

        if (time.isNotEmpty()) {
            Row(modifier = Modifier.padding(top = 4.dp)) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_access_time),
                    contentDescription = "",
                    tint = AppTheme.colorScheme.textDisabled,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = time,
                    style = AppTheme.appTypography.caption.copy(color = AppTheme.colorScheme.textDisabled)
                )
            }
        }
    }
}

fun shareLocation(context: Context, location: LatLng, state: MapScreenState): Boolean {
    if (location.latitude < -90 || location.latitude > 90 ||
        location.longitude < -180 || location.longitude > 180
    ) {
        state.errorMessage = context.getString(R.string.toast_invalid_coordinates)
        return false
    }

    val mapsLink =
        "https://www.google.com/maps/dir/?api=1&destination=${location.latitude},${location.longitude}"
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, "Check out my location: $mapsLink")
    }
    return try {
        context.startActivity(Intent.createChooser(intent, "Share Location"))
        true
    } catch (e: Exception) {
        e.printStackTrace()
        state.errorMessage = context.getString(R.string.toast_failed_share_location)
        false
    }
}

fun openNavigation(context: Context, destination: LatLng, state: MapScreenState): Boolean {
    if (destination.latitude < -90 || destination.latitude > 90 ||
        destination.longitude < -180 || destination.longitude > 180
    ) {
        state.errorMessage = context.getString(R.string.toast_invalid_coordinates)
        return false
    }

    val gmmIntentUri =
        Uri.parse("https://maps.google.com/maps?daddr=${destination.latitude},${destination.longitude}")
    val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
        setPackage("com.google.android.apps.maps")
    }
    return try {
        if (mapIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(mapIntent)
            true
        } else {
            // If Google Maps is not installed, open in browser
            val browserIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
            context.startActivity(browserIntent)
            true
        }
    } catch (e: Exception) {
        e.printStackTrace()
        state.errorMessage = context.getString(R.string.toast_failed_open_navigation)
        false
    }
}
