package com.rave.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** A soft coloured tile (habit icon background) and the colour of content drawn on it. */
@Immutable
data class TileColor(val container: Color, val content: Color)

/**
 * Design tokens that don't have a slot in Material 3's ColorScheme.
 * Read via [RaveTheme.colors].
 */
@Immutable
data class ExtendedColors(
    val backgroundTop: Color,
    val backgroundBottom: Color,
    val accentCard: Color,
    val onAccentCard: Color,
    val navBar: Color,
    val navBarShadow: Color,
    val streakFlame: Color,
    val tilePeach: TileColor,
    val tileGreen: TileColor,
    val tilePink: TileColor,
    val tileBlue: TileColor,
    val chart: List<Color>,
    val splashBackground: Color = SplashBackground,
    val splashMark: Color = SplashMark,
    val splashText: Color = SplashText,
) {
    val tiles: List<TileColor> get() = listOf(tilePeach, tileGreen, tilePink, tileBlue)
}

internal val LightExtendedColors = ExtendedColors(
    backgroundTop = LightBackgroundTop,
    backgroundBottom = LightBackgroundBottom,
    accentCard = LightAccentCard,
    onAccentCard = LightOnAccentCard,
    navBar = LightSurface,
    navBarShadow = LightNavShadow,
    streakFlame = LightAccent,
    tilePeach = TileColor(TilePeach, OnTilePeach),
    tileGreen = TileColor(TileGreen, OnTileGreen),
    tilePink = TileColor(TilePink, OnTilePink),
    tileBlue = TileColor(TileBlue, OnTileBlue),
    chart = listOf(Chart1, Chart2, Chart3, Chart4),
)

internal val DarkExtendedColors = ExtendedColors(
    backgroundTop = DarkBackground,
    backgroundBottom = DarkBackground,
    accentCard = DarkSurfaceHighest,
    onAccentCard = DarkTextPrimary,
    navBar = DarkSurface,
    navBarShadow = DarkNavShadow,
    streakFlame = DarkAccent,
    tilePeach = TileColor(DarkTilePeach, DarkOnTilePeach),
    tileGreen = TileColor(DarkTileGreen, DarkOnTileGreen),
    tilePink = TileColor(DarkTilePink, DarkOnTilePink),
    tileBlue = TileColor(DarkTileBlue, DarkOnTileBlue),
    chart = listOf(DarkChart1, DarkChart2, DarkChart3, DarkChart4),
)

val LocalExtendedColors = staticCompositionLocalOf { LightExtendedColors }
