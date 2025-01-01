package com.canopas.yourspace.ui.flow.pin.setpin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.canopas.yourspace.R
import com.canopas.yourspace.ui.component.OtpInputField
import com.canopas.yourspace.ui.component.PrimaryButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetPinScreen() {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.set_pin_top_bar_title)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) {
        SetPinContent(modifier = Modifier.padding(it))
    }
}

@Composable
private fun SetPinContent(modifier: Modifier) {
    val viewModel = hiltViewModel<SetPinViewModel>()
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val lengthErrorText by remember {
        mutableStateOf(context.getString(R.string.set_pin_error_text_length))
    }

    Column(
        modifier = modifier
            .padding(32.dp)
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.set_pin_header_text_part_one),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            textAlign = TextAlign.Center
        )

        Text(
            text = stringResource(R.string.set_pin_header_text_part_two),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            textAlign = TextAlign.Center
        )

        OtpInputField(
            pinText = state.pin,
            onPinTextChange = { viewModel.onPinChanged(it) },
            digitCount = 4
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = state.pinError ?: "",
            color = if (!state.pinError.isNullOrEmpty()) MaterialTheme.colorScheme.error else Color.Transparent,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        PrimaryButton(
            label = stringResource(R.string.set_pin_button_text),
            onClick = {
                viewModel.processPin(lengthErrorText)
            },
            enabled = state.pin != "" && state.pinError == "",
            modifier = Modifier.fillMaxWidth(),
            showLoader = state.showLoader
        )
    }
}
