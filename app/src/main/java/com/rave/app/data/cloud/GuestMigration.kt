package com.rave.app.data.cloud

import com.google.firebase.firestore.SetOptions
import com.rave.app.data.local.dao.ActivityDao
import com.rave.app.data.local.dao.HabitDao
import com.rave.app.data.local.dao.HabitLogDao
import com.rave.app.data.local.entity.HabitEntity
import com.rave.app.data.local.entity.HabitLogEntity
import com.rave.app.data.repository.StreakRepository
import com.rave.app.domain.model.ActivityGoalUnit
import com.rave.app.domain.model.ActivityType
import com.rave.app.domain.model.HabitColor
import com.rave.app.domain.model.HabitType
import com.rave.app.domain.model.LogSource
import com.rave.app.domain.model.ScheduleType
import com.rave.app.work.ReminderScheduler
import com.rave.app.work.SyncScheduler
import com.rave.app.data.local.toDomain
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * First sign-in: the guest's local data becomes the account's data. Activities are marked
 * unsynced so the sync worker uploads their summaries; habits + logs go to a cloud backup.
 * On a fresh install with an empty database, the backup is restored instead.
 */
@Singleton
class GuestMigration @Inject constructor(
    private val firebase: FirebaseProvider,
    private val habitDao: HabitDao,
    private val logDao: HabitLogDao,
    private val activityDao: ActivityDao,
    private val streaks: StreakRepository,
    private val reminders: ReminderScheduler,
    private val sync: SyncScheduler,
) {
    suspend fun onSignedIn() {
        runCatching {
            if (habitDao.count() == 0) restoreBackup() else uploadBackup()
        }
        activityDao.markAllUnsynced()
        sync.requestSync()
    }

    private fun backup() = firebase.requireFirestore().collection(ProfileRepository.USERS)
        .document(firebase.requireUid()).collection("backup")

    /** Habits and logs as two compact documents (fine for years of daily logs). */
    suspend fun uploadBackup() {
        val habits = habitDao.getAll()
        val logs = logDao.getAll()
        backup().document("habits").set(mapOf("items" to habits.map { it.toMap() })).await()
        backup().document("logs").set(mapOf("items" to logs.takeLast(5000).map { it.toMap() }), SetOptions.merge()).await()
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun restoreBackup() {
        val habitsDoc = backup().document("habits").get().await()
        val items = habitsDoc.get("items") as? List<Map<String, Any?>> ?: return
        val idMap = mutableMapOf<Long, Long>()
        for (m in items) {
            val oldId = (m["id"] as? Number)?.toLong() ?: continue
            val entity = m.toHabitEntity() ?: continue
            idMap[oldId] = habitDao.upsert(entity.copy(id = 0))
        }
        val logs = backup().document("logs").get().await().get("items") as? List<Map<String, Any?>> ?: emptyList()
        for (m in logs) {
            val habitId = idMap[(m["habitId"] as? Number)?.toLong()] ?: continue
            logDao.upsert(
                HabitLogEntity(
                    habitId = habitId,
                    date = (m["date"] as? Number)?.toLong() ?: continue,
                    value = (m["value"] as? Number)?.toInt() ?: 0,
                    completed = m["completed"] == true,
                    skipped = m["skipped"] == true,
                    source = runCatching { LogSource.valueOf(m["source"] as String) }.getOrDefault(LogSource.MANUAL),
                ),
            )
        }
        reminders.rescheduleAll(habitDao.getActive().map { it.toDomain() })
        streaks.recompute()
    }

    private fun HabitEntity.toMap() = mapOf(
        "id" to id, "name" to name, "icon" to icon, "color" to color.name, "type" to type.name, "target" to target,
        "unit" to unit, "scheduleType" to scheduleType.name, "daysMask" to scheduleDaysMask, "timesPerWeek" to timesPerWeek,
        "reminders" to reminderTimes, "remindersEnabled" to remindersEnabled, "linkedActivityType" to linkedActivityType?.name,
        "goalUnit" to activityGoalUnit.name, "archived" to archived, "createdAt" to createdAt, "createdEpochDay" to createdEpochDay,
        "archivedEpochDay" to archivedEpochDay, "sortOrder" to sortOrder,
    )

    private fun HabitLogEntity.toMap() = mapOf(
        "habitId" to habitId, "date" to date, "value" to value, "completed" to completed, "skipped" to skipped, "source" to source.name,
    )

    @Suppress("UNCHECKED_CAST")
    private fun Map<String, Any?>.toHabitEntity(): HabitEntity? = runCatching {
        HabitEntity(
            name = this["name"] as String,
            icon = this["icon"] as? String ?: "check",
            color = HabitColor.valueOf(this["color"] as String),
            type = HabitType.valueOf(this["type"] as String),
            target = (this["target"] as Number).toInt(),
            unit = this["unit"] as? String,
            scheduleType = ScheduleType.valueOf(this["scheduleType"] as String),
            scheduleDaysMask = (this["daysMask"] as Number).toInt(),
            timesPerWeek = (this["timesPerWeek"] as Number).toInt(),
            reminderTimes = (this["reminders"] as? List<String>).orEmpty(),
            remindersEnabled = this["remindersEnabled"] == true,
            linkedActivityType = (this["linkedActivityType"] as? String)?.let { ActivityType.valueOf(it) },
            activityGoalUnit = (this["goalUnit"] as? String)?.let { ActivityGoalUnit.valueOf(it) } ?: ActivityGoalUnit.METERS,
            archived = this["archived"] == true,
            createdAt = (this["createdAt"] as Number).toLong(),
            createdEpochDay = (this["createdEpochDay"] as Number).toLong(),
            archivedEpochDay = (this["archivedEpochDay"] as? Number)?.toLong(),
            sortOrder = (this["sortOrder"] as? Number)?.toInt() ?: 0,
        )
    }.getOrNull()
}
