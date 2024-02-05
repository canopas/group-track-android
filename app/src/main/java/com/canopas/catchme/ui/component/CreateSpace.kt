package com.canopas.catchme.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.canopas.catchme.R
import com.canopas.catchme.ui.theme.AppTheme

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun CreateSpace(
    modifier: Modifier = Modifier,
    spaceName: String = "",
    showLoader: Boolean = false,
    onSpaceNameChanged: (String) -> Unit = {},
    onNext: () -> Unit
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val scrollState = rememberScrollState()
    Column(
        modifier = modifier.verticalScroll(scrollState)
    ) {
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.onboard_create_space_give_name_title),
            style = AppTheme.appTypography.header1,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = stringResource(R.string.onboard_create_space_subtitle),
            style = AppTheme.appTypography.body1,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp)
        )
        Spacer(modifier = Modifier.height(30.dp))

        PickNameTextField(
            stringResource(id = R.string.onboard_create_space_name_label),
            spaceName
        ) {
            onSpaceNameChanged(it)
        }

        Spacer(modifier = Modifier.height(30.dp))
        Text(
            text = stringResource(R.string.home_create_space_suggestions),
            style = AppTheme.appTypography.label2.copy(color = AppTheme.colorScheme.textSecondary),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))

        Suggestions {
            onSpaceNameChanged(it)
            keyboardController?.hide()
        }
        Spacer(modifier = Modifier.height(40.dp))

        PrimaryButton(
            label = stringResource(R.string.common_btn_next),
            modifier = Modifier.align(Alignment.CenterHorizontally),
            onClick = {
                onNext()
                keyboardController?.hide()
            },
            enabled = spaceName.trim().isNotEmpty(),
            showLoader = showLoader
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Suggestions(onSelect: (String) -> Unit) {
    val suggestion = stringArrayResource(id = R.array.home_create_space_name_suggestion)
    FlowRow(modifier = Modifier.padding(horizontal = 20.dp), maxItemsInEachRow = 4) {
        suggestion.forEach {
            Text(
                text = it,
                style = AppTheme.appTypography.label2,
                modifier = Modifier
                    .padding(horizontal = 6.dp, vertical = 4.dp)
                    .background(AppTheme.colorScheme.containerNormal, RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        onSelect(it)
                    }
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            )
        }
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
            .padding(horizontal = 28.dp)
    )

    Spacer(modifier = Modifier.height(6.dp))

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
            textStyle = AppTheme.appTypography.header4.copy(AppTheme.colorScheme.textPrimary),
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
