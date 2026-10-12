package com.rave.app.ui.competitions

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.rave.app.R
import com.rave.app.core.TimeProvider
import com.rave.app.data.cloud.AuthRepository
import com.rave.app.data.cloud.CompetitionRepository
import com.rave.app.data.cloud.Friend
import com.rave.app.data.cloud.FriendsRepository
import com.rave.app.data.cloud.InviteRepository
import com.rave.app.data.cloud.InviteType
import com.rave.app.data.cloud.ParticipantRow
import com.rave.app.data.preferences.SettingsRepository
import com.rave.app.domain.competition.Competition
import com.rave.app.domain.competition.CompetitionMode
import com.rave.app.domain.competition.CompetitionRanking
import com.rave.app.domain.competition.CompetitionStatus
import com.rave.app.domain.competition.CompetitionType
import com.rave.app.domain.competition.RankedParticipant
import com.rave.app.domain.model.DistanceUnit
import com.rave.app.ui.components.Avatar
import com.rave.app.ui.components.EmptyState
import com.rave.app.ui.components.LineChart
import com.rave.app.ui.components.LoadingState
import com.rave.app.ui.components.PillButton
import com.rave.app.ui.components.PillProgress
import com.rave.app.ui.components.RaveScreen
import com.rave.app.ui.components.SectionCard
import com.rave.app.ui.components.TonalPillButton
import com.rave.app.ui.navigation.CompetitionDetailRoute
import com.rave.app.ui.share.ShareHelper
import com.rave.app.ui.theme.PillShape
import com.rave.app.ui.theme.RaveTheme
import com.rave.app.ui.friends.Picker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject

data class CompetitionDetailState(
    val loading: Boolean = true,
    val competition: Competition? = null,
    val ranked: List<RankedParticipant> = emptyList(),
    val rows: List<ParticipantRow> = emptyList(),
    val me: String = "",
    val now: Long = 0,
    val unit: DistanceUnit = DistanceUnit.KM,
    val friends: List<Friend> = emptyList(),
)

