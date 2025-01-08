package com.canopas.yourspace.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.canopas.yourspace.ui.flow.journey.timeline.component.CalendarViewModel
import com.canopas.yourspace.ui.theme.AppTheme
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun HorizontalDatePicker(
    modifier: Modifier = Modifier,
    selectedTimestamp: Long? = null,
    onDateClick: (Long) -> Unit
) {
    val today = LocalDate.now()
    val viewModel = hiltViewModel<CalendarViewModel>()
    val calendarState by viewModel.state.collectAsState()

    val initialSelectedDate = selectedTimestamp?.let {
        LocalDate.ofEpochDay(it / (24 * 60 * 60 * 1000))
    } ?: calendarState.selectedDate

    var currentList by remember {
        mutableStateOf(
            getDatesBetween(
                calendarState.weekStartDate.minusDays(initialSelectedDate.toEpochDay()),
                calendarState.weekStartDate.plusDays(initialSelectedDate.toEpochDay())
            ).filter { it <= today }
        )
    }

    var selectedDate by remember { mutableStateOf(initialSelectedDate) }

    fun chunkDates(dates: List<LocalDate>, chunkSize: Int): List<List<LocalDate>> {
        return dates.chunked(chunkSize)
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

    LaunchedEffect(pagerState.currentPage) {
        when (pagerState.currentPage) {
            0 -> {
                currentList = (currentList).distinct().filter { it <= today }
            }

            currentList.size - 1 -> {
                currentList = (currentList).distinct()
            }
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
                    DateCard(
                        date = date,
                        isSelected = isSelected,
                        onClick = {
                            selectedDate = date
                            viewModel.setSelectedDate(date)
                            onDateClick(date.toEpochDay() * 24 * 60 * 60 * 1000)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun DateCard(
    date: LocalDate,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .wrapContentSize()
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(if (isSelected) 8.dp else 4.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .background(if (isSelected) AppTheme.colorScheme.primary else AppTheme.colorScheme.secondaryInverseVariant)
                .padding(8.dp)
        ) {
            Text(
                text = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                style = AppTheme.appTypography.label3,
                modifier = Modifier.padding(4.dp),
                color = if (isSelected) AppTheme.colorScheme.textInversePrimary else AppTheme.colorScheme.textPrimary
            )
            Text(
                text = date.dayOfMonth.toString(),
                style = AppTheme.appTypography.label1,
                color = if (isSelected) AppTheme.colorScheme.textInversePrimary else AppTheme.colorScheme.textPrimary,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
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
