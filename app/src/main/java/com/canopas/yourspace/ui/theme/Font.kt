package com.canopas.yourspace.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.canopas.yourspace.R

val InterFontFamily = FontFamily(
    Font(
        R.font.inter_light,
        weight = FontWeight.Light
    ),
    Font(
        R.font.inter_regular,
        weight = FontWeight.Normal
    ),
    Font(
        R.font.inter_medium,
        weight = FontWeight.Medium
    ),
    Font(
        R.font.inter_semi_bold,
        weight = FontWeight.SemiBold
    ),
    Font(
        R.font.inter_italic,
        weight = FontWeight.Normal,
        style = FontStyle.Italic
    )
)
val KalamBoldFont = FontFamily(Font(R.font.kalam_bold))

val InterBoldFont = FontFamily(Font(R.font.inter_bold))
