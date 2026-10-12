package com.rave.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.runtime.staticCompositionLocalOf

internal val LightColors = lightColorScheme(
    primary = LightAccent,
    onPrimary = LightOnAccent,
    primaryContainer = TilePeach,
    onPrimaryContainer = LightTextPrimary,
    secondary = LightAccentCard,
    onSecondary = LightOnAccentCard,
    secondaryContainer = TilePeach,
    onSecondaryContainer = LightTextPrimary,
    tertiary = OnTileGreen,
    onTertiary = White,
    tertiaryContainer = TileGreen,
    onTertiaryContainer = LightTextPrimary,
    background = LightBackgroundTop,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightBackgroundBottom,
    onSurfaceVariant = LightTextSecondary,
    surfaceTint = LightAccent,
    surfaceBright = LightSurface,
    surfaceDim = LightBackgroundBottom,
    surfaceContainerLowest = LightSurface,
    surfaceContainerLow = LightSurface,
    surfaceContainer = LightSurfaceSoft,
    surfaceContainerHigh = LightSurfaceWarm,
    surfaceContainerHighest = LightBackgroundBottom,
    inverseSurface = LightAccentCard,
    inverseOnSurface = LightBackgroundTop,
    inversePrimary = DarkAccent,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    error = LightError,
    onError = White,
    scrim = Black,
)

internal val DarkColors = darkColorScheme(
    primary = DarkAccent,
    onPrimary = DarkOnAccent,
    primaryContainer = DarkAccentContainer,
    onPrimaryContainer = DarkOnAccentContainer,
    secondary = DarkTextPrimary,
    onSecondary = DarkBackground,
    secondaryContainer = DarkSurfaceHighest,
    onSecondaryContainer = DarkTextPrimary,
    tertiary = DarkOnTileGreen,
    onTertiary = DarkBackground,
    tertiaryContainer = DarkTileGreen,
    onTertiaryContainer = DarkTextPrimary,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceHigh,
    onSurfaceVariant = DarkTextSecondary,
    surfaceTint = DarkAccent,
    surfaceBright = DarkSurfaceHighest,
    surfaceDim = DarkBackground,
    surfaceContainerLowest = DarkBackground,
    surfaceContainerLow = DarkSurface,
    surfaceContainer = DarkSurface,
    surfaceContainerHigh = DarkSurfaceHigh,
    surfaceContainerHighest = DarkSurfaceHighest,
    inverseSurface = DarkTextPrimary,
    inverseOnSurface = DarkBackground,
    inversePrimary = LightAccent,
    outline = DarkBorder,
    outlineVariant = DarkBorder,
    error = DarkError,
    onError = DarkBackground,
    scrim = Black,
)

/**
 * App theme. Wraps MaterialTheme and also provides [ExtendedColors] for
 * tokens Material 3 has no slot for (gradient, habit tiles, chart colours…).
 */
@Composable
fun RaveTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val extended = if (darkTheme) DarkExtendedColors else LightExtendedColors
    // Ignore the system font-size setting so text, buttons and cards keep the same size on
    // every phone (the layout is designed around this fixed scale).
    val density = LocalDensity.current
    val fixedDensity = remember(density.density) { Density(density.density, fontScale = 1f) }
    CompositionLocalProvider(
        LocalExtendedColors provides extended,
        LocalIsDarkTheme provides darkTheme,
        LocalDensity provides fixedDensity,
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColors else LightColors,
            typography = RaveTypography,
            shapes = RaveShapes,
            content = content,
        )
    }
}

/** Whether the in-app theme (not just the system) is dark. Used for map styles. */
val LocalIsDarkTheme = staticCompositionLocalOf { false }

/** Shortcut for the extra design tokens: `RaveTheme.colors.tilePeach`. */
object RaveTheme {
    val colors: ExtendedColors
        @Composable @ReadOnlyComposable
        get() = LocalExtendedColors.current

    val isDark: Boolean
        @Composable @ReadOnlyComposable
        get() = LocalIsDarkTheme.current
}
