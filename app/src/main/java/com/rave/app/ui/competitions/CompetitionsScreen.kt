package com.rave.app.ui.competitions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.rave.app.ui.components.EmptyState
import com.rave.app.ui.components.PillChip
import com.rave.app.ui.components.RaveScreen
import com.rave.app.ui.components.SectionCard
import com.rave.app.ui.friends.InvitationCard
import com.rave.app.ui.theme.PillShape
import com.rave.app.ui.theme.RaveTheme
import androidx.compose.material3.Surface
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
    var tab by rememberSaveable { mutableStateOf(CompetitionTab.ACTIVE) }
    RaveScreen(title = stringResource(R.string.competitions_title), onBack = onBack) {
        androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth().weight(1f)) {
            LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
            FloatingActionButton(
                onClick = onCreate,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
            ) { Icon(Icons.Rounded.Add, contentDescription = stringResource(R.string.comp_create)) }
        }
    }
}

@Composable
private fun CompetitionCard(c: Competition, now: Long, onClick: () -> Unit) {
    SectionCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(c.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${stringResource(competitionTypeRes(c.type))} · ${stringResource(competitionModeRes(c.mode))} · " +
                        stringResource(R.string.comp_people, c.members.size),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                when (c.statusAt(now)) {
                    CompetitionStatus.UPCOMING -> stringResource(R.string.comp_starts_in, formatRemaining(c.startAtMs - now))
                    CompetitionStatus.ACTIVE -> stringResource(R.string.comp_time_left, formatRemaining(c.endAtMs - now))
                    CompetitionStatus.ENDED -> stringResource(R.string.comp_ended)
                },
                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary,
            )
        }
        c.stake?.let {
            Spacer(Modifier.height(8.dp))
            Surface(shape = PillShape, color = RaveTheme.colors.tilePeach.container) {
                Text(stringResource(R.string.competition_stake, it), style = MaterialTheme.typography.labelMedium,
                    color = RaveTheme.colors.tilePeach.content, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
            }
        }
    }
}

