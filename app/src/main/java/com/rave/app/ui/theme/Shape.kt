package com.rave.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val RaveShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),   // habit cards, list items
    large = RoundedCornerShape(28.dp),    // bento tiles, big cards, sheets
    extraLarge = RoundedCornerShape(31.dp), // floating nav bar (half its height)
)

/** Fully rounded pill (chips, primary buttons, insight bars). */
val PillShape = RoundedCornerShape(percent = 50)
