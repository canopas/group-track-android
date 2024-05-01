package com.canopas.yourspace.ui.component

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import com.canopas.yourspace.ui.flow.home.map.member.PastOrPresentSelectableDates
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShowDatePicker(
    selectedTimestamp: Long? = null,
    confirmButtonClick: (Long) -> Unit,
    dismissButtonClick: () -> Unit
) {
    val calendar = Calendar.getInstance()

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedTimestamp ?: calendar.timeInMillis,
        selectableDates = PastOrPresentSelectableDates
    )
    DatePickerDialog(
        onDismissRequest = {},
        confirmButton = {
            TextButton(onClick = {
                confirmButtonClick(
                    datePickerState.selectedDateMillis ?: calendar.timeInMillis
                )
            }) {
                Text(text = "Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = dismissButtonClick) {
                Text(text = "Cancel")
            }
        }
    ) {
        DatePicker(
            state = datePickerState
        )
    }
}
