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

// Compact scale. The app pins fontScale to 1 (see RaveTheme), so these render at exactly this
// size on every phone regardless of the system font-size setting.
val RaveTypography = Typography(
    displayLarge = style(48, 52, FontWeight.Bold, -1.0),   // big live distance
    displayMedium = style(38, 44, FontWeight.Bold, -0.5),
    displaySmall = style(30, 36, FontWeight.Bold),
    headlineLarge = style(27, 33, FontWeight.Bold),
    headlineMedium = style(23, 29, FontWeight.SemiBold),
    headlineSmall = style(20, 26, FontWeight.SemiBold),
    titleLarge = style(18, 24, FontWeight.SemiBold),
    titleMedium = style(15, 21, FontWeight.SemiBold),
    titleSmall = style(14, 19, FontWeight.Medium),
    bodyLarge = style(14, 21, FontWeight.Normal),
    bodyMedium = style(13, 18, FontWeight.Normal),
    bodySmall = style(11, 15, FontWeight.Normal),
    labelLarge = style(14, 18, FontWeight.SemiBold),     // pill buttons
    labelMedium = style(12, 15, FontWeight.Medium),
    labelSmall = style(10, 13, FontWeight.Medium, 0.2),  // nav labels
)
