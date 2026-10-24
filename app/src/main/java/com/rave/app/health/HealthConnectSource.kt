package com.rave.app.health

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseRoute
import androidx.health.connect.client.records.ExerciseRouteResult
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.metadata.DataOrigin
import androidx.health.connect.client.request.AggregateGroupByPeriodRequest
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.rave.app.domain.model.ActivityType
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import javax.inject.Inject
import javax.inject.Singleton

/** A walk or run recorded by another app (usually a smartwatch) and shared through Health Connect. */
data class WatchSession(
    val id: String,
    val type: ActivityType,
    val startMs: Long,
    val endMs: Long,
    val distanceM: Double,
    val sourceApp: String,
    val route: List<ExerciseRoute.Location>,
)

/**
 * Read-only access to Health Connect, where watch apps (Samsung Health, Fitbit, Garmin Connect,
 * Zepp…) store steps and workouts. Everything is optional: when Health Connect is missing or
 * permissions aren't granted, the reads return nothing.
 */
@Singleton
class HealthConnectSource @Inject constructor(@ApplicationContext private val context: Context) {

    enum class Availability { AVAILABLE, NEEDS_UPDATE, UNAVAILABLE }

    fun availability(): Availability = when (runCatching { HealthConnectClient.getSdkStatus(context) }.getOrNull()) {
        HealthConnectClient.SDK_AVAILABLE -> Availability.AVAILABLE
        HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> Availability.NEEDS_UPDATE
        else -> Availability.UNAVAILABLE
    }

    /** Opens Play to install or update Health Connect. */
    fun installIntent(): Intent = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("market://details?id=$PROVIDER&url=healthconnect%3A%2F%2Fonboarding"),
    ).setPackage("com.android.vending").putExtra("overlay", true).putExtra("callerId", context.packageName)

    private val client: HealthConnectClient? by lazy {
        if (availability() == Availability.AVAILABLE) runCatching { HealthConnectClient.getOrCreate(context) }.getOrNull() else null
    }

    suspend fun hasPermissions(): Boolean {
        val c = client ?: return false
        return runCatching { c.permissionController.getGrantedPermissions().containsAll(PERMISSIONS) }.getOrDefault(false)
    }

    /** Steps per day for the last [days] days (including today), from all apps except Rave. */
    suspend fun dailySteps(days: Int, today: LocalDate): Map<LocalDate, Int> {
        val c = client ?: return emptyMap()
        val start = today.minusDays((days - 1).toLong()).atStartOfDay()
        val end = today.plusDays(1).atStartOfDay()
        return runCatching {
            c.aggregateGroupByPeriod(
                AggregateGroupByPeriodRequest(
                    metrics = setOf(StepsRecord.COUNT_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                    timeRangeSlicer = Period.ofDays(1),
                ),
            ).mapNotNull { group ->
                group.result[StepsRecord.COUNT_TOTAL]?.let { group.startTime.toLocalDate() to it.toInt() }
            }.toMap()
        }.getOrDefault(emptyMap())
    }

    /** Walks and runs that ended after [sinceMs], recorded by other apps. */
    suspend fun sessions(sinceMs: Long): List<WatchSession> {
        val c = client ?: return emptyList()
        val records = runCatching {
            c.readRecords(ReadRecordsRequest(ExerciseSessionRecord::class, TimeRangeFilter.after(Instant.ofEpochMilli(sinceMs)))).records
        }.getOrDefault(emptyList())
        return records.mapNotNull { r ->
            val origin = r.metadata.dataOrigin.packageName
            if (origin == context.packageName) return@mapNotNull null
            val type = typeOf(r.exerciseType) ?: return@mapNotNull null
            val distance = runCatching {
                c.aggregate(
                    AggregateRequest(
                        metrics = setOf(DistanceRecord.DISTANCE_TOTAL),
                        timeRangeFilter = TimeRangeFilter.between(r.startTime, r.endTime),
                        dataOriginFilter = setOf(DataOrigin(origin)),
                    ),
                )[DistanceRecord.DISTANCE_TOTAL]?.inMeters
            }.getOrNull() ?: 0.0
            // Routes from other apps are only included when the user has shared them with Rave.
            val route = (r.exerciseRouteResult as? ExerciseRouteResult.Data)?.exerciseRoute?.route.orEmpty()
            WatchSession(r.metadata.id, type, r.startTime.toEpochMilli(), r.endTime.toEpochMilli(), distance, origin, route)
        }
    }

    private fun typeOf(exerciseType: Int): ActivityType? = when (exerciseType) {
        ExerciseSessionRecord.EXERCISE_TYPE_WALKING, ExerciseSessionRecord.EXERCISE_TYPE_HIKING -> ActivityType.WALK
        ExerciseSessionRecord.EXERCISE_TYPE_RUNNING, ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL -> ActivityType.RUN
        else -> null
    }

    companion object {
        const val PROVIDER = "com.google.android.apps.healthdata"

        val PERMISSIONS: Set<String> = setOf(
            HealthPermission.getReadPermission(StepsRecord::class),
            HealthPermission.getReadPermission(ExerciseSessionRecord::class),
            HealthPermission.getReadPermission(DistanceRecord::class),
        )

        private val KNOWN_APPS = mapOf(
            "com.sec.android.app.shealth" to "Samsung Health",
            "com.fitbit.FitbitMobile" to "Fitbit",
            "com.google.android.apps.fitness" to "Google Fit",
            "com.garmin.android.apps.connectmobile" to "Garmin Connect",
            "com.huami.watch.hmwatchmanager" to "Zepp",
            "com.xiaomi.wearable" to "Mi Fitness",
            "com.mi.health" to "Mi Fitness",
            "com.google.android.apps.healthdata" to "Health Connect",
        )

        /** Friendly name for the app a workout came from. */
        fun appName(context: Context, packageName: String): String = KNOWN_APPS[packageName] ?: runCatching {
            val pm = context.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
        }.getOrDefault(packageName.substringAfterLast('.').replaceFirstChar { it.uppercase() })
    }
}
