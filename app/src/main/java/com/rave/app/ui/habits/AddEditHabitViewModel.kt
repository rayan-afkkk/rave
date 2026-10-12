package com.rave.app.ui.habits

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.rave.app.core.TimeProvider
import com.rave.app.data.cloud.AuthRepository
import com.rave.app.data.cloud.FriendsRepository
import com.rave.app.data.cloud.SharedStreakRepository
import com.rave.app.data.preferences.SettingsRepository
import com.rave.app.data.repository.HabitRepository
import com.rave.app.domain.model.ActivityGoalUnit
import com.rave.app.domain.model.ActivityType
import com.rave.app.domain.model.DistanceUnit
import com.rave.app.domain.model.Habit
import com.rave.app.domain.model.HabitColor
import com.rave.app.domain.model.HabitSchedule
import com.rave.app.domain.model.HabitType
import com.rave.app.domain.model.ScheduleType
import com.rave.app.ui.navigation.AddEditHabitRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalTime
import java.util.Locale
import javax.inject.Inject
import kotlin.math.roundToInt

data class FriendOption(val uid: String, val name: String)

data class HabitForm(
    val name: String = "",
    val icon: String = "check",
    val color: HabitColor = HabitColor.PEACH,
    val type: HabitType = HabitType.CHECKBOX,
    val counterTarget: String = "8",
    val counterUnit: String = "",
    val timedMinutes: String = "10",
    val activityType: ActivityType = ActivityType.WALK,
    val activityGoalUnit: ActivityGoalUnit = ActivityGoalUnit.METERS,
    /** Distance in the user's unit (km/mi) or minutes. */
    val activityGoal: String = "3",
    val scheduleType: ScheduleType = ScheduleType.EVERY_DAY,
    val days: Set<DayOfWeek> = DayOfWeek.entries.toSet(),
    val timesPerWeek: Int = 3,
    val reminders: List<LocalTime> = emptyList(),
    val remindersEnabled: Boolean = false,
    val shareWith: FriendOption? = null,
)

data class AddEditHabitState(
    val loading: Boolean = true,
    val isEdit: Boolean = false,
    val form: HabitForm = HabitForm(),
    val nameError: Boolean = false,
    val goalError: Boolean = false,
    val saving: Boolean = false,
    val saved: Boolean = false,
    val unit: DistanceUnit = DistanceUnit.KM,
    val signedIn: Boolean = false,
    val friends: List<FriendOption> = emptyList(),
    /** Name of the friend this habit is already shared with. */
    val sharedWith: String? = null,
)

@HiltViewModel
class AddEditHabitViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val habits: HabitRepository,
    private val settings: SettingsRepository,
    private val sharedStreaks: SharedStreakRepository,
    private val time: TimeProvider,
    auth: AuthRepository,
    friends: FriendsRepository,
) : ViewModel() {
    private val habitId = savedStateHandle.toRoute<AddEditHabitRoute>().habitId.takeIf { it > 0 }
    private var original: Habit? = null

    private val _state = MutableStateFlow(AddEditHabitState())
    val state: StateFlow<AddEditHabitState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val unit = settings.current().unit
            val habit = habitId?.let { habits.getHabit(it) }
            original = habit
            _state.update {
                it.copy(
                    loading = false,
                    isEdit = habit != null,
                    unit = unit,
                    form = habit?.toForm(unit) ?: HabitForm(),
                    sharedWith = habit?.sharedWithName,
                )
            }
        }
        viewModelScope.launch {
            auth.currentUser.collect { user -> _state.update { it.copy(signedIn = user != null) } }
        }
        viewModelScope.launch {
            friends.observeFriends().collect { list ->
                _state.update { s -> s.copy(friends = list.map { FriendOption(it.uid, it.name) }) }
            }
        }
    }

    fun edit(transform: (HabitForm) -> HabitForm) = _state.update { it.copy(form = transform(it.form), nameError = false, goalError = false) }

    fun save() {
        val s = _state.value
        val f = s.form
        if (f.name.isBlank()) {
            _state.update { it.copy(nameError = true) }
            return
        }
        val target = when (f.type) {
            HabitType.CHECKBOX -> 1
            HabitType.COUNTER -> f.counterTarget.toIntOrNull()
            HabitType.TIMED -> f.timedMinutes.toIntOrNull()
            HabitType.ACTIVITY -> f.activityGoal.replace(',', '.').toDoubleOrNull()?.let { v ->
                if (f.activityGoalUnit == ActivityGoalUnit.METERS) (v * s.unit.meters).roundToInt() else v.roundToInt()
            }
        }
        if (target == null || target <= 0 || (f.scheduleType == ScheduleType.SPECIFIC_DAYS && f.days.isEmpty())) {
            _state.update { it.copy(goalError = true) }
            return
        }
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            val base = original
            val habit = Habit(
                id = base?.id ?: 0,
                name = f.name.trim(),
                icon = f.icon,
                color = f.color,
                type = f.type,
                target = target,
                unit = if (f.type == HabitType.COUNTER) f.counterUnit.trim().ifEmpty { null } else null,
                schedule = HabitSchedule(f.scheduleType, HabitSchedule.maskOf(f.days), f.timesPerWeek),
                reminderTimes = f.reminders.distinct().sorted(),
                remindersEnabled = f.remindersEnabled && f.reminders.isNotEmpty(),
                linkedActivityType = if (f.type == HabitType.ACTIVITY) f.activityType else null,
                activityGoalUnit = f.activityGoalUnit,
                sharedStreakId = base?.sharedStreakId,
                sharedWithName = base?.sharedWithName,
                createdDate = base?.createdDate ?: time.today(),
            )
            val id = habits.save(habit)
            val friend = f.shareWith
            if (friend != null && habit.sharedStreakId == null) {
                sharedStreaks.invite(friend.uid, friend.name, habit.copy(id = id))
            }
            _state.update { it.copy(saving = false, saved = true) }
        }
    }

    fun archive() {
        val id = original?.id ?: return
        viewModelScope.launch {
            habits.archive(id)
            _state.update { it.copy(saved = true) }
        }
    }

    private fun Habit.toForm(unit: DistanceUnit) = HabitForm(
        name = name,
        icon = icon,
        color = color,
        type = type,
        counterTarget = if (type == HabitType.COUNTER) target.toString() else "8",
        counterUnit = this.unit.orEmpty(),
        timedMinutes = if (type == HabitType.TIMED) target.toString() else "10",
        activityType = linkedActivityType ?: ActivityType.WALK,
        activityGoalUnit = activityGoalUnit,
        activityGoal = if (type != HabitType.ACTIVITY) "3"
            else if (activityGoalUnit == ActivityGoalUnit.METERS) String.format(Locale.US, "%.1f", target / unit.meters)
            else target.toString(),
        scheduleType = schedule.type,
        days = DayOfWeek.entries.filter { schedule.includes(it) }.toSet(),
        timesPerWeek = schedule.timesPerWeek.coerceIn(1, 6),
        reminders = reminderTimes,
        remindersEnabled = remindersEnabled,
    )
}
