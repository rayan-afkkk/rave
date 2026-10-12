package com.rave.app.ui.tracking

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.rave.app.data.preferences.SettingsRepository
import com.rave.app.domain.model.ActivityType
import com.rave.app.domain.model.UserSettings
import com.rave.app.tracking.TrackingManager
import com.rave.app.tracking.TrackingState
import com.rave.app.tracking.TrackingStatus
import com.rave.app.ui.navigation.LiveTrackingRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface TrackingEvent {
    data class Finished(val activityId: Long) : TrackingEvent
    data object TooShort : TrackingEvent
    data object Discarded : TrackingEvent
}

@HiltViewModel
class LiveTrackingViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val manager: TrackingManager,
    settings: SettingsRepository,
) : ViewModel() {
    private val route = savedState.toRoute<LiveTrackingRoute>()
    val requestedType: ActivityType = runCatching { ActivityType.valueOf(route.activityType) }.getOrDefault(ActivityType.WALK)

    val state: StateFlow<TrackingState> = manager.state
    val settings: StateFlow<UserSettings> = settings.settings.stateIn(viewModelScope, SharingStarted.Eagerly, UserSettings())

    private val _events = Channel<TrackingEvent>(Channel.BUFFERED)
    val events: Flow<TrackingEvent> = _events.receiveAsFlow()

    init {
        // Finishing from the notification while this screen is open still lands on the summary.
        viewModelScope.launch {
            manager.finished.collect { _events.send(TrackingEvent.Finished(it.activityId)) }
        }
    }

    /** Called once location permission is granted. Re-attaches to a running session if any. */
    fun startIfNeeded() {
        viewModelScope.launch {
            if (!manager.restore()) manager.start(requestedType, route.guideActivityId)
        }
    }

    fun togglePause() {
        viewModelScope.launch {
            if (state.value.status == TrackingStatus.ACTIVE) manager.pause() else manager.resume()
        }
    }

    fun finish(force: Boolean = false) {
        viewModelScope.launch {
            if (!force && state.value.distanceM < MIN_SAVE_DISTANCE_M) {
                manager.pause()
                _events.send(TrackingEvent.TooShort)
                return@launch
            }
            // The Finished event comes through manager.finished.
            manager.finish()
        }
    }

    fun discard() {
        viewModelScope.launch {
            manager.discard()
            _events.send(TrackingEvent.Discarded)
        }
    }

    private companion object {
        const val MIN_SAVE_DISTANCE_M = 50.0
    }
}
