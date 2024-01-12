package com.canopas.catchme.ui.flow.onboard.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.canopas.catchme.R
import com.canopas.catchme.ui.component.PrimaryButton
import com.canopas.catchme.ui.theme.AppTheme
import com.canopas.catchme.ui.theme.CatchMeTheme

@Composable
fun PickNameOnboard(
    onNext: (String, String) -> Unit
) {
    val scrollState = rememberScrollState()
    var lastname by remember { mutableStateOf("") }
    var firstName by remember { mutableStateOf("") }

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
            firstName
        ) {
            firstName = it
        }
        Spacer(modifier = Modifier.height(30.dp))

        PickNameTextField(
            stringResource(id = R.string.onboard_pick_name_label_last_name),
            lastname
        ) {
            lastname = it
        }
        Spacer(modifier = Modifier.height(60.dp))
        PrimaryButton(
            label = stringResource(R.string.common_btn_next),
            modifier = Modifier.align(Alignment.CenterHorizontally),
            onClick = { onNext(firstName, lastname) })
    }
}

@Composable
private fun PickNameTextField(title: String, value: String, onValueChanged: (String) -> Unit) {

    Text(
        text = title.uppercase(),
        style = AppTheme.appTypography.subTitle2.copy()
            .copy(color = AppTheme.colorScheme.textSecondary),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp),
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
            textStyle = AppTheme.appTypography.header4,
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
        PickNameOnboard { _, _ -> }
    }
}