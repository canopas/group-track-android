package com.canopas.yourspace.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.canopas.yourspace.ui.theme.AppTheme

@Composable
fun OtpInputField(
    pinText: String,
    onPinTextChange: (String) -> Unit,
    textStyle: TextStyle = AppTheme.appTypography.header2,
    digitCount: Int = 6
) {
    val focusRequester = remember { FocusRequester() }
    BoxWithConstraints(
        Modifier
            .widthIn(max = 600.dp)
            .fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        val width = (maxWidth - 32.dp) / (digitCount + 1)

        BasicTextField(
            value = pinText,
            onValueChange = {
                if (it.length <= digitCount) {
                    onPinTextChange(it)
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            modifier = Modifier.focusRequester(focusRequester),
            decorationBox = {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .align(Alignment.Center),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    repeat(digitCount) { index ->
                        OTPDigit(index, pinText, textStyle, focusRequester, width = width)

                        if (index == 2) {
                            HorizontalDivider(
                                modifier = Modifier
                                    .width(16.dp)
                                    .align(Alignment.CenterVertically),
                                thickness = 2.dp,
                                color = AppTheme.colorScheme.textPrimary
                            )
                        }
                    }
                }
            }
        )
    }

    LaunchedEffect(key1 = Unit) {
        focusRequester.requestFocus()
    }
}

@Composable
private fun OTPDigit(
    index: Int,
    pinText: String,
    textStyle: TextStyle,
    focusRequester: FocusRequester,
    width: Dp,
) {
    val isFocused = pinText.length == index
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.clickable(onClick = {
            focusRequester.requestFocus()
            keyboardController?.show()
        }, indication = null, interactionSource = remember { MutableInteractionSource() })
    ) {
        Text(
            text = if (index >= pinText.length) "" else pinText[index].toString().uppercase(),
            modifier = Modifier
                .width(width)
                .height(64.dp)
                .background(
                    color = if (isFocused) AppTheme.colorScheme.containerLow else AppTheme.colorScheme.containerNormal,
                    shape = RoundedCornerShape(8.dp)
                )
                .wrapContentHeight(align = Alignment.CenterVertically),
            style = textStyle,
            color = AppTheme.colorScheme.textPrimary,
            textAlign = TextAlign.Center
        )
    }
}
