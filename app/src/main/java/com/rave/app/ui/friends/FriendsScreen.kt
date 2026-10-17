package com.rave.app.ui.friends

import android.widget.Toast
import com.rave.app.ui.components.SmoothCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rave.app.R
import com.rave.app.data.cloud.FeedItem
import com.rave.app.data.cloud.SharedStreak
import com.rave.app.domain.model.ActivityType
import com.rave.app.domain.model.DistanceUnit
import com.rave.app.domain.util.Format
import com.rave.app.ui.components.BentoAvatar
import com.rave.app.ui.components.BentoChip
import com.rave.app.ui.components.BentoGap
import com.rave.app.ui.components.BentoIconButton
import com.rave.app.ui.components.BentoKicker
import com.rave.app.ui.components.BentoOutlinedCard
import com.rave.app.ui.components.BentoTile
import com.rave.app.ui.components.BentoTitle
import com.rave.app.ui.components.EmptyState
import com.rave.app.ui.components.HabitIcons
import com.rave.app.ui.components.IconTile
import com.rave.app.ui.components.LoadingState
import com.rave.app.ui.components.PillButton
import com.rave.app.ui.components.SectionCard
import com.rave.app.ui.components.bentoTileFor
import com.rave.app.ui.components.tile
import com.rave.app.ui.home.LeagueUi
import com.rave.app.ui.home.LeagueViewModel
import com.rave.app.ui.theme.RaveTheme
import com.rave.app.ui.theme.TileColor
import com.rave.app.ui.unitLabel
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/*
 * Bento Pop Friends tab ("Squad"): avatar strip, black weekly league, Competitions / Shared streaks
 * tiles and the activity feed with outlined cards.
 */

private val SidePad = 18.dp

/** Bento tile colour for a person, with ink initials like the mockup. */
@Composable
private fun personTile(name: String): TileColor = TileColor(bentoTileFor(name).container, RaveTheme.colors.tileSand.content)

@Composable
fun FriendsScreen(
    contentPadding: PaddingValues,
    onSignIn: () -> Unit,
    onRequests: () -> Unit,
    onAddFriend: () -> Unit,
    onInvite: () -> Unit,
    onCompetitions: () -> Unit,
    onSharedStreaks: () -> Unit,
    viewModel: FriendsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val leagueViewModel: LeagueViewModel = hiltViewModel()
    val league by leagueViewModel.ui.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val sent = stringResource(R.string.nudge_sent)
    val already = stringResource(R.string.nudge_already)
    val failed = stringResource(R.string.error_generic)
    LaunchedEffect(Unit) {
        viewModel.messages.collect { r ->
            Toast.makeText(context, when (r) { NudgeResult.SENT -> sent; NudgeResult.ALREADY -> already; NudgeResult.FAILED -> failed }, Toast.LENGTH_SHORT).show()
        }
    }
    LaunchedEffect(state.user?.uid) { leagueViewModel.refresh() }

    if (state.user == null) {
        Box(Modifier.fillMaxSize().padding(contentPadding), contentAlignment = Alignment.Center) {
            EmptyState(
                icon = Icons.Rounded.People,
                title = stringResource(R.string.friends_empty_title),
                message = stringResource(if (state.cloudAvailable) R.string.friends_sign_in_body else R.string.cloud_unavailable),
                tile = RaveTheme.colors.tilePink,
                action = if (state.cloudAvailable) { { PillButton(stringResource(R.string.action_sign_in), onClick = onSignIn) } } else null,
            )
        }
        return
    }
    if (state.loading) {
        LoadingState()
        return
    }
    val me = viewModel.myUid().orEmpty()
    val c = RaveTheme.colors

    LazyColumn(
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding() + 12.dp, bottom = contentPadding.calculateBottomPadding() + 16.dp,
            start = SidePad, end = SidePad,
        ),
        verticalArrangement = Arrangement.spacedBy(BentoGap),
    ) {
        // Header: "Squad" + requests bell (sand) + add friend (black, lime icon).
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BentoTitle(stringResource(R.string.bfriends_title), Modifier.weight(1f).semantics { heading() })
                val badge = state.requestCount + state.invitationCount
                BadgedBox(badge = { if (badge > 0) Badge { Text(badge.toString()) } }) {
                    BentoIconButton(Icons.Rounded.Notifications, stringResource(R.string.friends_requests), onRequests, size = 44.dp)
                }
                Spacer(Modifier.width(8.dp))
                BentoIconButton(
                    Icons.Rounded.PersonAdd, stringResource(R.string.friends_add), onAddFriend,
                    container = c.tileInk.container, content = c.highlight, size = 44.dp,
                )
            }
        }

        // Friends strip, ending with "+ Invite".
        if (state.friends.isEmpty()) {
            item {
                EmptyState(Icons.Rounded.PersonAdd, stringResource(R.string.friends_none_title), stringResource(R.string.friends_none_body),
                    tile = c.tileGreen, action = { PillButton(stringResource(R.string.invite_title), onClick = onInvite) })
            }
        } else {
            item {
                LazyRow(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(state.friends, key = { "f_" + it.uid }) { f ->
                        SquadMember(f.name) { BentoAvatar(f.name, personTile(f.name), size = 56.dp) }
                    }
                    item(key = "invite") {
                        SquadMember(stringResource(R.string.action_invite), Modifier.clickable(onClick = onInvite)) {
                            Box(
                                Modifier.size(56.dp).clip(SmoothCornerShape(16.dp)).background(c.tileSand.container),
                                contentAlignment = Alignment.Center,
                            ) { Icon(Icons.Rounded.Add, contentDescription = null, tint = c.tileSand.content, modifier = Modifier.size(24.dp)) }
                        }
                    }
                }
            }
        }

        item { WeeklyLeagueCard(league, state, Modifier.padding(top = 4.dp)) }

        // Competitions (orange) / Shared streaks (lilac).
        item {
            Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(BentoGap)) {
                val ink = c.tileSand.content
                EntryTile(
                    Icons.Rounded.EmojiEvents, stringResource(R.string.competitions_title),
                    if (state.liveCompetitions > 0) stringResource(R.string.bfriends_live, state.liveCompetitions) else null,
                    c.tileOrange.container, ink, onCompetitions, Modifier.weight(1f).fillMaxHeight(), subAlpha = 0.8f,
                )
                val top = state.shared.maxByOrNull { it.current }
                EntryTile(
                    Icons.Rounded.LocalFireDepartment, stringResource(R.string.shared_title),
                    top?.let { stringResource(R.string.bfriends_streak_with, it.friendName(me), it.current) },
                    c.tilePink.container, ink, onSharedStreaks, Modifier.weight(1f).fillMaxHeight(),
                )
            }
        }

        // Shared streaks waiting on a friend keep their nudge action here.
        val nudgeable = state.shared.filter { it.doneToday(me) && !it.bothDoneToday }
        items(nudgeable, key = { "s_" + it.id }) { s ->
            SharedStreakCard(s, me, nudged = s.friendUid(me) in state.nudged) {
                viewModel.nudge(s.friendUid(me), "shared_streak", s.id)
            }
        }

        item {
            Text(
                stringResource(R.string.bfriends_feed),
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(top = 4.dp).semantics { heading() },
            )
        }
        if (state.feed.isEmpty()) {
            item { Text(stringResource(R.string.friends_feed_empty), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            items(state.feed, key = { "a_" + it.id }) { item -> FeedCard(item, me, state.unit) { viewModel.cheer(item) } }
        }
    }
}

