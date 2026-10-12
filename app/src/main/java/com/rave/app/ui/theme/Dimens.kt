package com.rave.app.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Shared sizes so every screen uses the same spacing, button heights and card padding.
 * Combined with the fixed font scale in [RaveTheme], layouts look identical on every phone
 * regardless of the system font-size setting.
 */
object Dimens {
    /** Horizontal padding of screen content. */
    val ScreenPadding = 16.dp
    /** Inner padding of cards. */
    val CardPadding = 14.dp
    /** Vertical gap between cards/sections. */
    val SectionGap = 10.dp
    /** Pill buttons. */
    val ButtonHeight = 48.dp
    /** Filter / choice chips. */
    val ChipHeight = 36.dp
    /** Minimum height of list rows and habit cards (keeps the 48dp touch target). */
    val RowMinHeight = 60.dp
    /** Icon tiles on cards. */
    val IconTile = 38.dp
    val IconTileIcon = 21.dp
    /** Floating bottom navigation. */
    val NavBarHeight = 62.dp
    val StartButton = 56.dp
}
