package com.rave.app.ui.friends

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Inbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.rave.app.R
import com.rave.app.data.cloud.CompetitionRepository
import com.rave.app.data.cloud.FriendRequest
import com.rave.app.data.cloud.FriendRequests
import com.rave.app.data.cloud.FriendsRepository
import com.rave.app.data.cloud.SharedStreak
import com.rave.app.data.cloud.SharedStreakRepository
import com.rave.app.domain.competition.Competition
import com.rave.app.ui.components.Avatar
import com.rave.app.ui.components.EmptyState
import com.rave.app.ui.components.PillButton
import com.rave.app.ui.components.RaveScreen
import com.rave.app.ui.components.SectionCard
import com.rave.app.ui.components.SectionHeader
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InboxState(
    val requests: FriendRequests = FriendRequests(),
    val sharedInvites: List<SharedStreak> = emptyList(),
    val competitionInvites: List<Competition> = emptyList(),
)

@HiltViewModel
class InboxViewModel @Inject constructor(
    private val friends: FriendsRepository,
    private val shared: SharedStreakRepository,
    private val competitions: CompetitionRepository,
) : ViewModel() {
    val state: StateFlow<InboxState> = combine(friends.observeRequests(), shared.observeInvitations(), competitions.observeInvitations()) { r, s, c ->
        InboxState(r, s, c)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), InboxState())

    fun accept(r: FriendRequest) = viewModelScope.launch { friends.accept(r) }
    fun decline(r: FriendRequest) = viewModelScope.launch { friends.decline(r) }
    fun cancel(r: FriendRequest) = viewModelScope.launch { friends.cancel(r) }
    fun acceptShared(s: SharedStreak) = viewModelScope.launch { shared.accept(s.id) }
    fun declineShared(s: SharedStreak) = viewModelScope.launch { shared.decline(s.id) }
    fun joinCompetition(c: Competition) = viewModelScope.launch { competitions.join(c.id) }
    fun declineCompetition(c: Competition) = viewModelScope.launch { competitions.decline(c.id) }
}

/** Friend requests (incoming / outgoing) and invitations, each with Accept / Decline. */
@Composable
fun FriendRequestsScreen(onBack: () -> Unit, onOpenCompetition: (String) -> Unit, viewModel: InboxViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    RaveScreen(title = stringResource(R.string.friends_requests), onBack = onBack) {
        val empty = state.requests.incoming.isEmpty() && state.requests.outgoing.isEmpty() &&
            state.sharedInvites.isEmpty() && state.competitionInvites.isEmpty()
        if (empty) {
            EmptyState(Icons.Rounded.Inbox, stringResource(R.string.inbox_empty_title), stringResource(R.string.inbox_empty_body))
            return@RaveScreen
        }
        LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (state.requests.incoming.isNotEmpty()) {
                item { SectionHeader(stringResource(R.string.inbox_incoming)) }
                items(state.requests.incoming, key = { it.id }) { r ->
                    InvitationCard(
                        avatarName = r.fromName,
                        title = r.fromName,
                        subtitle = stringResource(if (r.viaInvite) R.string.inbox_joined_with_code else R.string.inbox_wants_friend),
                        onAccept = { viewModel.accept(r) }, onDecline = { viewModel.decline(r) },
                    )
                }
            }
            if (state.sharedInvites.isNotEmpty()) {
                item { SectionHeader(stringResource(R.string.shared_title)) }
                items(state.sharedInvites, key = { it.id }) { s ->
                    InvitationCard(
                        avatarName = s.memberNames[s.invitedBy].orEmpty(),
                        title = s.habitName,
                        subtitle = stringResource(R.string.inbox_shared_invite, s.memberNames[s.invitedBy].orEmpty()),
                        onAccept = { viewModel.acceptShared(s) }, onDecline = { viewModel.declineShared(s) },
                    )
                }
            }
            if (state.competitionInvites.isNotEmpty()) {
                item { SectionHeader(stringResource(R.string.competitions_title)) }
                items(state.competitionInvites, key = { it.id }) { c ->
                    InvitationCard(
                        avatarName = c.name,
                        title = c.name,
                        subtitle = c.stake?.let { stringResource(R.string.competition_stake, it) } ?: stringResource(R.string.inbox_competition_invite),
                        onAccept = { viewModel.joinCompetition(c); onOpenCompetition(c.id) }, onDecline = { viewModel.declineCompetition(c) },
                    )
                }
            }
            if (state.requests.outgoing.isNotEmpty()) {
                item { SectionHeader(stringResource(R.string.inbox_outgoing)) }
                items(state.requests.outgoing, key = { it.id }) { r ->
                    SectionCard {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Avatar(r.toName)
                            Spacer(Modifier.width(12.dp))
                            Text(r.toName, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                            TextButton(onClick = { viewModel.cancel(r) }) { Text(stringResource(R.string.action_cancel)) }
                        }
                    }
                }
            }
        }
    }
}

/** Shows who invited you, to what, with Accept / Decline. */
@Composable
fun InvitationCard(avatarName: String, title: String, subtitle: String, onAccept: () -> Unit, onDecline: () -> Unit) {
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(avatarName)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onDecline) { Text(stringResource(R.string.action_decline)) }
            PillButton(stringResource(R.string.action_accept), onClick = onAccept)
        }
    }
}
