package com.rave.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rave.app.data.preferences.UserPreferencesRepository
import com.rave.app.domain.model.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Activity-scoped state: currently just the chosen appearance. */
@HiltViewModel
class MainViewModel @Inject constructor(
    preferences: UserPreferencesRepository,
) : ViewModel() {
    val themeMode: StateFlow<ThemeMode> = preferences.themeMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.SYSTEM)
}
