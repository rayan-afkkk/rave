package com.rave.app.ui.competitions

import androidx.compose.animation.AnimatedContent
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.rave.app.ui.components.BentoAvatar
import com.rave.app.ui.components.BentoBig
import com.rave.app.ui.components.BentoButton
import com.rave.app.ui.components.BentoChip
import com.rave.app.ui.components.BentoGap
import com.rave.app.ui.components.BentoKicker
import com.rave.app.ui.components.BentoOutlinedCard
import com.rave.app.ui.components.BentoTile
import com.rave.app.ui.components.bentoTileFor
import com.rave.app.ui.theme.TileColor
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.rave.app.domain.competition.RankedParticipant
import com.rave.app.domain.model.DistanceUnit
import com.rave.app.ui.components.EmptyState
import com.rave.app.ui.components.LineChart
import com.rave.app.ui.components.LoadingState
import com.rave.app.ui.components.RaveScreen
import com.rave.app.ui.navigation.CompetitionDetailRoute
import com.rave.app.ui.share.ShareHelper
import com.rave.app.ui.theme.PillShape
import com.rave.app.ui.theme.RaveTheme
import com.rave.app.ui.friends.NudgeResult
import com.rave.app.ui.friends.Picker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
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
    /** Other members who are also my friends (nudges need a friendship). */
    val squadFriends: List<String> = emptyList(),
    val squadNudged: Boolean = false,
)

