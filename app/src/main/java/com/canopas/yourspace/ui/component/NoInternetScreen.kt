package com.canopas.yourspace.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.canopas.yourspace.R
import com.canopas.yourspace.ui.theme.AppTheme

@Composable
fun NoInternetScreen(onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_no_internet_connection),
            contentDescription = null
        )

        Spacer(modifier = Modifier.height(40.dp))

        Text(
            text = stringResource(R.string.on_internet_error_title),
            style = MaterialTheme.typography.headlineMedium,
            color = AppTheme.colorScheme.textPrimary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = stringResource(R.string.on_internet_error_sub_title),
            style = MaterialTheme.typography.bodyLarge,
            color = AppTheme.colorScheme.textPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.size(20.dp))

        Button(onClick = onRetry) {
            Text(
                text = stringResource(R.string.common_btn_retry)
            )
        }
    }
}
