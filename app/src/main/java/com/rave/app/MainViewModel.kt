package com.rave.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rave.app.core.DeepLink
import com.rave.app.core.DeepLinkHandler
import com.rave.app.data.preferences.SettingsRepository
import com.rave.app.domain.model.ThemeMode
import com.rave.app.tracking.TrackingManager
import com.rave.app.tracking.TrackingState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Activity-scoped state: theme, start destination, live tracking banner and deep links. */
@HiltViewModel
class MainViewModel @Inject constructor(
    settings: SettingsRepository,
    private val tracking: TrackingManager,
    private val deepLinks: DeepLinkHandler,
    workouts: com.rave.app.data.repository.WorkoutRepository,
) : ViewModel() {
    /** Unfinished strength workout (drives the "Workout in progress" banner). */
    val activeWorkout: StateFlow<com.rave.app.data.local.entity.WorkoutEntity?> =
        workouts.observeActiveWorkout().stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val themeMode: StateFlow<ThemeMode> = settings.themeMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.SYSTEM)

    /** Null until settings are read (avoids flashing onboarding for returning users). */
    private val _needsOnboarding = MutableStateFlow<Boolean?>(null)
    val needsOnboarding: StateFlow<Boolean?> = _needsOnboarding.asStateFlow()

    val trackingState: StateFlow<TrackingState> = tracking.state

    val pendingDeepLink: StateFlow<DeepLink?> = deepLinks.pending

    /** Pending invite stored before sign-in / after install referrer. */
    val pendingInvite: StateFlow<String?> = settings.pendingInvite.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val onboardingDone: StateFlow<Boolean> = settings.settings.map { it.onboardingDone }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    init {
        viewModelScope.launch {
            _needsOnboarding.value = !settings.current().onboardingDone
            // Survive process death: re-attach to a tracking session saved in Room.
            tracking.restore()
        }
    }

    fun consumeDeepLink() = deepLinks.consume()
}
