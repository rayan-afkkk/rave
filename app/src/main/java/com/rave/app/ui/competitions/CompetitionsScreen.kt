package com.rave.app.ui.competitions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.rave.app.R
import com.rave.app.core.TimeProvider
import com.rave.app.data.cloud.CompetitionRepository
import com.rave.app.domain.competition.Competition
import com.rave.app.domain.competition.CompetitionStatus
import com.rave.app.ui.components.BentoBig
import com.rave.app.ui.components.BentoChip
import com.rave.app.ui.components.BentoKicker
import com.rave.app.ui.components.BentoSquareShape
import com.rave.app.ui.components.BentoTile
import com.rave.app.ui.components.EmptyState
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.unit.sp
import com.rave.app.ui.components.PillChip
import com.rave.app.ui.components.RaveScreen
import com.rave.app.ui.friends.InvitationCard
import com.rave.app.ui.theme.RaveTheme
import androidx.compose.foundation.layout.padding
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class CompetitionTab { ACTIVE, INVITATIONS, PAST }

data class CompetitionsState(
    val active: List<Competition> = emptyList(),
    val invitations: List<Competition> = emptyList(),
    val past: List<Competition> = emptyList(),
    val now: Long = 0,
)

@HiltViewModel
class CompetitionsViewModel @Inject constructor(
    private val repo: CompetitionRepository,
    private val time: TimeProvider,
) : ViewModel() {
    val state: StateFlow<CompetitionsState> = combine(repo.observeMine(), repo.observeInvitations()) { mine, inv ->
        val now = time.nowMs()
        CompetitionsState(
            active = mine.filter { it.statusAt(now) != CompetitionStatus.ENDED },
            invitations = inv,
            past = mine.filter { it.statusAt(now) == CompetitionStatus.ENDED },
            now = now,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CompetitionsState())

    fun join(c: Competition) = viewModelScope.launch { repo.join(c.id) }
    fun decline(c: Competition) = viewModelScope.launch { repo.decline(c.id) }
}

@Composable
fun CompetitionsScreen(
    onBack: () -> Unit,
    onCreate: () -> Unit,
    onOpen: (String) -> Unit,
    onResults: (String) -> Unit,
    viewModel: CompetitionsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val pro = com.rave.app.ui.pro.rememberPro()
    var tab by rememberSaveable { mutableStateOf(CompetitionTab.ACTIVE) }
    RaveScreen(title = stringResource(R.string.competitions_title), onBack = onBack) {
        androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth().weight(1f)) {
            LazyColumn(contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 4.dp, bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item { PillChip(tab == CompetitionTab.ACTIVE, stringResource(R.string.comp_tab_active), { tab = CompetitionTab.ACTIVE }) }
                        item {
                            PillChip(tab == CompetitionTab.INVITATIONS,
                                stringResource(R.string.comp_tab_invitations) + if (state.invitations.isNotEmpty()) " (${state.invitations.size})" else "",
                                { tab = CompetitionTab.INVITATIONS })
                        }
                        item { PillChip(tab == CompetitionTab.PAST, stringResource(R.string.comp_tab_past), { tab = CompetitionTab.PAST }) }
                    }
                }
                val list = when (tab) {
                    CompetitionTab.ACTIVE -> state.active
                    CompetitionTab.INVITATIONS -> state.invitations
                    CompetitionTab.PAST -> state.past
                }
                if (list.isEmpty()) {
                    item {
                        EmptyState(
                            Icons.Rounded.EmojiEvents,
                            stringResource(when (tab) {
                                CompetitionTab.ACTIVE -> R.string.comp_empty_active
                                CompetitionTab.INVITATIONS -> R.string.comp_empty_invitations
                                CompetitionTab.PAST -> R.string.comp_empty_past
                            }),
                            stringResource(R.string.comp_empty_body),
                            tile = RaveTheme.colors.tilePeach,
                        )
                    }
                }
                items(list, key = { it.id }) { c ->
                    if (tab == CompetitionTab.INVITATIONS) {
                        InvitationCard(c.name, c.name, stringResource(competitionTypeRes(c.type)),
                            onAccept = { viewModel.join(c); onOpen(c.id) }, onDecline = { viewModel.decline(c) })
                    } else {
                        CompetitionCard(c, state.now) { if (tab == CompetitionTab.PAST) onResults(c.id) else onOpen(c.id) }
                    }
                }
            }
            // Anyone can join competitions; creating one is Rave Pro.
            FloatingActionButton(
                onClick = { pro.gate(com.rave.app.data.billing.ProReason.COMPETITIONS, onCreate) },
                containerColor = RaveTheme.colors.tileInk.container,
                contentColor = RaveTheme.colors.highlight,
                shape = BentoSquareShape,
                modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
            ) { Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.comp_create)) }
        }
    }
}

/** Bento tile per competition: orange while live/upcoming, sand once finished. */
@Composable
private fun CompetitionCard(c: Competition, now: Long, onClick: () -> Unit) {
    val colors = RaveTheme.colors
    val status = c.statusAt(now)
    val tile = if (status == CompetitionStatus.ENDED) colors.tileSand else colors.tileOrange
    val ink = tile.content
    BentoTile(tile.container, Modifier.fillMaxWidth(), onClick = onClick) {
        BentoKicker("${stringResource(competitionTypeRes(c.type))} · ${stringResource(competitionModeRes(c.mode))}", ink)
        Spacer(Modifier.height(4.dp))
        BentoBig(c.name, ink, size = 24.sp, maxLines = 2)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BentoChip(
                pluralStringResource(R.plurals.bfriends_players, c.members.size, c.members.size),
                container = colors.tileInk.container, content = colors.tileInk.content,
            )
            BentoChip(
                when (status) {
                    CompetitionStatus.UPCOMING -> stringResource(R.string.comp_starts_in, formatRemaining(c.startAtMs - now))
                    CompetitionStatus.ACTIVE -> stringResource(R.string.bfriends_ends_in, formatRemaining(c.endAtMs - now))
                    CompetitionStatus.ENDED -> stringResource(R.string.comp_ended)
                },
                container = ink.copy(alpha = 0.14f), content = ink,
            )
        }
        c.stake?.let {
            Spacer(Modifier.height(8.dp))
            BentoChip(stringResource(R.string.competition_stake, it), container = colors.highlight, content = colors.onHighlight)
        }
    }
}
