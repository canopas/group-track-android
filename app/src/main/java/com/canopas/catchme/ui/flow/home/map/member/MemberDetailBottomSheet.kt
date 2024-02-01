package com.canopas.catchme.ui.flow.home.map.member

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.canopas.catchme.R
import com.canopas.catchme.data.models.location.ApiLocation
import com.canopas.catchme.data.models.user.UserInfo
import com.canopas.catchme.ui.component.UserProfile
import com.canopas.catchme.ui.theme.AppTheme
import com.canopas.catchme.utils.getAddress
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit
import kotlin.math.abs

@Composable
fun MemberDetailBottomSheetContent(
    userInfo: UserInfo,
) {
    val viewModel = hiltViewModel<MemberDetailViewModel>()
    val state by viewModel.state.collectAsState()

    LaunchedEffect(userInfo) {
        Timber.d("XXX fetchUserLocationHistory")
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.HOUR_OF_DAY, -24)
        val timestamp = calendar.timeInMillis
        viewModel.fetchUserLocationHistory(userInfo, timestamp)
    }

    Column(modifier = Modifier.fillMaxHeight(0.9f)) {
        UserInfoContent(userInfo)
        Spacer(modifier = Modifier.height(16.dp))
        Divider(thickness = 1.dp, color = AppTheme.colorScheme.outline)
        Spacer(modifier = Modifier.height(10.dp))

        FilterOption {}

        LocationHistory(state.location)

    }

}

@Composable
fun LocationHistory(
    locations: List<ApiLocation>
) {
    LazyColumn(contentPadding = PaddingValues(bottom = 30.dp)) {
        itemsIndexed(locations) { index, location ->
            LocationHistoryItem(location, index, isLastItem = index == locations.lastIndex)
        }
    }
}

@Composable
private fun LocationHistoryItem(location: ApiLocation, index: Int, isLastItem: Boolean) {
    val context = LocalContext.current
    var address by remember { mutableStateOf("") }
    LaunchedEffect(location) {
        withContext(Dispatchers.IO) {
            val latLng =
                LatLng(location.latitude, location.longitude)
            address = latLng.getAddress(context) ?: ""
        }
    }
    val lastUpdated = getFormattedTimeString(context, location.created_at ?: 0L)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxHeight()
                .padding(top = 10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        color = if (index == 0) AppTheme.colorScheme.primary.copy(alpha = 0.5f) else AppTheme.colorScheme.containerHigh,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = "",
                    tint = AppTheme.colorScheme.surface,
                    modifier = Modifier
                        .size(24.dp)
                        .padding(2.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            if (!isLastItem)
                Divider(
                    thickness = 2.dp,
                    modifier = Modifier
                        .width(2.dp)
                        .height(48.dp),
                    color = AppTheme.colorScheme.containerHigh
                )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp)
        ) {
            Text(
                text = address,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = AppTheme.appTypography.subTitle2.copy(color = AppTheme.colorScheme.textPrimary)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    painterResource(id = R.drawable.ic_access_time),
                    contentDescription = "",
                    tint = AppTheme.colorScheme.textSecondary,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = lastUpdated,
                    style = AppTheme.appTypography.label2.copy(color = AppTheme.colorScheme.textSecondary)
                )
            }

        }
    }

}

@Composable
fun FilterOption(onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 6.dp)
            .padding(bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Text(
            text = stringResource(id = R.string.member_detail_location_history),
            style = AppTheme.appTypography.body2.copy(color = AppTheme.colorScheme.textSecondary)
        )
        Spacer(modifier = Modifier.weight(1f))
        TextButton(onClick = onClick) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Today",
                    style = AppTheme.appTypography.body2.copy(color = AppTheme.colorScheme.textSecondary),
                    modifier = Modifier.padding(end = 5.dp)
                )
                Icon(
                    painter = painterResource(id = R.drawable.ic_filter_by),
                    contentDescription = "",
                    tint = AppTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(28.dp)
                )
            }
        }

    }

}

@Composable
fun UserInfoContent(userInfo: UserInfo) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        UserProfile(modifier = Modifier.size(54.dp), user = userInfo.user)
        Text(
            text = userInfo.user.fullName,
            modifier = Modifier
                .padding(start = 16.dp)
                .weight(1f),
            style = AppTheme.appTypography.header3,
            maxLines = 1,
        )

        Box(
            modifier = Modifier
                .size(38.dp)
                .background(
                    color = AppTheme.colorScheme.containerNormal,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_messages),
                contentDescription = "",
                tint = AppTheme.colorScheme.textSecondary,
                modifier = Modifier
                    .size(24.dp)
                    .padding(2.dp)
            )
        }
    }

}


private fun getFormattedTimeString(context: Context, timestamp: Long): String {
    val now = System.currentTimeMillis()
    val duration = abs(timestamp - now)
    val days = TimeUnit.MILLISECONDS.toDays(duration)
    val hours = TimeUnit.MILLISECONDS.toHours(duration)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(duration)

    return when {
        minutes < 1 -> context.getString(R.string.map_user_item_location_updated_now)
        hours < 1 -> context.getString(
            R.string.map_user_item_location_updated_minutes_ago,
            minutes.toString()
        )

        else -> {
            val output = SimpleDateFormat("h:mm a")
            output.format(Date(timestamp))
        }
    }
}

