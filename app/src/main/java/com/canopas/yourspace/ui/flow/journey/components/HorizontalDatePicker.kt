package com.canopas.yourspace.ui.flow.journey.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.canopas.yourspace.ui.flow.journey.timeline.JourneyTimelineViewModel
import java.time.LocalDate

@Composable
fun HorizontalDatePicker(
    modifier: Modifier = Modifier,
    selectedTimestamp: Long,
    weekStartDate: Long,
    groupCreationDate: Long
) {
    val viewModel = hiltViewModel<JourneyTimelineViewModel>()

    val initialSelectedDate = LocalDate.ofEpochDay(selectedTimestamp / (24 * 60 * 60 * 1000))
    var selectedDate by remember { mutableStateOf(initialSelectedDate) }
    val startDate = LocalDate.ofEpochDay(weekStartDate / (24 * 60 * 60 * 1000))
    val currentList by remember {
        mutableStateOf(
            getDatesBetween(
                startDate.minusDays(initialSelectedDate.toEpochDay()),
                startDate.plusDays(initialSelectedDate.toEpochDay())
            ).filter { it <= LocalDate.now() }
        )
    }

    val dateChunks = chunkDates(currentList, 7)

    val pagerState = remember {
        PagerState(
            currentPage = dateChunks.indexOfFirst { it.contains(selectedDate) },
            pageCount = { dateChunks.size }
        )
    }

    LaunchedEffect(selectedDate) {
        val selectedIndex = dateChunks.indexOfFirst { it.contains(selectedDate) }
        if (selectedIndex >= 0) {
            pagerState.scrollToPage(selectedIndex)
        }
    }

    HorizontalPager(
        state = pagerState,
        modifier = modifier.fillMaxWidth(),
        pageSpacing = 4.dp
    ) { pageIndex ->
        val dateChunk = dateChunks.getOrNull(pageIndex)
        if (dateChunk != null) {
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp)
            ) {
                dateChunk.forEach { date ->
                    val isSelected = date == selectedDate
                    val isFutureDate = date.isAfter(LocalDate.now())
                    val isBeforeGroupCreation = date.isBefore(
                        LocalDate.ofEpochDay(groupCreationDate / (24 * 60 * 60 * 1000))
                    )
                    val isDisabled = isBeforeGroupCreation || isFutureDate
                    DateCard(
                        date = date,
                        isSelected = isSelected,
                        isDisabled = isDisabled,
                        onClick = {
                            selectedDate = date
                            val timestamp = date.toEpochDay() * 24 * 60 * 60 * 1000
                            viewModel.setSelectedDate(timestamp)
                            viewModel.onFilterByDate(timestamp)
                        }
                    )
                }
            }
        }
    }
}

fun getDatesBetween(startDate: LocalDate, endDate: LocalDate): List<LocalDate> {
    val dates = mutableListOf<LocalDate>()
    var current = startDate
    while (!current.isAfter(endDate)) {
        dates.add(current)
        current = current.plusDays(1)
    }
    return dates
}

fun chunkDates(dates: List<LocalDate>, chunkSize: Int): List<List<LocalDate>> {
    return dates.chunked(chunkSize)
}
