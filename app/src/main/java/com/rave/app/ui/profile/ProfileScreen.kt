package com.rave.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rave.app.R
import com.rave.app.domain.buddy.BuddyStage
import com.rave.app.domain.habit.BadgeId
import com.rave.app.domain.habit.StreakEngine
import com.rave.app.domain.util.Format
import com.rave.app.ui.badgeDescriptionRes
import com.rave.app.ui.badgeIcon
import com.rave.app.ui.badgeTitleRes
import com.rave.app.ui.components.BentoAvatar
import com.rave.app.ui.components.BentoBig
import com.rave.app.ui.components.BentoButton
import com.rave.app.ui.components.BentoChip
import com.rave.app.ui.components.BentoGap
import com.rave.app.ui.components.BentoIconButton
import com.rave.app.ui.components.BentoKicker
import com.rave.app.ui.components.BentoTile
import com.rave.app.ui.components.BentoTitle
import com.rave.app.ui.components.LoadingState
import com.rave.app.ui.home.BuddyViewModel
import com.rave.app.ui.pro.rememberPro
import com.rave.app.ui.theme.Dimens
import com.rave.app.ui.theme.RaveTheme
import com.rave.app.ui.theme.TileColor
import com.rave.app.ui.unitLabel
import java.util.Locale

/*
 * Bento Pop Profile: black identity card with level + XP, three stat tiles,
 * a badge grid, Progress / Wrapped buttons, then streak freezes.
 */

private val BadgeShape = RoundedCornerShape(22.dp)

@Composable
fun ProfileScreen(
    contentPadding: PaddingValues,
    onOpenSettings: () -> Unit,
    onOpenProgress: () -> Unit,
    onOpenWrapped: (Int) -> Unit,
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
    val c = RaveTheme.colors

    Column(
        Modifier
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
            .padding(horizontal = Dimens.ScreenPadding, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(BentoGap),
    ) {
        // Title + settings
        Row(verticalAlignment = Alignment.CenterVertically) {
            BentoTitle(stringResource(R.string.nav_profile), Modifier.weight(1f).semantics { heading() })
            BentoIconButton(Icons.Rounded.Settings, stringResource(R.string.settings_title), onOpenSettings, size = 44.dp)
        }
        Spacer(Modifier.height(4.dp))

        IdentityCard(state, onSignIn)

        // Stats
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(BentoGap)) {
            val km = stats.totalDistanceM / state.unit.meters
            MiniStat(Format.distance(stats.totalDistanceM, state.unit, if (km >= 100) 0 else 1), unitLabel(state.unit), c.tileOrange, Modifier.weight(1f))
            MiniStat(stats.bestStreak.toString(), stringResource(R.string.profile_best_streak), c.tileGreen, Modifier.weight(1f))
            MiniStat(stats.totalActivities.toString(), stringResource(R.string.filter_walks), c.tileBlue, Modifier.weight(1f))
        }

        // Badges
        val all = BadgeId.entries
        val earnedCount = all.count { it in stats.badges }
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.profile_badges),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.weight(1f).semantics { heading() },
            )
            Text(
                stringResource(R.string.bprofile_badges_count, earnedCount, all.size),
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
            )
        }
        val badgeTiles = listOf(c.tilePink, c.tileOrange, c.tileBlue, c.tileGreen, c.tileYellow)
        val ordered = all.sortedBy { it !in stats.badges }
        ordered.chunked(4).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(BentoGap)) {
                row.forEach { badge ->
                    val earned = badge in stats.badges
                    val tile = if (earned) badgeTiles[badge.ordinal % badgeTiles.size] else c.tileSand
                    val title = stringResource(badgeTitleRes(badge))
                    val cd = stringResource(if (earned) R.string.cd_badge_earned else R.string.cd_badge_locked, title)
                    Box(
                        Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(BadgeShape)
                            .background(tile.container)
                            .clickable(role = Role.Button) { badgeInfo = badge }
                            .semantics { contentDescription = cd },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(if (earned) badgeIcon(badge) else Icons.Rounded.Lock, null, tint = tile.content, modifier = Modifier.size(30.dp))
                    }
                }
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }

        // Progress / Wrapped
        val wrappedYear = remember { com.rave.app.domain.wrapped.WrappedCalculator.seasonYear(java.time.LocalDate.now()) ?: java.time.LocalDate.now().year }
        Row(Modifier.fillMaxWidth().padding(top = 2.dp), horizontalArrangement = Arrangement.spacedBy(BentoGap)) {
            BentoButton(
                stringResource(R.string.progress_title), c.tilePink.container, c.tilePink.content, onOpenProgress,
                Modifier.weight(1f), leading = Icons.Rounded.Insights, height = 58.dp,
            )
            BentoButton(
                stringResource(R.string.bprofile_wrapped), c.tileSand.container, c.tileSand.content, { onOpenWrapped(wrappedYear) },
                Modifier.weight(1f), leading = Icons.Rounded.AutoAwesome, height = 58.dp,
            )
        }

        // Streak freezes
        BentoTile(c.tileYellow.container, Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(c.tileInk.container),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.AcUnit, null, tint = c.tileYellow.container, modifier = Modifier.size(26.dp)) }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    BentoKicker(stringResource(R.string.bprofile_freezes), c.tileYellow.content)
                    Text(
                        pluralStringResource(R.plurals.freezes_count, stats.freezes, stats.freezes),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold), color = c.tileYellow.content,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.profile_freezes_help, StreakEngine.DAYS_PER_FREEZE, StreakEngine.MAX_FREEZES),
                style = MaterialTheme.typography.bodySmall, color = c.tileYellow.content.copy(alpha = 0.75f),
            )
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

