package com.canopas.yourspace.ui.flow.journey.components

import android.location.Address
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.canopas.yourspace.R
import com.canopas.yourspace.data.models.location.LocationJourney
import com.canopas.yourspace.data.models.location.isSteadyLocation
import com.canopas.yourspace.domain.utils.getAddress
import com.canopas.yourspace.domain.utils.getPlaceAddress
import com.canopas.yourspace.ui.component.AppProgressIndicator
import com.canopas.yourspace.ui.component.DashedDivider
import com.canopas.yourspace.ui.theme.AppTheme
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

@Composable
fun LocationHistory(
    isLoading: Boolean,
    locations: List<LocationJourney>,
    addPlaceTap: (latitute: Double, longitude: Double) -> Unit,
    showJourneyDetails: (journeyId: String) -> Unit
) {
    val lazyListState = rememberLazyListState()

    Box {
        when {
            locations.isEmpty() && isLoading -> {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) { AppProgressIndicator() }
            }

            locations.isEmpty() -> {
                EmptyHistory()
            }

            else -> {
                LazyColumn(state = lazyListState) {
                    itemsIndexed(
                        locations,
                        key = { index, location -> location.id }
                    ) { index, location ->
                        LocationHistoryItem(
                            location,
                            isLastItem = index == locations.lastIndex,
                            addPlaceTap = addPlaceTap,
                            showJourneyDetails = {
                                showJourneyDetails(location.id)
                            }
                        )
                    }

                    if (locations.isNotEmpty() && isLoading) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) { AppProgressIndicator() }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LocationHistoryItem(
    location: LocationJourney,
    isLastItem: Boolean,
    addPlaceTap: (latitude: Double, longitude: Double) -> Unit,
    showJourneyDetails: () -> Unit
) {
    if (location.isSteadyLocation()) {
        SteadyLocationItem(location, isLastItem) {
            addPlaceTap(location.from_latitude, location.from_longitude)
        }
    } else {
        JourneyLocationItem(location, isLastItem, showJourneyDetails)
    }
}

@Composable
fun JourneyLocationItem(location: LocationJourney, lastItem: Boolean, onTap: () -> Unit) {
    val context = LocalContext.current
    var fromAddress by remember { mutableStateOf<Address?>(null) }
    var toAddress by remember { mutableStateOf<Address?>(null) }

    LaunchedEffect(location) {
        withContext(Dispatchers.IO) {
            val latLng =
                LatLng(location.from_latitude, location.from_longitude)
            val toLatLng = LatLng(location.to_latitude ?: 0.0, location.to_longitude ?: 0.0)
            fromAddress = latLng.getPlaceAddress(context)
            toAddress = toLatLng.getPlaceAddress(context)
        }
    }

    val title = fromAddress?.formattedTitle(toAddress) ?: ""

    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .height(210.dp)
    ) {
        DottedTimeline(false, isLastItem = lastItem)
        Column(
            modifier = Modifier
                .padding(start = 16.dp)
                .weight(1f)
        ) {
            val time = getFormattedLocationTime(location.created_at ?: 0, location.update_at ?: 0)
            val distance = getDistanceString(location.route_distance ?: 0.0)
            PlaceInfo(
                title,
                "$time - $distance"
            )
            JourneyMap(
                modifier = Modifier
                    .padding(end = 16.dp)
                    .height(125.dp)
                    .clip(shape = RoundedCornerShape(8.dp)),
                location,
                false,
                fromMarkerContent = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_tab_places_filled),
                        contentDescription = null,
                        tint = AppTheme.colorScheme.primary,
                        modifier = Modifier.size(30.dp)
                    )
                },
                toMarkerContent = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_journey_destination),
                        contentDescription = null,
                        tint = AppTheme.colorScheme.alertColor,
                        modifier = Modifier.size(28.dp)
                    )
                },
                onMapTap = onTap
            )
        }
    }
}

@Composable
fun SteadyLocationItem(location: LocationJourney, lastItem: Boolean, addPlace: () -> Unit) {
    val context = LocalContext.current
    var fromAddress by remember { mutableStateOf("") }

    LaunchedEffect(location) {
        withContext(Dispatchers.IO) {
            val latLng =
                LatLng(location.from_latitude, location.from_longitude)
            fromAddress = latLng.getAddress(context) ?: ""
        }
    }

    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .height(140.dp)
    ) {
        DottedTimeline(isSteadyLocation = true, isLastItem = lastItem)

        Column(
            modifier = Modifier
                .padding(start = 16.dp)
                .weight(1f)
        ) {
            PlaceInfo(fromAddress, getFormattedCreatedAt(location.created_at!!))

            Button(
                onClick = addPlace,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppTheme.colorScheme.primary.copy(0.1f),
                    contentColor = AppTheme.colorScheme.primary
                )
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_geofence),
                    contentDescription = null,
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .size(24.dp)

                )
                Text(
                    text = stringResource(id = R.string.common_btn_add_place),
                    style = AppTheme.appTypography.body2
                )
            }

            Spacer(modifier = Modifier.size(8.dp))

            if (!lastItem) {
                HorizontalDivider(thickness = 1.dp, color = AppTheme.colorScheme.outline)
            }
        }
    }
}

