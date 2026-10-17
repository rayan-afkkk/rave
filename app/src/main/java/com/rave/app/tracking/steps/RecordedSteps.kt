package com.rave.app.tracking.steps

import android.content.Context
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.fitness.FitnessLocal
import com.google.android.gms.fitness.LocalRecordingClient
import com.google.android.gms.fitness.data.LocalDataType
import com.google.android.gms.fitness.request.LocalDataReadRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Steps from Google Play services' on-device Recording API. Once subscribed, Play services
 * counts steps all day in the background (no Google account, nothing leaves the phone), so
 * the totals are right even when Rave hasn't been opened. Keeps up to 10 days.
 */
@Singleton
class RecordedSteps @Inject constructor(@ApplicationContext private val context: Context) {

    private val client by lazy { FitnessLocal.getLocalRecordingClient(context) }

    /** Play services is new enough for the Recording API. */
    val isAvailable: Boolean
        get() = runCatching {
            GoogleApiAvailability.getInstance()
                .isGooglePlayServicesAvailable(context, LocalRecordingClient.LOCAL_RECORDING_CLIENT_MIN_VERSION_CODE) == ConnectionResult.SUCCESS
        }.getOrDefault(false)

    /** Starts (or renews) background step recording. Safe to call often. */
    suspend fun subscribe(): Boolean = runCatching {
        client.subscribe(LocalDataType.TYPE_STEP_COUNT_DELTA).await()
        true
    }.getOrDefault(false)

    /** Steps per day for the last [days] days (today included), or null if unavailable. */
    suspend fun daily(days: Int): Map<LocalDate, Int>? = runCatching {
        val zone = ZoneId.systemDefault()
        val end = ZonedDateTime.now(zone)
        val start = LocalDate.now(zone).minusDays(days - 1L).atStartOfDay(zone)
        val request = LocalDataReadRequest.Builder()
            .aggregate(LocalDataType.TYPE_STEP_COUNT_DELTA)
            .bucketByTime(1, TimeUnit.DAYS)
            .setTimeRange(start.toEpochSecond(), end.toEpochSecond(), TimeUnit.SECONDS)
            .build()
        val response = client.readData(request).await()
        val totals = mutableMapOf<LocalDate, Int>()
        for (bucket in response.buckets) {
            for (dataSet in bucket.dataSets) {
                for (point in dataSet.dataPoints) {
                    val day = Instant.ofEpochMilli(point.getStartTime(TimeUnit.MILLISECONDS)).atZone(zone).toLocalDate()
                    val steps = point.dataType.fields.sumOf { point.getValue(it).asInt() }
                    totals[day] = (totals[day] ?: 0) + steps
                }
            }
        }
        totals
    }.getOrNull()
}
