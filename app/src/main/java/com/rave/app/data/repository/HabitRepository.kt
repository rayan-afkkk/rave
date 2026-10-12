package com.rave.app.data.repository

import com.rave.app.core.ApplicationScope
import com.rave.app.core.TimeProvider
import com.rave.app.data.local.dao.HabitDao
import com.rave.app.data.local.dao.HabitLogDao
import com.rave.app.data.local.entity.HabitLogEntity
import com.rave.app.data.local.toDomain
import com.rave.app.data.local.toEntity
import com.rave.app.domain.model.Habit
import com.rave.app.domain.model.HabitLog
import com.rave.app.domain.model.HabitType
import com.rave.app.domain.model.LogSource
import com.rave.app.work.ReminderScheduler
import com.rave.app.widget.WidgetRefresher
import dagger.Lazy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/** Callback for features that react to habit completions (shared streaks). */
interface HabitCompletionListener {
    suspend fun onHabitCompletionChanged(habit: Habit, date: LocalDate, completed: Boolean)
}

@Singleton
class HabitRepository @Inject constructor(
    private val habitDao: HabitDao,
    private val logDao: HabitLogDao,
    private val time: TimeProvider,
    private val streaks: StreakRepository,
    private val reminders: ReminderScheduler,
    private val widget: WidgetRefresher,
    private val completionListener: Lazy<HabitCompletionListener>,
    @ApplicationScope private val scope: CoroutineScope,
) {
    fun observeHabits(): Flow<List<Habit>> = habitDao.observeActive().map { list -> list.map { it.toDomain() } }

    fun observeAllHabits(): Flow<List<Habit>> = habitDao.observeAll().map { list -> list.map { it.toDomain() } }

    fun observeLogs(date: LocalDate): Flow<Map<Long, HabitLog>> =
        logDao.observeForDay(date.toEpochDay()).map { list -> list.associate { it.habitId to it.toDomain() } }

    fun observeLogsBetween(from: LocalDate, to: LocalDate): Flow<List<HabitLog>> =
        logDao.observeBetween(from.toEpochDay(), to.toEpochDay()).map { list -> list.map { it.toDomain() } }

    suspend fun logsBetween(from: LocalDate, to: LocalDate): List<HabitLog> =
        logDao.getBetween(from.toEpochDay(), to.toEpochDay()).map { it.toDomain() }

    suspend fun getHabit(id: Long): Habit? = habitDao.getById(id)?.toDomain()

    /** All habits including archived ones (for history lookups). */
    suspend fun observeAllHabitsOnce(): List<Habit> = habitDao.getAll().map { it.toDomain() }

    suspend fun activeHabits(): List<Habit> = habitDao.getActive().map { it.toDomain() }

    suspend fun logsFor(date: LocalDate): Map<Long, HabitLog> =
        logDao.getForDay(date.toEpochDay()).associate { it.habitId to it.toDomain() }

    /** Inserts or updates a habit and (re)schedules its reminders. Returns its id. */
    suspend fun save(habit: Habit): Long {
        val existing = if (habit.id != 0L) habitDao.getById(habit.id) else null
        val entity = habit.toEntity(
            createdAt = existing?.createdAt ?: time.nowMs(),
            sortOrder = existing?.sortOrder ?: habitDao.count(),
        ).let { if (existing != null) it.copy(createdEpochDay = existing.createdEpochDay) else it }
        val rowId = habitDao.upsert(entity)
        val id = if (habit.id != 0L) habit.id else rowId
        reminders.schedule(habit.copy(id = id))
        onChanged()
        return id
    }

    /** Archives instead of deleting so past streak history stays intact. */
    suspend fun archive(id: Long) {
        habitDao.archive(id, time.today().toEpochDay())
        reminders.cancel(id)
        onChanged()
    }

    /** Today is editable; yesterday stays editable for 24 h; older days are read-only. */
    fun canEdit(date: LocalDate): Boolean {
        val today = time.today()
        return date == today || date == today.minusDays(1)
    }

    /** Checkbox toggles; counter/timed toggle between 0 and the target. */
    suspend fun toggleComplete(habit: Habit, date: LocalDate) {
        val log = logDao.get(habit.id, date.toEpochDay())
        val completed = log?.completed == true
        val value = if (completed) 0 else habit.completionTarget
        setProgress(habit, date, value, LogSource.MANUAL)
    }

    /** Swipe-right: mark complete. */
    suspend fun complete(habit: Habit, date: LocalDate) = setProgress(habit, date, habit.completionTarget, LogSource.MANUAL)

    suspend fun increment(habit: Habit, date: LocalDate, by: Int = 1) {
        val current = logDao.get(habit.id, date.toEpochDay())?.value ?: 0
        setProgress(habit, date, (current + by).coerceAtLeast(0), LogSource.MANUAL)
    }

    /** Swipe-left: toggle "skipped today". Skipping uses no freeze and keeps the streak. */
    suspend fun toggleSkip(habit: Habit, date: LocalDate) {
        if (!canEdit(date)) return
        val epoch = date.toEpochDay()
        val log = logDao.get(habit.id, epoch)
        val skip = log?.skipped != true
        logDao.upsert(
            (log ?: HabitLogEntity(habitId = habit.id, date = epoch)).copy(
                skipped = skip, completed = false, value = 0, updatedAt = time.nowMs(),
            ),
        )
        if (log?.completed == true) completionListener.get().onHabitCompletionChanged(habit, date, false)
        onChanged()
    }

    /** Sets a log value; completion is derived from the habit target. GPS/AUTO sources bypass the edit window. */
    suspend fun setProgress(habit: Habit, date: LocalDate, value: Int, source: LogSource) {
        if (source == LogSource.MANUAL && !canEdit(date)) return
        val epoch = date.toEpochDay()
        val log = logDao.get(habit.id, epoch)
        val wasCompleted = log?.completed == true
        val completed = when (habit.type) {
            HabitType.CHECKBOX -> value >= 1
            else -> value >= habit.completionTarget
        }
        if (value <= 0 && log != null && !log.skipped) {
            logDao.delete(habit.id, epoch)
        } else {
            logDao.upsert(
                (log ?: HabitLogEntity(habitId = habit.id, date = epoch)).copy(
                    value = value, completed = completed, skipped = false, source = source, updatedAt = time.nowMs(),
                ),
            )
        }
        if (wasCompleted != completed) completionListener.get().onHabitCompletionChanged(habit, date, completed)
        onChanged()
    }

    private fun onChanged() {
        widget.refresh()
        scope.launch { streaks.recompute() }
    }
}
