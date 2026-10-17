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
/** Fixed colours used to draw the Rave buddy character. */
data class BuddyColors(
    val face: Color = BuddyFace,
    val cheek: Color = BuddyCheek,
    val gold: Color = BuddyGold,
    val leaf: Color = BuddyLeaf,
    val ribbon: Color = BuddyRibbon,
)

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
    val buddy: BuddyColors = BuddyColors(),
    val verdictGo: TileColor = TileColor(VerdictGoSoft, VerdictGo),
    val verdictSometimes: TileColor = TileColor(VerdictMidSoft, VerdictMid),
    val verdictSkip: TileColor = TileColor(VerdictSkipSoft, VerdictSkip),
    /** Bottom bar: always black with a lime highlight (Bento Pop). */
    val navContent: Color = NavInactive,
    val navActive: Color = BentoLime,
    /** Lime "Start" button and bright highlights on black tiles. */
    val highlight: Color = BentoLime,
    val onHighlight: Color = BentoInk,
    /** Big call-to-action tile ("Start a walk"). */
    val cta: TileColor = TileColor(BentoOrange, BentoInk),
    /** Bento tiles beyond the four habit tiles. */
    val tileYellow: TileColor = TileColor(TileYellow, OnTileYellow),
    val tileOrange: TileColor = TileColor(BentoOrange, BentoInk),
    val tileInk: TileColor = TileColor(BentoInk, White),
    val tileSand: TileColor = TileColor(BentoSand, BentoInk),
    /** Raised surface on an ink tile (Pause button, unselected plan rows). */
    val inkRaised: Color = BentoInkSoft,
    /** Bold verdict hero colours (Scan result, recent scans). */
    val verdictHeroGo: TileColor = TileColor(BentoLime, BentoInk),
    val verdictHeroSometimes: TileColor = TileColor(TileYellow, BentoInk),
    val verdictHeroSkip: TileColor = TileColor(TileCoral, BentoInk),
    val verdictChipSkip: TileColor = TileColor(TileCoralSoft, BentoInk),
) {
    val tiles: List<TileColor> get() = listOf(tilePeach, tileGreen, tilePink, tileBlue)
}

internal val LightExtendedColors = ExtendedColors(
    backgroundTop = LightBackgroundTop,
    backgroundBottom = LightBackgroundBottom,
    accentCard = LightAccentCard,
    onAccentCard = LightOnAccentCard,
    navBar = BentoInk,
    navBarShadow = LightNavShadow,
    streakFlame = BentoOrange,
    tilePeach = TileColor(TilePeach, OnTilePeach),
    tileGreen = TileColor(TileGreen, OnTileGreen),
    tilePink = TileColor(TilePink, OnTilePink),
    tileBlue = TileColor(TileBlue, OnTileBlue),
    chart = listOf(Chart1, Chart2, Chart3, Chart4),
)

internal val DarkExtendedColors = ExtendedColors(
    backgroundTop = DarkBackground,
    backgroundBottom = DarkBackground,
    accentCard = DarkSurfaceHigh,
    onAccentCard = DarkTextPrimary,
    navBar = BentoInk,
    navBarShadow = DarkNavShadow,
    streakFlame = BentoOrange,
    tilePeach = TileColor(DarkTilePeach, DarkOnTilePeach),
    tileGreen = TileColor(DarkTileGreen, DarkOnTileGreen),
    tilePink = TileColor(DarkTilePink, DarkOnTilePink),
    tileBlue = TileColor(DarkTileBlue, DarkOnTileBlue),
    chart = listOf(DarkChart1, DarkChart2, DarkChart3, DarkChart4),
)

internal val ClassicLightExtendedColors = ExtendedColors(
    backgroundTop = ClassicLightBackgroundTop,
    backgroundBottom = ClassicLightBackgroundBottom,
    accentCard = ClassicLightAccentCard,
    onAccentCard = White,
    navBar = LightSurface,
    navBarShadow = ClassicLightNavShadow,
    streakFlame = ClassicLightAccent,
    tilePeach = TileColor(ClassicTilePeach, ClassicOnTilePeach),
    tileGreen = TileColor(ClassicTileGreen, ClassicOnTileGreen),
    tilePink = TileColor(ClassicTilePink, ClassicOnTilePink),
    tileBlue = TileColor(ClassicTileBlue, ClassicOnTileBlue),
    chart = listOf(ClassicChart1, ClassicChart2, ClassicChart3, ClassicChart4),
    navContent = ClassicLightTextSecondary,
    navActive = ClassicLightAccent,
    highlight = ClassicLightAccent,
    onHighlight = White,
    cta = TileColor(ClassicLightAccent, White),
    tileYellow = TileColor(ClassicTileYellow, ClassicOnTileYellow),
    tileOrange = TileColor(ClassicLightAccent, White),
    tileInk = TileColor(ClassicLightAccentCard, White),
    tileSand = TileColor(ClassicLightSurfaceWarm, ClassicLightTextPrimary),
    inkRaised = ClassicLightAccentCardRaised,
    verdictHeroGo = TileColor(VerdictGoSoft, VerdictGo),
    verdictHeroSometimes = TileColor(VerdictMidSoft, VerdictMid),
    verdictHeroSkip = TileColor(VerdictSkipSoft, VerdictSkip),
    verdictChipSkip = TileColor(VerdictSkipSoft, VerdictSkip),
)

internal val ClassicDarkExtendedColors = ExtendedColors(
    backgroundTop = ClassicDarkBackground,
    backgroundBottom = ClassicDarkBackground,
    accentCard = ClassicDarkSurfaceHighest,
    onAccentCard = ClassicDarkTextPrimary,
    navBar = ClassicDarkSurface,
    navBarShadow = DarkNavShadow,
    streakFlame = ClassicDarkAccent,
    tilePeach = TileColor(ClassicDarkTilePeach, ClassicDarkOnTilePeach),
    tileGreen = TileColor(ClassicDarkTileGreen, ClassicDarkOnTileGreen),
    tilePink = TileColor(ClassicDarkTilePink, ClassicDarkOnTilePink),
    tileBlue = TileColor(ClassicDarkTileBlue, ClassicDarkOnTileBlue),
    chart = listOf(ClassicDarkChart1, ClassicDarkChart2, ClassicDarkChart3, ClassicDarkChart4),
    navContent = ClassicDarkTextSecondary,
    navActive = ClassicDarkAccent,
    highlight = ClassicDarkAccent,
    onHighlight = ClassicDarkOnAccent,
    cta = TileColor(ClassicDarkAccent, ClassicDarkOnAccent),
    tileYellow = TileColor(ClassicDarkTileYellow, ClassicDarkOnTileYellow),
    tileOrange = TileColor(ClassicDarkAccent, ClassicDarkOnAccent),
    tileInk = TileColor(ClassicDarkSurfaceHighest, ClassicDarkTextPrimary),
    tileSand = TileColor(ClassicDarkSurfaceHigh, ClassicDarkTextPrimary),
    inkRaised = ClassicDarkSurfaceHigh,
)

val LocalExtendedColors = staticCompositionLocalOf { LightExtendedColors }
