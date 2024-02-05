package com.canopas.catchme.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver

private val primaryColor = Color(0xFFFF9800)

private val secondaryLightColor = Color(0xFF34495E)
private val secondaryVariantLightColor = Color(0x5234495E)

private val secondaryDarkColor = Color(0xFFCEE5FF)
private val secondaryVariantDarkColor = Color(0x66CEE5FF)

private val tertiaryDarkColor = Color(0xFF58633A)
private val tertiaryLightColor = Color(0xFFDCE8B4)

private val containerHighLightColor = Color(0x2934495E)
private val containerNormalLightColor = Color(0x1434495E)
private val containerLowLightColor = Color(0x0A34495E)

private val containerHighDarkColor = Color(0x3DCEE5FF)
private val containerNormalDarkColor = Color(0x29CEE5FF)
private val containerLowDarkColor = Color(0x14CEE5FF)

private val textPrimaryLightColor = Color(0xDE000000)
private val textSecondaryLightColor = Color(0x99000000)
private val textDisabledLightColor = Color(0x66000000)

private val textPrimaryDarkColor = Color(0xFFFFFFFF)
private val textSecondaryDarkColor = Color(0xB3FFFFFF)
private val textDisabledDarkColor = Color(0x80FFFFFF)

private val outlineLightColor = Color(0x14000000)
private val outlineDarkColor = Color(0x14FFFFFF)

private val surfaceLightColor = Color(0xFFFFFFFF)
private val surfaceDarkColor = Color(0xFF495E14)

internal val themeLightColorScheme = lightColorScheme().copy(
    primary = primaryColor,
    onPrimary = textPrimaryLightColor,
    background = surfaceLightColor,
    onBackground = textPrimaryLightColor,
    onSecondary = textPrimaryLightColor
)

internal val appLightColorScheme = AppColorScheme(
    primary = primaryColor,
    secondary = secondaryLightColor,
    secondaryVariant = secondaryVariantLightColor,
    tertiary = tertiaryDarkColor,
    tertiaryVariant = tertiaryLightColor,
    outline = outlineLightColor,
    surface = surfaceLightColor,
    textPrimary = textPrimaryLightColor,
    textSecondary = textSecondaryLightColor,
    textDisabled = textDisabledLightColor,
    outlineInverse = outlineDarkColor,
    textInversePrimary = textPrimaryDarkColor,
    textInverseDisabled = textDisabledDarkColor,
    textInverseSecondary = textSecondaryDarkColor,
    containerInverseHigh = containerHighDarkColor,
    containerNormalInverse = containerNormalDarkColor,
    secondaryInverseVariant = secondaryVariantDarkColor,
    containerHigh = containerHighLightColor,
    containerNormal = containerNormalLightColor,
    containerLow = containerLowLightColor
)

internal val themeDarkColorScheme = darkColorScheme().copy(
    primary = primaryColor,
    onPrimary = textPrimaryDarkColor,
    background = surfaceDarkColor,
    onBackground = textPrimaryDarkColor,
    onSecondary = textPrimaryDarkColor
)

internal val appDarkColorScheme = AppColorScheme(
    primary = primaryColor,
    secondary = secondaryDarkColor,
    secondaryVariant = secondaryVariantDarkColor,
    tertiary = tertiaryLightColor,
    tertiaryVariant = tertiaryDarkColor,
    outline = outlineDarkColor,
    surface = surfaceDarkColor,
    textPrimary = textPrimaryDarkColor,
    textSecondary = textSecondaryDarkColor,
    textDisabled = textDisabledDarkColor,
    outlineInverse = outlineLightColor,
    textInversePrimary = textPrimaryLightColor,
    textInverseDisabled = textDisabledLightColor,
    textInverseSecondary = textSecondaryLightColor,
    containerInverseHigh = containerHighLightColor,
    containerNormalInverse = containerNormalLightColor,
    secondaryInverseVariant = secondaryVariantLightColor,
    containerHigh = containerHighDarkColor,
    containerNormal = containerNormalDarkColor,
    containerLow = containerLowDarkColor
)

val LocalDarkMode = staticCompositionLocalOf {
    false
}

val LocalAppColorScheme = staticCompositionLocalOf {
    appLightColorScheme
}

data class AppColorScheme(
    val primary: Color,
    val tertiary: Color,
    val tertiaryVariant: Color,
    val secondary: Color,
    val secondaryVariant: Color,
    val surface: Color,
    val outline: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textDisabled: Color,
    val outlineInverse: Color,
    val textInversePrimary: Color,
    val textInverseSecondary: Color,
    val textInverseDisabled: Color,
    val containerInverseHigh: Color,
    val containerNormalInverse: Color,
    val secondaryInverseVariant: Color,
    val containerHigh: Color,
    val containerNormal: Color,
    val containerLow: Color,
    val onPrimary: Color = textPrimaryDarkColor,
    val onPrimaryVariant: Color = textPrimaryLightColor,
    val onSecondary: Color = textSecondaryDarkColor,
    val onDisabled: Color = textDisabledLightColor
) {
    val containerNormalOnSurface: Color
        get() {
            return containerNormal.compositeOver(surface)
        }
}
