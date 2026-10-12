package com.rave.app.data.local

import com.rave.app.data.local.entity.ActivityEntity
import com.rave.app.data.local.entity.HabitEntity
import com.rave.app.data.local.entity.HabitLogEntity
import com.rave.app.data.local.entity.LocationPointEntity
import com.rave.app.domain.model.ActivitySummary
import com.rave.app.domain.model.Habit
import com.rave.app.domain.model.HabitLog
import com.rave.app.domain.model.HabitSchedule
import com.rave.app.domain.model.LatLngPoint
import com.rave.app.domain.model.PbTag
import com.rave.app.domain.model.TrackPoint
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

private val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

fun HabitEntity.toDomain() = Habit(
    id = id,
    name = name,
    icon = icon,
    color = color,
    type = type,
    target = target,
    unit = unit,
    schedule = HabitSchedule(scheduleType, scheduleDaysMask, timesPerWeek),
    reminderTimes = reminderTimes.mapNotNull { runCatching { LocalTime.parse(it, TIME_FORMAT) }.getOrNull() },
    remindersEnabled = remindersEnabled,
    linkedActivityType = linkedActivityType,
    activityGoalUnit = activityGoalUnit,
    sharedStreakId = sharedStreakId,
    sharedWithName = sharedWithName,
    archived = archived,
    createdDate = LocalDate.ofEpochDay(createdEpochDay),
    archivedDate = archivedEpochDay?.let(LocalDate::ofEpochDay),
)

fun Habit.toEntity(createdAt: Long, sortOrder: Int = 0) = HabitEntity(
    id = id,
    name = name.trim(),
    icon = icon,
    color = color,
    type = type,
    target = target,
    unit = unit,
    scheduleType = schedule.type,
    scheduleDaysMask = schedule.daysMask,
    timesPerWeek = schedule.timesPerWeek,
    reminderTimes = reminderTimes.sorted().map { it.format(TIME_FORMAT) },
    remindersEnabled = remindersEnabled,
    linkedActivityType = linkedActivityType,
    activityGoalUnit = activityGoalUnit,
    sharedStreakId = sharedStreakId,
    sharedWithName = sharedWithName,
    archived = archived,
    createdAt = createdAt,
    createdEpochDay = createdDate.toEpochDay(),
    archivedEpochDay = archivedDate?.toEpochDay(),
    sortOrder = sortOrder,
)

fun HabitLogEntity.toDomain() = HabitLog(
    id = id,
    habitId = habitId,
    date = LocalDate.ofEpochDay(date),
    value = value,
    completed = completed,
    skipped = skipped,
    source = source,
)

fun HabitLog.toEntity() = HabitLogEntity(
    id = id,
    habitId = habitId,
    date = date.toEpochDay(),
    value = value,
    completed = completed,
    skipped = skipped,
    source = source,
)

fun LocationPointEntity.toTrackPoint() = TrackPoint(
    lat = lat, lng = lng, altitude = alt, accuracy = accuracy, speed = speed, timeMs = timestamp, segment = segment,
)

fun LocationPointEntity.toLatLng() = LatLngPoint(lat, lng)

fun ActivityEntity.toSummary() = ActivitySummary(
    id = id,
    type = type,
    startTimeMs = startTime,
    distanceM = distanceM,
    movingTimeS = movingTimeS,
    autoDetected = autoDetected,
)

val ActivityEntity.pbTagSet: Set<PbTag>
    get() = pbTags.split(',').filter { it.isNotBlank() }.mapNotNull { runCatching { PbTag.valueOf(it) }.getOrNull() }.toSet()