/** Black card: initial avatar, name, handle, PRO chip, level + XP bar, and sign-in when signed out. */
@Composable
private fun IdentityCard(state: ProfileUiState, onSignIn: () -> Unit, buddyViewModel: BuddyViewModel = hiltViewModel()) {
    val c = RaveTheme.colors
    val ink = c.accentCard
    val on = c.onAccentCard
    val pro = rememberPro()
    val buddy by buddyViewModel.state.collectAsStateWithLifecycle()
    val name = state.name.ifBlank { stringResource(R.string.profile_guest) }

    BentoTile(ink, Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BentoAvatar(name, c.tilePeach, size = 64.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    name, color = on, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.4).sp),
                )
                Text(
                    state.profile?.username?.let { "@$it" } ?: stringResource(R.string.profile_guest_subtitle),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp), color = on.copy(alpha = 0.6f),
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            if (pro.isPro) {
                Spacer(Modifier.width(8.dp))
                BentoChip(stringResource(R.string.bprofile_pro), c.highlight, c.onHighlight)
            }
        }
        Spacer(Modifier.height(14.dp))
        val b = buddy
        val levelText: String
        val xpText: String?
        val progress: Float
        if (b != null) {
            levelText = stringResource(R.string.buddy_level, b.level, stringResource(stageName(b.stage)))
            xpText = stringResource(R.string.bprofile_xp, b.xpIntoLevel, b.xpForNextLevel)
            progress = b.levelProgress
        } else {
            levelText = stringResource(R.string.profile_level, state.stats.level)
            xpText = null
            progress = state.stats.levelProgress
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                levelText, color = on, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold),
            )
            if (xpText != null) {
                Text(xpText, style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp), color = on.copy(alpha = 0.6f))
            }
        }
        Spacer(Modifier.height(6.dp))
        XpBar(progress, track = on.copy(alpha = 0.16f), fill = c.highlight)

        if (state.user == null) {
            Spacer(Modifier.height(14.dp))
            if (state.cloudAvailable) {
                BentoButton(
                    stringResource(R.string.profile_sign_in_cta), c.highlight, c.onHighlight, onSignIn,
                    Modifier.fillMaxWidth(), height = 48.dp,
                )
            } else {
                Text(stringResource(R.string.cloud_unavailable), style = MaterialTheme.typography.bodySmall, color = on.copy(alpha = 0.6f))
            }
        }
    }
}

@Composable
private fun XpBar(progress: Float, track: Color, fill: Color) {
    val shape = RoundedCornerShape(5.dp)
    Box(Modifier.fillMaxWidth().height(10.dp).clip(shape).background(track)) {
        Box(Modifier.fillMaxWidth(progress.coerceIn(0.03f, 1f)).height(10.dp).clip(shape).background(fill))
    }
}

/** Compact value + caption tile (412 KM). */
@Composable
private fun MiniStat(value: String, label: String, tile: TileColor, modifier: Modifier) {
    BentoTile(tile.container, modifier, padding = 12.dp) {
        BentoBig(value, tile.content, size = 22.sp, maxLines = 1)
        Text(
            label.uppercase(Locale.getDefault()), color = tile.content.copy(alpha = 0.6f), maxLines = 1, overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp),
        )
    }
}

private fun stageName(stage: BuddyStage) = when (stage) {
    BuddyStage.SPROUT -> R.string.buddy_stage_sprout
    BuddyStage.BUDDY -> R.string.buddy_stage_buddy
    BuddyStage.RUNNER -> R.string.buddy_stage_runner
    BuddyStage.CHAMP -> R.string.buddy_stage_champ
    BuddyStage.LEGEND -> R.string.buddy_stage_legend
}
