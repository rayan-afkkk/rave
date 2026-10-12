package com.rave.app.ui.workout

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rave.app.core.TimeProvider
import com.rave.app.data.local.entity.WorkoutEntity
import com.rave.app.data.local.entity.WorkoutSetEntity
import com.rave.app.data.preferences.SettingsRepository
import com.rave.app.data.repository.WorkoutRepository
import com.rave.app.domain.model.DistanceUnit
import com.rave.app.domain.workout.Exercise
import com.rave.app.domain.workout.ExerciseKind
import com.rave.app.domain.workout.MuscleGroup
import com.rave.app.domain.workout.WorkoutMath
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** One exercise inside the active workout, with its sets and last session's sets. */
data class ExerciseBlock(
    val exercise: Exercise,
    val sets: List<WorkoutSetEntity>,
    val previous: List<WorkoutSetEntity>,
)

data class WorkoutUiState(
    val loading: Boolean = true,
    val workout: WorkoutEntity? = null,
    val blocks: List<ExerciseBlock> = emptyList(),
    val library: List<Exercise> = emptyList(),
    val nowMs: Long = 0,
    val restEndsAt: Long? = null,
    val restTotalS: Int = DEFAULT_REST_S,
    /** Show weights in lb (imperial users). Values are always stored in kg. */
    val pounds: Boolean = false,
) {
    val elapsedS: Long get() = workout?.let { ((nowMs - it.startTime) / 1000).coerceAtLeast(0) } ?: 0
    val restLeftS: Int get() = restEndsAt?.let { ((it - nowMs + 999) / 1000).toInt().coerceAtLeast(0) } ?: 0
    val doneSets: Int get() = blocks.sumOf { b -> b.sets.count { it.done && !it.warmup } }
    val volumeKg: Double get() = blocks.sumOf { b -> b.sets.filter { it.done && !it.warmup }.sumOf { it.weightKg * it.reps } }
}

const val DEFAULT_REST_S = 90

