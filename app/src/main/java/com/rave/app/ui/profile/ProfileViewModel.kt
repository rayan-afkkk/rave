package com.rave.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rave.app.data.cloud.AuthRepository
import com.rave.app.data.cloud.CloudUser
import com.rave.app.data.cloud.ProfileRepository
import com.rave.app.data.cloud.UserProfile
import com.rave.app.data.preferences.SettingsRepository
import com.rave.app.data.repository.ProfileStats
import com.rave.app.data.repository.StatsRepository
import com.rave.app.domain.model.DistanceUnit
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class ProfileUiState(
    val loading: Boolean = true,
    val name: String = "",
    val user: CloudUser? = null,
    val profile: UserProfile? = null,
    val stats: ProfileStats = ProfileStats(),
    val unit: DistanceUnit = DistanceUnit.KM,
    val cloudAvailable: Boolean = false,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    auth: AuthRepository,
    profiles: ProfileRepository,
    stats: StatsRepository,
    settings: SettingsRepository,
) : ViewModel() {
    val state: StateFlow<ProfileUiState> = combine(
        auth.currentUser, profiles.observeMyProfile(), stats.observeProfileStats(), settings.settings,
    ) { user, profile, s, prefs ->
        ProfileUiState(
            loading = false,
            name = profile?.name ?: prefs.displayName,
            user = user,
            profile = profile,
            stats = s,
            unit = prefs.unit,
            cloudAvailable = auth.isAvailable,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfileUiState())
}
