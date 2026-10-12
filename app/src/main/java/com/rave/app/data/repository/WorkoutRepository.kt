package com.rave.app.data.repository

import android.content.Context
import com.rave.app.R
import com.rave.app.core.TimeProvider
import com.rave.app.data.local.dao.WorkoutDao
import com.rave.app.data.local.entity.ExerciseEntity
import com.rave.app.data.local.entity.WorkoutEntity
import com.rave.app.data.local.entity.WorkoutSetEntity
import com.rave.app.domain.model.HabitType
import com.rave.app.domain.model.LogSource
import com.rave.app.domain.util.DayPeriod
import com.rave.app.domain.util.dayPeriodFor
import com.rave.app.domain.workout.Exercise
import com.rave.app.domain.workout.ExerciseBests
import com.rave.app.domain.workout.ExerciseKind
import com.rave.app.domain.workout.MuscleGroup
import com.rave.app.domain.workout.SetEntry
import com.rave.app.domain.workout.SetPr
import com.rave.app.domain.workout.WorkoutMath
import com.rave.app.widget.WidgetRefresher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/** Result of finishing a workout, shown on the summary. */
data class WorkoutResult(val workoutId: Long, val prs: Map<Long, Set<SetPr>>, val tickedHabits: List<String>)

fun ExerciseEntity.toDomain() = Exercise(id, name, muscle, kind, custom)
fun WorkoutSetEntity.toEntry() = SetEntry(id, exerciseId, setOrder, weightKg, reps, durationS, warmup, done)

/**
 * Set tracker. Every edit is written straight to Room, so an unfinished workout
 * (endTime == null) survives the app being killed and is resumed next time.
 */
