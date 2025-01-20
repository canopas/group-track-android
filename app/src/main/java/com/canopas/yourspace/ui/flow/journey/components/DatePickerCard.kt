package com.canopas.yourspace.ui.flow.journey.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.canopas.yourspace.ui.theme.AppTheme
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun DateCard(
    date: LocalDate,
    isSelected: Boolean,
    isDisabled: Boolean,
    onClick: () -> Unit
) {
    val isToday = date == LocalDate.now()

    val cardColor = if (isSelected) {
        AppTheme.colorScheme.primary
    } else {
        AppTheme.colorScheme.secondaryInverseVariant
    }

    val textColor = if (!isDisabled) {
        if (isSelected) AppTheme.colorScheme.textInversePrimary else AppTheme.colorScheme.textPrimary
    } else {
        AppTheme.colorScheme.textDisabled
    }

    Card(
        modifier = Modifier
            .wrapContentSize()
            .clip(RoundedCornerShape(8.dp))
            .clickable { if (!isDisabled) onClick() }
            .then(
                if (isToday) {
                    Modifier.border(2.dp, AppTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                } else {
                    Modifier
                }
            ),
        elevation = CardDefaults.cardElevation(if (isSelected) 8.dp else 4.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .background(cardColor)
                .padding(8.dp)
        ) {
            Text(
                text = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                style = AppTheme.appTypography.label3,
                modifier = Modifier.padding(4.dp),
                color = textColor
            )
            Text(
                text = date.dayOfMonth.toString(),
                style = AppTheme.appTypography.label1,
                color = textColor,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}
