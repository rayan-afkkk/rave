package com.rave.app.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.rave.app.domain.model.DistanceUnit
import com.rave.app.domain.model.NotificationCategory
import com.rave.app.domain.model.ProfileVisibility
import com.rave.app.domain.model.ThemeMode
import com.rave.app.domain.model.UserSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** User settings + small bits of app state, backed by Preferences DataStore. */
@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    private val prefs: Flow<Preferences> = dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }

    val settings: Flow<UserSettings> = prefs.map { it.toSettings() }.distinctUntilChanged()

    val themeMode: Flow<ThemeMode> = settings.map { it.themeMode }.distinctUntilChanged()

    suspend fun current(): UserSettings = settings.first()

    suspend fun update(transform: (UserSettings) -> UserSettings) {
        dataStore.edit { p -> p.write(transform(p.toSettings())) }
    }

    suspend fun setThemeMode(mode: ThemeMode) = update { it.copy(themeMode = mode) }

    // ---- Misc app state ----

    /** Invite code captured from a deep link / install referrer before the user signed in. */
    val pendingInvite: Flow<String?> = prefs.map { it[Keys.PENDING_INVITE] }

    suspend fun setPendingInvite(code: String?) = dataStore.edit {
        if (code == null) it.remove(Keys.PENDING_INVITE) else it[Keys.PENDING_INVITE] = code
    }

    suspend fun isInstallReferrerChecked(): Boolean = prefs.first()[Keys.REFERRER_CHECKED] == true
    suspend fun setInstallReferrerChecked() = dataStore.edit { it[Keys.REFERRER_CHECKED] = true }

    /** Opaque per-key string storage for small caches (e.g. last seen competition ranks). */
    suspend fun getString(key: String): String? = prefs.first()[stringPreferencesKey("kv_$key")]
    suspend fun putString(key: String, value: String?) = dataStore.edit {
        val k = stringPreferencesKey("kv_$key")
        if (value == null) it.remove(k) else it[k] = value
    }

    suspend fun clearAll() = dataStore.edit { it.clear() }

    private fun Preferences.toSettings(): UserSettings {
        val d = UserSettings()
        return UserSettings(
            themeMode = enumOr(this[Keys.THEME], d.themeMode),
            unit = enumOr(this[Keys.UNIT], d.unit),
            weightKg = this[Keys.WEIGHT] ?: d.weightKg,
            heightCm = this[Keys.HEIGHT] ?: d.heightCm,
            dailyGoalKm = this[Keys.DAILY_GOAL] ?: d.dailyGoalKm,
            dailyStepGoal = this[Keys.STEP_GOAL] ?: d.dailyStepGoal,
            autoDetectWalks = this[Keys.AUTO_DETECT] ?: d.autoDetectWalks,
            batterySaverGps = this[Keys.BATTERY_SAVER] ?: d.batterySaverGps,
            autoPause = this[Keys.AUTO_PAUSE] ?: d.autoPause,
            voiceFeedback = this[Keys.VOICE] ?: d.voiceFeedback,
            hideStartEnd = this[Keys.HIDE_ENDS] ?: d.hideStartEnd,
            visibility = enumOr(this[Keys.VISIBILITY], d.visibility),
            notifications = this[Keys.NOTIFICATIONS]
                ?.mapNotNull { runCatching { NotificationCategory.valueOf(it) }.getOrNull() }?.toSet()
                ?: d.notifications,
            displayName = this[Keys.NAME] ?: d.displayName,
            onboardingDone = this[Keys.ONBOARDING] ?: d.onboardingDone,
        )
    }

    private fun MutablePreferences.write(s: UserSettings) {
        this[Keys.THEME] = s.themeMode.name
        this[Keys.UNIT] = s.unit.name
        this[Keys.WEIGHT] = s.weightKg
        this[Keys.HEIGHT] = s.heightCm
        this[Keys.DAILY_GOAL] = s.dailyGoalKm
        this[Keys.STEP_GOAL] = s.dailyStepGoal
        this[Keys.AUTO_DETECT] = s.autoDetectWalks
        this[Keys.BATTERY_SAVER] = s.batterySaverGps
        this[Keys.AUTO_PAUSE] = s.autoPause
        this[Keys.VOICE] = s.voiceFeedback
        this[Keys.HIDE_ENDS] = s.hideStartEnd
        this[Keys.VISIBILITY] = s.visibility.name
        this[Keys.NOTIFICATIONS] = s.notifications.map { it.name }.toSet()
        this[Keys.NAME] = s.displayName
        this[Keys.ONBOARDING] = s.onboardingDone
    }

    private inline fun <reified T : Enum<T>> enumOr(value: String?, default: T): T =
        value?.let { v -> enumValues<T>().firstOrNull { it.name == v } } ?: default

    private object Keys {
        val THEME = stringPreferencesKey("theme_mode")
        val UNIT = stringPreferencesKey("unit")
        val WEIGHT = floatPreferencesKey("weight_kg")
        val HEIGHT = floatPreferencesKey("height_cm")
        val DAILY_GOAL = floatPreferencesKey("daily_goal_km")
        val STEP_GOAL = intPreferencesKey("daily_step_goal")
        val AUTO_DETECT = booleanPreferencesKey("auto_detect")
        val BATTERY_SAVER = booleanPreferencesKey("battery_saver")
        val AUTO_PAUSE = booleanPreferencesKey("auto_pause")
        val VOICE = booleanPreferencesKey("voice")
        val HIDE_ENDS = booleanPreferencesKey("hide_start_end")
        val VISIBILITY = stringPreferencesKey("visibility")
        val NOTIFICATIONS = stringSetPreferencesKey("notifications")
        val NAME = stringPreferencesKey("display_name")
        val ONBOARDING = booleanPreferencesKey("onboarding_done")
        val PENDING_INVITE = stringPreferencesKey("pending_invite")
        val REFERRER_CHECKED = booleanPreferencesKey("referrer_checked")
    }
}
