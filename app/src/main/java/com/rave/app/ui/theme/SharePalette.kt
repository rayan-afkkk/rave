package com.rave.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/** Share-card templates. Cards look the same in light and dark app themes. */
enum class ShareTemplate { WARM, DARK, MINIMAL, STICKER }

@Immutable
data class SharePalette(
    val background: Brush?,
    val text: Color,
    val secondaryText: Color,
    val route: Color,
    val accent: Color,
    /** Sticker template draws text with a soft shadow so it reads on any photo. */
    val shadow: Boolean = false,
    val shadowColor: Color = Color.Transparent,
)

fun ShareTemplate.palette(): SharePalette = when (this) {
    ShareTemplate.WARM -> SharePalette(
        background = Brush.linearGradient(listOf(TilePeach, LightAccent)),
        text = LightTextPrimary, secondaryText = LightTextPrimary.copy(alpha = 0.75f),
        route = White, accent = LightTextPrimary,
    )
    ShareTemplate.DARK -> SharePalette(
        background = Brush.verticalGradient(listOf(LightAccentCard, DarkBackground)),
        text = DarkTextPrimary, secondaryText = DarkTextSecondary, route = DarkAccent, accent = DarkAccent,
    )
    ShareTemplate.MINIMAL -> SharePalette(
        background = Brush.verticalGradient(listOf(White, White)),
        text = LightTextPrimary, secondaryText = LightTextSecondary, route = LightAccent, accent = LightAccent,
    )
    ShareTemplate.STICKER -> SharePalette(
        background = null,
        text = White, secondaryText = White.copy(alpha = 0.9f), route = White, accent = DarkAccent, shadow = true,
        shadowColor = Black.copy(alpha = 0.45f),
    )
}

/** Colours for the checkerboard shown behind the transparent sticker preview (not exported). */
val CheckerLight = Color(0xFFE9E1DA)
val CheckerDark = Color(0xFFD4C9C0)
