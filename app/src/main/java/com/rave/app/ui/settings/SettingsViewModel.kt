package com.rave.app.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rave.app.data.cloud.AuthRepository
import com.rave.app.data.cloud.CloudUser
import com.rave.app.data.cloud.ProfileRepository
import com.rave.app.data.cloud.UserProfile
import com.rave.app.data.export.DataExporter
import com.rave.app.data.preferences.SettingsRepository
import com.rave.app.data.repository.LocalDataWiper
import com.rave.app.domain.model.NotificationCategory
import com.rave.app.domain.model.ProfileVisibility
import com.rave.app.domain.model.UserSettings
import com.rave.app.domain.social.PhoneHasher
import com.rave.app.tracking.autodetect.AutoDetectManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val settings: UserSettings = UserSettings(),
    val user: CloudUser? = null,
    val profile: UserProfile? = null,
    val cloudAvailable: Boolean = false,
)

sealed interface SettingsEvent {
    data class Exported(val ok: Boolean) : SettingsEvent
    data object Deleted : SettingsEvent
    data object SignedOut : SettingsEvent
    data class Error(val message: String) : SettingsEvent
    data object ProfileSaved : SettingsEvent
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val auth: AuthRepository,
    private val profiles: ProfileRepository,
    private val exporter: DataExporter,
    private val wiper: LocalDataWiper,
    private val autoDetect: AutoDetectManager,
) : ViewModel() {

    val state: StateFlow<SettingsUiState> = combine(settings.settings, auth.currentUser, profiles.observeMyProfile()) { s, u, p ->
        SettingsUiState(s, u, p, auth.isAvailable)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    private val _events = Channel<SettingsEvent>(Channel.BUFFERED)
    val events: Flow<SettingsEvent> = _events.receiveAsFlow()

    fun update(transform: (UserSettings) -> UserSettings) {
        viewModelScope.launch { settings.update(transform) }
    }

    fun setVisibility(v: ProfileVisibility) {
        update { it.copy(visibility = v) }
        viewModelScope.launch { profiles.setVisibility(v) }
    }

    fun toggleNotification(category: NotificationCategory, on: Boolean) =
        update { it.copy(notifications = if (on) it.notifications + category else it.notifications - category) }

    fun setAutoDetect(on: Boolean) {
        viewModelScope.launch {
            settings.update { it.copy(autoDetectWalks = on) }
            autoDetect.sync()
        }
    }

    fun saveProfile(name: String, username: String) {
        viewModelScope.launch {
            if (auth.currentUser.value == null) {
                settings.update { it.copy(displayName = name.trim()) }
                _events.send(SettingsEvent.ProfileSaved)
                return@launch
            }
            profiles.updateProfile(name, username)
                .onSuccess { _events.send(SettingsEvent.ProfileSaved) }
                .onFailure { _events.send(SettingsEvent.Error(it.message ?: "")) }
        }
    }

    /** Stores only a salted hash of the user's own number so friends can find them. */
    fun setPhoneNumber(number: String) {
        viewModelScope.launch { profiles.setPhoneHash(number.takeIf { it.isNotBlank() }?.let { PhoneHasher.hash(it) }) }
    }

    fun export(uri: Uri) {
        viewModelScope.launch { _events.send(SettingsEvent.Exported(exporter.exportTo(uri))) }
    }

    fun signOut() {
        viewModelScope.launch {
            auth.signOut()
            _events.send(SettingsEvent.SignedOut)
        }
    }

    /** Deletes the cloud account (if any) and all local data. */
    fun deleteAccount() {
        viewModelScope.launch {
            if (auth.currentUser.value != null) {
                val result = auth.deleteAccount()
                if (result.isFailure) {
                    _events.send(SettingsEvent.Error(result.exceptionOrNull()?.message ?: ""))
                    return@launch
                }
            }
            wiper.wipe()
            _events.send(SettingsEvent.Deleted)
        }
    }
}
