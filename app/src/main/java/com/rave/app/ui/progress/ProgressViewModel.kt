package com.rave.app.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rave.app.core.TimeProvider
import com.rave.app.data.local.entity.ActivityEntity
import com.rave.app.data.preferences.SettingsRepository
import com.rave.app.data.repository.ActivityRepository
import com.rave.app.data.repository.HabitRepository
import com.rave.app.data.repository.ProfileStats
import com.rave.app.data.repository.StatsRepository
import com.rave.app.data.repository.StreakRepository
import com.rave.app.domain.habit.StreakState
import com.rave.app.domain.model.ActivityType
import com.rave.app.domain.model.DistanceUnit
import com.rave.app.domain.model.Habit
import com.rave.app.domain.model.HabitColor
import com.rave.app.domain.model.HabitLog
import com.rave.app.domain.model.HabitType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

enum class ChartMode { WEEKLY, MONTHLY }

data class WeekRow(val label: String, val color: HabitColor?, val type: ActivityType?, val values: List<Float>)
data class StreakRow(val name: String, val color: HabitColor, val current: Int, val best: Int)

data class ProgressState(
    val loading: Boolean = true,
    val weekLabels: List<String> = emptyList(),
    val weekRows: List<WeekRow> = emptyList(),
    val month: YearMonth = YearMonth.now(),
    val monthValues: List<Float?> = emptyList(),
    val monthLeadingBlanks: Int = 0,
    val chartMode: ChartMode = ChartMode.WEEKLY,
    val chartValues: List<Double> = emptyList(),
    val chartLabels: List<String> = emptyList(),
    val streaks: List<StreakRow> = emptyList(),
    val stats: ProfileStats = ProfileStats(),
    val unit: DistanceUnit = DistanceUnit.KM,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ProgressViewModel @Inject constructor(
    habits: HabitRepository,
    activities: ActivityRepository,
    streaks: StreakRepository,
    stats: StatsRepository,
    settings: SettingsRepository,
    private val time: TimeProvider,
) : ViewModel() {
    private val mode = MutableStateFlow(ChartMode.WEEKLY)

    private val data = time.todayFlow.flatMapLatest { today ->
        val from = today.minusDays(190)
        combine(
            habits.observeHabits(),
            habits.observeLogsBetween(from, today),
            activities.observeBetween(from, today),
            streaks.observeStreaks(),
        ) { h, l, a, s -> Data(today, h, l, a, s) }
    }

    val state: StateFlow<ProgressState> = combine(data, mode, stats.observeProfileStats(), settings.settings) { d, m, st, prefs ->
        build(d, m, st, prefs.unit)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProgressState())

    fun setMode(m: ChartMode) {
        mode.value = m
    }

    private data class Data(
        val today: LocalDate,
        val habits: List<Habit>,
        val logs: List<HabitLog>,
        val activities: List<ActivityEntity>,
        val streaks: Map<Long, StreakState>,
    )

    private fun build(d: Data, m: ChartMode, stats: ProfileStats, unit: DistanceUnit): ProgressState {
        val locale = Locale.getDefault()
        val weekStart = time.weekStart(d.today)
        val week = (0..6).map { weekStart.plusDays(it.toLong()) }
        val logIndex = d.logs.associateBy { it.habitId to it.date }
        val kmByDay = d.activities.groupBy { time.dateOf(it.startTime) to it.type }.mapValues { (_, l) -> l.sumOf { it.distanceM } }

        val habitRows = d.habits.map { h ->
            WeekRow(h.name, h.color, null, week.map { day ->
                val log = logIndex[h.id to day]
                when {
                    log == null -> 0f
                    log.completed -> 1f
                    h.type == HabitType.CHECKBOX -> 0f
                    else -> (log.value / h.completionTarget.toFloat()).coerceIn(0f, 1f)
                }
            })
        }
        val activityRows = ActivityType.entries.mapNotNull { type ->
            val values = week.map { kmByDay[it to type] ?: 0.0 }
            val max = values.maxOrNull() ?: 0.0
            if (max <= 0) null else WeekRow(type.name, null, type, values.map { (it / max).toFloat() })
        }

        // Month heatmap: share of due habits completed, or 1 if any activity was tracked.
        val month = YearMonth.from(d.today)
        val monthValues = (1..month.lengthOfMonth()).map { dayOfMonth ->
            val day = month.atDay(dayOfMonth)
            if (day.isAfter(d.today)) return@map null
            val due = d.habits.filter { it.isActiveOn(day) && it.schedule.isScheduledOn(day) }
            val done = due.count { logIndex[it.id to day]?.completed == true }
            val tracked = ActivityType.entries.any { (kmByDay[day to it] ?: 0.0) > 0 }
            when {
                due.isNotEmpty() -> maxOf(done / due.size.toFloat(), if (tracked) 0.5f else 0f)
                tracked -> 1f
                else -> 0f
            }
        }

        val (chartValues, chartLabels) = when (m) {
            ChartMode.WEEKLY -> (7 downTo 0).map { back ->
                val start = weekStart.minusWeeks(back.toLong())
                val end = start.plusDays(6)
                val km = d.activities.filter { val day = time.dateOf(it.startTime); !day.isBefore(start) && !day.isAfter(end) }
                    .sumOf { it.distanceM } / unit.meters
                km to "${start.dayOfMonth}/${start.monthValue}"
            }.unzip()
            ChartMode.MONTHLY -> (5 downTo 0).map { back ->
                val ym = month.minusMonths(back.toLong())
                val km = d.activities.filter { YearMonth.from(time.dateOf(it.startTime)) == ym }.sumOf { it.distanceM } / unit.meters
                km to ym.month.getDisplayName(TextStyle.SHORT, locale)
            }.unzip()
        }

        return ProgressState(
            loading = false,
            weekLabels = week.map { it.dayOfWeek.getDisplayName(TextStyle.NARROW, locale) },
            weekRows = habitRows + activityRows,
            month = month,
            monthValues = monthValues,
            monthLeadingBlanks = month.atDay(1).dayOfWeek.value - 1,
            chartMode = m,
            chartValues = chartValues,
            chartLabels = chartLabels,
            streaks = d.habits.map { h ->
                val s = d.streaks[h.id] ?: StreakState()
                StreakRow(h.name, h.color, s.current, s.best)
            }.sortedByDescending { it.best },
            stats = stats,
            unit = unit,
        )
    }
}
