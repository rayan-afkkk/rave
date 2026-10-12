package com.rave.app.core

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject
import javax.inject.Singleton

/** Single source of "now" so logic is testable and day boundaries are consistent. */
@Singleton
class TimeProvider @Inject constructor() {
    var clock: Clock = Clock.systemDefaultZone()

    val zone: ZoneId get() = clock.zone
    fun nowMs(): Long = clock.millis()
    fun today(): LocalDate = LocalDate.now(clock)
    fun now(): LocalDateTime = LocalDateTime.now(clock)

    fun startOfDayMs(date: LocalDate): Long = date.atStartOfDay(zone).toInstant().toEpochMilli()
    fun endOfDayMs(date: LocalDate): Long = startOfDayMs(date.plusDays(1)) - 1
    fun dateOf(ms: Long): LocalDate = Instant.ofEpochMilli(ms).atZone(zone).toLocalDate()
    fun dateTimeOf(ms: Long): LocalDateTime = Instant.ofEpochMilli(ms).atZone(zone).toLocalDateTime()

    fun weekStart(date: LocalDate = today()): LocalDate = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    /** Emits today's date and re-emits when the day rolls over. */
    val todayFlow: Flow<LocalDate> = flow {
        while (true) {
            emit(today())
            delay(30_000)
        }
    }.distinctUntilChanged()
}
