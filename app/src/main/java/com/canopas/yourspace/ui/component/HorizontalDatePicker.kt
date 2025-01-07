package com.canopas.yourspace.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.unit.dp
import com.canopas.yourspace.ui.flow.journey.timeline.component.CalendarDataSource
import com.canopas.yourspace.ui.flow.journey.timeline.component.CalendarUiModel
import com.canopas.yourspace.ui.theme.AppTheme
import java.time.LocalDate

@Composable
fun HorizontalDatePicker(
    modifier: Modifier = Modifier,
    selectedTimestamp: Long? = null,
    onDateClick: (Long) -> Unit
) {
    val calendarDataSource = remember { CalendarDataSource() }
    val today = calendarDataSource.today
    val initialSelectedDate = selectedTimestamp?.let {
        LocalDate.ofEpochDay(it / (24 * 60 * 60 * 1000))
    } ?: today

    var currentList by remember {
        mutableStateOf(
            calendarDataSource.getDatesBetween(today.minusDays(15), today.plusDays(15))
                .filter { it <= today }
        )
    }
    var selectedDate by remember { mutableStateOf(initialSelectedDate) }
    val listState = rememberLazyListState()

    LaunchedEffect(selectedDate, Unit) {
        val selectedIndex = currentList.indexOf(selectedDate)
        if (selectedIndex >= 0) {
            listState.scrollToItem(selectedIndex)
        }
    }

    LaunchedEffect(listState.firstVisibleItemIndex) {
        when {
            listState.firstVisibleItemIndex == 0 -> {
                val updatedModel =
                    calendarDataSource.getData(currentList, selectedDate, isScrollUp = true)
                currentList = (updatedModel.visibleDates.map { it.date } + currentList)
                    .distinct()
                    .filter { it <= today }
            }

            listState.firstVisibleItemIndex + listState.layoutInfo.visibleItemsInfo.size == currentList.size -> {
                val updatedModel =
                    calendarDataSource.getData(currentList, selectedDate, isScrollUp = false)
                currentList = (currentList + updatedModel.visibleDates.map { it.date })
                    .distinct()
                    .filter { it <= today }
            }
        }
    }

    AnimatedVisibility(
        visible = true,
        enter = slideInVertically { -it } + fadeIn(),
        exit = slideOutVertically { -it } + fadeOut()
    ) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(16.dp)
        ) {
            LazyRow(
                state = listState,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = modifier.fillMaxWidth()
            ) {
                items(currentList) { date ->
                    val isSelected = date == selectedDate
                    DateCard(
                        date = CalendarUiModel.Date(date, isSelected, date == today),
                        onClick = {
                            selectedDate = date
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
    date: CalendarUiModel.Date,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .wrapContentSize()
            .padding(4.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(if (date.isSelected) 8.dp else 4.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .background(if (date.isSelected) AppTheme.colorScheme.primary else AppTheme.colorScheme.secondaryInverseVariant)
                .padding(8.dp)
        ) {
            Text(
                text = date.day,
                style = AppTheme.appTypography.label3,
                modifier = Modifier.padding(4.dp),
                color = if (date.isSelected) AppTheme.colorScheme.textInversePrimary else AppTheme.colorScheme.textPrimary
            )
            Text(
                text = date.date.dayOfMonth.toString(),
                style = AppTheme.appTypography.label1,
                color = if (date.isSelected) AppTheme.colorScheme.textInversePrimary else AppTheme.colorScheme.textPrimary,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}
