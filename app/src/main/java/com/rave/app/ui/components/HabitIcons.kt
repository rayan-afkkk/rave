package com.rave.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.Medication
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Park
import androidx.compose.material.icons.rounded.PedalBike
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.vector.ImageVector
import com.rave.app.domain.model.HabitColor
import com.rave.app.ui.theme.RaveTheme
import com.rave.app.ui.theme.TileColor

/** Icon keys stored in the database → vector icons. Keys must stay stable. */
object HabitIcons {
    val all: List<Pair<String, ImageVector>> = listOf(
        "check" to Icons.Rounded.CheckCircle,
        "water" to Icons.Rounded.WaterDrop,
        "walk" to Icons.AutoMirrored.Rounded.DirectionsWalk,
        "run" to Icons.AutoMirrored.Rounded.DirectionsRun,
        "meditate" to Icons.Rounded.SelfImprovement,
        "read" to Icons.Rounded.AutoStories,
        "sleep" to Icons.Rounded.Bedtime,
        "workout" to Icons.Rounded.FitnessCenter,
        "food" to Icons.Rounded.Restaurant,
        "journal" to Icons.Rounded.EditNote,
        "pill" to Icons.Rounded.Medication,
        "study" to Icons.Rounded.School,
        "music" to Icons.Rounded.MusicNote,
        "sun" to Icons.Rounded.WbSunny,
        "heart" to Icons.Rounded.Favorite,
        "code" to Icons.Rounded.Code,
        "art" to Icons.Rounded.Brush,
        "pets" to Icons.Rounded.Pets,
        "bike" to Icons.Rounded.PedalBike,
        "coffee" to Icons.Rounded.LocalCafe,
        "nature" to Icons.Rounded.Park,
        "language" to Icons.Rounded.Translate,
        "spa" to Icons.Rounded.Spa,
    )
    private val map = all.toMap()

    fun get(key: String): ImageVector = map[key] ?: Icons.Rounded.CheckCircle
}

@Composable
@ReadOnlyComposable
fun HabitColor.tile(): TileColor = when (this) {
    HabitColor.PEACH -> RaveTheme.colors.tilePeach
    HabitColor.GREEN -> RaveTheme.colors.tileGreen
    HabitColor.PINK -> RaveTheme.colors.tilePink
    HabitColor.BLUE -> RaveTheme.colors.tileBlue
}
