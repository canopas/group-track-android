package com.canopas.yourspace.ui.flow.home.map.journeyview

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.canopas.yourspace.R
import com.canopas.yourspace.data.models.location.LocationJourney
import com.canopas.yourspace.data.models.location.isSteadyLocation
import com.canopas.yourspace.domain.utils.getAddress
import com.canopas.yourspace.ui.component.ShowDatePicker
import com.canopas.yourspace.ui.component.WavesAnimation
import com.canopas.yourspace.ui.theme.AppTheme
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.JointType
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MarkerInfoWindow
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun UserJourneyView() {
    val viewModel = hiltViewModel<UserJourneyViewModel>()
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val isDarkMode = isSystemInDarkTheme()

    LaunchedEffect(key1 = Unit) {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = state.selectedTimeFrom
            set(Calendar.HOUR_OF_DAY, 0)
        }
        val startTimeStamp = calendar.timeInMillis
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        viewModel.onDateSelected(calendar.timeInMillis, startTimeStamp)
    }

    val mapProperties = MapProperties(
        mapStyleOptions = if (isDarkMode) {
            MapStyleOptions.loadRawResourceStyle(context, R.raw.map_theme_night)
        } else {
            null
        }
    )

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            cameraPositionState = CameraPositionState(
                CameraPosition.Builder()
                    .target(
                        if (state.journey != null) {
                            LatLng(state.journey!!.from_latitude, state.journey!!.from_longitude)
                        } else if (state.journeyList.isNotEmpty()) {
                            LatLng(
                                state.journeyList.first().from_latitude,
                                state.journeyList.first().from_longitude
                            )
                        } else {
                            LatLng(0.0, 0.0)
                        }
                    )
                    .zoom(13f)
                    .build()
            ),
            properties = mapProperties
        ) {
            state.journey?.let { journey ->
                DrawJourney(context, journey)
            } ?: state.journeyList.forEach { journey ->
                DrawJourney(context, journey)
            }
        }

        HorizontalDatePicker()

        ExpandableProfileImage(viewModel)

        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AppTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun ExpandableProfileImage(viewModel: UserJourneyViewModel) {
    val state by viewModel.state.collectAsState()
    var showUserDetails by remember {
        mutableStateOf(false)
    }
    state.user?.let {
        val painter = rememberAsyncImagePainter(
            ImageRequest.Builder(LocalContext.current).data(
                it.profile_image
            ).placeholder(R.drawable.ic_user_profile_placeholder).build()
        )
        WavesAnimation {
            ExtendedFloatingActionButton(
                text = {
                    Text(text = it.fullName)
                },
                icon = {
                    Image(
                        painter = painter,
                        contentDescription = "Profile Image",
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .clickable {
                                showUserDetails = !showUserDetails
                            }
                    )
                },
                modifier = Modifier.clip(CircleShape),
                onClick = {
                    showUserDetails = !showUserDetails
                },
                expanded = showUserDetails
            )
        }
    }
}

@Composable
private fun DrawJourney(
    context: Context,
    journey: LocationJourney
) {
    var address by remember { mutableStateOf("") }
    var toAddress by remember { mutableStateOf("") }

    LaunchedEffect(key1 = journey) {
        withContext(Dispatchers.IO) {
            val fromLatLng = LatLng(journey.from_latitude, journey.from_longitude)
            address = fromLatLng.getAddress(context) ?: ""
            if (!journey.isSteadyLocation()) {
                val toLatLng = LatLng(journey.to_latitude!!, journey.to_longitude!!)
                toAddress = toLatLng.getAddress(context) ?: ""
            }
        }
    }

    if (journey.isSteadyLocation()) {
        val markerState =
            MarkerState(position = LatLng(journey.from_latitude, journey.from_longitude))
        MarkerInfoWindow(state = markerState) {
            MarkerInfoText(address = address, time = journey.created_at!!)
        }
    } else {
        val fromMarkerState =
            MarkerState(position = LatLng(journey.from_latitude, journey.from_longitude))
        val toMarkerState =
            MarkerState(position = LatLng(journey.to_latitude!!, journey.to_longitude!!))
        MarkerInfoWindow(state = fromMarkerState) {
            MarkerInfoText(address = address, time = journey.created_at!!)
        }
        MarkerInfoWindow(state = toMarkerState) {
            MarkerInfoText(address = toAddress, time = journey.created_at!!)
        }
        Polyline(
            points = listOf(
                LatLng(journey.from_latitude, journey.from_longitude),
                LatLng(journey.to_latitude!!, journey.to_longitude!!)
            ),
            jointType = JointType.BEVEL
        )
    }
}