@Composable
private fun SquadMember(label: String, modifier: Modifier = Modifier, avatar: @Composable () -> Unit) {
    Column(modifier.width(56.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        avatar()
        Spacer(Modifier.height(4.dp))
        Text(
            label, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

/** Black weekly steps league (falls back to this week's distance board when the league can't load). */
@Composable
private fun WeeklyLeagueCard(league: LeagueUi, state: FriendsUiState, modifier: Modifier = Modifier) {
    val c = RaveTheme.colors
    val on = c.tileInk.content
    BentoTile(c.tileInk.container, modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BentoKicker(stringResource(R.string.league_title), on, Modifier.weight(1f))
            if (league is LeagueUi.Ready) {
                val days = league.standings.daysLeft
                BentoChip(
                    if (days == 0) stringResource(R.string.bfriends_last_day) else pluralStringResource(R.plurals.bfriends_days_left, days, days),
                    container = c.highlight, content = c.onHighlight,
                )
            }
        }
        val message: String? = when (league) {
            LeagueUi.Loading -> stringResource(R.string.league_loading)
            is LeagueUi.Ready -> if (league.standings.hasFriends) null else stringResource(R.string.league_no_friends)
            LeagueUi.SignedOut, LeagueUi.Unavailable ->
                if (state.leaderboard.size > 1) null
                else stringResource(if (league == LeagueUi.SignedOut) R.string.league_signed_out else R.string.league_unavailable)
        }
        if (message != null) {
            Spacer(Modifier.height(12.dp))
            Text(message, style = MaterialTheme.typography.bodySmall, color = on.copy(alpha = 0.7f))
        }
        val numbers = NumberFormat.getIntegerInstance()
        // (rank, name, value, isMe)
        val rows: List<LeagueLine> = when (league) {
            is LeagueUi.Ready -> {
                val entries = league.standings.entries
                val top = entries.take(LEAGUE_TOP).mapIndexed { i, e -> LeagueLine(i + 1, e.name, numbers.format(e.steps), e.isMe) }
                val meIndex = entries.indexOfFirst { it.isMe }
                if (meIndex >= LEAGUE_TOP) top + entries[meIndex].let { LeagueLine(meIndex + 1, it.name, numbers.format(it.steps), true) } else top
            }
            LeagueUi.SignedOut, LeagueUi.Unavailable -> {
                val unit = unitLabel(state.unit)
                val board = state.leaderboard
                val top = board.take(LEAGUE_TOP).mapIndexed { i, e -> LeagueLine(i + 1, e.name, "${Format.distance(e.weekKm * 1000, state.unit, 1)} $unit", e.isMe) }
                val meIndex = board.indexOfFirst { it.isMe }
                if (meIndex >= LEAGUE_TOP) top + board[meIndex].let { LeagueLine(meIndex + 1, it.name, "${Format.distance(it.weekKm * 1000, state.unit, 1)} $unit", true) }
                else top
            }
            LeagueUi.Loading -> emptyList()
        }
        rows.forEach { r ->
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    r.rank.toString(), modifier = Modifier.width(22.dp),
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = if (r.isMe) c.highlight else on,
                )
                BentoAvatar(r.name, TileColor(bentoTileFor(r.name).container, on), size = 34.dp, circle = true)
                Spacer(Modifier.width(10.dp))
                Text(
                    if (r.isMe) stringResource(R.string.league_you, r.name) else r.name,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = if (r.isMe) FontWeight.ExtraBold else FontWeight.SemiBold),
                    color = on, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                Text(r.value, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.ExtraBold), color = on)
            }
        }
    }
}

