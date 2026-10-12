package com.rave.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val RaveShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),   // habit cards, list items
    large = RoundedCornerShape(24.dp),    // big cards, sheets
    extraLarge = RoundedCornerShape(36.dp), // floating nav bar
)

/** Fully rounded pill (chips, primary buttons, insight bars). */
val PillShape = RoundedCornerShape(percent = 50)
