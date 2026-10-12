package com.rave.app.ui.habits

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.rave.app.R
import com.rave.app.core.TimeProvider
import com.rave.app.data.repository.HabitRepository
import com.rave.app.domain.model.Habit
import com.rave.app.domain.model.LogSource
import com.rave.app.domain.util.Format
import com.rave.app.ui.components.HabitIcons
import com.rave.app.ui.components.IconTile
import com.rave.app.ui.components.LoadingState
import com.rave.app.ui.components.PillButton
import com.rave.app.ui.components.ProgressRing
import com.rave.app.ui.components.RaveScreen
import com.rave.app.ui.components.TonalPillButton
import com.rave.app.ui.components.tile
import com.rave.app.ui.navigation.HabitTimerRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TimerState(
    val habit: Habit? = null,
    val totalMs: Long = 0,
    val remainingMs: Long = 0,
    val running: Boolean = false,
    val finished: Boolean = false,
)

/**
 * Countdown for TIMED habits. Running state is kept as an absolute end time in
 * [SavedStateHandle], so rotation and process death don't lose progress.
 */
@HiltViewModel
class HabitTimerViewModel @Inject constructor(
    private val savedState: SavedStateHandle,
    private val habits: HabitRepository,
    private val time: TimeProvider,
) : ViewModel() {
    private val habitId = savedState.toRoute<HabitTimerRoute>().habitId
    private val _state = MutableStateFlow(TimerState())
    val state: StateFlow<TimerState> = _state.asStateFlow()
    private var ticker: Job? = null

    init {
        viewModelScope.launch {
            val habit = habits.getHabit(habitId) ?: return@launch
            val total = habit.target * 60_000L
            val endAt = savedState.get<Long>(KEY_END_AT)
            val remaining = savedState.get<Long>(KEY_REMAINING) ?: total
            _state.value = TimerState(habit, total, remaining)
            if (endAt != null) start(endAt)
        }
    }

    fun toggle() = if (_state.value.running) pause() else start()

    private fun start(existingEndAt: Long? = null) {
        val s = _state.value
        if (s.finished || s.habit == null) return
        val endAt = existingEndAt ?: (time.nowMs() + s.remainingMs)
        savedState[KEY_END_AT] = endAt
        _state.update { it.copy(running = true) }
        ticker?.cancel()
        ticker = viewModelScope.launch {
            while (isActive) {
                val left = (endAt - time.nowMs()).coerceAtLeast(0)
                _state.update { it.copy(remainingMs = left) }
                if (left == 0L) {
                    finish(completed = true)
                    break
                }
                delay(250)
            }
        }
    }

    private fun pause() {
        ticker?.cancel()
        savedState.remove<Long>(KEY_END_AT)
        savedState[KEY_REMAINING] = _state.value.remainingMs
        _state.update { it.copy(running = false) }
    }

    fun reset() {
        pause()
        savedState.remove<Long>(KEY_REMAINING)
        _state.update { it.copy(remainingMs = it.totalMs, finished = false) }
    }

    /** Logs elapsed minutes (full target if [completed]). */
    fun finish(completed: Boolean = false) {
        val s = _state.value
        val habit = s.habit ?: return
        ticker?.cancel()
        savedState.remove<Long>(KEY_END_AT)
        savedState.remove<Long>(KEY_REMAINING)
        val minutes = if (completed) habit.target else ((s.totalMs - s.remainingMs) / 60_000L).toInt()
        _state.update { it.copy(running = false, finished = true) }
        viewModelScope.launch {
            if (minutes > 0) habits.setProgress(habit, time.today(), minutes, LogSource.MANUAL)
        }
    }

    private companion object {
        const val KEY_END_AT = "end_at"
        const val KEY_REMAINING = "remaining"
    }
}

@Composable
fun HabitTimerScreen(onBack: () -> Unit, viewModel: HabitTimerViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val habit = state.habit
    RaveScreen(title = habit?.name ?: "", onBack = onBack) {
        if (habit == null) {
            LoadingState()
            return@RaveScreen
        }
        LaunchedEffect(state.finished) { if (state.finished) { delay(1200); onBack() } }
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            IconTile(HabitIcons.get(habit.icon), habit.color.tile(), size = 56.dp, iconSize = 30.dp)
            Spacer(Modifier.height(24.dp))
            val progress = if (state.totalMs == 0L) 0f else 1f - state.remainingMs / state.totalMs.toFloat()
            ProgressRing(progress = progress, size = 240.dp, strokeWidth = 16.dp) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        if (state.finished) stringResource(R.string.timer_done) else Format.duration((state.remainingMs + 999) / 1000),
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                    Text(stringResource(R.string.minutes_short, habit.target), style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(32.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                TonalPillButton(stringResource(R.string.timer_reset), onClick = viewModel::reset, icon = Icons.Rounded.Replay,
                    enabled = !state.finished, modifier = Modifier.weight(1f))
                PillButton(
                    stringResource(if (state.running) R.string.action_pause else R.string.action_start),
                    onClick = viewModel::toggle,
                    icon = if (state.running) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    enabled = !state.finished,
                    modifier = Modifier.weight(1f),
                )
            }
            TextButton(onClick = { viewModel.finish() }, enabled = !state.finished && state.remainingMs < state.totalMs) {
                Text(stringResource(R.string.timer_finish_early))
            }
        }
    }
}