sealed interface WorkoutEvent {
    data class Finished(val workoutId: Long) : WorkoutEvent
    data object Empty : WorkoutEvent
    data object Discarded : WorkoutEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class WorkoutViewModel @Inject constructor(
    private val savedState: SavedStateHandle,
    private val repo: WorkoutRepository,
    settings: SettingsRepository,
    private val time: TimeProvider,
) : ViewModel() {
    private val workoutId = MutableStateFlow<Long?>(null)
    private val rest = MutableStateFlow(savedState.get<Long>(KEY_REST_END) to (savedState.get<Int>(KEY_REST_TOTAL) ?: DEFAULT_REST_S))
    private val previousCache = mutableMapOf<Long, List<WorkoutSetEntity>>()
    private val clock = flow { while (true) { emit(time.nowMs()); delay(500) } }

    private val _events = Channel<WorkoutEvent>(Channel.BUFFERED)
    val events: Flow<WorkoutEvent> = _events.receiveAsFlow()

    private val workoutAndSets = workoutId.filterNotNull().flatMapLatest { id ->
        combine(repo.observeWorkout(id), repo.observeSets(id)) { w, s -> w to s }
    }

    val state: StateFlow<WorkoutUiState> = combine(
        workoutAndSets, repo.observeExercises(), clock, rest, settings.settings,
    ) { (w, sets), library, now, (restEnd, restTotal), prefs ->
        val byId = library.associateBy { it.id }
        val blocks = sets.groupBy { it.exerciseId }.entries
            .sortedBy { (_, s) -> s.minOf { it.exerciseOrder } }
            .mapNotNull { (exId, s) ->
                val ex = byId[exId] ?: return@mapNotNull null
                ExerciseBlock(ex, s.sortedBy { it.setOrder }, previousCache[exId].orEmpty())
            }
        WorkoutUiState(
            loading = false, workout = w, blocks = blocks, library = library, nowMs = now,
            restEndsAt = restEnd?.takeIf { it > now }, restTotalS = restTotal, pounds = prefs.unit == DistanceUnit.MILES,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WorkoutUiState())

    init {
        viewModelScope.launch {
            val id = repo.startOrResume()
            // Warm the "Previous" column for exercises already in a resumed workout.
            repo.observeSets(id).collect { sets ->
                sets.map { it.exerciseId }.distinct().filter { it !in previousCache }.forEach { exId ->
                    previousCache[exId] = repo.lastSession(exId, id)
                }
                if (workoutId.value == null) workoutId.value = id
            }
        }
    }

    private fun id(): Long? = workoutId.value

    fun addExercise(exercise: Exercise) {
        val id = id() ?: return
        viewModelScope.launch {
            previousCache[exercise.id] = repo.lastSession(exercise.id, id)
            repo.addExercise(id, exercise.id)
        }
    }

    fun createExercise(name: String, muscle: MuscleGroup, kind: ExerciseKind) {
        viewModelScope.launch {
            repo.createExercise(name, muscle, kind).onSuccess { newId ->
                addExercise(Exercise(newId, name.trim(), muscle, kind, custom = true))
            }
        }
    }

    fun addSet(exerciseId: Long) {
        val id = id() ?: return
        viewModelScope.launch { repo.addSet(id, exerciseId) }
    }

    fun removeExercise(exerciseId: Long) {
        val id = id() ?: return
        viewModelScope.launch { repo.removeExercise(id, exerciseId) }
    }

    fun deleteSet(set: WorkoutSetEntity) = viewModelScope.launch { repo.deleteSet(set.id) }

    /** [weight] is in the display unit (kg or lb). */
    fun setWeight(set: WorkoutSetEntity, weight: Double) {
        val kg = if (state.value.pounds) weight / WorkoutMath.LB_PER_KG else weight
        update(set.copy(weightKg = kg))
    }

    fun setReps(set: WorkoutSetEntity, reps: Int) = update(set.copy(reps = reps))
    fun setDuration(set: WorkoutSetEntity, seconds: Int) = update(set.copy(durationS = seconds))
    fun toggleWarmup(set: WorkoutSetEntity) = update(set.copy(warmup = !set.warmup))

    /** Ticking a set off starts the rest timer. */
    fun toggleDone(set: WorkoutSetEntity) {
        val done = !set.done
        update(set.copy(done = done))
        if (done && !set.warmup) startRest(rest.value.second)
    }

    private fun update(set: WorkoutSetEntity) {
        viewModelScope.launch { repo.updateSet(set) }
    }

    fun startRest(seconds: Int) {
        val end = time.nowMs() + seconds * 1000L
        rest.value = end to seconds
        savedState[KEY_REST_END] = end
        savedState[KEY_REST_TOTAL] = seconds
    }

    fun adjustRest(deltaS: Int) {
        val (end, total) = rest.value
        if (end == null) return
        val newTotal = (total + deltaS).coerceAtLeast(15)
        val newEnd = (end + deltaS * 1000L).coerceAtLeast(time.nowMs())
        rest.value = newEnd to newTotal
        savedState[KEY_REST_END] = newEnd
        savedState[KEY_REST_TOTAL] = newTotal
    }

    fun skipRest() {
        rest.value = null to rest.value.second
        savedState.remove<Long>(KEY_REST_END)
    }

    fun finish() {
        val id = id() ?: return
        viewModelScope.launch {
            val result = repo.finish(id)
            _events.send(if (result == null) WorkoutEvent.Empty else WorkoutEvent.Finished(result.workoutId))
        }
    }

    fun discard() {
        val id = id() ?: return
        viewModelScope.launch {
            repo.discard(id)
            _events.send(WorkoutEvent.Discarded)
        }
    }

    private companion object {
        const val KEY_REST_END = "rest_end"
        const val KEY_REST_TOTAL = "rest_total"
    }
}