@Composable
fun HorizontalDatePicker() {
    val viewModel = hiltViewModel<UserJourneyViewModel>()
    val state by viewModel.state.collectAsState()
    var showDatePicker by remember { mutableStateOf(false) }

    fun updateSelectedDate(deltaDays: Int, timestamp: Long? = null) {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = if (deltaDays == 0) state.selectedTimeFrom else state.selectedTimeTo
            if (timestamp != null) {
                timeInMillis = timestamp
            }
            add(Calendar.DAY_OF_MONTH, deltaDays)
            set(Calendar.HOUR_OF_DAY, 0)
        }
        val startTimeStamp = calendar.timeInMillis
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        viewModel.onDateSelected(calendar.timeInMillis, startTimeStamp)
    }

    LaunchedEffect(key1 = Unit) {
        updateSelectedDate(0)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        ArrowButton(icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft) {
            updateSelectedDate(-1)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 20.dp)
                .background(
                    color = AppTheme.colorScheme.iconsBackground,
                    shape = MaterialTheme.shapes.small
                )
                .clickable { showDatePicker = true }
        ) {
            Text(
                text = getFormattedJourneyDate(
                    state.selectedTimeFrom,
                    state.selectedTimeTo,
                    context = LocalContext.current
                ),
                color = AppTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
                textAlign = TextAlign.Center
            )
        }

        ArrowButton(icon = Icons.AutoMirrored.Filled.KeyboardArrowRight) {
            if (state.selectedTimeTo < System.currentTimeMillis()) {
                updateSelectedDate(1)
            }
        }
    }

    if (showDatePicker) {
        ShowDatePicker(
            confirmButtonClick = { timestamp ->
                showDatePicker = false
                updateSelectedDate(0, timestamp)
            },
            dismissButtonClick = { showDatePicker = false }
        )
    }
}

@Composable
fun ArrowButton(icon: ImageVector, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .wrapContentWidth()
            .fillMaxHeight()
            .background(
                color = AppTheme.colorScheme.iconsBackground,
                shape = MaterialTheme.shapes.small
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "Arrow",
            tint = AppTheme.colorScheme.onPrimary,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}

@Composable
fun MarkerInfoText(address: String, time: Long) {
    Column(
        modifier = Modifier
            .wrapContentWidth()
            .padding(horizontal = 16.dp)
            .background(AppTheme.colorScheme.markerInfoWindow, shape = MaterialTheme.shapes.small)
    ) {
        Text(
            text = address,
            color = AppTheme.colorScheme.textPrimary,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(8.dp)
        )
        Text(
            text = getFormattedTime(time),
            color = AppTheme.colorScheme.textSecondary,
            modifier = Modifier.padding(8.dp)
        )
    }
}

fun getFormattedJourneyDate(startTimestamp: Long, endTimeStamp: Long, context: Context): String {
    val startDate = Date(startTimestamp)
    val startDateFormat = SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault())
    return if (System.currentTimeMillis() in startTimestamp until endTimeStamp) {
        context.getString(R.string.user_journey_view_today_text)
    } else {
        startDateFormat.format(startDate)
    }
}

fun getFormattedTime(startTimestamp: Long): String {
    val startDate = Date(startTimestamp)
    val startDateFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    return startDateFormat.format(startDate)
}
