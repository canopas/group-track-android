package com.canopas.catchme.ui.flow.onboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.canopas.catchme.R
import com.canopas.catchme.ui.component.PrimaryButton
import com.canopas.catchme.ui.flow.onboard.OnboardViewModel
import com.canopas.catchme.ui.theme.AppTheme
import com.canopas.catchme.ui.theme.CatchMeTheme

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun PickNameOnboard() {
    val viewModel = hiltViewModel<OnboardViewModel>()
    val state by viewModel.state.collectAsState()
    val keyboard = LocalSoftwareKeyboardController.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTheme.colorScheme.surface)
            .verticalScroll(scrollState)
            .padding(top = 80.dp)
    ) {
        Spacer(modifier = Modifier.height(30.dp))
        TitleContent()
        Spacer(modifier = Modifier.height(30.dp))

        PickNameTextField(
            stringResource(id = R.string.onboard_pick_name_label_first_name),
            state.firstName
        ) {
            viewModel.onFirstNameChange(it)
        }
        Spacer(modifier = Modifier.height(30.dp))

        PickNameTextField(
            stringResource(id = R.string.onboard_pick_name_label_last_name),
            state.lastName
        ) {
            viewModel.onLastNameChange(it)
        }
        Spacer(modifier = Modifier.height(60.dp))
        PrimaryButton(
            label = stringResource(R.string.common_btn_next),
            modifier = Modifier.align(Alignment.CenterHorizontally),
            onClick = {
                keyboard?.hide()
                viewModel.navigateToSpaceInfo()
            },
            enabled = state.firstName.trim().isNotEmpty() && state.lastName.trim().isNotEmpty(),
            showLoader = state.updatingUserName
        )
    }
}

@Composable
private fun PickNameTextField(title: String, value: String, onValueChanged: (String) -> Unit) {
    Text(
        text = title.uppercase(),
        style = AppTheme.appTypography.subTitle2
            .copy(color = AppTheme.colorScheme.textSecondary),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp)
    )

    Spacer(modifier = Modifier.height(10.dp))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        BasicTextField(
            value = value,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            textStyle = AppTheme.appTypography.header4.copy(
                color = AppTheme.colorScheme.textSecondary
            ),
            onValueChange = { value ->
                onValueChanged(value)
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next
            ),
            singleLine = true
        )

        Divider(
            Modifier.align(Alignment.BottomCenter),
            thickness = 1.dp,
            color = AppTheme.colorScheme.outline
        )
    }
}

@Composable
private fun TitleContent() {
    Text(
        text = stringResource(R.string.onboard_pick_name_title),
        style = AppTheme.appTypography.header1,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp)
    )
}

@Preview
@Composable
fun PreviewPickNameOnboardScreen() {
    CatchMeTheme {
        PickNameOnboard()
    }
}
