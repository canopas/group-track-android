package com.canopas.yourspace.ui.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.canopas.yourspace.R
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
object PastOrPresentSelectableDates : SelectableDates {
    private var groupCreationDate: Long? = null

    fun setGroupCreationDate(date: Long?) {
        groupCreationDate = date
    }

    override fun isSelectableDate(utcTimeMillis: Long): Boolean {
        val currentTimeMillis = System.currentTimeMillis()

        groupCreationDate?.let {
            val groupCreationLocalDate = LocalDate.ofInstant(
                Date(it).toInstant(),
                ZoneId.systemDefault()
            ).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            if (utcTimeMillis < groupCreationLocalDate) {
                return false
            }
        }

        return utcTimeMillis <= currentTimeMillis
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShowDatePicker(
    selectedTimestamp: Long? = System.currentTimeMillis(),
    confirmButtonClick: (Long) -> Unit,
    dismissButtonClick: () -> Unit,
    groupCreationDate: Long? = System.currentTimeMillis()
) {
    PastOrPresentSelectableDates.setGroupCreationDate(groupCreationDate)

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedTimestamp ?: System.currentTimeMillis(),
        selectableDates = PastOrPresentSelectableDates
    )
    DatePickerDialog(
        onDismissRequest = {},
        confirmButton = {
            TextButton(onClick = {
                confirmButtonClick(
                    datePickerState.selectedDateMillis ?: System.currentTimeMillis()
                )
            }) {
                Text(text = stringResource(id = R.string.common_btn_select))
            }
        },
        dismissButton = {
            TextButton(onClick = dismissButtonClick) {
                Text(text = stringResource(id = R.string.common_btn_cancel))
            }
        }
    ) {
        Box(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .fillMaxWidth()
        ) {
            DatePicker(
                state = datePickerState
            )
        }
    }
}
