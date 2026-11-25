package com.example.screenlock.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val SoftLightColorScheme = lightColorScheme(
    primary = SoftSagePrimary,
    onPrimary = SoftSageOnPrimary,
    primaryContainer = SoftSageContainer,
    onPrimaryContainer = SoftSageOnContainer,
    secondary = SoftSagePrimaryDark,
    onSecondary = SoftSageOnPrimary,
    secondaryContainer = SoftSurfaceVariant,
    onSecondaryContainer = SoftSageOnContainer,
    background = SoftBackground,
    onBackground = SoftTextPrimary,
    surface = SoftSurface,
    onSurface = SoftTextPrimary,
    surfaceVariant = SoftSurfaceVariant,
    onSurfaceVariant = SoftTextSecondary,
    outline = SoftBorder,
)

@Composable
fun ScreenLockTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SoftLightColorScheme,
        typography = ScreenLockTypography,
        content = content
    )
}
