package com.rave.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rave.app.core.TimeProvider
import com.rave.app.data.local.entity.StreakEntity
import com.rave.app.data.preferences.SettingsRepository
import com.rave.app.data.repository.ActivityRepository
import com.rave.app.data.repository.HabitRepository
import com.rave.app.data.repository.StreakRepository
import com.rave.app.domain.habit.StreakEngine
import com.rave.app.domain.habit.StreakState
import com.rave.app.domain.model.DistanceUnit
import com.rave.app.domain.model.Habit
import com.rave.app.domain.model.HabitLog
import com.rave.app.domain.model.ScheduleType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

/** One habit card on Home for the selected day. */
data class HabitItem(
    val habit: Habit,
    val log: HabitLog?,
    val streak: Int,
    /** Completions this week (TIMES_PER_WEEK habits). */
    val weekCount: Int,
) {
    val completed: Boolean get() = log?.completed == true
    val skipped: Boolean get() = log?.skipped == true
    val value: Int get() = log?.value ?: 0
}

data class HomeUiState(
    val loading: Boolean = true,
    val name: String = "",
    val today: LocalDate = LocalDate.now(),
    val selectedDate: LocalDate = LocalDate.now(),
    val week: List<LocalDate> = emptyList(),
    /** Fraction of due habits done per day of the week strip. */
    val weekProgress: Map<LocalDate, Float> = emptyMap(),
    val items: List<HabitItem> = emptyList(),
    val doneCount: Int = 0,
    val totalCount: Int = 0,
    val nextHabit: String? = null,
    val kmToday: Double = 0.0,
    val kmGoal: Double = 3.0,
    val overallStreak: Int = 0,
    val freezes: Int = 0,
    val editable: Boolean = true,
    val unit: DistanceUnit = DistanceUnit.KM,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val habits: HabitRepository,
    streaks: StreakRepository,
    activities: ActivityRepository,
    settings: SettingsRepository,
    private val time: TimeProvider,
) : ViewModel() {

    private val selected = MutableStateFlow<LocalDate?>(null)

    private val weekLogs = time.todayFlow.flatMapLatest { today ->
        val start = time.weekStart(today)
        habits.observeLogsBetween(start, start.plusDays(6))
    }

    private val base = combine(time.todayFlow, selected, habits.observeHabits(), weekLogs, streaks.observeStreaks()) {
            today, sel, habitList, logs, streakMap ->
        Base(today, sel ?: today, habitList, logs, streakMap)
    }

    val state: StateFlow<HomeUiState> = combine(
        base, activities.observeDistanceToday(), settings.settings, streaks.observeFreezes(),
    ) { b, kmToday, prefs, freezes ->
        build(b, kmToday, prefs.displayName, prefs.dailyGoalKm.toDouble(), prefs.unit, freezes)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    private data class Base(
        val today: LocalDate,
        val selected: LocalDate,
        val habits: List<Habit>,
        val weekLogs: List<HabitLog>,
        val streaks: Map<Long, StreakState>,
    )

    private fun build(b: Base, distanceM: Double, name: String, goalKm: Double, unit: DistanceUnit, freezes: Int): HomeUiState {
        val weekStart = time.weekStart(b.today)
        val week = (0..6).map { weekStart.plusDays(it.toLong()) }
        val logsByDay = b.weekLogs.groupBy { it.date }.mapValues { (_, l) -> l.associateBy { it.habitId } }
        val selectedLogs = logsByDay[b.selected].orEmpty()
        val todayLogs = logsByDay[b.today].orEmpty()

        fun due(date: LocalDate, logs: Map<Long, HabitLog>) =
            b.habits.filter { it.isActiveOn(date) && (it.schedule.isScheduledOn(date) || logs[it.id]?.completed == true) }

        val items = due(b.selected, selectedLogs).map { habit ->
            val stored = b.streaks[habit.id] ?: StreakState()
            val streak = if (b.selected == b.today) StreakEngine.displayStreak(stored, todayLogs[habit.id]?.completed == true) else stored.current
            val weekCount = if (habit.schedule.type == ScheduleType.TIMES_PER_WEEK) {
                week.count { logsByDay[it]?.get(habit.id)?.completed == true }
            } else 0
            HabitItem(habit, selectedLogs[habit.id], streak, weekCount)
        }.sortedWith(compareBy<HabitItem> { it.completed || it.skipped }.thenBy { it.habit.reminderTimes.minOrNull() ?: LocalTime.MAX })

        val counted = items.filterNot { it.skipped }
        val now = time.now().toLocalTime()
        val next = counted.filterNot { it.completed }
            .minByOrNull { item -> item.habit.reminderTimes.filter { it >= now }.minOrNull() ?: LocalTime.MAX }

        val weekProgress = week.filter { !it.isAfter(b.today) }.associateWith { day ->
            val dayItems = due(day, logsByDay[day].orEmpty()).filter { logsByDay[day]?.get(it.id)?.skipped != true }
            if (dayItems.isEmpty()) 0f else dayItems.count { logsByDay[day]?.get(it.id)?.completed == true } / dayItems.size.toFloat()
        }

        val overallStored = b.streaks[StreakEntity.OVERALL] ?: StreakState()
        val anyDoneToday = todayLogs.values.any { it.completed }

        return HomeUiState(
            loading = false,
            name = name,
            today = b.today,
            selectedDate = b.selected,
            week = week,
            weekProgress = weekProgress,
            items = items,
            doneCount = counted.count { it.completed },
            totalCount = counted.size,
            nextHabit = next?.habit?.name,
            kmToday = distanceM / 1000.0,
            kmGoal = goalKm,
            overallStreak = StreakEngine.displayStreak(overallStored, anyDoneToday),
            freezes = freezes,
            editable = habits.canEdit(b.selected),
            unit = unit,
        )
    }

    fun selectDate(date: LocalDate) {
        selected.value = date
    }

    fun toggle(item: HabitItem) = launchIfEditable { habits.toggleComplete(item.habit, state.value.selectedDate) }
    fun complete(item: HabitItem) = launchIfEditable { if (!item.completed) habits.complete(item.habit, state.value.selectedDate) }
    fun skip(item: HabitItem) = launchIfEditable { habits.toggleSkip(item.habit, state.value.selectedDate) }
    fun increment(item: HabitItem) = launchIfEditable { habits.increment(item.habit, state.value.selectedDate) }

    private fun launchIfEditable(block: suspend () -> Unit) {
        if (!state.value.editable) return
        viewModelScope.launch { block() }
    }
}