@HiltViewModel
class CompetitionDetailViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val repo: CompetitionRepository,
    private val invites: InviteRepository,
    private val settings: SettingsRepository,
    friends: FriendsRepository,
    auth: AuthRepository,
    private val time: TimeProvider,
) : ViewModel() {
    val id: String = savedState.toRoute<CompetitionDetailRoute>().id
    private val previousRanks = MutableStateFlow<Map<String, Int>>(emptyMap())
    private val ticker = flow { while (true) { emit(time.nowMs()); delay(30_000) } }

    val state: StateFlow<CompetitionDetailState> = combine(
        repo.observe(id), repo.observeParticipants(id), combine(ticker, previousRanks) { t, p -> t to p }, settings.settings, friends.observeFriends(),
    ) { c, rows, (now, prev), prefs, friendList ->
        CompetitionDetailState(
            loading = false,
            competition = c,
            ranked = if (c == null) emptyList() else CompetitionRanking.rank(rows.map { it.score }, c.type, prev),
            rows = rows,
            me = auth.currentUser.value?.uid.orEmpty(),
            now = now,
            unit = prefs.unit,
            friends = friendList.filter { f -> c == null || (f.uid !in c.members && f.uid !in c.invited) },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CompetitionDetailState())

    init {
        viewModelScope.launch {
            // Ranks the user saw last time drive the ↑/↓ arrows.
            previousRanks.value = settings.getString("comp_ranks_$id")?.let { json ->
                runCatching { JSONObject(json).let { o -> o.keys().asSequence().associateWith { o.getInt(it) } } }.getOrNull()
            }.orEmpty()
            runCatching { repo.refreshMyScore(id) }
        }
    }

    fun join() = viewModelScope.launch { repo.join(id) }

    fun invite(friend: Friend) = viewModelScope.launch { repo.inviteMore(id, listOf(friend.uid)) }

    /** Creates (or reuses) a short code for this competition and returns the share text. */
    suspend fun shareText(competitionName: String, appName: String, template: (String, String, String, String) -> String): String {
        val cached = settings.getString("comp_code_$id")
        val code = cached ?: invites.createTargetInvite(InviteType.COMPETITION, id).also { settings.putString("comp_code_$id", it) }
        return template(appName, competitionName, code, invites.competitionLink(id))
    }
}

@Composable
fun CompetitionDetailScreen(onBack: () -> Unit, onResults: (String) -> Unit, viewModel: CompetitionDetailViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var shareRequested by rememberSaveable { mutableStateOf(false) }
    var inviting by rememberSaveable { mutableStateOf(false) }
    val appName = stringResource(R.string.app_name)
    val c = state.competition
    LaunchedEffect(shareRequested) {
        if (shareRequested && c != null) {
            shareRequested = false
            runCatching {
                viewModel.shareText(c.name, appName) { a, n, code, link -> context.getString(R.string.comp_share_message, a, n, code, link) }
            }.onSuccess { ShareHelper.shareText(context, it) }
        }
    }

    RaveScreen(title = c?.name ?: stringResource(R.string.competitions_title), onBack = onBack) {
        if (state.loading) {
            LoadingState()
            return@RaveScreen
        }
        if (c == null) {
            EmptyState(Icons.Rounded.EmojiEvents, stringResource(R.string.comp_missing_title), stringResource(R.string.comp_missing_body))
            return@RaveScreen
        }
        val status = c.statusAt(state.now)
        val me = state.me
        val isMember = me in c.members
        val myRank = state.ranked.firstOrNull { it.score.uid == me }
        val gap = CompetitionRanking.gapToNext(state.ranked, me)
        val maxValue = when (c.type) {
            CompetitionType.TARGET_RACE -> c.targetKm
            else -> state.ranked.maxOfOrNull { it.score.value }?.coerceAtLeast(0.001) ?: 1.0
        }

        LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                SectionCard(color = RaveTheme.colors.accentCard) {
                    val on = RaveTheme.colors.onAccentCard
                    Text("${stringResource(competitionTypeRes(c.type))} · ${stringResource(competitionModeRes(c.mode))}",
                        style = MaterialTheme.typography.labelLarge, color = on.copy(alpha = 0.8f))
                    Text(
                        when (status) {
                            CompetitionStatus.UPCOMING -> stringResource(R.string.comp_starts_in, formatRemaining(c.startAtMs - state.now))
                            CompetitionStatus.ACTIVE -> stringResource(R.string.comp_time_left, formatRemaining(c.endAtMs - state.now))
                            CompetitionStatus.ENDED -> stringResource(R.string.comp_ended)
                        },
                        style = MaterialTheme.typography.headlineSmall, color = on,
                    )
                    if (myRank != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(stringResource(R.string.comp_your_rank, myRank.rank, state.ranked.size), style = MaterialTheme.typography.titleMedium, color = on)
                        gap?.let { (ahead, diff) ->
                            Text(stringResource(R.string.comp_gap, competitionValue(c, diff, state.unit), ahead.score.name),
                                style = MaterialTheme.typography.bodyMedium, color = on.copy(alpha = 0.85f))
                        }
                    }
                    c.stake?.let {
                        Spacer(Modifier.height(10.dp))
                        Surface(shape = PillShape, color = MaterialTheme.colorScheme.primary) {
                            Text(stringResource(R.string.competition_stake, it), style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                        }
                    }
                }
            }
            if (!isMember && status != CompetitionStatus.ENDED) {
                item { PillButton(stringResource(R.string.comp_join), onClick = { viewModel.join() }, modifier = Modifier.fillMaxWidth()) }
            }
            if (c.mode == CompetitionMode.TEAMS) {
                item {
                    val totals = CompetitionRanking.teamTotals(state.ranked.map { it.score })
                    SectionCard {
                        Text(stringResource(R.string.comp_teams), style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(8.dp))
                        listOf(0, 1).forEach { team ->
                            val name = c.teamNames.getOrNull(team) ?: stringResource(if (team == 0) R.string.team_a else R.string.team_b)
                            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Text(name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                                Text(competitionValue(c, totals[team] ?: 0.0, state.unit), style = MaterialTheme.typography.titleSmall)
                            }
                        }
                    }
                }
            }
            item { Text(stringResource(R.string.comp_leaderboard), style = MaterialTheme.typography.titleMedium) }
            items(state.ranked, key = { it.score.uid }) { r ->
                LeaderRow(r, c, isMe = r.score.uid == me, max = maxValue, unit = state.unit, modifier = Modifier.animateItem())
            }
            val myRow = state.rows.firstOrNull { it.score.uid == me }
            val leaderRow = state.ranked.firstOrNull()?.let { l -> state.rows.firstOrNull { it.score.uid == l.score.uid } }
            if (myRow != null && myRow.daily.size >= 2) {
                item {
                    SectionCard {
                        Text(stringResource(R.string.comp_daily_progress), style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(10.dp))
                        val mine = myRow.daily.toSortedMap().values.toList()
                        val series = mutableListOf(mine to MaterialTheme.colorScheme.primary)
                        if (leaderRow != null && leaderRow.score.uid != me) {
                            series += leaderRow.daily.toSortedMap().values.toList() to RaveTheme.colors.chart[0]
                        }
                        LineChart(series, description = stringResource(R.string.comp_daily_progress))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(top = 6.dp)) {
                            Legend(stringResource(R.string.you), MaterialTheme.colorScheme.primary)
                            if (leaderRow != null && leaderRow.score.uid != me) Legend(leaderRow.score.name, RaveTheme.colors.chart[0])
                        }
                    }
                }
            }
            if (myRow != null && myRow.score.flaggedActivities > 0) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Warning, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.comp_flagged, myRow.score.flaggedActivities), style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (status == CompetitionStatus.ENDED) {
                        PillButton(stringResource(R.string.comp_view_results), onClick = { onResults(c.id) }, icon = Icons.Rounded.EmojiEvents,
                            modifier = Modifier.weight(1f))
                    } else {
                        TonalPillButton(stringResource(R.string.comp_invite_friends), onClick = { inviting = true }, icon = Icons.Rounded.PersonAdd,
                            enabled = state.friends.isNotEmpty() && c.members.size < Competition.MAX_PARTICIPANTS, modifier = Modifier.weight(1f))
                    }
                    TonalPillButton(stringResource(R.string.comp_share_code), onClick = { shareRequested = true }, icon = Icons.Rounded.Share,
                        modifier = Modifier.weight(1f))
                }
            }
        }
    }
    if (inviting) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { inviting = false },
            title = { Text(stringResource(R.string.comp_invite_friends)) },
            text = { Picker(stringResource(R.string.shared_pick_friend), "", state.friends.map { it.name }) { i -> viewModel.invite(state.friends[i]); inviting = false } },
            confirmButton = { androidx.compose.material3.TextButton(onClick = { inviting = false }) { Text(stringResource(R.string.action_done)) } },
            containerColor = MaterialTheme.colorScheme.surface,
        )
    }
}

