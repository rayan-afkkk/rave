package com.rave.app.widget

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.material3.ColorProviders
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.rave.app.MainActivity
import com.rave.app.R
import com.rave.app.core.TimeProvider
import com.rave.app.data.local.entity.StreakEntity
import com.rave.app.data.repository.HabitRepository
import com.rave.app.data.repository.StreakRepository
import com.rave.app.domain.habit.StreakEngine
import com.rave.app.domain.model.Habit
import com.rave.app.domain.model.HabitLog
import com.rave.app.ui.theme.DarkColors
import com.rave.app.ui.theme.LightColors
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun habits(): HabitRepository
    fun streaks(): StreakRepository
    fun time(): TimeProvider
    fun steps(): com.rave.app.data.repository.StepRepository
    fun stepSensor(): com.rave.app.tracking.steps.StepSensor
    fun settings(): com.rave.app.data.preferences.SettingsRepository
    fun activities(): com.rave.app.data.repository.ActivityRepository
}

internal fun entryPoint(context: Context) = EntryPointAccessors.fromApplication(context.applicationContext, WidgetEntryPoint::class.java)

internal val WidgetColors = ColorProviders(light = LightColors, dark = DarkColors)

private data class WidgetData(val habits: List<Pair<Habit, HabitLog?>>, val done: Int, val total: Int, val streak: Int)

/**
 * Home-screen widget. Small: streak + progress ring. Medium: today's habits with tap-to-tick
 * and a "Start walk" button. Follows the system light/dark mode.
 */
class HabitWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(setOf(SMALL, MEDIUM))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = load(context)
        val dark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val ring = ringBitmap(if (data.total == 0) 0f else data.done / data.total.toFloat(), dark)
        provideContent {
            GlanceTheme(colors = WidgetColors) {
                if (LocalSize.current.width < MEDIUM.width) Small(data, ring) else Medium(context, data, ring)
            }
        }
    }

    private suspend fun load(context: Context): WidgetData {
        val ep = entryPoint(context)
        val today = ep.time().today()
        val habits = ep.habits().activeHabits().filter { it.isActiveOn(today) && it.schedule.isScheduledOn(today) }
        val logs = ep.habits().logsFor(today)
        val counted = habits.filter { logs[it.id]?.skipped != true }
        val overall = ep.streaks().observeStreaks().first()[StreakEntity.OVERALL]
        val streak = (overall?.current ?: 0) + if (logs.values.any { it.completed }) 1 else 0
        return WidgetData(habits.map { it to logs[it.id] }, counted.count { logs[it.id]?.completed == true }, counted.size, streak)
    }

    @Composable
    private fun Small(data: WidgetData, ring: Bitmap) {
        Box(
            GlanceModifier.fillMaxSize().background(GlanceTheme.colors.surface).cornerRadius(24.dp).padding(12.dp)
                .clickable(actionStartActivity<MainActivity>()),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(contentAlignment = Alignment.Center) {
                    Image(ImageProvider(ring), contentDescription = null, modifier = GlanceModifier.size(72.dp))
                    Text("${data.done}/${data.total}", style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Bold))
                }
                Spacer(GlanceModifier.height(6.dp))
                Text("🔥 ${data.streak}", style = TextStyle(color = GlanceTheme.colors.primary, fontSize = 15.sp, fontWeight = FontWeight.Bold))
            }
        }
    }

    @Composable
    private fun Medium(context: Context, data: WidgetData, ring: Bitmap) {
        Column(GlanceModifier.fillMaxSize().background(GlanceTheme.colors.surface).cornerRadius(24.dp).padding(12.dp)) {
            Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(contentAlignment = Alignment.Center) {
                    Image(ImageProvider(ring), contentDescription = null, modifier = GlanceModifier.size(44.dp))
                    Text("${data.done}/${data.total}", style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 11.sp, fontWeight = FontWeight.Bold))
                }
                Spacer(GlanceModifier.width(8.dp))
                Column(GlanceModifier.defaultWeight()) {
                    Text(context.getString(R.string.widget_today), style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Bold))
                    Text("🔥 ${data.streak}", style = TextStyle(color = GlanceTheme.colors.primary, fontSize = 13.sp))
                }
                Box(
                    GlanceModifier.background(GlanceTheme.colors.primary).cornerRadius(20.dp).padding(horizontal = 12.dp, vertical = 8.dp)
                        .clickable(actionStartActivity(Intent(Intent.ACTION_VIEW, Uri.parse("rave://start/walk"), context, MainActivity::class.java))),
                ) {
                    Text(context.getString(R.string.start_walk), style = TextStyle(color = GlanceTheme.colors.onPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold))
                }
            }
            Spacer(GlanceModifier.height(8.dp))
            if (data.habits.isEmpty()) {
                Text(context.getString(R.string.widget_empty), style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 13.sp))
            } else {
                LazyColumn {
                    items(data.habits, itemId = { it.first.id }) { (habit, log) ->
                        val done = log?.completed == true
                        Row(
                            GlanceModifier.fillMaxWidth().padding(vertical = 4.dp)
                                .clickable(actionRunCallback<ToggleHabitAction>(actionParametersOf(ToggleHabitAction.HABIT_ID to habit.id))),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(if (done) "✓" else "○", style = TextStyle(color = if (done) GlanceTheme.colors.primary else GlanceTheme.colors.onSurfaceVariant,
                                fontSize = 16.sp, fontWeight = FontWeight.Bold))
                            Spacer(GlanceModifier.width(8.dp))
                            Text(habit.name, maxLines = 1, style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 14.sp))
                        }
                    }
                }
            }
        }
    }

    companion object {
        val SMALL = DpSize(110.dp, 110.dp)
        val MEDIUM = DpSize(250.dp, 110.dp)

        /** Glance has no arc drawing; render the progress ring to a bitmap. */
        fun ringBitmap(progress: Float, dark: Boolean, sizePx: Int = 200): Bitmap {
            val scheme = if (dark) DarkColors else LightColors
            val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            val stroke = sizePx * 0.11f
            val rect = RectF(stroke / 2, stroke / 2, sizePx - stroke / 2, sizePx - stroke / 2)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = stroke; strokeCap = Paint.Cap.ROUND }
            paint.color = scheme.surfaceVariant.toArgb()
            canvas.drawArc(rect, 0f, 360f, false, paint)
            if (progress > 0f) {
                paint.color = scheme.primary.toArgb()
                canvas.drawArc(rect, -90f, 360f * progress.coerceIn(0f, 1f), false, paint)
            }
            return bmp
        }
    }
}

/** Tap-to-tick from the widget. */
class ToggleHabitAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val id = parameters[HABIT_ID] ?: return
        val ep = entryPoint(context)
        val habit = ep.habits().getHabit(id) ?: return
        ep.habits().toggleComplete(habit, ep.time().today())
        HabitWidget().update(context, glanceId)
    }

    companion object {
        val HABIT_ID = ActionParameters.Key<Long>("habit_id")
    }
}

class HabitWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = HabitWidget()
}
