package com.radiantengineering.orbittime.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val OrbitColorScheme = darkColorScheme(
    primary = OrbitColors.violet600,
    onPrimary = OrbitColors.cream50,
    secondary = OrbitColors.blue500,
    onSecondary = OrbitColors.cream50,
    tertiary = OrbitColors.coral500,
    onTertiary = OrbitColors.ink900,
    background = OrbitColors.cream100,
    onBackground = OrbitColors.ink900,
    surface = OrbitColors.cream50,
    onSurface = OrbitColors.ink900,
    surfaceVariant = OrbitColors.mist,
    onSurfaceVariant = OrbitColors.slate600,
    error = OrbitColors.danger,
    onError = OrbitColors.cream50,
    outline = OrbitColors.slate300,
)

/**
 * Root theme for Orbit Time. Wraps every screen with the shared colour, typography,
 * shape and spacing tokens so components never hard-code design values.
 */
@Composable
fun OrbitTimeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = OrbitColorScheme,
        typography = OrbitMaterialTypography,
        shapes = OrbitMaterialShapes,
        content = content,
    )
}
