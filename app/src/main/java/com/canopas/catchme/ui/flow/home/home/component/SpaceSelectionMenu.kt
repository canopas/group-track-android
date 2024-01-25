package com.canopas.catchme.ui.flow.home.home.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults.smallShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.canopas.catchme.R
import com.canopas.catchme.ui.theme.AppTheme

@Composable
fun SpaceSelectionMenu(modifier: Modifier, onClick: () -> Unit) {
    FloatingActionButton(
        onClick,
        modifier = modifier
            .padding(6.dp)
            .height(40.dp),
        containerColor = AppTheme.colorScheme.surface,
        contentColor = AppTheme.colorScheme.primary,
        shape = smallShape
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .padding(start = 20.dp, end = 10.dp)
        ) {
            Text(
                text = "Family",
                style = AppTheme.appTypography.label1.copy(color = AppTheme.colorScheme.textPrimary)
            )
            Icon(
                painter = painterResource(id = R.drawable.ic_arrow_down),
                contentDescription = "drop-down-arrow"
            )
        }
    }
}
