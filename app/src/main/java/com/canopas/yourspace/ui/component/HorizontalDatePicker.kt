package com.canopas.yourspace.ui.component

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.canopas.yourspace.ui.theme.AppTheme
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun HorizontalDatePicker(
    modifier: Modifier,
    selectedTimestamp: Long? = System.currentTimeMillis(),
    onDateClick: (Long) -> Unit
) {
    val currentTimestamp = selectedTimestamp ?: System.currentTimeMillis()

    val calendar = Calendar.getInstance()
    calendar.timeInMillis = currentTimestamp

    var selectedDate by remember { mutableLongStateOf(currentTimestamp) }

    val dateRange = generateDateRange(selectedDate)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(16.dp)
    ) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
            reverseLayout = true
        ) {
            items(dateRange.size) { index ->
                DateCard(date = dateRange[index], isSelected = dateRange[index] == selectedDate) {
                    selectedDate = dateRange[index]
                    onDateClick(selectedDate)
                }
            }
        }
    }
}

@Composable
fun DateCard(
    date: Long,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val dayOfWeek = SimpleDateFormat("EEE", Locale.getDefault()).format(date)
    val day = SimpleDateFormat("dd", Locale.getDefault()).format(date)
    val month = SimpleDateFormat("MMM", Locale.getDefault()).format(date)

    Card(
        modifier = Modifier
            .wrapContentSize()
            .padding(4.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(if (isSelected) 8.dp else 4.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .background(if (isSelected) AppTheme.colorScheme.primary else AppTheme.colorScheme.containerInverseHigh)
                .padding(8.dp)
        ) {
            Text(
                text = dayOfWeek,
                style = AppTheme.appTypography.body2,
                modifier = Modifier.padding(4.dp),
                color = if (isSelected) AppTheme.colorScheme.textInversePrimary else Color.White
            )
            Text(
                text = day,
                style = AppTheme.appTypography.header4,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) AppTheme.colorScheme.textInversePrimary else Color.White,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Text(
                text = month,
                style = AppTheme.appTypography.body2,
                modifier = Modifier.padding(4.dp),
                color = if (isSelected) AppTheme.colorScheme.textInversePrimary else Color.White
            )
        }
    }
}

fun generateDateRange(selectedDate: Long): List<Long> {
    val calendar = Calendar.getInstance()
    calendar.timeInMillis = selectedDate

    val dateRange = mutableListOf<Long>()

    for (i in 0 until 365) {
        dateRange.add(calendar.timeInMillis)
        calendar.add(Calendar.DATE, -1)
    }

    return dateRange
}
