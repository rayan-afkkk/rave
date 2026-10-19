package com.rave.app.ui.friends

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.rave.app.R
import com.rave.app.data.cloud.FeedItem
import com.rave.app.data.cloud.FeedRepository
import com.rave.app.data.cloud.ProfileRepository
import com.rave.app.data.cloud.UserProfile
import com.rave.app.data.preferences.SettingsRepository
import com.rave.app.domain.habit.BadgeId
import com.rave.app.domain.model.ActivityType
import com.rave.app.domain.model.DistanceUnit
import com.rave.app.domain.util.Format
import com.rave.app.ui.badgeIcon
import com.rave.app.ui.badgeTitleRes
import com.rave.app.ui.components.BentoAvatar
import com.rave.app.ui.components.BentoKicker
import com.rave.app.ui.components.BentoSquareShape
import com.rave.app.ui.components.BentoStat
import com.rave.app.ui.components.BentoTile
import com.rave.app.ui.components.BentoTitle
import com.rave.app.ui.components.EmptyState
import com.rave.app.ui.components.LoadingState
import com.rave.app.ui.components.RaveScreen
import com.rave.app.ui.components.RouteCanvas
import com.rave.app.ui.components.bentoTileFor
import com.rave.app.ui.navigation.FriendProfileRoute
import com.rave.app.ui.theme.RaveTheme
import com.rave.app.ui.unitLabel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import javax.inject.Inject

data class FriendProfileUi(
    val loading: Boolean = true,
    val profile: UserProfile? = null,
    val activities: List<FeedItem> = emptyList(),
    val unit: DistanceUnit = DistanceUnit.KM,
    val weekStart: Long = 0,
)

@HiltViewModel
class FriendProfileViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val profiles: ProfileRepository,
    private val feed: FeedRepository,
    private val settings: SettingsRepository,
    private val time: com.rave.app.core.TimeProvider,
    private val friends: com.rave.app.data.cloud.FriendsRepository,
) : ViewModel() {
    private val _done = kotlinx.coroutines.channels.Channel<Int>(kotlinx.coroutines.channels.Channel.BUFFERED)
    /** One-off results: a message to show, then whether to leave the screen. */
    val done = _done.receiveAsFlow()

    fun remove() = viewModelScope.launch { _done.send(if (friends.removeFriend(route.uid).isSuccess) R.string.friend_removed else R.string.error_generic) }
    fun block() = viewModelScope.launch { _done.send(if (friends.block(route.uid).isSuccess) R.string.friend_blocked else R.string.error_generic) }
    fun report(reason: String, details: String) = viewModelScope.launch {
        _done.send(if (friends.report(route.uid, reason, details).isSuccess) R.string.friend_reported else R.string.error_generic)
    }
    private val route = savedState.toRoute<FriendProfileRoute>()
    val fallbackName: String = route.name
    private val _ui = MutableStateFlow(FriendProfileUi())
    val ui: StateFlow<FriendProfileUi> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            val profile = profiles.getProfile(route.uid)
            val activities = runCatching { feed.activitiesOf(route.uid) }.getOrDefault(emptyList())
            _ui.value = FriendProfileUi(false, profile, activities, settings.current().unit, time.weekStart().toEpochDay())
        }
    }
}

/** A friend's profile: their stats, badges and the routes they shared with you. */
@Composable
fun FriendProfileScreen(onBack: () -> Unit, viewModel: FriendProfileViewModel = hiltViewModel()) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.done.collect { msg ->
            android.widget.Toast.makeText(context, context.getString(msg), android.widget.Toast.LENGTH_SHORT).show()
            if (msg == R.string.friend_removed || msg == R.string.friend_blocked) onBack()
        }
    }
    FriendProfileContent(ui, viewModel.fallbackName, onBack, viewModel::remove, viewModel::block, viewModel::report)
}

private val REPORT_REASONS = listOf(
    R.string.report_reason_harassment, R.string.report_reason_inappropriate, R.string.report_reason_spam,
    R.string.report_reason_impersonation, R.string.report_reason_cheating, R.string.report_reason_other,
)

