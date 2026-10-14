package com.rave.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.rave.app.data.local.entity.ExerciseEntity
import com.rave.app.data.local.entity.WorkoutEntity
import com.rave.app.data.local.entity.WorkoutSetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutDao {
    // Exercises
    @Query("SELECT * FROM exercises WHERE archived = 0 ORDER BY name COLLATE NOCASE")
    fun observeExercises(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises")
    suspend fun allExercises(): List<ExerciseEntity>

    @Query("SELECT COUNT(*) FROM exercises")
    suspend fun exerciseCount(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertExercises(exercises: List<ExerciseEntity>)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertExercise(exercise: ExerciseEntity): Long

    // Workouts
    @Insert
    suspend fun insertWorkout(workout: WorkoutEntity): Long

    @Update
    suspend fun updateWorkout(workout: WorkoutEntity)

    @Query("SELECT * FROM workouts WHERE id = :id")
    suspend fun workout(id: Long): WorkoutEntity?

    @Query("SELECT * FROM workouts WHERE id = :id")
    fun observeWorkout(id: Long): Flow<WorkoutEntity?>

    @Query("SELECT * FROM workouts WHERE endTime IS NULL ORDER BY startTime DESC LIMIT 1")
    suspend fun activeWorkout(): WorkoutEntity?

    @Query("SELECT * FROM workouts WHERE endTime IS NULL ORDER BY startTime DESC LIMIT 1")
    fun observeActiveWorkout(): Flow<WorkoutEntity?>

    @Query("SELECT * FROM workouts WHERE endTime IS NOT NULL ORDER BY startTime DESC")
    fun observeHistory(): Flow<List<WorkoutEntity>>

    @Query("SELECT COUNT(*) FROM workouts WHERE endTime IS NOT NULL")
    fun observeCount(): Flow<Int>

    @Query("SELECT * FROM workouts WHERE endTime IS NOT NULL")
    suspend fun finishedWorkouts(): List<WorkoutEntity>

    @Query("SELECT * FROM workouts WHERE endTime IS NOT NULL AND startTime BETWEEN :from AND :to")
    suspend fun finishedBetween(from: Long, to: Long): List<WorkoutEntity>

    @Query("DELETE FROM workouts WHERE id = :id")
    suspend fun deleteWorkout(id: Long)

    // Sets
    @Query("SELECT * FROM workout_sets WHERE workoutId = :workoutId ORDER BY exerciseOrder, setOrder")
    fun observeSets(workoutId: Long): Flow<List<WorkoutSetEntity>>

    @Query("SELECT * FROM workout_sets WHERE workoutId = :workoutId ORDER BY exerciseOrder, setOrder")
    suspend fun sets(workoutId: Long): List<WorkoutSetEntity>

    @Upsert
    suspend fun upsertSet(set: WorkoutSetEntity): Long

    @Query("DELETE FROM workout_sets WHERE id = :id")
    suspend fun deleteSet(id: Long)

    @Query("DELETE FROM workout_sets WHERE workoutId = :workoutId AND exerciseId = :exerciseId")
    suspend fun deleteExerciseSets(workoutId: Long, exerciseId: Long)

    /** Completed sets of an exercise in finished workouts other than [excludeWorkoutId]. */
    @Query(
        """SELECT s.* FROM workout_sets s JOIN workouts w ON w.id = s.workoutId
           WHERE s.exerciseId = :exerciseId AND s.done = 1 AND w.endTime IS NOT NULL AND w.id != :excludeWorkoutId
           ORDER BY w.startTime DESC, s.setOrder""",
    )
    suspend fun history(exerciseId: Long, excludeWorkoutId: Long): List<WorkoutSetEntity>

    @Query("SELECT COUNT(*) FROM workout_sets s JOIN workouts w ON w.id = s.workoutId WHERE s.done = 1 AND s.warmup = 0 AND w.endTime IS NOT NULL")
    fun observeWorkingSetCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM workout_sets s JOIN workouts w ON w.id = s.workoutId WHERE s.done = 1 AND s.warmup = 0 AND w.endTime IS NOT NULL AND w.startTime >= :fromMs")
    fun observeWorkingSetCountSince(fromMs: Long): Flow<Int>

    @Query("SELECT * FROM workout_sets s WHERE s.done = 1 AND s.warmup = 0 AND s.workoutId IN (SELECT id FROM workouts WHERE endTime IS NOT NULL)")
    suspend fun allWorkingSets(): List<WorkoutSetEntity>
}
