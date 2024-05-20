package com.canopas.yourspace.ui.flow.journey.timeline

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.capitalize
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.canopas.yourspace.R
import com.canopas.yourspace.data.models.location.LocationJourney
import com.canopas.yourspace.domain.utils.formattedMessageDateHeader
import com.canopas.yourspace.ui.component.AppBanner
import com.canopas.yourspace.ui.component.AppProgressIndicator
import com.canopas.yourspace.ui.component.reachedBottom
import com.canopas.yourspace.ui.flow.home.map.member.components.LocationHistoryItem
import com.canopas.yourspace.ui.theme.AppTheme

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
        } else if (state.isCurrentUserTimeline) {
            stringResource(id = R.string.journey_timeline_title_your_timeline)
        } else {
            stringResource(
                id = R.string.journey_timeline_title_other_user,
                state.selectedUser?.first_name?.capitalize() ?: ""
            )
        }
    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(containerColor = AppTheme.colorScheme.surface),
        title = {
            Text(
                text = title,
                style = AppTheme.appTypography.header3
            )
        },
        navigationIcon = {
            IconButton(onClick = viewModel::navigateBack) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = ""
                )
            }
        },
        actions = {
            IconButton(onClick = viewModel::showDatePicker) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_action_filter),
                    contentDescription = ""
                )
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

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        JourneyList(
            appending = state.appending,
            journeys = state.groupedLocation,
            onScrollToBottom = viewModel::loadMoreLocations,
            onAddPlaceClicked = viewModel::addPlace,
            showJourneyDetails = viewModel::showJourneyDetails
        )

        if (state.isLoading) {
            AppProgressIndicator()
        }
    }
}

@Composable
private fun JourneyList(
    appending: Boolean,
    journeys: Map<Long, List<LocationJourney>>,
    onScrollToBottom: () -> Unit,
    onAddPlaceClicked: (Double, Double) -> Unit,
    showJourneyDetails: (String) -> Unit
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
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        journeys.forEach { section ->
            item(key = section.key, contentType = "Header") {
                Text(
                    text = section.key.formattedMessageDateHeader(LocalContext.current),
                    style = AppTheme.appTypography.subTitle1.copy(color = AppTheme.colorScheme.textSecondary),
                    modifier = Modifier.padding(bottom = 10.dp)
                        .fillMaxWidth()
                        .padding(vertical = 16.dp, horizontal = 16.dp)
                )
            }

            itemsIndexed(
                section.value,
                key = { index, item -> item.id },
                contentType = { _, _ -> "Journey" }
            ) { index, journey ->
                LocationHistoryItem(
                    journey,
                    isLastItem = index == section.value.lastIndex,
                    addPlaceTap = onAddPlaceClicked,
                    showJourneyDetails = {
                        showJourneyDetails(journey.id)
                    }
                )
            }
        }

        if (appending) {
            item {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    AppProgressIndicator()
                }
            }
        }
    }
}