/** Overflow menu: remove friend, block, report (with confirmation / reason dialogs). */
@Composable
private fun SafetyMenu(name: String, onRemove: () -> Unit, onBlock: () -> Unit, onReport: (String, String) -> Unit) {
    var menu by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    var dialog by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf("") }
    Box {
        androidx.compose.material3.IconButton(onClick = { menu = true }) {
            Icon(Icons.Rounded.MoreVert, stringResource(R.string.friend_options))
        }
        androidx.compose.material3.DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            androidx.compose.material3.DropdownMenuItem(text = { Text(stringResource(R.string.friend_remove)) }, onClick = { menu = false; dialog = "remove" })
            androidx.compose.material3.DropdownMenuItem(text = { Text(stringResource(R.string.friend_block)) }, onClick = { menu = false; dialog = "block" })
            androidx.compose.material3.DropdownMenuItem(text = { Text(stringResource(R.string.friend_report)) }, onClick = { menu = false; dialog = "report" })
        }
    }
    when (dialog) {
        "remove" -> com.rave.app.ui.components.ConfirmDialog(
            title = stringResource(R.string.friend_remove_title, name), message = stringResource(R.string.friend_remove_body),
            confirmLabel = stringResource(R.string.friend_remove), destructive = true,
            onConfirm = { dialog = ""; onRemove() }, onDismiss = { dialog = "" },
        )
        "block" -> com.rave.app.ui.components.ConfirmDialog(
            title = stringResource(R.string.friend_block_title, name), message = stringResource(R.string.friend_block_body),
            confirmLabel = stringResource(R.string.friend_block), destructive = true,
            onConfirm = { dialog = ""; onBlock() }, onDismiss = { dialog = "" },
        )
        "report" -> {
            var reason by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(REPORT_REASONS.first()) }
            var details by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf("") }
            val reasonText = stringResource(reason)
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { dialog = "" },
                title = { Text(stringResource(R.string.friend_report_title, name)) },
                text = {
                    Column {
                        REPORT_REASONS.forEach { r ->
                            Row(Modifier.fillMaxWidth().clickable { reason = r }, verticalAlignment = Alignment.CenterVertically) {
                                androidx.compose.material3.RadioButton(selected = reason == r, onClick = { reason = r })
                                Text(stringResource(r), style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        androidx.compose.material3.OutlinedTextField(
                            details, { details = it.take(500) }, label = { Text(stringResource(R.string.friend_report_details)) },
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp), minLines = 2,
                        )
                    }
                },
                confirmButton = { androidx.compose.material3.TextButton(onClick = { dialog = ""; onReport(reasonText, details) }) { Text(stringResource(R.string.friend_report_send)) } },
                dismissButton = { androidx.compose.material3.TextButton(onClick = { dialog = "" }) { Text(stringResource(R.string.action_cancel)) } },
            )
        }
    }
}


