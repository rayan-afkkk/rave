package com.rave.app.ui.theme

import androidx.compose.ui.graphics.Color

/*
 * Raw palette. This is the ONLY file that may contain colour literals.
 * Composables must read colours through MaterialTheme.colorScheme or
 * RaveTheme.colors (see Theme.kt), never from here directly.
 */

// ---- Light ----
internal val LightBackgroundTop = Color(0xFFFFF6EC)
internal val LightBackgroundBottom = Color(0xFFFDEBDD)
internal val LightSurface = Color(0xFFFFFFFF)
internal val LightSurfaceSoft = Color(0xFFFFFAF5)
internal val LightSurfaceWarm = Color(0xFFFFF3E8)
internal val LightTextPrimary = Color(0xFF3B2219)
internal val LightTextSecondary = Color(0xFF7A5A4C)
internal val LightAccent = Color(0xFFF26B1D)
internal val LightOnAccent = Color(0xFFFFFFFF)
internal val LightAccentCard = Color(0xFF3B2219)
internal val LightOnAccentCard = Color(0xFFFFFFFF)
internal val LightOutline = Color(0xFFEAD7C8)
internal val LightOutlineVariant = Color(0xFFF3E4D8)
internal val LightError = Color(0xFFC4372B)
internal val LightNavShadow = Color(0x333B2219)

internal val TilePeach = Color(0xFFFDE3CF)
internal val TileGreen = Color(0xFFE3EDD2)
internal val TilePink = Color(0xFFF4DCEF)
internal val TileBlue = Color(0xFFDDE9F4)
internal val OnTilePeach = Color(0xFF9A4518)
internal val OnTileGreen = Color(0xFF5F7A2A)
internal val OnTilePink = Color(0xFFB8417F)
internal val OnTileBlue = Color(0xFF3A6A96)

internal val Chart1 = Color(0xFF4A2C22)
internal val Chart2 = Color(0xFF9A4518)
internal val Chart3 = Color(0xFF5F7A2A)
internal val Chart4 = Color(0xFFB8417F)

// ---- Splash (same in light and dark: the brand launch screen is always black) ----
internal val SplashBackground = Color(0xFF17110E)
internal val SplashMark = Color(0xFFFF7A2E)
internal val SplashText = Color(0xFFF6E9DF)

// ---- Rave buddy (character drawing, same in both themes) ----
internal val BuddyFace = Color(0xE63B2219)
internal val BuddyCheek = Color(0xFFFFB98C)
internal val BuddyGold = Color(0xFFF6C344)
internal val BuddyLeaf = Color(0xFF6E9A3A)
internal val BuddyRibbon = Color(0xFF3A6A96)

// ---- Food verdicts (traffic light) ----
internal val VerdictGo = Color(0xFF2E8B57)
internal val VerdictGoSoft = Color(0xFFE2F2E8)
internal val VerdictMid = Color(0xFFB7791F)
internal val VerdictMidSoft = Color(0xFFFFF0CC)
internal val VerdictSkip = Color(0xFFC9402F)
internal val VerdictSkipSoft = Color(0xFFFBE2DE)

// ---- Dark ----
internal val DarkBackground = Color(0xFF17110E)
internal val DarkSurface = Color(0xFF241A16)
internal val DarkSurfaceHigh = Color(0xFF2C211C)
internal val DarkSurfaceHighest = Color(0xFF342721)
internal val DarkBorder = Color(0xFF3A2A23)
internal val DarkTextPrimary = Color(0xFFF6E9DF)
internal val DarkTextSecondary = Color(0xFFB49A8D)
internal val DarkAccent = Color(0xFFFF7A2E)
internal val DarkOnAccent = Color(0xFF1A0F0A)
internal val DarkAccentContainer = Color(0xFF4A2A18)
internal val DarkOnAccentContainer = Color(0xFFFFD9C2)
internal val DarkError = Color(0xFFFF8A7A)
internal val DarkNavShadow = Color(0x66000000)

internal val DarkTilePeach = Color(0xFF45291A)
internal val DarkTileGreen = Color(0xFF2E3A20)
internal val DarkTilePink = Color(0xFF41243A)
internal val DarkTileBlue = Color(0xFF233242)
internal val DarkOnTilePeach = Color(0xFFFFB98C)
internal val DarkOnTileGreen = Color(0xFFB9D58A)
internal val DarkOnTilePink = Color(0xFFF2A6D3)
internal val DarkOnTileBlue = Color(0xFFA9C8E6)

internal val DarkChart1 = Color(0xFFE7CDBE)
internal val DarkChart2 = Color(0xFFFF9A5C)
internal val DarkChart3 = Color(0xFFA9C46B)
internal val DarkChart4 = Color(0xFFE07AB4)

internal val Black = Color(0xFF000000)
internal val White = Color(0xFFFFFFFF)

// Premium share-card templates and route colours (Rave Pro)
internal val ShareNightTop = Color(0xFF0E1530)
internal val ShareNightBottom = Color(0xFF05070F)
internal val ShareNeon = Color(0xFFFF7A2E)
internal val ShareNightText = Color(0xFFEAF0FF)
internal val ShareNightMuted = Color(0xFF9AA6C8)
internal val ShareSunsetA = Color(0xFF5B2A86)
internal val ShareSunsetB = Color(0xFFE2557A)
internal val ShareSunsetC = Color(0xFFFFB25C)
internal val RouteBlue = Color(0xFF3D8BFF)
internal val RouteGreen = Color(0xFF3CCB7F)
internal val RoutePink = Color(0xFFFF4FA3)
internal val RouteGold = Color(0xFFFFC23D)
