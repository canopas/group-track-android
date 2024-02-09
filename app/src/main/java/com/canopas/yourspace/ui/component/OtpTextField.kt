package com.canopas.yourspace.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.canopas.yourspace.ui.theme.AppTheme

@Composable
fun OtpInputField(
    pinText: String,
    onPinTextChange: (String) -> Unit,
    textStyle: TextStyle = AppTheme.appTypography.header1,
    digitCount: Int = 6
) {
    val focusRequester = remember { FocusRequester() }

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
            Row {
                repeat(digitCount) { index ->
                    OTPDigit(index, pinText, textStyle, focusRequester)
                    Spacer(modifier = Modifier.width(8.dp))
                    if (index == 2) {
                        Divider(
                            thickness = 2.dp,
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .width(12.dp)
                                .align(Alignment.CenterVertically),
                            color = AppTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                }
            }
        }
    )
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun OTPDigit(
    index: Int,
    pinText: String,
    textStyle: TextStyle,
    focusRequester: FocusRequester
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
                .width(40.dp)
                .height(60.dp)
                .border(
                    width = 2.dp,
                    color = if (isFocused) AppTheme.colorScheme.primary else Color.Transparent,
                    shape = RoundedCornerShape(8.dp)
                )
                .background(
                    color = if (isFocused) AppTheme.colorScheme.containerLow else AppTheme.colorScheme.containerNormal,
                    shape = RoundedCornerShape(8.dp)
                )
                .wrapContentHeight(align = Alignment.CenterVertically),
            style = textStyle,
            textAlign = TextAlign.Center
        )
    }
}
