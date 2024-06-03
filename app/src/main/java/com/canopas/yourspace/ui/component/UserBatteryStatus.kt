package com.canopas.yourspace.ui.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.canopas.yourspace.R
import com.canopas.yourspace.data.models.user.ApiUserSession
import com.canopas.yourspace.ui.theme.AppTheme

@Composable
fun UserBatteryStatus(
    modifier: Modifier = Modifier,
    session: ApiUserSession
) {
    val batteryPrc = session.battery_pct ?: 0f
    val icon = if (batteryPrc > 70f) {
        R.drawable.ic_battery_full
    } else if (batteryPrc > 50f) {
        R.drawable.ic_battery_50
    } else if (batteryPrc > 30f) {
        R.drawable.ic_battery_30
    } else {
        R.drawable.ic_battery_low
    }

    val color = if (batteryPrc > 70f) {
        Color.Green
    } else if (batteryPrc > 30f) {
        AppTheme.colorScheme.permissionWarning
    } else {
        AppTheme.colorScheme.alertColor
    }

    Row(
        modifier = modifier.wrapContentSize(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = null,
            tint = color,
            modifier = Modifier
                .size(16.dp)
                .padding(end = 4.dp)
        )

        Text(
            text = "${batteryPrc.toInt()}%",
            color = color,
            style = AppTheme.appTypography.caption
        )
    }
}
