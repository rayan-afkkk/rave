package com.rave.app.data.repository

import com.rave.app.core.TimeProvider
import com.rave.app.data.local.dao.ActivityDao
import com.rave.app.data.local.dao.LocationPointDao
import com.rave.app.data.local.entity.ActivityEntity
import com.rave.app.data.local.toSummary
import com.rave.app.data.local.toTrackPoint
import com.rave.app.domain.model.ActivitySummary
import com.rave.app.domain.model.TrackPoint
import com.rave.app.widget.WidgetRefresher
import com.rave.app.data.preferences.SettingsRepository
import com.rave.app.work.SyncScheduler
import com.rave.app.work.SyncWorker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ActivityRepository @Inject constructor(
    private val activityDao: ActivityDao,
    private val pointDao: LocationPointDao,
    private val time: TimeProvider,
    private val widget: WidgetRefresher,
    private val settings: SettingsRepository,
    private val sync: SyncScheduler,
) {
    fun observeCompleted(): Flow<List<ActivityEntity>> = activityDao.observeCompleted()

    fun observeBetween(from: LocalDate, to: LocalDate): Flow<List<ActivityEntity>> =
        activityDao.observeCompletedBetween(time.startOfDayMs(from), time.endOfDayMs(to))

    fun observe(id: Long): Flow<ActivityEntity?> = activityDao.observeById(id)

    suspend fun get(id: Long): ActivityEntity? = activityDao.getById(id)

    suspend fun points(id: Long): List<TrackPoint> = pointDao.forActivity(id).map { it.toTrackPoint() }

    fun observePoints(id: Long): Flow<List<TrackPoint>> = pointDao.observeForActivity(id).map { l -> l.map { it.toTrackPoint() } }

    /** Total km tracked today (re-subscribes when the day rolls over). */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeDistanceToday(): Flow<Double> = time.todayFlow.flatMapLatest { today ->
        observeBetween(today, today).map { list -> list.sumOf { it.distanceM } }
    }

    suspend fun summariesBetween(fromMs: Long, toMs: Long): List<ActivitySummary> =
        activityDao.getCompletedBetween(fromMs, toMs).map { it.toSummary() }

    suspend fun allCompleted(): List<ActivityEntity> = activityDao.getAllCompleted()

    suspend fun updateDetails(id: Long, name: String, note: String, favorite: Boolean) {
        val a = activityDao.getById(id) ?: return
        activityDao.update(a.copy(name = name.trim().ifEmpty { a.name }, note = note.trim(), favorite = favorite, synced = false))
    }

    suspend fun setFavorite(id: Long, favorite: Boolean) = activityDao.setFavorite(id, favorite)

    /** Deletes locally; a synced activity is queued for removal from the cloud by the sync worker. */
    suspend fun delete(id: Long) {
        val remoteId = activityDao.getById(id)?.remoteId
        activityDao.delete(id)
        if (remoteId != null) {
            val pending = settings.getString(SyncWorker.PENDING_DELETES)?.split(',')?.filter { it.isNotBlank() }.orEmpty()
            settings.putString(SyncWorker.PENDING_DELETES, (pending + remoteId).joinToString(","))
            sync.requestSync()
        }
        widget.refresh()
    }
}
