package com.rave.app.ui.activity

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.rave.app.core.TimeProvider
import com.rave.app.data.local.entity.ActivityEntity
import com.rave.app.data.local.pbTagSet
import com.rave.app.data.preferences.SettingsRepository
import com.rave.app.data.repository.ActivityRepository
import com.rave.app.data.repository.HabitRepository
import com.rave.app.domain.model.DistanceUnit
import com.rave.app.domain.model.LatLngPoint
import com.rave.app.domain.model.LogSource
import com.rave.app.domain.model.PbTag
import com.rave.app.domain.model.Split
import com.rave.app.domain.tracking.TrackStats
import com.rave.app.ui.navigation.ActivitySummaryRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SummaryState(
    val loading: Boolean = true,
    val missing: Boolean = false,
    val activity: ActivityEntity? = null,
    val segments: List<List<LatLngPoint>> = emptyList(),
    val splits: List<Split> = emptyList(),
    val pbTags: Set<PbTag> = emptySet(),
    val tickedHabits: List<String> = emptyList(),
    val unit: DistanceUnit = DistanceUnit.KM,
    val name: String = "",
    val note: String = "",
    val favorite: Boolean = false,
    val closed: Boolean = false,
)

@HiltViewModel
class ActivitySummaryViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val activities: ActivityRepository,
    private val habits: HabitRepository,
    private val settings: SettingsRepository,
    private val time: TimeProvider,
) : ViewModel() {
    private val route = savedState.toRoute<ActivitySummaryRoute>()
    val isNew: Boolean = route.isNew

    private val _state = MutableStateFlow(SummaryState())
    val state: StateFlow<SummaryState> = _state.asStateFlow()

    init {
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        val activity = activities.get(route.activityId)
        if (activity == null) {
            _state.update { it.copy(loading = false, missing = true) }
            return
        }
        val unit = settings.current().unit
        val points = activities.points(activity.id)
        val date = time.dateOf(activity.startTime)
        val logs = habits.logsFor(date)
        val ticked = habits.activeHabits().filter { h ->
            val log = logs[h.id]
            h.linkedActivityType == activity.type && log?.completed == true && (log.source == LogSource.GPS || log.source == LogSource.AUTO)
        }.map { it.name }
        _state.value = SummaryState(
            loading = false,
            activity = activity,
            segments = points.groupBy { it.segment }.toSortedMap().values.map { seg -> seg.map { LatLngPoint(it.lat, it.lng) } },
            splits = TrackStats.splits(points, unit.meters),
            pbTags = activity.pbTagSet,
            tickedHabits = ticked,
            unit = unit,
            name = activity.name,
            note = activity.note,
            favorite = activity.favorite,
        )
    }

    fun setName(v: String) = _state.update { it.copy(name = v.take(60)) }
    fun setNote(v: String) = _state.update { it.copy(note = v.take(500)) }

    fun toggleFavorite() {
        val s = _state.value
        val id = s.activity?.id ?: return
        _state.update { it.copy(favorite = !s.favorite) }
        viewModelScope.launch { activities.setFavorite(id, !s.favorite) }
    }

    fun save() {
        val s = _state.value
        val id = s.activity?.id ?: return
        viewModelScope.launch {
            activities.updateDetails(id, s.name, s.note, s.favorite)
            _state.update { it.copy(closed = true) }
        }
    }

    /** Persists edits without closing (used before sharing / leaving detail). */
    fun saveSilently() {
        val s = _state.value
        val id = s.activity?.id ?: return
        viewModelScope.launch { activities.updateDetails(id, s.name, s.note, s.favorite) }
    }

    fun delete() {
        val id = _state.value.activity?.id ?: return
        viewModelScope.launch {
            activities.delete(id)
            _state.update { it.copy(closed = true) }
        }
    }
}
