package com.rave.app.ui

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Stars
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.rave.app.R
import com.rave.app.domain.habit.BadgeId
import com.rave.app.domain.model.ActivityType
import com.rave.app.domain.model.DistanceUnit
import com.rave.app.domain.model.PbTag

/* Shared mappings from domain values to user-facing labels and icons. */

@Composable
fun unitLabel(unit: DistanceUnit): String =
    stringResource(if (unit == DistanceUnit.MILES) R.string.unit_mi else R.string.unit_km)

/** [unitLabel] for code outside Compose (notifications). */
fun unitLabelText(context: android.content.Context, unit: DistanceUnit): String =
    context.getString(if (unit == DistanceUnit.MILES) R.string.unit_mi else R.string.unit_km)

@Composable
fun paceUnitLabel(unit: DistanceUnit): String =
    stringResource(if (unit == DistanceUnit.MILES) R.string.unit_pace_mi else R.string.unit_pace_km)

@StringRes
fun activityTypeRes(type: ActivityType): Int = if (type == ActivityType.RUN) R.string.activity_run else R.string.activity_walk

fun activityTypeIcon(type: ActivityType): ImageVector =
    if (type == ActivityType.RUN) Icons.AutoMirrored.Rounded.DirectionsRun else Icons.AutoMirrored.Rounded.DirectionsWalk

@StringRes
fun pbTagRes(tag: PbTag): Int = when (tag) {
    PbTag.FASTEST_1K -> R.string.pb_fastest_1k
    PbTag.FASTEST_5K -> R.string.pb_fastest_5k
    PbTag.LONGEST_DISTANCE -> R.string.pb_longest
}

@StringRes
fun badgeTitleRes(badge: BadgeId): Int = when (badge) {
    BadgeId.FIRST_ACTIVITY -> R.string.badge_first_activity
    BadgeId.FIRST_5K -> R.string.badge_first_5k
    BadgeId.RUN_10K -> R.string.badge_run_10k
    BadgeId.KM_100 -> R.string.badge_km_100
    BadgeId.STREAK_7 -> R.string.badge_streak_7
    BadgeId.STREAK_30 -> R.string.badge_streak_30
    BadgeId.EARLY_BIRD -> R.string.badge_early_bird
    BadgeId.HABITS_100 -> R.string.badge_habits_100
    BadgeId.COMPETITION_WINNER -> R.string.badge_competition_winner
    BadgeId.SOCIAL -> R.string.badge_social
    BadgeId.FIRST_WORKOUT -> R.string.badge_first_workout
    BadgeId.SETS_100 -> R.string.badge_sets_100
}

@StringRes
fun badgeDescriptionRes(badge: BadgeId): Int = when (badge) {
    BadgeId.FIRST_ACTIVITY -> R.string.badge_first_activity_desc
    BadgeId.FIRST_5K -> R.string.badge_first_5k_desc
    BadgeId.RUN_10K -> R.string.badge_run_10k_desc
    BadgeId.KM_100 -> R.string.badge_km_100_desc
    BadgeId.STREAK_7 -> R.string.badge_streak_7_desc
    BadgeId.STREAK_30 -> R.string.badge_streak_30_desc
    BadgeId.EARLY_BIRD -> R.string.badge_early_bird_desc
    BadgeId.HABITS_100 -> R.string.badge_habits_100_desc
    BadgeId.COMPETITION_WINNER -> R.string.badge_competition_winner_desc
    BadgeId.SOCIAL -> R.string.badge_social_desc
    BadgeId.FIRST_WORKOUT -> R.string.badge_first_workout_desc
    BadgeId.SETS_100 -> R.string.badge_sets_100_desc
}

fun badgeIcon(badge: BadgeId): ImageVector = when (badge) {
    BadgeId.FIRST_ACTIVITY -> Icons.AutoMirrored.Rounded.DirectionsWalk
    BadgeId.FIRST_5K -> Icons.Rounded.Flag
    BadgeId.RUN_10K -> Icons.AutoMirrored.Rounded.DirectionsRun
    BadgeId.KM_100 -> Icons.Rounded.Stars
    BadgeId.STREAK_7 -> Icons.Rounded.LocalFireDepartment
    BadgeId.STREAK_30 -> Icons.Rounded.Whatshot
    BadgeId.EARLY_BIRD -> Icons.Rounded.WbTwilight
    BadgeId.HABITS_100 -> Icons.Rounded.CheckCircle
    BadgeId.COMPETITION_WINNER -> Icons.Rounded.EmojiEvents
    BadgeId.SOCIAL -> Icons.Rounded.Group
    BadgeId.FIRST_WORKOUT -> Icons.Rounded.FitnessCenter
    BadgeId.SETS_100 -> Icons.Rounded.Bolt
}

@StringRes
fun unitRes(unit: DistanceUnit): Int = if (unit == DistanceUnit.MILES) R.string.unit_mi else R.string.unit_km