@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FriendProfileContent(
    ui: FriendProfileUi,
    fallbackName: String,
    onBack: () -> Unit,
    onRemove: () -> Unit = {},
    onBlock: () -> Unit = {},
    onReport: (String, String) -> Unit = { _, _ -> },
) {
    val c = RaveTheme.colors
    val p = ui.profile
    val name = p?.name?.ifBlank { null } ?: fallbackName
    RaveScreen(title = name, onBack = onBack, actions = { SafetyMenu(name, onRemove, onBlock, onReport) }) {
        if (ui.loading) { LoadingState(); return@RaveScreen }
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                val tile = bentoTileFor(name)
                BentoTile(tile.container, Modifier.fillMaxWidth(), padding = 18.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BentoAvatar(name, c.tileInk, size = 64.dp)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            BentoTitle(name, color = tile.content)
                            val handle = p?.username?.takeIf { it.isNotBlank() }?.let { "@$it" }
                            val level = stringResource(R.string.friend_level, p?.level ?: 1)
                            Text(listOfNotNull(handle, level).joinToString(" · "), style = MaterialTheme.typography.bodyMedium,
                                color = tile.content.copy(alpha = 0.75f))
                        }
                    }
                }
            }
            if (p == null) {
                item { EmptyState(Icons.Rounded.Route, stringResource(R.string.friend_unavailable_title), stringResource(R.string.friend_unavailable_body), tile = c.tileSand) }
                return@LazyColumn
            }
            item {
                val km = { v: Double -> Format.distance(v * 1000, ui.unit, 1) }
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        BentoStat(km(p.totalKm), stringResource(R.string.friend_total, unitLabel(ui.unit)), c.tileOrange, Modifier.weight(1f))
                        BentoStat(km(p.weekKmFor(ui.weekStart)), stringResource(R.string.friend_this_week), c.tileBlue, Modifier.weight(1f))
                        BentoStat(p.activities.toString(), stringResource(R.string.friend_activities), c.tileGreen, Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        BentoStat("🔥 ${p.currentStreak}", stringResource(R.string.friend_streak), c.tilePink, Modifier.weight(1f))
                        BentoStat(p.bestStreak.toString(), stringResource(R.string.friend_best_streak), c.tileYellow, Modifier.weight(1f))
                        BentoStat(p.pointsWeek.toString(), stringResource(R.string.friend_points), c.tileInk, Modifier.weight(1f))
                    }
                }
            }
            item {
                val earned = p.badges.mapNotNull { id -> runCatching { BadgeId.valueOf(id) }.getOrNull() }
                BentoTile(c.tileSand.container, Modifier.fillMaxWidth()) {
                    BentoKicker(stringResource(R.string.friend_badges, earned.size, BadgeId.entries.size), c.tileSand.content)
                    Spacer(Modifier.height(10.dp))
                    if (earned.isEmpty()) {
                        Text(stringResource(R.string.friend_no_badges), style = MaterialTheme.typography.bodyMedium, color = c.tileSand.content.copy(alpha = 0.7f))
                    } else {
                        val tiles = listOf(c.tilePink, c.tileOrange, c.tileBlue, c.tileGreen, c.tileYellow)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            earned.forEach { b ->
                                val t = tiles[b.ordinal % tiles.size]
                                Column(Modifier.width(72.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(Modifier.size(52.dp).clip(BentoSquareShape).background(t.container), contentAlignment = Alignment.Center) {
                                        Icon(badgeIcon(b), null, tint = t.content, modifier = Modifier.size(26.dp))
                                    }
                                    Text(stringResource(badgeTitleRes(b)), style = MaterialTheme.typography.labelSmall, maxLines = 2,
                                        overflow = TextOverflow.Ellipsis, color = c.tileSand.content, modifier = Modifier.padding(top = 4.dp))
                                }
                            }
                        }
                    }
                }
            }
            item { BentoKicker(stringResource(R.string.friend_routes), MaterialTheme.colorScheme.onBackground, Modifier.padding(top = 6.dp)) }
            if (ui.activities.isEmpty()) {
                item { Text(stringResource(R.string.friend_no_routes), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            items(ui.activities, key = { it.id }) { a -> RouteRow(a, ui.unit) }
        }
    }
}

@Composable
private fun RouteRow(a: FeedItem, unit: DistanceUnit) {
    val c = RaveTheme.colors
    val date = Instant.ofEpochMilli(a.startTime).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
    val minutes = (a.durationS / 60).toInt()
    BentoTile(MaterialTheme.colorScheme.surface, Modifier.fillMaxWidth(), padding = 10.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(72.dp).clip(BentoSquareShape).background(c.tileSand.container)) {
                if (a.segments.flatten().size >= 2) {
                    RouteCanvas(a.segments, c.tileOrange.container, Modifier.fillMaxSize(), strokeWidth = 3.dp, padding = 10.dp)
                } else {
                    Icon(Icons.Rounded.Route, null, tint = c.tileSand.content, modifier = Modifier.align(Alignment.Center))
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    a.name.ifBlank { stringResource(if (a.type == ActivityType.RUN) R.string.friend_run else R.string.friend_walk) },
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 15.sp),
                    color = MaterialTheme.colorScheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${Format.distance(a.distanceM, unit, 2)} ${unitLabel(unit)} · ${stringResource(R.string.minutes_short, minutes)}",
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface,
                )
                Text(date, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