@Singleton
class WorkoutRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: WorkoutDao,
    private val habits: HabitRepository,
    private val stats: StatsRepository,
    private val widget: WidgetRefresher,
    private val time: TimeProvider,
) {
    private val seedMutex = Mutex()

    fun observeExercises(): Flow<List<Exercise>> = dao.observeExercises().map { l -> l.map { it.toDomain() } }

    fun observeActiveWorkout(): Flow<WorkoutEntity?> = dao.observeActiveWorkout()

    fun observeWorkout(id: Long): Flow<WorkoutEntity?> = dao.observeWorkout(id)

    fun observeSets(workoutId: Long): Flow<List<WorkoutSetEntity>> = dao.observeSets(workoutId)

    fun observeHistory(): Flow<List<WorkoutEntity>> = dao.observeHistory()

    /** Adds the built-in exercise library the first time it's needed. */
    suspend fun ensureSeeded() = seedMutex.withLock {
        if (dao.exerciseCount() > 0) return@withLock
        dao.insertExercises(DEFAULTS.map { (res, muscle, kind) -> ExerciseEntity(name = context.getString(res), muscle = muscle, kind = kind) })
    }

    suspend fun createExercise(name: String, muscle: MuscleGroup, kind: ExerciseKind): Result<Long> = runCatching {
        dao.insertExercise(ExerciseEntity(name = name.trim(), muscle = muscle, kind = kind, custom = true))
    }

    /** Resumes the unfinished workout, or starts a new one. */
    suspend fun startOrResume(): Long {
        ensureSeeded()
        dao.activeWorkout()?.let { return it.id }
        val now = time.nowMs()
        return dao.insertWorkout(WorkoutEntity(name = defaultName(time.dateTimeOf(now).hour), startTime = now))
    }

    /** Adds an exercise with sets pre-filled from the last time it was done (or one empty set). */
    suspend fun addExercise(workoutId: Long, exerciseId: Long) {
        val existing = dao.sets(workoutId)
        if (existing.any { it.exerciseId == exerciseId }) return
        val order = (existing.maxOfOrNull { it.exerciseOrder } ?: -1) + 1
        val last = lastSession(exerciseId, workoutId)
        val template = last.ifEmpty { listOf(WorkoutSetEntity(workoutId = workoutId, exerciseId = exerciseId, exerciseOrder = order, setOrder = 0)) }
        template.forEachIndexed { i, s ->
            dao.upsertSet(
                WorkoutSetEntity(
                    workoutId = workoutId, exerciseId = exerciseId, exerciseOrder = order, setOrder = i,
                    weightKg = s.weightKg, reps = s.reps, durationS = s.durationS, warmup = s.warmup,
                ),
            )
        }
    }

    /** Adds a set copying the previous set's values. */
    suspend fun addSet(workoutId: Long, exerciseId: Long) {
        val sets = dao.sets(workoutId).filter { it.exerciseId == exerciseId }
        val prev = sets.lastOrNull() ?: return
        dao.upsertSet(prev.copy(id = 0, setOrder = prev.setOrder + 1, done = false, completedAt = null, warmup = false))
    }

    suspend fun updateSet(set: WorkoutSetEntity) {
        val completedAt = if (set.done) set.completedAt ?: time.nowMs() else null
        dao.upsertSet(set.copy(completedAt = completedAt))
    }

    suspend fun deleteSet(id: Long) = dao.deleteSet(id)

    suspend fun removeExercise(workoutId: Long, exerciseId: Long) = dao.deleteExerciseSets(workoutId, exerciseId)

    /** Sets from the most recent finished session of this exercise ("Previous" column). */
    suspend fun lastSession(exerciseId: Long, excludeWorkoutId: Long): List<WorkoutSetEntity> {
        val history = dao.history(exerciseId, excludeWorkoutId)
        val lastWorkout = history.firstOrNull()?.workoutId ?: return emptyList()
        return history.filter { it.workoutId == lastWorkout }.sortedBy { it.setOrder }
    }

    suspend fun allTimeBests(exerciseId: Long, excludeWorkoutId: Long): ExerciseBests =
        WorkoutMath.bests(dao.history(exerciseId, excludeWorkoutId).map { it.toEntry() })

    /**
     * Finishes the workout: drops empty unfinished sets, stores totals and PRs, auto-ticks
     * WORKOUT habits for today and checks badges. Returns null if nothing was completed.
     */
    suspend fun finish(workoutId: Long): WorkoutResult? {
        val workout = dao.workout(workoutId) ?: return null
        val sets = dao.sets(workoutId)
        sets.filter { !it.done }.forEach { dao.deleteSet(it.id) }
        val done = sets.filter { it.done }
        if (done.isEmpty()) {
            dao.deleteWorkout(workoutId)
            return null
        }
        val exercises = dao.allExercises().associateBy { it.id }
        val prs = done.groupBy { it.exerciseId }.mapValues { (exId, exSets) ->
            val kind = exercises[exId]?.kind ?: ExerciseKind.WEIGHT_REPS
            WorkoutMath.detectPrs(allTimeBests(exId, workoutId), exSets.map { it.toEntry() }, kind)
        }.filterValues { it.isNotEmpty() }
        val entries = done.map { it.toEntry() }
        val working = entries.count { it.counts }
        dao.updateWorkout(
            workout.copy(
                endTime = time.nowMs(),
                totalVolumeKg = WorkoutMath.volume(entries),
                workingSets = working,
                prCount = prs.size,
                prTags = prs.entries.joinToString(";") { (id, tags) -> "$id:" + tags.joinToString("|") { it.name } },
            ),
        )

        // Auto-tick "Workout" habits due today.
        val today = time.dateOf(workout.startTime)
        val todaysSets = dao.finishedBetween(time.startOfDayMs(today), time.endOfDayMs(today)).sumOf { it.workingSets }
        val ticked = mutableListOf<String>()
        val logs = habits.logsFor(today)
        habits.activeHabits()
            .filter { it.type == HabitType.WORKOUT && it.schedule.isScheduledOn(today) && logs[it.id]?.completed != true }
            .forEach { h ->
                habits.setProgress(h, today, todaysSets, LogSource.WORKOUT)
                if (todaysSets >= h.completionTarget) ticked += h.name
            }
        stats.checkBadges()
        widget.refresh()
        return WorkoutResult(workoutId, prs, ticked)
    }

    suspend fun discard(workoutId: Long) = dao.deleteWorkout(workoutId)

    suspend fun delete(workoutId: Long) {
        dao.deleteWorkout(workoutId)
        widget.refresh()
    }

    suspend fun rename(workoutId: Long, name: String, note: String) {
        val w = dao.workout(workoutId) ?: return
        dao.updateWorkout(w.copy(name = name.trim().ifEmpty { w.name }, note = note.trim()))
    }

    /** All-time best marks per exercise (for the records list). */
    suspend fun personalRecords(): List<Pair<Exercise, ExerciseBests>> {
        val exercises = dao.allExercises().associateBy { it.id }
        return dao.allWorkingSets().groupBy { it.exerciseId }.mapNotNull { (id, sets) ->
            exercises[id]?.let { it.toDomain() to WorkoutMath.bests(sets.map { s -> s.toEntry() }) }
        }.sortedByDescending { it.second.best1rmKg }
    }

    fun parsePrTags(raw: String): Map<Long, Set<SetPr>> = raw.split(';').filter { ':' in it }.associate { part ->
        val (id, tags) = part.split(':', limit = 2)
        id.toLong() to tags.split('|').mapNotNull { t -> runCatching { SetPr.valueOf(t) }.getOrNull() }.toSet()
    }

    private fun defaultName(hour: Int): String = context.getString(
        when (dayPeriodFor(hour)) {
            DayPeriod.MORNING -> R.string.name_morning_workout
            DayPeriod.AFTERNOON -> R.string.name_afternoon_workout
            DayPeriod.EVENING -> R.string.name_evening_workout
            DayPeriod.NIGHT -> R.string.name_night_workout
        },
    )

    private companion object {
        val DEFAULTS = listOf(
            Triple(R.string.ex_bench_press, MuscleGroup.CHEST, ExerciseKind.WEIGHT_REPS),
            Triple(R.string.ex_incline_db_press, MuscleGroup.CHEST, ExerciseKind.WEIGHT_REPS),
            Triple(R.string.ex_push_up, MuscleGroup.CHEST, ExerciseKind.BODYWEIGHT_REPS),
            Triple(R.string.ex_deadlift, MuscleGroup.BACK, ExerciseKind.WEIGHT_REPS),
            Triple(R.string.ex_barbell_row, MuscleGroup.BACK, ExerciseKind.WEIGHT_REPS),
            Triple(R.string.ex_lat_pulldown, MuscleGroup.BACK, ExerciseKind.WEIGHT_REPS),
            Triple(R.string.ex_pull_up, MuscleGroup.BACK, ExerciseKind.BODYWEIGHT_REPS),
            Triple(R.string.ex_squat, MuscleGroup.LEGS, ExerciseKind.WEIGHT_REPS),
            Triple(R.string.ex_leg_press, MuscleGroup.LEGS, ExerciseKind.WEIGHT_REPS),
            Triple(R.string.ex_lunge, MuscleGroup.LEGS, ExerciseKind.WEIGHT_REPS),
            Triple(R.string.ex_rdl, MuscleGroup.LEGS, ExerciseKind.WEIGHT_REPS),
            Triple(R.string.ex_calf_raise, MuscleGroup.LEGS, ExerciseKind.WEIGHT_REPS),
            Triple(R.string.ex_ohp, MuscleGroup.SHOULDERS, ExerciseKind.WEIGHT_REPS),
            Triple(R.string.ex_lateral_raise, MuscleGroup.SHOULDERS, ExerciseKind.WEIGHT_REPS),
            Triple(R.string.ex_bicep_curl, MuscleGroup.ARMS, ExerciseKind.WEIGHT_REPS),
            Triple(R.string.ex_tricep_pushdown, MuscleGroup.ARMS, ExerciseKind.WEIGHT_REPS),
            Triple(R.string.ex_dip, MuscleGroup.ARMS, ExerciseKind.BODYWEIGHT_REPS),
            Triple(R.string.ex_plank, MuscleGroup.CORE, ExerciseKind.DURATION),
            Triple(R.string.ex_crunch, MuscleGroup.CORE, ExerciseKind.BODYWEIGHT_REPS),
            Triple(R.string.ex_kettlebell_swing, MuscleGroup.FULL_BODY, ExerciseKind.WEIGHT_REPS),
            Triple(R.string.ex_burpee, MuscleGroup.FULL_BODY, ExerciseKind.BODYWEIGHT_REPS),
        )
    }
}
