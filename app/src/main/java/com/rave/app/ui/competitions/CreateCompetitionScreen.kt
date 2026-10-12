package com.rave.app.ui.competitions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.rave.app.R
import com.rave.app.core.TimeProvider
import com.rave.app.data.cloud.CompetitionDraft
import com.rave.app.data.cloud.CompetitionRepository
import com.rave.app.data.cloud.Friend
import com.rave.app.data.cloud.FriendsRepository
import com.rave.app.data.repository.HabitRepository
import com.rave.app.domain.competition.Competition
import com.rave.app.domain.competition.CompetitionMode
import com.rave.app.domain.competition.CompetitionType
import com.rave.app.domain.model.ActivityType
import com.rave.app.ui.components.PillButton
import com.rave.app.ui.components.PillChip
import com.rave.app.ui.components.RaveScreen
import com.rave.app.ui.components.SectionCard
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class DurationOption(val days: Int) { THREE_DAYS(3), WEEK(7), TWO_WEEKS(14), MONTH(30), CUSTOM(0) }

data class CreateCompetitionState(
    val name: String = "",
    val type: CompetitionType = CompetitionType.DISTANCE,
    val mode: CompetitionMode = CompetitionMode.GROUP,
    val activityFilter: ActivityType? = null,
    val targetKm: String = "50",
    val duration: DurationOption = DurationOption.WEEK,
    val customDays: Int = 10,
    val stake: String = "",
    val habitNames: Set<String> = emptySet(),
    val availableHabits: List<String> = emptyList(),
    val friends: List<Friend> = emptyList(),
    val invited: Set<String> = emptySet(),
    val busy: Boolean = false,
    val createdId: String? = null,
    val error: Int? = null,
)

