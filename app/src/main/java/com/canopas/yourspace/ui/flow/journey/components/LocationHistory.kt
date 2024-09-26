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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.canopas.yourspace.domain.utils.isToday
import com.canopas.yourspace.ui.component.DashedDivider
import com.canopas.yourspace.ui.theme.AppTheme
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun LocationHistoryItem(
    location: LocationJourney,
    isFirstItem: Boolean = false,
    isLastItem: Boolean,
    addPlaceTap: (latitude: Double, longitude: Double) -> Unit,
    showJourneyDetails: () -> Unit
) {
    if (location.isSteadyLocation()) {
        SteadyLocationItem(location, isFirstItem, isLastItem) {
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
        modifier = Modifier.height(210.dp)
    ) {
        DottedTimeline(false, isLastItem = lastItem)
        Column(
            modifier = Modifier
                .padding(start = 16.dp)
                .weight(1f)
        ) {
            val time = getFormattedJourneyTime(location.created_at ?: 0, location.update_at ?: 0)
            val distance = getDistanceString(location.route_distance ?: 0.0)

            PlaceInfo(title, "$time - $distance")
            Spacer(modifier = Modifier.size(16.dp))

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
fun SteadyLocationItem(
    location: LocationJourney,
    isFirstItem: Boolean,
    isLastItem: Boolean,
    addPlace: () -> Unit
) {
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
        modifier = Modifier.height(140.dp)
    ) {
        DottedTimeline(isSteadyLocation = true, isLastItem = isLastItem)

        Column(
            modifier = Modifier
                .padding(start = 16.dp)
                .weight(1f)
        ) {
            val formattedTime =
                if (isFirstItem) {
                    getFormattedLocationTimeForFirstItem(location.created_at!!)
                } else {
                    getFormattedLocationTime(location.created_at!!)
                }

            PlaceInfo(fromAddress, formattedTime)

            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = addPlace,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppTheme.colorScheme.containerLow,
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

            Spacer(modifier = Modifier.height(8.dp))

            if (!isLastItem) {
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
    Spacer(modifier = Modifier.size(8.dp))

    Text(
        text = formattedTime,
        style = AppTheme.appTypography.caption.copy(color = AppTheme.colorScheme.textDisabled)
    )
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
                        AppTheme.colorScheme.containerLow
                    } else {
                        AppTheme.colorScheme.surface
                    },
                    shape = CircleShape
                )
                .border(
                    1.dp,
                    if (isSteadyLocation) Color.Transparent else AppTheme.colorScheme.outline,
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSteadyLocation) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = "",
                    tint = AppTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(AppTheme.colorScheme.primary, CircleShape)
                )
            }
        }

        if (!isLastItem) {
            DashedDivider(
                thickness = 1.dp,
                color = AppTheme.colorScheme.textPrimary,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun EmptyHistory() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_empty_location_history),
            contentDescription = "",
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            text = stringResource(id = R.string.member_detail_empty_location_history),
            style = AppTheme.appTypography.subTitle3.copy(color = AppTheme.colorScheme.textDisabled),
            modifier = Modifier.padding(bottom = 30.dp)
        )
    }
}

internal fun getFormattedJourneyTime(startAt: Long, endsAt: Long): String {
    val inputFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    val dateFormatter = SimpleDateFormat("d MMMM", Locale.getDefault())

    val formattedDate1 = dateFormatter.format(startAt)
    val formattedDate2 = dateFormatter.format(endsAt)

    return if (formattedDate1 == formattedDate2) {
        "${getFormattedLocationTime(startAt)} - ${inputFormat.format(endsAt)}"
    } else {
        "${getFormattedLocationTime(startAt)} - ${getFormattedLocationTime(endsAt)}"
    }
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

internal fun getFormattedLocationTime(createdAt: Long): String {
    if (createdAt.isToday()) {
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return "Today ${sdf.format(createdAt)}"
    } else {
        val sdf = SimpleDateFormat("dd MMMM hh:mm a", Locale.getDefault())
        return sdf.format(createdAt)
    }
}

internal fun getFormattedLocationTimeForFirstItem(createdAt: Long): String {
    val sdf = if (createdAt.isToday()) {
        SimpleDateFormat("hh:mm a", Locale.getDefault())
    } else {
        SimpleDateFormat("dd MMMM hh:mm a", Locale.getDefault())
    }

    return "Since ${sdf.format(createdAt)}"
}

fun Address.formattedTitle(toAddress: Address?): String {
    val fromCity = this.locality ?: this.subAdminArea ?: this.adminArea ?: this.featureName ?: "Unknown"
    val toCity = toAddress?.locality ?: toAddress?.subAdminArea ?: toAddress?.adminArea ?: toAddress?.featureName ?: "Unknown"

    val fromArea = this.subLocality ?: this.thoroughfare ?: this.featureName ?: fromCity
    val toArea = toAddress?.subLocality ?: toAddress?.thoroughfare ?: toAddress?.featureName ?: toCity

    val fromState = this.adminArea ?: this.countryName ?: "Unknown"
    val toState = toAddress?.adminArea ?: toAddress?.countryName ?: "Unknown"

    return when {
        toAddress == null -> "$fromArea, $fromCity"
        fromArea == toArea -> "$fromArea, $fromCity"
        fromCity == toCity -> "$fromArea to $toArea, $fromCity"
        fromState == toState -> "$fromArea, $fromCity to $toArea, $toCity"
        else -> "$fromCity, $fromState to $toCity, $toState"
    }
}
