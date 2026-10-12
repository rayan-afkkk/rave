package com.rave.app.data.export

import android.content.Context
import android.net.Uri
import com.rave.app.data.local.dao.ActivityDao
import com.rave.app.data.local.dao.HabitDao
import com.rave.app.data.local.dao.HabitLogDao
import com.rave.app.data.local.dao.LocationPointDao
import com.rave.app.data.local.dao.PrivacyZoneDao
import com.rave.app.data.local.dao.StatsDao
import com.rave.app.data.local.entity.ActivityEntity
import com.rave.app.data.local.entity.LocationPointEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/** "Export my data": a zip with data.json (everything) and one GPX file per activity. */
@Singleton
class DataExporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val habitDao: HabitDao,
    private val logDao: HabitLogDao,
    private val activityDao: ActivityDao,
    private val pointDao: LocationPointDao,
    private val zoneDao: PrivacyZoneDao,
    private val statsDao: StatsDao,
) {
    suspend fun exportTo(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val activities = activityDao.getAllCompleted()
            context.contentResolver.openOutputStream(uri)?.use { out ->
                ZipOutputStream(out).use { zip ->
                    zip.putNextEntry(ZipEntry("data.json"))
                    zip.write(buildJson(activities).toString(2).toByteArray())
                    zip.closeEntry()
                    for (a in activities) {
                        val points = pointDao.forActivity(a.id)
                        if (points.isEmpty()) continue
                        zip.putNextEntry(ZipEntry("gpx/activity-${a.id}.gpx"))
                        zip.write(gpx(a, points).toByteArray())
                        zip.closeEntry()
                    }
                }
            } ?: return@runCatching false
            true
        }.getOrDefault(false)
    }

    private suspend fun buildJson(activities: List<ActivityEntity>): JSONObject {
        val root = JSONObject()
        root.put("exportedAt", Instant.now().toString())
        root.put("habits", JSONArray().apply {
            habitDao.getAll().forEach { h ->
                put(JSONObject().apply {
                    put("id", h.id); put("name", h.name); put("icon", h.icon); put("color", h.color.name); put("type", h.type.name)
                    put("target", h.target); put("unit", h.unit ?: JSONObject.NULL); put("schedule", h.scheduleType.name)
                    put("daysMask", h.scheduleDaysMask); put("timesPerWeek", h.timesPerWeek)
                    put("reminders", JSONArray(h.reminderTimes)); put("archived", h.archived); put("createdAt", h.createdAt)
                })
            }
        })
        root.put("habitLogs", JSONArray().apply {
            logDao.getAll().forEach { l ->
                put(JSONObject().apply {
                    put("habitId", l.habitId); put("epochDay", l.date); put("value", l.value)
                    put("completed", l.completed); put("skipped", l.skipped); put("source", l.source.name)
                })
            }
        })
        root.put("activities", JSONArray().apply {
            activities.forEach { a ->
                put(JSONObject().apply {
                    put("id", a.id); put("type", a.type.name); put("name", a.name); put("note", a.note)
                    put("startTime", a.startTime); put("endTime", a.endTime ?: JSONObject.NULL)
                    put("distanceM", a.distanceM); put("durationS", a.durationS); put("movingTimeS", a.movingTimeS)
                    put("calories", a.calories); put("elevationGainM", a.elevationGainM); put("favorite", a.favorite)
                    put("autoDetected", a.autoDetected); put("pbTags", a.pbTags)
                })
            }
        })
        root.put("privacyZones", JSONArray().apply {
            zoneDao.getAll().forEach { z -> put(JSONObject().apply { put("label", z.label); put("lat", z.lat); put("lng", z.lng); put("radiusM", z.radiusM) }) }
        })
        root.put("badges", JSONArray().apply {
            statsDao.getBadges().forEach { b -> put(JSONObject().apply { put("id", b.id); put("earnedAt", b.earnedAt) }) }
        })
        return root
    }

    private fun gpx(a: ActivityEntity, points: List<LocationPointEntity>): String = buildString {
        append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        append("<gpx version=\"1.1\" creator=\"Rave\" xmlns=\"http://www.topografix.com/GPX/1/1\">\n")
        append("  <trk><name>").append(escape(a.name)).append("</name><type>").append(a.type.name.lowercase()).append("</type>\n")
        points.groupBy { it.segment }.toSortedMap().values.forEach { seg ->
            append("    <trkseg>\n")
            seg.forEach { p ->
                append("      <trkpt lat=\"").append(p.lat).append("\" lon=\"").append(p.lng).append("\">")
                p.alt?.let { append("<ele>").append(it).append("</ele>") }
                append("<time>").append(Instant.ofEpochMilli(p.timestamp)).append("</time></trkpt>\n")
            }
            append("    </trkseg>\n")
        }
        append("  </trk>\n</gpx>\n")
    }

    private fun escape(s: String) = s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
}