private data class LeagueLine(val rank: Int, val name: String, val value: String, val isMe: Boolean)

private const val LEAGUE_TOP = 3

@Composable
private fun EntryTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String?,
    container: androidx.compose.ui.graphics.Color,
    content: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    modifier: Modifier,
    subAlpha: Float = 0.65f,
) {
    BentoTile(container, modifier, onClick = onClick) {
        Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(30.dp))
        Spacer(Modifier.height(8.dp))
        Text(
            title, color = content, maxLines = 2, overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, fontWeight = FontWeight.ExtraBold),
        )
        if (subtitle != null) {
            Text(
                subtitle, color = content.copy(alpha = subAlpha), maxLines = 1, overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
            )
        }
    }
}

@Composable
fun SharedStreakCard(s: SharedStreak, me: String, nudged: Boolean, onNudge: () -> Unit) {
    val friendName = s.friendName(me)
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconTile(HabitIcons.get(s.icon), s.color.tile())
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(s.habitName, style = MaterialTheme.typography.titleSmall)
                Text(
                    when {
                        s.bothDoneToday -> stringResource(R.string.shared_both_done)
                        s.doneToday(me) -> stringResource(R.string.shared_waiting_on, friendName)
                        s.doneToday(s.friendUid(me)) -> stringResource(R.string.shared_your_turn, friendName)
                        else -> stringResource(R.string.shared_with, friendName)
                    },
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.LocalFireDepartment, null, tint = RaveTheme.colors.streakFlame, modifier = Modifier.size(18.dp))
                Text(s.current.toString(), style = MaterialTheme.typography.titleMedium)
            }
        }
        if (s.doneToday(me) && !s.bothDoneToday) {
            TextButton(onClick = onNudge, enabled = !nudged) {
                Text(stringResource(if (nudged) R.string.nudge_done else R.string.nudge_action, friendName))
            }
        }
    }
}

@Composable
private fun FeedCard(item: FeedItem, me: String, unit: DistanceUnit, onCheer: () -> Unit) {
    val c = RaveTheme.colors
    val cheered = me in item.cheers
    val distance = "${Format.distance(item.distanceM, unit, 1)} ${unitLabel(unit)}"
    val minutes = item.durationS / 60
    val time = if (minutes >= 1) stringResource(R.string.minutes_short, minutes.toInt()) else Format.duration(item.durationS)
    val date = Instant.ofEpochMilli(item.startTime).atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT))
    BentoOutlinedCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BentoAvatar(item.ownerName, personTile(item.ownerName), size = 36.dp, circle = true)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(if (item.type == ActivityType.RUN) R.string.bfriends_feed_ran else R.string.bfriends_feed_walked, item.ownerName, distance),
                    style = MaterialTheme.typography.titleSmall.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Text(
                    listOf(time, item.name, date).filter { it.isNotBlank() }.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f), maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            val cd = stringResource(if (cheered) R.string.cd_uncheer else R.string.cd_cheer)
            BentoChip(
                if (item.cheers.isEmpty()) stringResource(R.string.bfriends_hype) else stringResource(R.string.bfriends_hype_count, item.cheers.size),
                container = if (cheered) c.tileInk.container else c.highlight,
                content = if (cheered) c.highlight else c.onHighlight,
                modifier = Modifier.semantics { contentDescription = cd },
                onClick = onCheer,
            )
        }
    }
}
