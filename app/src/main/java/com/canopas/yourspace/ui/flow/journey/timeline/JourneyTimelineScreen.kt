package com.canopas.yourspace.ui.flow.journey.timeline

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.canopas.yourspace.R
import com.canopas.yourspace.data.models.location.LocationJourney
import com.canopas.yourspace.domain.utils.ConnectivityObserver
import com.canopas.yourspace.domain.utils.formattedMessageDateHeader
import com.canopas.yourspace.ui.component.AppBanner
import com.canopas.yourspace.ui.component.AppProgressIndicator
import com.canopas.yourspace.ui.component.HorizontalDatePicker
import com.canopas.yourspace.ui.component.NoInternetScreen
import com.canopas.yourspace.ui.component.ShowDatePicker
import com.canopas.yourspace.ui.component.reachedBottom
import com.canopas.yourspace.ui.flow.journey.components.EmptyHistory
import com.canopas.yourspace.ui.flow.journey.components.LocationHistoryItem
import com.canopas.yourspace.ui.theme.AppTheme
import java.util.Locale

@Composable
fun JourneyTimelineScreen() {
    Scaffold(topBar = {
        TimelineTopBar()
    }) {
        TimelineContent(modifier = Modifier.padding(it))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineTopBar() {
    val viewModel = hiltViewModel<JourneyTimelineViewModel>()
    val state by viewModel.state.collectAsState()

    val title =
        if (state.selectedUser == null) {
            stringResource(id = R.string.journey_timeline_title)
        } else {
            stringResource(
                id = R.string.journey_timeline_title_other_user,
                state.selectedUser?.first_name?.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
                } ?: ""
            )
        }
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(containerColor = AppTheme.colorScheme.surface),
        title = {
            Column {
                if (state.isCurrentUserTimeline) {
                    Text(
                        text = state.selectedTimeFrom.formattedMessageDateHeader(LocalContext.current),
                        style = AppTheme.appTypography.subTitle1.copy(fontWeight = FontWeight.Bold),
                        color = AppTheme.colorScheme.textPrimary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Text(
                        text = title,
                        style = AppTheme.appTypography.subTitle1,
                        color = AppTheme.colorScheme.textPrimary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = state.selectedTimeFrom.formattedMessageDateHeader(LocalContext.current),
                        style = AppTheme.appTypography.body2.copy(fontWeight = FontWeight.Bold),
                        color = AppTheme.colorScheme.textPrimary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = viewModel::navigateBack) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_nav_back_arrow_icon),
                    contentDescription = ""
                )
            }
        },
        actions = {
            if (state.connectivityStatus == ConnectivityObserver.Status.Available) {
                IconButton(onClick = viewModel::showDatePicker) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_calendar),
                        contentDescription = "",
                        tint = AppTheme.colorScheme.textPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    )
}

@Composable
private fun TimelineContent(modifier: Modifier) {
    val viewModel = hiltViewModel<JourneyTimelineViewModel>()
    val state by viewModel.state.collectAsState()

    if (!state.error.isNullOrEmpty()) {
        AppBanner(msg = state.error!!, onDismiss = viewModel::resetErrorState)
    }

    if (state.showDatePicker) {
        ShowDatePicker(
            selectedTimestamp = state.selectedTimeTo,
            confirmButtonClick = viewModel::onFilterByDate,
            dismissButtonClick = viewModel::dismissDatePicker
        )
    }

    Column(modifier = modifier.fillMaxSize()) {
        key(state.selectedTimeTo) {
            HorizontalDatePicker(
                modifier = Modifier.fillMaxWidth(),
                selectedTimestamp = state.selectedTimeTo
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider()

        if (state.connectivityStatus == ConnectivityObserver.Status.Available) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (state.isLoading) {
                    AppProgressIndicator()
                } else if (state.groupedLocation.isEmpty()) {
                    EmptyHistory()
                } else {
                    JourneyList(
                        appending = state.appending,
                        journeys = state.groupedLocation,
                        onScrollToBottom = viewModel::loadMoreLocations,
                        onAddPlaceClicked = viewModel::addPlace,
                        showJourneyDetails = viewModel::showJourneyDetails,
                        selectedMapStyle = state.selectedMapStyle
                    )
                }
            }
        } else {
            NoInternetScreen(viewModel::checkInternetConnection)
        }
    }
}

@Composable
private fun JourneyList(
    appending: Boolean,
    journeys: Map<Long, List<LocationJourney>>,
    onScrollToBottom: () -> Unit,
    onAddPlaceClicked: (Double, Double) -> Unit,
    showJourneyDetails: (String) -> Unit,
    selectedMapStyle: String
) {
    val lazyState = rememberLazyListState()
    val reachedBottom by remember {
        derivedStateOf { lazyState.reachedBottom() }
    }

    LaunchedEffect(reachedBottom) {
        if (reachedBottom && journeys.isNotEmpty()) {
            onScrollToBottom()
        }
    }

    LazyColumn(
        state = lazyState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp, top = 10.dp)
    ) {
        val allJourney = journeys.values.flatten()
        itemsIndexed(
            allJourney,
            key = { _, item -> item.id },
            contentType = { _, _ -> "Journey" }
        ) { index, journey ->
            LocationHistoryItem(
                journey,
                isFirstItem = index == 0,
                isLastItem = index == allJourney.lastIndex,
                addPlaceTap = onAddPlaceClicked,
                journeyList = allJourney,
                showJourneyDetails = {
                    showJourneyDetails(journey.id)
                },
                selectedMapStyle = selectedMapStyle
            )
        }

        if (appending) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    AppProgressIndicator()
                }
            }
        }
    }
}
