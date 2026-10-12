package com.rave.app.ui.friends

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.rave.app.R
import com.rave.app.data.cloud.AuthRepository
import com.rave.app.data.cloud.Friend
import com.rave.app.data.cloud.FriendsRepository
import com.rave.app.data.cloud.SharedStreak
import com.rave.app.data.cloud.SharedStreakRepository
import com.rave.app.data.repository.HabitRepository
import com.rave.app.domain.model.Habit
import com.rave.app.ui.components.EmptyState
import com.rave.app.ui.components.PillButton
import com.rave.app.ui.components.RaveScreen
import com.rave.app.ui.components.SectionHeader
import com.rave.app.ui.theme.RaveTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SharedStreaksState(
    val active: List<SharedStreak> = emptyList(),
    val invitations: List<SharedStreak> = emptyList(),
    val sent: List<SharedStreak> = emptyList(),
    val habits: List<Habit> = emptyList(),
    val friends: List<Friend> = emptyList(),
    val nudged: Set<String> = emptySet(),
)

@HiltViewModel
class SharedStreaksViewModel @Inject constructor(
    private val shared: SharedStreakRepository,
    private val friendsRepo: FriendsRepository,
    habits: HabitRepository,
    private val auth: AuthRepository,
) : ViewModel() {
    private val nudged = MutableStateFlow<Set<String>>(emptySet())

    val state: StateFlow<SharedStreaksState> = combine(
        shared.observeActive(), shared.observeInvitations(), shared.observePendingSent(),
        combine(habits.observeHabits(), friendsRepo.observeFriends()) { h, f -> h to f }, nudged,
    ) { a, i, s, (h, f), n ->
        SharedStreaksState(a, i, s, h.filter { it.sharedStreakId == null }, f, n)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SharedStreaksState())

    val me: String get() = auth.currentUser.value?.uid.orEmpty()

    fun create(habit: Habit, friend: Friend) = viewModelScope.launch { shared.invite(friend.uid, friend.name, habit) }
    fun accept(s: SharedStreak) = viewModelScope.launch { shared.accept(s.id) }
    fun decline(s: SharedStreak) = viewModelScope.launch { shared.decline(s.id) }
    fun nudge(s: SharedStreak) = viewModelScope.launch {
        friendsRepo.nudge(s.friendUid(me), "shared_streak", s.id)
        nudged.update { it + s.friendUid(me) }
    }
}

@Composable
fun SharedStreaksScreen(onBack: () -> Unit, viewModel: SharedStreaksViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var creating by rememberSaveable { mutableStateOf(false) }
    val me = viewModel.me
    RaveScreen(title = stringResource(R.string.shared_title), onBack = onBack) {
        LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Text(stringResource(R.string.shared_explainer), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            item {
                PillButton(stringResource(R.string.shared_new), onClick = { creating = true }, icon = Icons.Rounded.Add,
                    enabled = state.friends.isNotEmpty() && state.habits.isNotEmpty(), modifier = Modifier.fillMaxWidth())
            }
            if (state.invitations.isNotEmpty()) {
                item { SectionHeader(stringResource(R.string.inbox_incoming)) }
                items(state.invitations, key = { "i" + it.id }) { s ->
                    InvitationCard(s.memberNames[s.invitedBy].orEmpty(), s.habitName,
                        stringResource(R.string.inbox_shared_invite, s.memberNames[s.invitedBy].orEmpty()),
                        onAccept = { viewModel.accept(s) }, onDecline = { viewModel.decline(s) })
                }
            }
            if (state.active.isEmpty() && state.invitations.isEmpty() && state.sent.isEmpty()) {
                item {
                    EmptyState(Icons.Rounded.Whatshot, stringResource(R.string.shared_empty_title), stringResource(R.string.shared_empty_body),
                        tile = RaveTheme.colors.tilePink)
                }
            }
            items(state.active, key = { "a" + it.id }) { s ->
                SharedStreakCard(s, me, nudged = s.friendUid(me) in state.nudged) { viewModel.nudge(s) }
            }
            if (state.sent.isNotEmpty()) {
                item { SectionHeader(stringResource(R.string.inbox_outgoing)) }
                items(state.sent, key = { "s" + it.id }) { s ->
                    Text(stringResource(R.string.shared_pending, s.habitName, s.friendName(me)), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
    if (creating) {
        NewSharedStreakDialog(state.habits, state.friends, onDismiss = { creating = false }) { h, f ->
            creating = false
            viewModel.create(h, f)
        }
    }
}

@Composable
private fun NewSharedStreakDialog(habits: List<Habit>, friends: List<Friend>, onDismiss: () -> Unit, onCreate: (Habit, Friend) -> Unit) {
    var habit by remember { mutableStateOf(habits.firstOrNull()) }
    var friend by remember { mutableStateOf(friends.firstOrNull()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.shared_new)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Picker(stringResource(R.string.shared_pick_habit), habit?.name.orEmpty(), habits.map { it.name }) { habit = habits[it] }
                Picker(stringResource(R.string.shared_pick_friend), friend?.name.orEmpty(), friends.map { it.name }) { friend = friends[it] }
            }
        },
        confirmButton = {
            TextButton(onClick = { val h = habit; val f = friend; if (h != null && f != null) onCreate(h, f) }, enabled = habit != null && friend != null) {
                Text(stringResource(R.string.action_invite))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
        containerColor = MaterialTheme.colorScheme.surface,
    )
}

@Composable
fun Picker(label: String, value: String, options: List<String>, onPick: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box {
            OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) { Text(value.ifEmpty { "—" }) }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                options.forEachIndexed { i, o -> DropdownMenuItem(text = { Text(o) }, onClick = { onPick(i); open = false }) }
            }
        }
    }
}
