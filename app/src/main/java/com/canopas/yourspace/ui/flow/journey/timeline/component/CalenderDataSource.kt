package com.canopas.yourspace.ui.flow.journey.timeline.component

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.stream.Collectors
import java.util.stream.Stream

class CalendarDataSource {

    val today: LocalDate
        get() = LocalDate.now()

    fun getData(
        currentList: List<LocalDate>,
        lastSelectedDate: LocalDate,
        isScrollUp: Boolean
    ): CalendarUiModel {
        val startDate: LocalDate
        val endDate: LocalDate

        if (isScrollUp) {
            startDate = currentList.first().minusDays(30)
            endDate = currentList.first().minusDays(1)
        } else {
            startDate = currentList.last().plusDays(1)
            endDate = currentList.last().plusDays(30)
        }

        val additionalDates = getDatesBetween(startDate, endDate)
        val combinedDates = if (isScrollUp) {
            additionalDates + currentList
        } else {
            currentList + additionalDates
        }

        return toUiModel(combinedDates, lastSelectedDate)
    }

    fun getDatesBetween(startDate: LocalDate, endDate: LocalDate): List<LocalDate> {
        val numOfDays = ChronoUnit.DAYS.between(startDate, endDate) + 1
        return Stream.iterate(startDate) { date -> date.plusDays(1) }
            .limit(numOfDays)
            .collect(Collectors.toList())
    }

    private fun toUiModel(
        dateList: List<LocalDate>,
        lastSelectedDate: LocalDate
    ): CalendarUiModel {
        return CalendarUiModel(
            selectedDate = toItemUiModel(lastSelectedDate, true),
            visibleDates = dateList.map {
                toItemUiModel(it, it.isEqual(lastSelectedDate))
            }
        )
    }

    private fun toItemUiModel(date: LocalDate, isSelectedDate: Boolean) = CalendarUiModel.Date(
        isSelected = isSelectedDate,
        isToday = date.isEqual(today),
        date = date
    )
}

data class CalendarUiModel(
    val selectedDate: Date,
    val visibleDates: List<Date>
) {

    data class Date(
        val date: LocalDate,
        val isSelected: Boolean,
        val isToday: Boolean
    ) {
        val day: String = date.format(DateTimeFormatter.ofPattern("EE"))
    }
}
