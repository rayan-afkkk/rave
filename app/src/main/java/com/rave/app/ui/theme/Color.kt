package com.rave.app.ui.theme

import androidx.compose.ui.graphics.Color

/*
 * Raw palette. This is the ONLY file that may contain colour literals.
 * Composables must read colours through MaterialTheme.colorScheme or
 * RaveTheme.colors (see Theme.kt), never from here directly.
 */

// ---- Light ----
internal val LightBackgroundTop = Color(0xFFFBF8F1)
internal val LightBackgroundBottom = Color(0xFFFBF8F1)
internal val LightSurface = Color(0xFFFFFFFF)
internal val LightSurfaceSoft = Color(0xFFF6F2E8)
internal val LightSurfaceWarm = Color(0xFFECE6D8)
internal val LightTextPrimary = Color(0xFF111111)
internal val LightTextSecondary = Color(0xFF6B6457)
internal val LightAccent = Color(0xFF111111)
internal val LightOnAccent = Color(0xFFFFFFFF)
internal val LightAccentCard = Color(0xFF111111)
internal val LightOnAccentCard = Color(0xFFFFFFFF)
internal val LightOutline = Color(0xFFE2DACB)
internal val LightOutlineVariant = Color(0xFFECE6D8)
internal val LightError = Color(0xFFC4372B)
internal val LightNavShadow = Color(0x33111111)

internal val TilePeach = Color(0xFFFFB385)
internal val TileGreen = Color(0xFFD7F36B)
internal val TilePink = Color(0xFFC9B8FF)
internal val TileBlue = Color(0xFFA8DBFF)
internal val OnTilePeach = Color(0xFF6E2A06)
internal val OnTileGreen = Color(0xFF3A4A00)
internal val OnTilePink = Color(0xFF34237A)
internal val OnTileBlue = Color(0xFF0B4370)

internal val Chart1 = Color(0xFF111111)
internal val Chart2 = Color(0xFFFF7A2E)
internal val Chart3 = Color(0xFF7A5CFF)
internal val Chart4 = Color(0xFF8DB800)

// ---- Bento Pop accents (same in both themes) ----
internal val BentoLime = Color(0xFFD7F36B)
internal val BentoOrange = Color(0xFFFF7A2E)
internal val BentoInk = Color(0xFF111111)
internal val NavInactive = Color(0xFF8A8A8A)

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
internal val DarkBackground = Color(0xFF0D0D0D)
internal val DarkSurface = Color(0xFF1A1A1A)
internal val DarkSurfaceHigh = Color(0xFF242424)
internal val DarkSurfaceHighest = Color(0xFF2E2E2E)
internal val DarkBorder = Color(0xFF2C2C2C)
internal val DarkTextPrimary = Color(0xFFF3F0E8)
internal val DarkTextSecondary = Color(0xFFA39E92)
internal val DarkAccent = Color(0xFFD7F36B)
internal val DarkOnAccent = Color(0xFF111111)
internal val DarkAccentContainer = Color(0xFF2F3A0A)
internal val DarkOnAccentContainer = Color(0xFFD7F36B)
internal val DarkError = Color(0xFFFF8A7A)
internal val DarkNavShadow = Color(0x66000000)

internal val DarkTilePeach = Color(0xFFFFB385)
internal val DarkTileGreen = Color(0xFFD7F36B)
internal val DarkTilePink = Color(0xFFC9B8FF)
internal val DarkTileBlue = Color(0xFFA8DBFF)
internal val DarkOnTilePeach = Color(0xFF6E2A06)
internal val DarkOnTileGreen = Color(0xFF3A4A00)
internal val DarkOnTilePink = Color(0xFF34237A)
internal val DarkOnTileBlue = Color(0xFF0B4370)

internal val DarkChart1 = Color(0xFFD7F36B)
internal val DarkChart2 = Color(0xFFFF7A2E)
internal val DarkChart3 = Color(0xFFC9B8FF)
internal val DarkChart4 = Color(0xFFA8DBFF)

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
