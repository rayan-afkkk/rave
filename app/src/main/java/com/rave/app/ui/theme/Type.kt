package com.rave.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.rave.app.R

/** Outfit (SIL OFL 1.1), bundled in res/font. */
val Outfit = FontFamily(
    Font(R.font.outfit_light, FontWeight.Light),
    Font(R.font.outfit_regular, FontWeight.Normal),
    Font(R.font.outfit_medium, FontWeight.Medium),
    Font(R.font.outfit_semibold, FontWeight.SemiBold),
    Font(R.font.outfit_bold, FontWeight.Bold),
    Font(R.font.outfit_extrabold, FontWeight.ExtraBold),
)

private fun style(size: Int, line: Int, weight: FontWeight, tracking: Double = 0.0) = TextStyle(
    fontFamily = Outfit,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = tracking.sp,
)

// All sizes are in sp so they scale with the user's font-size setting.
val RaveTypography = Typography(
    displayLarge = style(56, 60, FontWeight.Bold, -1.0),   // big live distance
    displayMedium = style(44, 50, FontWeight.Bold, -0.5),
    displaySmall = style(36, 42, FontWeight.Bold),
    headlineLarge = style(32, 38, FontWeight.Bold),
    headlineMedium = style(28, 34, FontWeight.SemiBold),
    headlineSmall = style(24, 30, FontWeight.SemiBold),
    titleLarge = style(22, 28, FontWeight.SemiBold),
    titleMedium = style(17, 24, FontWeight.SemiBold),
    titleSmall = style(15, 20, FontWeight.Medium),
    bodyLarge = style(16, 24, FontWeight.Normal),
    bodyMedium = style(14, 20, FontWeight.Normal),
    bodySmall = style(12, 16, FontWeight.Normal),
    labelLarge = style(16, 20, FontWeight.SemiBold),     // pill buttons
    labelMedium = style(13, 16, FontWeight.Medium),
    labelSmall = style(11, 14, FontWeight.Medium, 0.2),  // nav labels
)
