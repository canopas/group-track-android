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
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.LocationOn
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.canopas.yourspace.R
import com.canopas.yourspace.data.models.location.ApiLocation
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
import com.google.maps.android.compose.MarkerComposable
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberMarkerState
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

    Box(modifier = Modifier.fillMaxSize()) {
        MapView(viewModel = viewModel)

        HorizontalDatePicker(viewModel)

        ExpandableProfileImage(viewModel)

        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AppTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun MapView(viewModel: UserJourneyViewModel) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val isDarkMode = isSystemInDarkTheme()
    val mapProperties = MapProperties(
        mapStyleOptions = if (isDarkMode) {
            MapStyleOptions.loadRawResourceStyle(context, R.raw.map_theme_night)
        } else {
            null
        }
    )
    GoogleMap(
        cameraPositionState = CameraPositionState(
            CameraPosition.Builder()
                .target(
                    if (state.journeyWithLocation != null) {
                        LatLng(state.journeyWithLocation?.journey!!.from_latitude, state.journeyWithLocation?.journey!!.from_longitude)
                    } else if (state.journeyWithLocations.isNotEmpty()) {
                        LatLng(
                            state.journeyWithLocations.first().journey.from_latitude,
                            state.journeyWithLocations.first().journey.from_longitude
                        )
                    } else {
                        LatLng(
                            state.currentLocation?.latitude ?: 0.0,
                            state.currentLocation?.longitude ?: 0.0
                        )
                    }
                )
                .zoom(15f)
                .build()
        ),
        properties = mapProperties
    ) {
        state.journeyWithLocation?.let { journey ->
            DrawJourney(context, journey.journey, journey.locationsList)
        } ?: state.journeyWithLocations.forEach { journey ->
            DrawJourney(context, journey.journey, journey.locationsList)
        }
    }
}

@Composable
fun HorizontalDatePicker(viewModel: UserJourneyViewModel) {
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
        ArrowButton(
            icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
            enabled = state.journeyId == null
        ) {
            updateSelectedDate(-1)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 20.dp)
                .background(
                    color = AppTheme.colorScheme.iconsBackground.copy(
                        alpha = if (state.journeyId == null) 1f else 0.5f
                    ),
                    shape = MaterialTheme.shapes.small
                )
                .clickable {
                    if (state.journeyId == null) {
                        showDatePicker = true
                    }
                }
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

        ArrowButton(
            icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            enabled = state.selectedTimeTo < System.currentTimeMillis() && state.journeyId == null
        ) {
            updateSelectedDate(1)
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
    journey: LocationJourney,
    locationsList: List<ApiLocation>
) {
    var address by remember { mutableStateOf("") }
    var toAddress by remember { mutableStateOf("") }
    var fromExpandState by remember { mutableStateOf(false) }
    var toExpandState by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = journey) {
        withContext(Dispatchers.IO) {
            val fromLatLng = LatLng(journey.from_latitude, journey.from_longitude)
            address = fromLatLng.getAddress(context) ?: ""
            if (!journey.isSteadyLocation()) {
                val toLatLng = LatLng(journey.to_latitude ?: 0.0, journey.to_longitude ?: 0.0)
                toAddress = toLatLng.getAddress(context) ?: ""
            }
        }
    }

    if (journey.isSteadyLocation()) {
        val markerState = MarkerState(
            position = LatLng(journey.from_latitude, journey.from_longitude)
        )
        MarkerComposable(
            keys = arrayOf(fromExpandState, address),
            state = markerState,
            zIndex = if (fromExpandState) 1f else 0f,
            onClick = {
                fromExpandState = !fromExpandState
                true
            }
        ) {
            MarkerInfoText(address = address, time = journey.created_at!!, fromExpandState)
        }
    } else {
        val fromMarkerState = rememberMarkerState(
            key = address,
            position = LatLng(journey.from_latitude, journey.from_longitude)
        )
        val toMarkerState = rememberMarkerState(
            key = toAddress,
            position = LatLng(journey.to_latitude ?: 0.0, journey.to_longitude ?: 0.0)
        )
        MarkerComposable(
            keys = arrayOf(fromExpandState, address),
            state = fromMarkerState,
            content = {
                MarkerInfoText(address = address, time = journey.created_at!!, expanded = fromExpandState)
            },
            onClick = {
                fromExpandState = !fromExpandState
                true
            },
            tag = address
        )
        MarkerComposable(
            keys = arrayOf(toExpandState, toAddress),
            state = toMarkerState,
            content = {
                MarkerInfoText(address = toAddress, time = journey.created_at!! + (journey.route_duration ?: 0), expanded = toExpandState)
            },
            onClick = {
                toExpandState = !toExpandState
                true
            },
            tag = toAddress
        )
        Polyline(
            points = mutableListOf(
                LatLng(journey.to_latitude ?: journey.from_latitude, journey.to_longitude ?: journey.from_longitude)
            ) + locationsList.map {
                LatLng(it.latitude, it.longitude)
            } + mutableListOf(
                LatLng(journey.from_latitude, journey.from_longitude)
            ),
            jointType = JointType.BEVEL
        )
    }
}

@Composable
fun ArrowButton(icon: ImageVector, enabled: Boolean, onClick: () -> Unit) {
    val backgroundAlpha = if (enabled) 1f else 0.5f
    Box(
        modifier = Modifier
            .wrapContentWidth()
            .fillMaxHeight()
            .background(
                color = AppTheme.colorScheme.iconsBackground.copy(alpha = backgroundAlpha),
                shape = MaterialTheme.shapes.small
            )
            .clickable(enabled = enabled) { onClick() },
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
fun MarkerInfoText(address: String, time: Long, expanded: Boolean = false) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .wrapContentWidth()
                .background(AppTheme.colorScheme.textPrimary, shape = MaterialTheme.shapes.small)
                .align(Alignment.CenterHorizontally)
        ) {
            Row(
                modifier = Modifier.padding(4.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_pin_icon),
                    contentDescription = "",
                    tint = AppTheme.colorScheme.textInversePrimary,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 16.dp).size(24.dp)
                )
                Text(
                    text = if (expanded) address else address.take(20),
                    color = AppTheme.colorScheme.textInversePrimary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            }
            if (expanded) {
                Text(
                    text = getFormattedTime(time),
                    color = AppTheme.colorScheme.textInversePrimary,
                    modifier = Modifier.padding(start = 16.dp, bottom = 16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = "Location Icon",
            tint = AppTheme.colorScheme.locationMarker,
            modifier = Modifier
                .size(40.dp)
                .align(Alignment.CenterHorizontally)
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
