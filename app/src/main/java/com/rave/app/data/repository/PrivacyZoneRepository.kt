package com.rave.app.data.repository

import com.rave.app.data.local.dao.PrivacyZoneDao
import com.rave.app.data.local.entity.PrivacyZoneEntity
import com.rave.app.data.preferences.SettingsRepository
import com.rave.app.domain.model.LatLngPoint
import com.rave.app.domain.model.TrackPoint
import com.rave.app.domain.tracking.PrivacyArea
import com.rave.app.domain.tracking.PrivacyFilter
import com.rave.app.domain.tracking.RouteCleaner
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrivacyZoneRepository @Inject constructor(
    private val dao: PrivacyZoneDao,
    private val settings: SettingsRepository,
) {
    fun observeZones(): Flow<List<PrivacyZoneEntity>> = dao.observeAll()

    suspend fun save(zone: PrivacyZoneEntity) = dao.upsert(zone)

    suspend fun delete(id: Long) = dao.delete(id)

    /** The route as it may be shown to others: ends trimmed + zones removed. */
    suspend fun publicSegments(points: List<TrackPoint>): List<List<LatLngPoint>> {
        val zones = dao.getAll().map { PrivacyArea(it.lat, it.lng, it.radiusM) }
        val hideEnds = settings.current().hideStartEnd
        return PrivacyFilter.apply(points.map { LatLngPoint(it.lat, it.lng) }, zones, trimEnds = hideEnds)
    }

    /**
     * The route for a share card: the same line as the Routes list (see [RouteCleaner.asTracked]).
     * Cards show only the line on a plain background, with no map or coordinates, so the start
     * and end aren't trimmed; privacy zones are still removed.
     */
    suspend fun shareCardSegments(points: List<TrackPoint>): List<List<LatLngPoint>> {
        val zones = dao.getAll().map { PrivacyArea(it.lat, it.lng, it.radiusM) }
        return RouteCleaner.asTracked(points).flatMap { PrivacyFilter.apply(it, zones, trimEnds = false) }
    }
}
