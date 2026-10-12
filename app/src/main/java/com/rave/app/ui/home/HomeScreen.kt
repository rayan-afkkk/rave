package com.rave.app.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.rave.app.R
import com.rave.app.domain.model.ActivityType
import com.rave.app.domain.util.DayPeriod
import com.rave.app.domain.util.dayPeriodFor
import com.rave.app.ui.components.EmptyState
import com.rave.app.ui.components.PillButton
import com.rave.app.ui.components.StreakChip
import com.rave.app.ui.components.TonalPillButton
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Home. Phase 1: greeting header, streak chip and quick-start buttons.
 * The progress ring, calendar strip and routine list arrive in Phase 2.
 */
@Composable
fun HomeScreen(
    contentPadding: PaddingValues,
    onStartActivity: (ActivityType) -> Unit,
) {
    val now = remember { LocalDateTime.now() }
    val greetingRes = when (dayPeriodFor(now.hour)) {
        DayPeriod.MORNING -> R.string.greeting_morning
        DayPeriod.AFTERNOON -> R.string.greeting_afternoon
        DayPeriod.EVENING -> R.string.greeting_evening
    }
    val date = remember(now) {
        now.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault()))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(greetingRes, stringResource(R.string.guest_name)),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.semantics { heading() },
                )
                Text(
                    text = date,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            StreakChip(days = 0)
        }

        Spacer(Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PillButton(
                text = stringResource(R.string.start_walk),
                icon = Icons.AutoMirrored.Rounded.DirectionsWalk,
                onClick = { onStartActivity(ActivityType.WALK) },
                modifier = Modifier.weight(1f),
            )
            TonalPillButton(
                text = stringResource(R.string.start_run),
                icon = Icons.AutoMirrored.Rounded.DirectionsRun,
                onClick = { onStartActivity(ActivityType.RUN) },
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(32.dp))
        EmptyState(
            icon = Icons.Rounded.Spa,
            title = stringResource(R.string.home_empty_title),
            message = stringResource(R.string.home_empty_message),
        )
    }
}
