package com.rave.app.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.rave.app.core.ApplicationScope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/** Asks home-screen widgets to redraw. Debounced so bursts of changes cause one update. */
@Singleton
class WidgetRefresher @Inject constructor(
    @ApplicationContext private val context: Context,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private var pending: Job? = null
    private var pendingSteps: Job? = null

    /** Habits, steps and last-route widgets (habit ticks, finished activities, day changes). */
    fun refresh() {
        pending?.cancel()
        pending = scope.launch {
            delay(300)
            runCatching { HabitWidget().updateAll(context) }
            runCatching { StepsWidget().updateAll(context) }
            runCatching { RouteWidget().updateAll(context) }
        }
    }

    /** Steps change often while walking: batch them into at most one redraw every few seconds. */
    fun refreshSteps() {
        if (pendingSteps?.isActive == true) return
        pendingSteps = scope.launch {
            delay(3_000)
            runCatching { StepsWidget().updateAll(context) }
        }
    }
}
