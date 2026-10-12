package com.rave.app.ui.competitions

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.rave.app.R
import com.rave.app.domain.competition.Competition
import com.rave.app.domain.competition.CompetitionMode
import com.rave.app.domain.competition.CompetitionType
import com.rave.app.domain.model.DistanceUnit
import com.rave.app.domain.util.Format
import com.rave.app.ui.unitLabel
import java.util.Locale

@StringRes
fun competitionTypeRes(type: CompetitionType): Int = when (type) {
    CompetitionType.DISTANCE -> R.string.comp_type_distance
    CompetitionType.ACTIVE_DAYS -> R.string.comp_type_active_days
    CompetitionType.HABITS_COMPLETED -> R.string.comp_type_habits
    CompetitionType.LONGEST_STREAK -> R.string.comp_type_streak
    CompetitionType.TARGET_RACE -> R.string.comp_type_race
}

@StringRes
fun competitionModeRes(mode: CompetitionMode): Int = when (mode) {
    CompetitionMode.DUEL -> R.string.comp_mode_duel
    CompetitionMode.GROUP -> R.string.comp_mode_group
    CompetitionMode.TEAMS -> R.string.comp_mode_teams
}

/** Formats a participant value in the competition's unit (km in the user's unit, days, habits…). */
@Composable
fun competitionValue(c: Competition, value: Double, unit: DistanceUnit): String = when (c.type) {
    CompetitionType.DISTANCE, CompetitionType.TARGET_RACE -> "${Format.distance(value * 1000, unit, 1)} ${unitLabel(unit)}"
    CompetitionType.ACTIVE_DAYS, CompetitionType.LONGEST_STREAK -> stringResource(R.string.comp_value_days, value.toInt())
    CompetitionType.HABITS_COMPLETED -> stringResource(R.string.comp_value_habits, value.toInt())
}

/** "2d 4h", "5h 12m", "12m". */
fun formatRemaining(ms: Long): String {
    val totalMin = (ms / 60_000).coerceAtLeast(0)
    val d = totalMin / (60 * 24)
    val h = (totalMin / 60) % 24
    val m = totalMin % 60
    return when {
        d > 0 -> String.format(Locale.getDefault(), "%dd %dh", d, h)
        h > 0 -> String.format(Locale.getDefault(), "%dh %dm", h, m)
        else -> String.format(Locale.getDefault(), "%dm", m)
    }
}
