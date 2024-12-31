package com.canopas.yourspace.ui.flow.pin.enterpin

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.canopas.yourspace.ui.component.OtpInputField
import com.canopas.yourspace.ui.component.PrimaryButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnterPinScreen() {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Enter Your PIN") })
        },
        contentColor = MaterialTheme.colorScheme.background
    ) {
        EnterPinContent(modifier = Modifier.padding(it))
    }
}

@Composable
private fun EnterPinContent(modifier: Modifier) {
    val viewModel = hiltViewModel<EnterPinViewModel>()
    val state by viewModel.state.collectAsState()

    Column(
        modifier = modifier
            .padding(32.dp)
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Please enter your 4-digit PIN to access your account",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            textAlign = TextAlign.Center
        )

        Text(
            text = "Your PIN ensures that only you can access your account",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
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
            label = "Continue",
            onClick = {
                viewModel.processPin()
            },
            enabled = state.pin != "" && state.pinError == "",
            modifier = Modifier.fillMaxWidth(),
            showLoader = state.showLoader
        )
    }
}