@HiltViewModel
class CreateCompetitionViewModel @Inject constructor(
    private val repo: CompetitionRepository,
    friends: FriendsRepository,
    habits: HabitRepository,
    private val time: TimeProvider,
) : ViewModel() {
    private val _state = MutableStateFlow(CreateCompetitionState())
    val state: StateFlow<CreateCompetitionState> = _state.asStateFlow()

    init {
        viewModelScope.launch { friends.observeFriends().collect { f -> _state.update { it.copy(friends = f) } } }
        viewModelScope.launch { habits.observeHabits().collect { h -> _state.update { it.copy(availableHabits = h.map { x -> x.name }) } } }
    }

    fun edit(t: (CreateCompetitionState) -> CreateCompetitionState) = _state.update { t(it).copy(error = null) }

    fun toggleInvite(uid: String) = _state.update { s ->
        val max = if (s.mode == CompetitionMode.DUEL) 1 else Competition.MAX_PARTICIPANTS - 1
        val next = if (uid in s.invited) s.invited - uid else if (s.mode == CompetitionMode.DUEL) setOf(uid) else s.invited + uid
        s.copy(invited = if (next.size > max) s.invited else next)
    }

    fun create() {
        val s = _state.value
        if (s.name.isBlank()) return _state.update { it.copy(error = R.string.comp_error_name) }
        if (s.mode == CompetitionMode.DUEL && s.invited.size != 1) return _state.update { it.copy(error = R.string.comp_error_duel) }
        val target = s.targetKm.replace(',', '.').toDoubleOrNull() ?: 0.0
        if (s.type == CompetitionType.TARGET_RACE && target <= 0) return _state.update { it.copy(error = R.string.comp_error_target) }
        val days = if (s.duration == DurationOption.CUSTOM) s.customDays else s.duration.days
        val start = time.nowMs()
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            repo.create(
                CompetitionDraft(
                    name = s.name, type = s.type, mode = s.mode,
                    activityFilter = s.activityFilter, targetKm = target,
                    habitNames = s.habitNames.toList(),
                    startAtMs = start, endAtMs = start + days * 24L * 60 * 60 * 1000,
                    stake = s.stake, invited = s.invited.toList(), teamNames = emptyList(),
                ),
            ).onSuccess { id -> _state.update { it.copy(busy = false, createdId = id) } }
                .onFailure { _state.update { it.copy(busy = false, error = R.string.error_generic) } }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateCompetitionScreen(onBack: () -> Unit, onCreated: (String) -> Unit, viewModel: CreateCompetitionViewModel = hiltViewModel()) {
    val s by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(s.createdId) { s.createdId?.let(onCreated) }
    RaveScreen(title = stringResource(R.string.comp_create), onBack = onBack) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).imePadding().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            OutlinedTextField(s.name, { v -> viewModel.edit { it.copy(name = v.take(40)) } }, label = { Text(stringResource(R.string.comp_name)) },
                singleLine = true, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium)

            SectionCard {
                Text(stringResource(R.string.comp_type), style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CompetitionType.entries.forEach { t ->
                        PillChip(s.type == t, stringResource(competitionTypeRes(t)), { viewModel.edit { it.copy(type = t) } })
                    }
                }
                if (s.type == CompetitionType.DISTANCE || s.type == CompetitionType.TARGET_RACE) {
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PillChip(s.activityFilter == null, stringResource(R.string.comp_filter_both), { viewModel.edit { it.copy(activityFilter = null) } })
                        PillChip(s.activityFilter == ActivityType.WALK, stringResource(R.string.filter_walks), { viewModel.edit { it.copy(activityFilter = ActivityType.WALK) } })
                        PillChip(s.activityFilter == ActivityType.RUN, stringResource(R.string.filter_runs), { viewModel.edit { it.copy(activityFilter = ActivityType.RUN) } })
                    }
                    Text(stringResource(R.string.comp_anticheat_note), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
                }
                if (s.type == CompetitionType.TARGET_RACE) {
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(s.targetKm, { v -> viewModel.edit { it.copy(targetKm = v.filter { c -> c.isDigit() || c == '.' }.take(5)) } },
                        label = { Text(stringResource(R.string.comp_target_km)) }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                }
                if (s.type == CompetitionType.HABITS_COMPLETED && s.availableHabits.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Text(stringResource(R.string.comp_which_habits), style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        s.availableHabits.forEach { h ->
                            PillChip(h in s.habitNames, h, { viewModel.edit { it.copy(habitNames = if (h in it.habitNames) it.habitNames - h else it.habitNames + h) } })
                        }
                    }
                    Text(stringResource(R.string.comp_habits_help), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            SectionCard {
                Text(stringResource(R.string.comp_duration), style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        DurationOption.THREE_DAYS to R.string.comp_dur_3d, DurationOption.WEEK to R.string.comp_dur_1w,
                        DurationOption.TWO_WEEKS to R.string.comp_dur_2w, DurationOption.MONTH to R.string.comp_dur_1m,
                        DurationOption.CUSTOM to R.string.comp_dur_custom,
                    ).forEach { (d, label) -> PillChip(s.duration == d, stringResource(label), { viewModel.edit { it.copy(duration = d) } }) }
                }
                if (s.duration == DurationOption.CUSTOM) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { viewModel.edit { it.copy(customDays = (it.customDays - 1).coerceAtLeast(1)) } }) { Text("−", style = MaterialTheme.typography.titleLarge) }
                        Text(stringResource(R.string.comp_value_days, s.customDays), style = MaterialTheme.typography.titleMedium)
                        IconButton(onClick = { viewModel.edit { it.copy(customDays = (it.customDays + 1).coerceAtMost(90)) } }) { Text("+", style = MaterialTheme.typography.titleLarge) }
                    }
                }
            }

            SectionCard {
                Text(stringResource(R.string.comp_mode), style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CompetitionMode.entries.forEach { m ->
                        PillChip(s.mode == m, stringResource(competitionModeRes(m)), { viewModel.edit { it.copy(mode = m, invited = if (m == CompetitionMode.DUEL) it.invited.take(1).toSet() else it.invited) } })
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(stringResource(if (s.mode == CompetitionMode.DUEL) R.string.comp_pick_opponent else R.string.comp_pick_friends), style = MaterialTheme.typography.labelLarge)
                if (s.friends.isEmpty()) {
                    Text(stringResource(R.string.comp_no_friends), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    s.friends.forEach { f -> PillChip(f.uid in s.invited, f.name, { viewModel.toggleInvite(f.uid) }) }
                }
            }

            OutlinedTextField(s.stake, { v -> viewModel.edit { it.copy(stake = v.take(60)) } }, label = { Text(stringResource(R.string.comp_stake)) },
                placeholder = { Text(stringResource(R.string.comp_stake_hint)) }, supportingText = { Text(stringResource(R.string.comp_stake_help)) },
                singleLine = true, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium)

            s.error?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            PillButton(stringResource(R.string.comp_create_button), onClick = viewModel::create, enabled = !s.busy, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))
        }
    }
}
