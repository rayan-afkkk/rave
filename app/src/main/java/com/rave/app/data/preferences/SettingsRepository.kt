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
import com.rave.app.domain.model.AppStyle
import com.rave.app.domain.model.NotificationCategory
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

    val appStyle: Flow<AppStyle> = settings.map { it.appStyle }.distinctUntilChanged()

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

    fun observeString(key: String): Flow<String?> = prefs.map { it[stringPreferencesKey("kv_$key")] }.distinctUntilChanged()
    suspend fun putString(key: String, value: String?) = dataStore.edit {
        val k = stringPreferencesKey("kv_$key")
        if (value == null) it.remove(k) else it[k] = value
    }

    suspend fun clearAll() = dataStore.edit { it.clear() }

    private fun Preferences.toSettings(): UserSettings {
        val d = UserSettings()
        return UserSettings(
            themeMode = enumOr(this[Keys.THEME], d.themeMode),
            appStyle = enumOr(this[Keys.STYLE], d.appStyle),
            unit = enumOr(this[Keys.UNIT], d.unit),
            weightKg = this[Keys.WEIGHT] ?: d.weightKg,
            heightCm = this[Keys.HEIGHT] ?: d.heightCm,
            dailyGoalKm = this[Keys.DAILY_GOAL] ?: d.dailyGoalKm,
            dailyStepGoal = this[Keys.STEP_GOAL] ?: d.dailyStepGoal,
            birthYear = this[Keys.BIRTH_YEAR] ?: d.birthYear,
            sex = enumOr(this[Keys.SEX], d.sex),
            food = com.rave.app.domain.food.FoodPreferences(
                goal = enumOr(this[Keys.FOOD_GOAL], d.food.goal),
                halalOnly = this[Keys.HALAL] ?: d.food.halalOnly,
                vegetarian = this[Keys.VEG] ?: d.food.vegetarian,
                allergies = this[Keys.ALLERGIES]
                    ?.mapNotNull { runCatching { com.rave.app.domain.food.Allergen.valueOf(it) }.getOrNull() }?.toSet()
                    ?: d.food.allergies,
            ),
            budgetMeals = this[Keys.BUDGET] ?: d.budgetMeals,
            autoDetectWalks = this[Keys.AUTO_DETECT] ?: d.autoDetectWalks,
            batterySaverGps = this[Keys.BATTERY_SAVER] ?: d.batterySaverGps,
            autoPause = this[Keys.AUTO_PAUSE] ?: d.autoPause,
            voiceFeedback = this[Keys.VOICE] ?: d.voiceFeedback,
            hideStartEnd = this[Keys.HIDE_ENDS] ?: d.hideStartEnd,
            visibility = enumOr(this[Keys.VISIBILITY], d.visibility),
            notifications = this[Keys.NOTIFICATIONS]?.let { stored ->
                // Categories added after the user last saved settings start switched on.
                val seen = this[Keys.NOTIFICATIONS_SEEN] ?: LEGACY_CATEGORIES
                (stored.mapNotNull { runCatching { NotificationCategory.valueOf(it) }.getOrNull() } +
                    NotificationCategory.entries.filter { it.name !in seen }).toSet()
            } ?: d.notifications,
            waterGoalGlasses = this[Keys.WATER_GOAL] ?: d.waterGoalGlasses,
            displayName = this[Keys.NAME] ?: d.displayName,
            onboardingDone = this[Keys.ONBOARDING] ?: d.onboardingDone,
        )
    }

    private fun MutablePreferences.write(s: UserSettings) {
        this[Keys.THEME] = s.themeMode.name
        this[Keys.STYLE] = s.appStyle.name
        this[Keys.UNIT] = s.unit.name
        this[Keys.WEIGHT] = s.weightKg
        this[Keys.HEIGHT] = s.heightCm
        this[Keys.DAILY_GOAL] = s.dailyGoalKm
        this[Keys.STEP_GOAL] = s.dailyStepGoal
        this[Keys.BIRTH_YEAR] = s.birthYear
        this[Keys.SEX] = s.sex.name
        this[Keys.FOOD_GOAL] = s.food.goal.name
        this[Keys.HALAL] = s.food.halalOnly
        this[Keys.VEG] = s.food.vegetarian
        this[Keys.ALLERGIES] = s.food.allergies.map { it.name }.toSet()
        this[Keys.BUDGET] = s.budgetMeals
        this[Keys.AUTO_DETECT] = s.autoDetectWalks
        this[Keys.BATTERY_SAVER] = s.batterySaverGps
        this[Keys.AUTO_PAUSE] = s.autoPause
        this[Keys.VOICE] = s.voiceFeedback
        this[Keys.HIDE_ENDS] = s.hideStartEnd
        this[Keys.VISIBILITY] = s.visibility.name
        this[Keys.NOTIFICATIONS] = s.notifications.map { it.name }.toSet()
        this[Keys.NOTIFICATIONS_SEEN] = NotificationCategory.entries.map { it.name }.toSet()
        this[Keys.WATER_GOAL] = s.waterGoalGlasses
        this[Keys.NAME] = s.displayName
        this[Keys.ONBOARDING] = s.onboardingDone
    }

    private inline fun <reified T : Enum<T>> enumOr(value: String?, default: T): T =
        value?.let { v -> enumValues<T>().firstOrNull { it.name == v } } ?: default

    private object Keys {
        val THEME = stringPreferencesKey("theme_mode")
        val STYLE = stringPreferencesKey("app_style")
        val UNIT = stringPreferencesKey("unit")
        val WEIGHT = floatPreferencesKey("weight_kg")
        val HEIGHT = floatPreferencesKey("height_cm")
        val DAILY_GOAL = floatPreferencesKey("daily_goal_km")
        val STEP_GOAL = intPreferencesKey("daily_step_goal")
        val BIRTH_YEAR = intPreferencesKey("birth_year")
        val SEX = stringPreferencesKey("sex")
        val FOOD_GOAL = stringPreferencesKey("food_goal")
        val HALAL = booleanPreferencesKey("food_halal")
        val VEG = booleanPreferencesKey("food_veg")
        val ALLERGIES = stringSetPreferencesKey("food_allergies")
        val BUDGET = booleanPreferencesKey("food_budget")
        val AUTO_DETECT = booleanPreferencesKey("auto_detect")
        val BATTERY_SAVER = booleanPreferencesKey("battery_saver")
        val AUTO_PAUSE = booleanPreferencesKey("auto_pause")
        val VOICE = booleanPreferencesKey("voice")
        val HIDE_ENDS = booleanPreferencesKey("hide_start_end")
        val VISIBILITY = stringPreferencesKey("visibility")
        val NOTIFICATIONS = stringSetPreferencesKey("notifications")
        val NOTIFICATIONS_SEEN = stringSetPreferencesKey("notifications_seen")
        val WATER_GOAL = intPreferencesKey("water_goal")
        val NAME = stringPreferencesKey("display_name")
        val ONBOARDING = booleanPreferencesKey("onboarding_done")
        val PENDING_INVITE = stringPreferencesKey("pending_invite")
        val REFERRER_CHECKED = booleanPreferencesKey("referrer_checked")
    }
}

/** Notification categories that existed before "seen" tracking was added. */
private val LEGACY_CATEGORIES = setOf("REMINDERS", "FRIENDS", "COMPETITIONS", "AUTO_DETECT")
