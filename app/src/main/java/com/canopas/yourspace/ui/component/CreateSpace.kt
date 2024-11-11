package com.canopas.yourspace.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.canopas.yourspace.R
import com.canopas.yourspace.ui.theme.AppTheme

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
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.onboard_create_space_give_name_title),
            style = AppTheme.appTypography.header4,
            modifier = Modifier.fillMaxWidth()

        )
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.onboard_create_space_subtitle),
            style = AppTheme.appTypography.body1,
            color = AppTheme.colorScheme.textDisabled,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(28.dp))

        PickNameTextField(
            stringResource(id = R.string.onboard_create_space_name_label),
            spaceName
        ) {
            onSpaceNameChanged(it)
        }

        Spacer(modifier = Modifier.height(28.dp))
        Text(
            text = stringResource(R.string.home_create_space_suggestions),
            style = AppTheme.appTypography.caption.copy(color = AppTheme.colorScheme.textDisabled)

        )
        Spacer(modifier = Modifier.height(16.dp))

        Suggestions {
            onSpaceNameChanged(it)
            keyboardController?.hide()
        }
        Spacer(modifier = Modifier.weight(1f))

        PrimaryButton(
            label = stringResource(R.string.common_btn_next),
            modifier = Modifier.fillMaxWidth().align(Alignment.CenterHorizontally),
            onClick = {
                onNext()
                keyboardController?.hide()
            },
            enabled = spaceName.trim().isNotEmpty(),
            showLoader = showLoader
        )
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Suggestions(onSelect: (String) -> Unit) {
    val suggestion = stringArrayResource(id = R.array.home_create_space_name_suggestion)
    FlowRow(modifier = Modifier, maxItemsInEachRow = 4) {
        suggestion.forEach {
            Text(
                text = it,
                style = AppTheme.appTypography.body2,
                color = AppTheme.colorScheme.textSecondary,
                modifier = Modifier
                    .padding(end = 8.dp, bottom = 8.dp)
                    .background(AppTheme.colorScheme.containerNormal, RoundedCornerShape(30.dp))
                    .clip(RoundedCornerShape(30.dp))
                    .clickable { onSelect(it) }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun PickNameTextField(title: String, value: String, onValueChanged: (String) -> Unit) {
    val focusManager = LocalFocusManager.current
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val outlineColor =
        if (isFocused) AppTheme.colorScheme.primary else AppTheme.colorScheme.outline

    Text(
        text = title,
        style = AppTheme.appTypography.caption.copy()
            .copy(color = AppTheme.colorScheme.textDisabled),
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(6.dp))

    Column {
        BasicTextField(
            value = value,
            modifier = Modifier.fillMaxWidth(),
            textStyle = AppTheme.appTypography.subTitle2.copy(AppTheme.colorScheme.textPrimary),
            onValueChange = { value ->
                onValueChanged(value)
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = {
                focusManager.clearFocus()
            }),
            singleLine = true,
            interactionSource = interactionSource,
            cursorBrush = SolidColor(AppTheme.colorScheme.primary)
        )

        HorizontalDivider(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            thickness = 1.dp,
            color = outlineColor
        )
    }
}