@HiltViewModel
class CompetitionDetailViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val repo: CompetitionRepository,
    private val invites: InviteRepository,
    private val settings: SettingsRepository,
    private val friendsRepo: FriendsRepository,
    auth: AuthRepository,
    private val time: TimeProvider,
) : ViewModel() {
    val id: String = savedState.toRoute<CompetitionDetailRoute>().id
    private val previousRanks = MutableStateFlow<Map<String, Int>>(emptyMap())
    private val squadNudged = MutableStateFlow(false)
    private val _messages = Channel<NudgeResult>(Channel.BUFFERED)
    val messages: Flow<NudgeResult> = _messages.receiveAsFlow()
    private val ticker = flow { while (true) { emit(time.nowMs()); delay(30_000) } }

    val state: StateFlow<CompetitionDetailState> = combine(
        repo.observe(id), repo.observeParticipants(id), combine(ticker, previousRanks, squadNudged) { t, p, n -> Triple(t, p, n) }, settings.settings,
        friendsRepo.observeFriends(),
    ) { c, rows, (now, prev, nudgedSquad), prefs, friendList ->
        val myUid = auth.currentUser.value?.uid.orEmpty()
        CompetitionDetailState(
            loading = false,
            competition = c,
            ranked = if (c == null) emptyList() else CompetitionRanking.rank(rows.map { it.score }, c.type, prev),
            rows = rows,
            me = myUid,
            now = now,
            unit = prefs.unit,
            friends = friendList.filter { f -> c == null || (f.uid !in c.members && f.uid !in c.invited) },
            squadFriends = if (c == null) emptyList() else friendList.filter { it.uid in c.members && it.uid != myUid }.map { it.uid },
            squadNudged = nudgedSquad,
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

    /** Nudges every competitor who is also a friend (max one nudge per friend per day, see FriendsRepository). */
    fun nudgeSquad() = viewModelScope.launch {
        val targets = state.value.squadFriends
        if (targets.isEmpty()) return@launch
        val results = targets.map { friendsRepo.nudge(it, "competition", id) }
        squadNudged.value = true
        _messages.send(
            when {
                results.any { it.getOrNull() == true } -> NudgeResult.SENT
                results.all { it.isFailure } -> NudgeResult.FAILED
                else -> NudgeResult.ALREADY
            },
        )
    }

    fun invite(friend: Friend) = viewModelScope.launch { repo.inviteMore(id, listOf(friend.uid)) }

    /** Creates (or reuses) a short code for this competition and returns the share text. */
    suspend fun shareText(competitionName: String, appName: String, template: (String, String, String, String?) -> String): String {
        val cached = settings.getString("comp_code_$id")
        val code = cached ?: invites.createTargetInvite(InviteType.COMPETITION, id).also { settings.putString("comp_code_$id", it) }
        return template(appName, competitionName, code, null)
    }
}

/*
 * Bento Pop competition detail: orange hero, podium for the top three, sand list for the rest
 * and a black primary action ("Nudge the squad" / join / results).
 */

private val SidePad = 18.dp

/** Bento tile colour for a person, with ink initials. */
@Composable
private fun personTile(name: String): TileColor = TileColor(bentoTileFor(name).container, RaveTheme.colors.tileSand.content)

@Composable
fun CompetitionDetailScreen(onBack: () -> Unit, onResults: (String) -> Unit, viewModel: CompetitionDetailViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var shareRequested by rememberSaveable { mutableStateOf(false) }
    var inviting by rememberSaveable { mutableStateOf(false) }
    val appName = stringResource(R.string.app_name)
    val sent = stringResource(R.string.nudge_sent)
    val already = stringResource(R.string.nudge_already)
    val failed = stringResource(R.string.error_generic)
    val c = state.competition
    LaunchedEffect(shareRequested) {
        if (shareRequested && c != null) {
            shareRequested = false
            runCatching {
                viewModel.shareText(c.name, appName) { a, n, code, link ->
                    if (link != null) context.getString(R.string.comp_share_message, a, n, code, link)
                    else context.getString(R.string.comp_share_message_code_only, a, n, code)
                }
            }.onSuccess { ShareHelper.shareText(context, it) }
        }
    }
    LaunchedEffect(Unit) {
        viewModel.messages.collect { r ->
            Toast.makeText(context, when (r) { NudgeResult.SENT -> sent; NudgeResult.ALREADY -> already; NudgeResult.FAILED -> failed }, Toast.LENGTH_SHORT).show()
        }
    }

    RaveScreen(
        title = "",
        onBack = onBack,
        actions = {
            if (c != null) {
                IconButton(onClick = { shareRequested = true }) {
                    Icon(Icons.Rounded.IosShare, contentDescription = stringResource(R.string.comp_share_code))
                }
            }
        },
    ) {
        if (state.loading) {
            LoadingState()
            return@RaveScreen
        }
        if (c == null) {
            EmptyState(Icons.Rounded.EmojiEvents, stringResource(R.string.comp_missing_title), stringResource(R.string.comp_missing_body))
            return@RaveScreen
        }
        val colors = RaveTheme.colors
        val ink = colors.tileSand.content
        val status = c.statusAt(state.now)
        val me = state.me
        val isMember = me in c.members
        val myRank = state.ranked.firstOrNull { it.score.uid == me }
        val gap = CompetitionRanking.gapToNext(state.ranked, me)

        LazyColumn(
            contentPadding = PaddingValues(start = SidePad, end = SidePad, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(BentoGap),
        ) {
            // Orange hero.
            item {
                BentoTile(colors.tileOrange.container, Modifier.fillMaxWidth()) {
                    BentoKicker(stringResource(R.string.bfriends_comp_kicker, stringResource(competitionTypeRes(c.type))), ink)
                    Spacer(Modifier.height(6.dp))
                    BentoBig(c.name, ink, size = 28.sp, maxLines = 3)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        BentoChip(
                            pluralStringResource(R.plurals.bfriends_players, c.members.size, c.members.size),
                            container = colors.tileInk.container, content = colors.tileInk.content,
                        )
                        BentoChip(
                            when (status) {
                                CompetitionStatus.UPCOMING -> stringResource(R.string.comp_starts_in, formatRemaining(c.startAtMs - state.now))
                                CompetitionStatus.ACTIVE -> stringResource(R.string.bfriends_ends_in, formatRemaining(c.endAtMs - state.now))
                                CompetitionStatus.ENDED -> stringResource(R.string.comp_ended)
                            },
                            container = ink.copy(alpha = 0.14f), content = ink,
                        )
                    }
                    if (myRank != null) {
                        Spacer(Modifier.height(10.dp))
                        val rankText = stringResource(R.string.comp_your_rank, myRank.rank, state.ranked.size)
                        val gapText = gap?.let { (ahead, diff) -> stringResource(R.string.comp_gap, competitionValue(c, diff, state.unit), ahead.score.name) }
                        Text(
                            listOfNotNull(rankText, gapText).joinToString(" · "),
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                            color = ink.copy(alpha = 0.8f),
                        )
                    }
                    c.stake?.let {
                        Spacer(Modifier.height(10.dp))
                        BentoChip(stringResource(R.string.competition_stake, it), container = colors.highlight, content = colors.onHighlight)
                    }
                }
            }

            if (c.mode == CompetitionMode.TEAMS) {
                item {
                    val totals = CompetitionRanking.teamTotals(state.ranked.map { it.score })
                    BentoTile(colors.tileSand.container, Modifier.fillMaxWidth()) {
                        BentoKicker(stringResource(R.string.comp_teams), ink)
                        Spacer(Modifier.height(6.dp))
                        listOf(0, 1).forEach { team ->
                            val name = c.teamNames.getOrNull(team) ?: stringResource(if (team == 0) R.string.team_a else R.string.team_b)
                            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                                Text(name, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), color = ink,
                                    modifier = Modifier.weight(1f))
                                Text(competitionValue(c, totals[team] ?: 0.0, state.unit),
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.ExtraBold), color = ink)
                            }
                        }
                    }
                }
            }

            if (state.ranked.isNotEmpty()) {
                item { Podium(state.ranked.take(3), Modifier.padding(top = 4.dp)) }
            }
            val rest = state.ranked.drop(3)
            if (rest.isNotEmpty()) {
                item {
                    BentoTile(colors.tileSand.container, Modifier.fillMaxWidth()) {
                        rest.forEach { r -> RestRow(r, c, isMe = r.score.uid == me, unit = state.unit) }
                    }
                }
            }

            val myRow = state.rows.firstOrNull { it.score.uid == me }
            val leaderRow = state.ranked.firstOrNull()?.let { l -> state.rows.firstOrNull { it.score.uid == l.score.uid } }
            if (myRow != null && myRow.daily.size >= 2) {
                item {
                    BentoOutlinedCard {
                        Text(stringResource(R.string.comp_daily_progress), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
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

            // Actions: one black primary, then a sand "Invite friends".
            item {
                Column(Modifier.padding(top = 2.dp), verticalArrangement = Arrangement.spacedBy(BentoGap)) {
                    val primaryBg = colors.tileInk.container
                    val primaryFg = colors.tileInk.content
                    when {
                        status == CompetitionStatus.ENDED -> BentoButton(
                            stringResource(R.string.comp_view_results), primaryBg, primaryFg, onClick = { onResults(c.id) },
                            modifier = Modifier.fillMaxWidth(), leading = Icons.Rounded.EmojiEvents, height = 50.dp,
                        )
                        !isMember -> BentoButton(
                            stringResource(R.string.comp_join), primaryBg, primaryFg, onClick = { viewModel.join() },
                            modifier = Modifier.fillMaxWidth(), height = 50.dp,
                        )
                        status == CompetitionStatus.ACTIVE && state.squadFriends.isNotEmpty() -> BentoButton(
                            stringResource(if (state.squadNudged) R.string.bfriends_nudged_squad else R.string.bfriends_nudge_squad),
                            primaryBg, primaryFg, onClick = { viewModel.nudgeSquad() },
                            modifier = Modifier.fillMaxWidth(), leading = Icons.Rounded.Bolt, enabled = !state.squadNudged, height = 50.dp,
                        )
                        else -> Unit
                    }
                    if (status != CompetitionStatus.ENDED) {
                        BentoButton(
                            stringResource(R.string.comp_invite_friends), colors.tileSand.container, colors.tileSand.content,
                            onClick = { inviting = true }, modifier = Modifier.fillMaxWidth(), leading = Icons.Rounded.PersonAdd,
                            enabled = state.friends.isNotEmpty() && c.members.size < Competition.MAX_PARTICIPANTS, height = 50.dp,
                        )
                    }
                }
            }
        }
    }
    if (inviting) {
        AlertDialog(
            onDismissRequest = { inviting = false },
            title = { Text(stringResource(R.string.comp_invite_friends)) },
            text = { Picker(stringResource(R.string.shared_pick_friend), "", state.friends.map { it.name }) { i -> viewModel.invite(state.friends[i]); inviting = false } },
            confirmButton = { TextButton(onClick = { inviting = false }) { Text(stringResource(R.string.action_done)) } },
            containerColor = MaterialTheme.colorScheme.surface,
        )
    }
}

/** Top three: 2nd (lilac) · 1st (black, lime number, tallest) · 3rd (lime). */
@Composable
private fun Podium(top: List<RankedParticipant>, modifier: Modifier = Modifier) {
    val colors = RaveTheme.colors
    val ink = colors.tileSand.content
    Row(
        modifier.fillMaxWidth().height(190.dp),
        horizontalArrangement = Arrangement.spacedBy(BentoGap),
        verticalAlignment = Alignment.Bottom,
    ) {
        // (index in ranking, block colour, number colour, block height)
        val slots = listOf(
            PodiumSlot(1, colors.tilePink.container, ink, 70.dp),
            PodiumSlot(0, colors.tileInk.container, colors.highlight, 110.dp),
            PodiumSlot(2, colors.tileGreen.container, ink, 50.dp),
        )
        slots.forEach { slot ->
            val p = top.getOrNull(slot.index)
            if (p == null) {
                Spacer(Modifier.weight(1f))
            } else {
                Column(Modifier.weight(1f)) {
                    BentoAvatar(p.score.name, personTile(p.score.name), size = 46.dp)
                    Text(
                        p.score.name, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    )
                    Box(
                        Modifier.fillMaxWidth().height(slot.height)
                            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomEnd = 8.dp, bottomStart = 8.dp))
                            .background(slot.block),
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        Text(
                            p.rank.toString(), color = slot.number, modifier = Modifier.padding(top = 10.dp),
                            style = MaterialTheme.typography.headlineMedium.copy(fontSize = 26.sp, fontWeight = FontWeight.ExtraBold),
                        )
                    }
                }
            }
        }
    }
}

private data class PodiumSlot(val index: Int, val block: Color, val number: Color, val height: Dp)

/** Rank 4+ row on the sand list: rank · circle avatar · name · score. */
@Composable
private fun RestRow(r: RankedParticipant, c: Competition, isMe: Boolean, unit: DistanceUnit) {
    val ink = RaveTheme.colors.tileSand.content
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(r.rank.toString(), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.ExtraBold), color = ink,
            modifier = Modifier.width(22.dp))
        BentoAvatar(r.score.name, personTile(r.score.name), size = 30.dp, circle = true)
        Spacer(Modifier.width(10.dp))
        Text(
            if (isMe) stringResource(R.string.league_you, r.score.name) else r.score.name,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = if (isMe) FontWeight.ExtraBold else FontWeight.SemiBold),
            color = ink, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false),
        )
        AnimatedContent(targetState = r.change, label = "rankChange") { change ->
            when {
                change > 0 -> Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 6.dp)) {
                    Icon(Icons.Rounded.ArrowUpward, stringResource(R.string.cd_rank_up, change), tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(14.dp))
                    Text(change.toString(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                }
                change < 0 -> Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 6.dp)) {
                    Icon(Icons.Rounded.ArrowDownward, stringResource(R.string.cd_rank_down, -change), tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(14.dp))
                    Text((-change).toString(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                }
                else -> Spacer(Modifier.width(0.dp))
            }
        }
        Spacer(Modifier.weight(1f).width(8.dp))
        Text(competitionValue(c, r.score.value, unit), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.ExtraBold), color = ink)
    }
}

@Composable
private fun Legend(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(PillShape).background(color))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}
