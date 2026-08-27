package com.hasim.orbittime.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * Built on [lightColorScheme] (not `darkColorScheme`) and with every container/surface
 * role explicitly set — leaving any role unset lets stock Material3 components (AlertDialog,
 * DatePickerDialog, TimePicker, DropdownMenu) fall back to Material's own default palette,
 * which is dark and clashes badly with this app's light cream design regardless of the
 * phone's system light/dark setting.
 */
private val OrbitColorScheme = lightColorScheme(
    primary = OrbitColors.violet600,
    onPrimary = OrbitColors.cream50,
    primaryContainer = OrbitColors.lavenderWhite,
    onPrimaryContainer = OrbitColors.ink900,
    secondary = OrbitColors.blue500,
    onSecondary = OrbitColors.cream50,
    secondaryContainer = OrbitColors.lavenderWhite,
    onSecondaryContainer = OrbitColors.ink900,
    tertiary = OrbitColors.coral500,
    onTertiary = OrbitColors.ink900,
    tertiaryContainer = OrbitColors.sand100,
    onTertiaryContainer = OrbitColors.ink900,
    background = OrbitColors.cream100,
    onBackground = OrbitColors.ink900,
    surface = OrbitColors.cream50,
    onSurface = OrbitColors.ink900,
    surfaceVariant = OrbitColors.mist,
    onSurfaceVariant = OrbitColors.slate600,
    surfaceContainerLowest = OrbitColors.cream50,
    surfaceContainerLow = OrbitColors.cream100,
    surfaceContainer = OrbitColors.cream200,
    surfaceContainerHigh = OrbitColors.cream50,
    surfaceContainerHighest = OrbitColors.mist,
    outline = OrbitColors.slate300,
    outlineVariant = OrbitColors.slate200,
    error = OrbitColors.danger,
    onError = OrbitColors.cream50,
    errorContainer = OrbitColors.dangerBg,
    onErrorContainer = OrbitColors.danger,
    inverseSurface = OrbitColors.ink900,
    inverseOnSurface = OrbitColors.cream50,
    inversePrimary = OrbitColors.violet500,
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
