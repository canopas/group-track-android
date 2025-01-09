package com.canopas.yourspace.ui.flow.journey.timeline.component

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

@HiltViewModel
class CalendarViewModel @Inject constructor() : ViewModel() {
    private val _state = MutableStateFlow(
        CalendarViewState(
            selectedDate = LocalDate.now().atStartOfDay().toLocalDate(),
            weekStartDate = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        )
    )
    val state: StateFlow<CalendarViewState> = _state.asStateFlow()

    fun setSelectedDate(date: LocalDate?, isPickerDate: Boolean = false) {
        _state.update { it.copy(selectedDate = date ?: LocalDate.now()) }
        if (isPickerDate && date != null) {
            onSelectDateFromDatePicker(date)
        }
    }

    private fun onSelectDateFromDatePicker(date: LocalDate) {
        _state.update { it.copy(weekStartDate = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.entries[0]))) }
        setContainsToday()
    }

    private fun setContainsToday() {
        val today = LocalDate.now()
        val endOfWeek = state.value.weekStartDate
        val containsToday = today.isAfter(state.value.weekStartDate) && today.isBefore(endOfWeek)
        _state.update { it.copy(containsToday = containsToday) }
    }
}

data class CalendarViewState(
    val selectedDate: LocalDate,
    val weekStartDate: LocalDate,
    val containsToday: Boolean = true,
    val date: LocalDate = LocalDate.now(),
    val isSelected: Boolean = true
)