@Composable
private fun LeaderRow(r: RankedParticipant, c: Competition, isMe: Boolean, max: Double, unit: DistanceUnit, modifier: Modifier) {
    Row(
        modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium)
            .background(if (isMe) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(r.rank.toString(), style = MaterialTheme.typography.titleMedium, modifier = Modifier.width(28.dp))
        AnimatedContent(targetState = r.change, label = "rankChange") { change ->
            when {
                change > 0 -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.ArrowUpward, stringResource(R.string.cd_rank_up, change), tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(14.dp))
                    Text(change.toString(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                }
                change < 0 -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.ArrowDownward, stringResource(R.string.cd_rank_down, -change), tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
                    Text((-change).toString(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                }
                else -> Spacer(Modifier.width(24.dp))
            }
        }
        Spacer(Modifier.width(6.dp))
        Avatar(r.score.name, size = 36.dp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(if (isMe) stringResource(R.string.you) else r.score.name, style = MaterialTheme.typography.titleSmall,
                fontWeight = if (isMe) FontWeight.Bold else FontWeight.Medium)
            Spacer(Modifier.height(4.dp))
            PillProgress((r.score.value / max).toFloat())
        }
        Spacer(Modifier.width(10.dp))
        Text(competitionValue(c, r.score.value, unit), style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun Legend(label: String, color: androidx.compose.ui.graphics.Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        androidx.compose.foundation.layout.Box(Modifier.size(10.dp).clip(PillShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}
