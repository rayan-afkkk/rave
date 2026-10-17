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
import com.rave.app.domain.model.AppStyle

internal val LightColors = lightColorScheme(
    primary = LightAccent,
    onPrimary = LightOnAccent,
    primaryContainer = TileGreen,
    onPrimaryContainer = LightTextPrimary,
    secondary = BentoOrange,
    onSecondary = BentoInk,
    secondaryContainer = LightSurfaceWarm,
    onSecondaryContainer = LightTextPrimary,
    tertiary = OnTileGreen,
    onTertiary = White,
    tertiaryContainer = TileGreen,
    onTertiaryContainer = LightTextPrimary,
    background = LightBackgroundTop,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceWarm,
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
    secondary = BentoOrange,
    onSecondary = BentoInk,
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

internal val ClassicLightColors = lightColorScheme(
    primary = ClassicLightAccent,
    onPrimary = White,
    primaryContainer = ClassicTilePeach,
    onPrimaryContainer = ClassicLightTextPrimary,
    secondary = ClassicLightAccentCard,
    onSecondary = White,
    secondaryContainer = ClassicTilePeach,
    onSecondaryContainer = ClassicLightTextPrimary,
    tertiary = ClassicOnTileGreen,
    onTertiary = White,
    tertiaryContainer = ClassicTileGreen,
    onTertiaryContainer = ClassicLightTextPrimary,
    background = ClassicLightBackgroundTop,
    onBackground = ClassicLightTextPrimary,
    surface = LightSurface,
    onSurface = ClassicLightTextPrimary,
    surfaceVariant = ClassicLightSurfaceWarm,
    onSurfaceVariant = ClassicLightTextSecondary,
    surfaceTint = ClassicLightAccent,
    surfaceBright = LightSurface,
    surfaceDim = ClassicLightBackgroundBottom,
    surfaceContainerLowest = LightSurface,
    surfaceContainerLow = LightSurface,
    surfaceContainer = ClassicLightSurfaceSoft,
    surfaceContainerHigh = ClassicLightSurfaceWarm,
    surfaceContainerHighest = ClassicLightBackgroundBottom,
    inverseSurface = ClassicLightAccentCard,
    inverseOnSurface = ClassicLightBackgroundTop,
    inversePrimary = ClassicDarkAccent,
    outline = ClassicLightOutline,
    outlineVariant = ClassicLightOutlineVariant,
    error = LightError,
    onError = White,
    scrim = Black,
)

internal val ClassicDarkColors = darkColorScheme(
    primary = ClassicDarkAccent,
    onPrimary = ClassicDarkOnAccent,
    primaryContainer = ClassicDarkAccentContainer,
    onPrimaryContainer = ClassicDarkOnAccentContainer,
    secondary = ClassicDarkTextPrimary,
    onSecondary = ClassicDarkBackground,
    secondaryContainer = ClassicDarkSurfaceHighest,
    onSecondaryContainer = ClassicDarkTextPrimary,
    tertiary = ClassicDarkOnTileGreen,
    onTertiary = ClassicDarkBackground,
    tertiaryContainer = ClassicDarkTileGreen,
    onTertiaryContainer = ClassicDarkTextPrimary,
    background = ClassicDarkBackground,
    onBackground = ClassicDarkTextPrimary,
    surface = ClassicDarkSurface,
    onSurface = ClassicDarkTextPrimary,
    surfaceVariant = ClassicDarkSurfaceHigh,
    onSurfaceVariant = ClassicDarkTextSecondary,
    surfaceTint = ClassicDarkAccent,
    surfaceBright = ClassicDarkSurfaceHighest,
    surfaceDim = ClassicDarkBackground,
    surfaceContainerLowest = ClassicDarkBackground,
    surfaceContainerLow = ClassicDarkSurface,
    surfaceContainer = ClassicDarkSurface,
    surfaceContainerHigh = ClassicDarkSurfaceHigh,
    surfaceContainerHighest = ClassicDarkSurfaceHighest,
    inverseSurface = ClassicDarkTextPrimary,
    inverseOnSurface = ClassicDarkBackground,
    inversePrimary = ClassicLightAccent,
    outline = ClassicDarkBorder,
    outlineVariant = ClassicDarkBorder,
    error = DarkError,
    onError = ClassicDarkBackground,
    scrim = Black,
)

/**
 * App theme. Wraps MaterialTheme and also provides [ExtendedColors] for
 * tokens Material 3 has no slot for (gradient, habit tiles, chart colours…).
 */
@Composable
fun RaveTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    style: AppStyle = AppStyle.BENTO,
    content: @Composable () -> Unit,
) {
    val classic = style == AppStyle.CLASSIC
    val extended = when {
        classic && darkTheme -> ClassicDarkExtendedColors
        classic -> ClassicLightExtendedColors
        darkTheme -> DarkExtendedColors
        else -> LightExtendedColors
    }
    val scheme = when {
        classic && darkTheme -> ClassicDarkColors
        classic -> ClassicLightColors
        darkTheme -> DarkColors
        else -> LightColors
    }
    // Ignore the system font-size setting so text, buttons and cards keep the same size on
    // every phone (the layout is designed around this fixed scale).
    val density = LocalDensity.current
    // Slightly compact UI scale so the chunky bento tiles and text don't feel oversized.
    val fixedDensity = remember(density.density) { Density(density.density * UI_SCALE, fontScale = 1f) }
    CompositionLocalProvider(
        LocalExtendedColors provides extended,
        LocalIsDarkTheme provides darkTheme,
        LocalDensity provides fixedDensity,
    ) {
        MaterialTheme(
            colorScheme = scheme,
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


/** Global UI scale applied on top of the device density (1 = Android default sizes). */
private const val UI_SCALE = 0.92f
