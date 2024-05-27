package com.canopas.yourspace.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver

private val primaryColor = Color(0xFF1679AB)

private val secondaryLightColor = Color(0xFF34495E)
private val secondaryVariantLightColor = Color(0x5234495E)

private val secondaryDarkColor = Color(0xFFCEE5FF)
private val secondaryVariantDarkColor = Color(0x66CEE5FF)

private val tertiaryDarkColor = Color(0xFF58633A)
private val tertiaryLightColor = Color(0xFFDCE8B4)

private val containerHighLightColor = Color(0x1F2884B2)
private val containerNormalLightColor = Color(0x0F2884B2)
private val containerLowLightColor = Color(0x0A2884B2)

private val containerHighDarkColor = Color(0x1F2884B2)
private val containerNormalDarkColor = Color(0x0F2884B2)
private val containerLowDarkColor = Color(0x0A2884B2)

private val containerB40Color = Color(0x0A61A4C6)

private val textPrimaryLightColor = Color(0xDE000000)
private val textSecondaryLightColor = Color(0x99000000)
private val textDisabledLightColor = Color(0x66000000)

private val textPrimaryDarkColor = Color(0xFFFFFFFF)
private val textSecondaryDarkColor = Color(0xDEFFFFFF)
private val textDisabledDarkColor = Color(0x99FFFFFF)

private val outlineLightColor = Color(0xFFEBEBEB)
private val outlineDarkColor = Color(0x14EBEBEB)

private val surfaceLightColor = Color(0xFFFFFFFF)
private val surfaceDarkColor = Color(0xFF212121)

private val successStatusColor = Color(0xFF34A853)
private val permissionWarningColor = Color(0xFFFBBC05)
private val awarenessAlertColor = Color(0xFFD32F2F)

private val iconsBackgroundColor = Color(0xFFFF9800)
private val locationMarkerColor = Color(0xFFFF5722)
private val markerInfoWindowColor = Color(0xE7E0E0E0)

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
    val containerB40: Color = containerB40Color,
    val onPrimary: Color = textPrimaryDarkColor,
    val onPrimaryVariant: Color = textPrimaryLightColor,
    val onSecondary: Color = textSecondaryDarkColor,
    val onDisabled: Color = textDisabledLightColor,
    val permissionWarning: Color = permissionWarningColor,
    val alertColor: Color = awarenessAlertColor,
    val iconsBackground: Color = iconsBackgroundColor,
    val locationMarker: Color = locationMarkerColor,
    val markerInfoWindow: Color = markerInfoWindowColor,
    val successColor: Color = successStatusColor
) {
    val containerNormalOnSurface: Color
        get() {
            return containerNormal.compositeOver(surface)
        }
}
