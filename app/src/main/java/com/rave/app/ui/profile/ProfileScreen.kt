package com.rave.app.ui.profile

import androidx.compose.foundation.background
import com.rave.app.ui.theme.Dimens
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rave.app.R
import com.rave.app.domain.habit.BadgeId
import com.rave.app.domain.habit.StreakEngine
import com.rave.app.ui.badgeDescriptionRes
import com.rave.app.ui.badgeIcon
import com.rave.app.ui.badgeTitleRes
import com.rave.app.ui.components.Avatar
import com.rave.app.ui.components.LoadingState
import com.rave.app.ui.components.PillButton
import com.rave.app.ui.components.PillProgress
import com.rave.app.ui.components.SectionCard
import com.rave.app.ui.components.StatItem
import com.rave.app.ui.components.TonalPillButton
import com.rave.app.ui.theme.RaveTheme
import com.rave.app.ui.unitLabel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    contentPadding: PaddingValues,
    onOpenSettings: () -> Unit,
    onOpenProgress: () -> Unit,
    onSignIn: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    if (state.loading) {
        LoadingState()
        return
    }
    var badgeInfo by remember { mutableStateOf<BadgeId?>(null) }
    val stats = state.stats

    Column(
        Modifier
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
            .padding(horizontal = Dimens.ScreenPadding, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(Dimens.SectionGap),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.nav_profile), style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f).semantics { heading() })
            IconButton(onClick = onOpenSettings) { Icon(Icons.Rounded.Settings, contentDescription = stringResource(R.string.settings_title)) }
        }

        SectionCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(state.name.ifBlank { "?" }, size = 64.dp)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(state.name.ifBlank { stringResource(R.string.profile_guest) }, style = MaterialTheme.typography.titleLarge)
                    Text(
                        state.profile?.username?.let { "@$it" } ?: stringResource(R.string.profile_guest_subtitle),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.profile_level, stats.level), style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                Text(stringResource(R.string.profile_points_week, stats.pointsThisWeek), style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(6.dp))
            PillProgress(stats.levelProgress)
            if (state.user == null) {
                Spacer(Modifier.height(14.dp))
                if (state.cloudAvailable) {
                    PillButton(stringResource(R.string.profile_sign_in_cta), onClick = onSignIn, modifier = Modifier.fillMaxWidth())
                } else {
                    Text(stringResource(R.string.cloud_unavailable), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        SectionCard {
            Row(Modifier.fillMaxWidth()) {
                StatItem("%.1f".format(stats.totalDistanceM / state.unit.meters), stringResource(R.string.profile_total_distance),
                    Modifier.weight(1f), unit = unitLabel(state.unit))
                StatItem(stats.bestStreak.toString(), stringResource(R.string.profile_best_streak), Modifier.weight(1f))
                StatItem(stats.totalActivities.toString(), stringResource(R.string.profile_total_activities), Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            TonalPillButton(stringResource(R.string.profile_view_progress), onClick = onOpenProgress, icon = Icons.Rounded.Insights,
                modifier = Modifier.fillMaxWidth())
        }

        // Streak freezes
        SectionCard(color = RaveTheme.colors.tileBlue.container) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.AcUnit, null, tint = RaveTheme.colors.tileBlue.content, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(pluralStringResource(R.plurals.freezes_count, stats.freezes, stats.freezes), style = MaterialTheme.typography.titleSmall,
                        color = RaveTheme.colors.tileBlue.content)
                    Text(stringResource(R.string.profile_freezes_help, StreakEngine.DAYS_PER_FREEZE, StreakEngine.MAX_FREEZES),
                        style = MaterialTheme.typography.bodySmall, color = RaveTheme.colors.tileBlue.content)
                }
            }
        }

        // Badges
        SectionCard {
            Text(stringResource(R.string.profile_badges), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(12.dp), maxItemsInEachRow = 4) {
                BadgeId.entries.forEach { badge ->
                    val earned = badge in stats.badges
                    val title = stringResource(badgeTitleRes(badge))
                    val cd = stringResource(if (earned) R.string.cd_badge_earned else R.string.cd_badge_locked, title)
                    Column(
                        Modifier.weight(1f).semantics(mergeDescendants = true) { contentDescription = cd },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        val tiles = RaveTheme.colors.tiles
                        val tile = tiles[badge.ordinal % tiles.size]
                        Box(
                            Modifier.size(56.dp).clip(CircleShape)
                                .background(if (earned) tile.container else MaterialTheme.colorScheme.surfaceVariant)
                                .then(Modifier),
                            contentAlignment = Alignment.Center,
                        ) {
                            IconButton(onClick = { badgeInfo = badge }) {
                                Icon(
                                    if (earned) badgeIcon(badge) else Icons.Rounded.Lock, null,
                                    tint = if (earned) tile.content else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.alpha(if (earned) 1f else 0.6f),
                                )
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(title, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center, maxLines = 2,
                            color = if (earned) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }

    badgeInfo?.let { badge ->
        AlertDialog(
            onDismissRequest = { badgeInfo = null },
            confirmButton = { TextButton(onClick = { badgeInfo = null }) { Text(stringResource(R.string.action_ok)) } },
            icon = { Icon(badgeIcon(badge), null) },
            title = { Text(stringResource(badgeTitleRes(badge))) },
            text = { Text(stringResource(badgeDescriptionRes(badge))) },
            containerColor = MaterialTheme.colorScheme.surface,
        )
    }
}
