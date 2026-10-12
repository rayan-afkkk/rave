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

    fun refresh() {
        pending?.cancel()
        pending = scope.launch {
            delay(300)
            runCatching { HabitWidget().updateAll(context) }
        }
    }
}
