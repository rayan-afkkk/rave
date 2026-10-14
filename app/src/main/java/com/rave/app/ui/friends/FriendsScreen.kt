package com.rave.app.ui.friends

import android.widget.Toast
import com.rave.app.ui.theme.Dimens
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rave.app.R
import com.rave.app.data.cloud.FeedItem
import com.rave.app.data.cloud.SharedStreak
import com.rave.app.domain.model.DistanceUnit
import com.rave.app.domain.util.Format
import com.rave.app.ui.activityTypeRes
import com.rave.app.ui.components.Avatar
import com.rave.app.ui.components.EmptyState
import com.rave.app.ui.components.HabitIcons
import com.rave.app.ui.components.IconTile
import com.rave.app.ui.components.LoadingState
import com.rave.app.ui.components.PillButton
import com.rave.app.ui.components.RouteCanvas
import com.rave.app.ui.components.SectionCard
import com.rave.app.ui.components.SectionHeader
import com.rave.app.ui.components.tile
import com.rave.app.ui.theme.RaveTheme
import com.rave.app.ui.theme.TileColor
import com.rave.app.ui.unitLabel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

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
    val context = LocalContext.current
    val sent = stringResource(R.string.nudge_sent)
    val already = stringResource(R.string.nudge_already)
    val failed = stringResource(R.string.error_generic)
    LaunchedEffect(Unit) {
        viewModel.messages.collect { r ->
            Toast.makeText(context, when (r) { NudgeResult.SENT -> sent; NudgeResult.ALREADY -> already; NudgeResult.FAILED -> failed }, Toast.LENGTH_SHORT).show()
        }
    }

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

    LazyColumn(
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding() + 12.dp, bottom = contentPadding.calculateBottomPadding() + 16.dp,
            start = Dimens.ScreenPadding, end = Dimens.ScreenPadding,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.nav_friends), style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.weight(1f).semantics { heading() })
                val badge = state.requestCount + state.invitationCount
                IconButton(onClick = onRequests) {
                    BadgedBox(badge = { if (badge > 0) Badge { Text(badge.toString()) } }) {
                        Icon(Icons.Rounded.Notifications, contentDescription = stringResource(R.string.friends_requests))
                    }
                }
                IconButton(onClick = onAddFriend) { Icon(Icons.Rounded.PersonAdd, contentDescription = stringResource(R.string.friends_add)) }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                EntryCard(Icons.Rounded.EmojiEvents, stringResource(R.string.competitions_title), RaveTheme.colors.tilePeach, onCompetitions, Modifier.weight(1f))
                EntryCard(Icons.Rounded.Whatshot, stringResource(R.string.shared_title), RaveTheme.colors.tilePink, onSharedStreaks, Modifier.weight(1f))
                EntryCard(Icons.Rounded.Share, stringResource(R.string.invite_title), RaveTheme.colors.tileGreen, onInvite, Modifier.weight(1f))
            }
        }

        if (state.shared.isNotEmpty()) {
            item { SectionHeader(stringResource(R.string.shared_title)) }
            items(state.shared, key = { "s_" + it.id }) { s ->
                SharedStreakCard(s, me, nudged = s.friendUid(me) in state.nudged) {
                    viewModel.nudge(s.friendUid(me), "shared_streak", s.id)
                }
            }
        }

        item { SectionHeader(stringResource(R.string.friends_leaderboard)) }
        item {
            SectionCard {
                if (state.leaderboard.size <= 1) {
                    Text(stringResource(R.string.friends_leaderboard_empty), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                state.leaderboard.forEachIndexed { i, e ->
                    Row(
                        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small)
                            .background(if (e.isMe) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("${i + 1}", style = MaterialTheme.typography.titleSmall, modifier = Modifier.width(28.dp))
                        Avatar(e.name, size = 32.dp)
                        Spacer(Modifier.width(10.dp))
                        Text(if (e.isMe) stringResource(R.string.you) else e.name, style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (e.isMe) FontWeight.SemiBold else FontWeight.Normal, modifier = Modifier.weight(1f))
                        Text("${Format.distance(e.weekKm * 1000, state.unit, 1)} ${unitLabel(state.unit)}", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }

        item { SectionHeader(stringResource(R.string.friends_list)) }
        if (state.friends.isEmpty()) {
            item {
                EmptyState(Icons.Rounded.PersonAdd, stringResource(R.string.friends_none_title), stringResource(R.string.friends_none_body),
                    tile = RaveTheme.colors.tileGreen, action = { PillButton(stringResource(R.string.invite_title), onClick = onInvite) })
            }
        } else {
            items(state.friends, key = { "f_" + it.uid }) { f ->
                Row(
                    Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).background(MaterialTheme.colorScheme.surface)
                        .heightIn(min = 64.dp).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Avatar(f.name)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(f.name, style = MaterialTheme.typography.titleSmall)
                        Text("@${f.username}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.LocalFireDepartment, null, tint = RaveTheme.colors.streakFlame, modifier = Modifier.size(16.dp))
                            Text(f.currentStreak.toString(), style = MaterialTheme.typography.labelLarge)
                        }
                        Text("${Format.distance(f.weekKm * 1000, state.unit, 1)} ${unitLabel(state.unit)}", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        item { SectionHeader(stringResource(R.string.friends_feed)) }
        if (state.feed.isEmpty()) {
            item { Text(stringResource(R.string.friends_feed_empty), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            items(state.feed, key = { "a_" + it.id }) { item -> FeedCard(item, me, state.unit) { viewModel.cheer(item) } }
        }
    }
}

@Composable
private fun EntryCard(icon: ImageVector, label: String, tile: TileColor, onClick: () -> Unit, modifier: Modifier) {
    SectionCard(modifier = modifier, color = tile.container, onClick = onClick, contentPadding = PaddingValues(14.dp)) {
        Icon(icon, null, tint = tile.content)
        Spacer(Modifier.height(8.dp))
        Text(label, style = MaterialTheme.typography.labelLarge, color = tile.content, maxLines = 2, overflow = TextOverflow.Ellipsis)
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
    val cheered = me in item.cheers
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(item.ownerName, size = 36.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(item.ownerName, style = MaterialTheme.typography.titleSmall)
                Text(
                    Instant.ofEpochMilli(item.startTime).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(84.dp).clip(MaterialTheme.shapes.medium).background(RaveTheme.colors.tilePeach.container)) {
                RouteCanvas(item.segments, RaveTheme.colors.tilePeach.content, Modifier.fillMaxSize(), strokeWidth = 3.dp)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(item.name.ifBlank { stringResource(activityTypeRes(item.type)) }, style = MaterialTheme.typography.titleSmall)
                Text("${Format.distance(item.distanceM, unit)} ${unitLabel(unit)} · ${Format.duration(item.durationS)}",
                    style = MaterialTheme.typography.bodyMedium)
            }
            IconButton(onClick = onCheer) {
                Icon(
                    if (cheered) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = stringResource(if (cheered) R.string.cd_uncheer else R.string.cd_cheer),
                    tint = if (cheered) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (item.cheers.isNotEmpty()) Text(item.cheers.size.toString(), style = MaterialTheme.typography.labelMedium)
        }
    }
}