@Composable
internal fun PlaceInfo(title: String, formattedTime: String) {
    Text(
        text = title,
        style = AppTheme.appTypography.body2.copy(
            color = AppTheme.colorScheme.textPrimary,
            fontWeight = FontWeight.Medium
        ),
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(end = 16.dp)
    )
    Spacer(modifier = Modifier.size(6.dp))

    Text(
        text = formattedTime,
        style = AppTheme.appTypography.caption.copy(
            fontWeight = FontWeight.Medium,
            color = AppTheme.colorScheme.textSecondary
        )
    )
    Spacer(modifier = Modifier.size(10.dp))
}

@Composable
fun DottedTimeline(isSteadyLocation: Boolean, isLastItem: Boolean) {
    Column(
        modifier = Modifier
            .padding(start = 16.dp)
            .fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    if (isSteadyLocation) {
                        AppTheme.colorScheme.containerNormal
                    } else {
                        AppTheme.colorScheme.surface
                    },
                    shape = CircleShape
                )
                .border(
                    1.dp,
                    if (isSteadyLocation) Color.Transparent else AppTheme.colorScheme.containerNormal,
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSteadyLocation) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = "",
                    tint = AppTheme.colorScheme.primary,
                    modifier = Modifier
                        .size(24.dp)
                        .padding(2.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            AppTheme.colorScheme.primary,
                            CircleShape
                        )
                )
            }
        }

        if (!isLastItem) DashedDivider(thickness = 1.dp, modifier = Modifier.weight(1f))
    }
}

@Composable
fun EmptyHistory() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_empty_location_history),
            contentDescription = "",
            modifier = Modifier
                .padding(bottom = 30.dp)
                .alpha(0.8f)
        )

        Text(
            text = stringResource(id = R.string.member_detail_empty_location_history),
            style = AppTheme.appTypography.body1.copy(
                color = AppTheme.colorScheme.containerHigh.copy(
                    alpha = 0.8f
                )
            ),
            modifier = Modifier.padding(bottom = 30.dp)
        )
    }
}

internal fun getFormattedLocationTime(timestamp1: Long, timestamp2: Long): String {
    val inputFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

    val timeString1 = inputFormat.format(Date(timestamp1))
    val timeString2 = inputFormat.format(Date(timestamp2))

    return "$timeString1 - $timeString2"
}

internal fun getDistanceString(
    routeDistance: Double
): String {
    return if (routeDistance < 1000) {
        "${routeDistance.roundToInt()} m"
    } else {
        val distanceInKm = (routeDistance / 1000)
        "${distanceInKm.roundToInt()} km"
    }
}

internal fun getRouteDurationString(
    routeDuration: Long
): String {
    val hours = TimeUnit.MILLISECONDS.toHours(routeDuration)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(routeDuration) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(routeDuration) % 60
    return when {
        hours > 0 -> {
            "$hours hr $minutes mins"
        }

        minutes > 0 -> {
            "$minutes mins"
        }

        else -> {
            "$seconds sec"
        }
    }
}

internal fun getFormattedCreatedAt(createdAt: Long): String {
    val createdAtTime = Date(createdAt)
    val createdAtFormat = SimpleDateFormat("d MMM HH:mm", Locale.getDefault())
    return createdAtFormat.format(createdAtTime)
}

fun Address.formattedTitle(toAddress: Address?): String {
    val fromCity = this.locality
    val toCity = toAddress?.locality ?: ""

    val fromArea = this.subLocality
    val toArea = toAddress?.subLocality ?: ""

    val fromState = this.adminArea
    val toState = toAddress?.adminArea ?: ""

    return when {
        toAddress == null -> "$fromArea, $fromCity"
        fromArea == toArea -> "$fromArea, $fromCity"
        fromCity == toCity -> "$fromArea to $toArea, $fromCity"
        fromState == toState -> "$fromArea, $fromCity to $toArea, $toCity"
        else -> "$fromCity, $fromState to $toCity, $toState"
    }
}
