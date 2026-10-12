package com.rave.app.ui.routes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rave.app.core.TimeProvider
import com.rave.app.data.local.entity.ActivityEntity
import com.rave.app.data.preferences.SettingsRepository
import com.rave.app.data.repository.ActivityRepository
import com.rave.app.domain.model.ActivityType
import com.rave.app.domain.model.DistanceUnit
import com.rave.app.domain.model.LatLngPoint
import com.rave.app.domain.tracking.Polyline
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class RouteFilter { ALL, WALKS, RUNS, FAVORITES }

data class WeekSummary(val distanceM: Double = 0.0, val count: Int = 0, val movingTimeS: Long = 0)

data class RoutesState(
    val loading: Boolean = true,
    val filter: RouteFilter = RouteFilter.ALL,
    val activities: List<ActivityEntity> = emptyList(),
    val week: WeekSummary = WeekSummary(),
    val unit: DistanceUnit = DistanceUnit.KM,
    val hasAny: Boolean = false,
)

@HiltViewModel
class RoutesViewModel @Inject constructor(
    private val activities: ActivityRepository,
    settings: SettingsRepository,
    private val time: TimeProvider,
) : ViewModel() {
    private val filter = MutableStateFlow(RouteFilter.ALL)
    private val thumbnails = mutableMapOf<Long, List<List<LatLngPoint>>>()

    val state: StateFlow<RoutesState> = combine(activities.observeCompleted(), filter, settings.settings) { all, f, prefs ->
        val weekStartMs = time.startOfDayMs(time.weekStart())
        val week = all.filter { it.startTime >= weekStartMs }
        RoutesState(
            loading = false,
            filter = f,
            activities = all.filter {
                when (f) {
                    RouteFilter.ALL -> true
                    RouteFilter.WALKS -> it.type == ActivityType.WALK
                    RouteFilter.RUNS -> it.type == ActivityType.RUN
                    RouteFilter.FAVORITES -> it.favorite
                }
            },
            week = WeekSummary(week.sumOf { it.distanceM }, week.size, week.sumOf { it.movingTimeS }),
            unit = prefs.unit,
            hasAny = all.isNotEmpty(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RoutesState())

    fun setFilter(f: RouteFilter) {
        filter.value = f
    }

    fun toggleFavorite(a: ActivityEntity) {
        viewModelScope.launch { activities.setFavorite(a.id, !a.favorite) }
    }

    /** Simplified route segments for a list thumbnail (cached). */
    suspend fun thumbnail(id: Long): List<List<LatLngPoint>> = thumbnails.getOrPut(id) {
        activities.points(id).groupBy { it.segment }.toSortedMap().values.map { seg ->
            Polyline.simplify(seg.map { LatLngPoint(it.lat, it.lng) }, toleranceM = 10.0)
        }
    }
}
