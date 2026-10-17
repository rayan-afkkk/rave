package com.rave.app.widget

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.rave.app.MainActivity
import com.rave.app.R
import com.rave.app.domain.fitness.FitnessMath
import com.rave.app.domain.model.DistanceUnit
import com.rave.app.domain.util.Format
import java.text.NumberFormat

private data class StepsData(
    val steps: Int,
    val goal: Int,
    val distance: String,
    val kcal: Int,
    val canCount: Boolean,
)

/**
 * Today's steps on the home screen. Small: ring + steps. Medium: ring, steps vs goal,
 * distance and calories. Reads the step counter on each update so it is fresh.
 */
class StepsWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(setOf(SMALL, MEDIUM))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val data = load(context)
        val dark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val ring = HabitWidget.ringBitmap(if (data.goal <= 0) 0f else data.steps / data.goal.toFloat(), dark)
        provideContent {
            GlanceTheme(colors = WidgetColors) {
                if (LocalSize.current.width < MEDIUM.width) Small(context, data, ring) else Medium(context, data, ring)
            }
        }
    }

    private suspend fun load(context: Context): StepsData {
        val ep = entryPoint(context)
        ep.stepSensor().readOnce(timeoutMs = 2_000)?.let { ep.steps().record(it) }
        val prefs = ep.settings().current()
        val steps = ep.steps().todaySteps()
        val unit = prefs.unit
        val distance = Format.distance(FitnessMath.distanceM(steps, prefs.heightCm), unit) + " " +
            context.getString(if (unit == DistanceUnit.KM) R.string.unit_km else R.string.unit_mi)
        return StepsData(
            steps = steps,
            goal = prefs.dailyStepGoal,
            distance = distance,
            kcal = FitnessMath.stepCalories(steps, prefs.weightKg, prefs.heightCm),
            canCount = ep.stepSensor().canCount,
        )
    }

    @Composable
    private fun Small(context: Context, data: StepsData, ring: Bitmap) {
        Box(
            GlanceModifier.fillMaxSize().background(GlanceTheme.colors.surface).cornerRadius(24.dp).padding(10.dp)
                .clickable(actionStartActivity<MainActivity>()),
            contentAlignment = Alignment.Center,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Image(ImageProvider(ring), contentDescription = null, modifier = GlanceModifier.size(92.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(format(data.steps), style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 17.sp, fontWeight = FontWeight.Bold))
                    Text(context.getString(R.string.fitness_steps), style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 11.sp))
                }
            }
        }
    }

    @Composable
    private fun Medium(context: Context, data: StepsData, ring: Bitmap) {
        Row(
            GlanceModifier.fillMaxSize().background(GlanceTheme.colors.surface).cornerRadius(24.dp).padding(12.dp)
                .clickable(actionStartActivity<MainActivity>()),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Image(ImageProvider(ring), contentDescription = null, modifier = GlanceModifier.size(78.dp))
                Text("${(100f * data.steps / data.goal.coerceAtLeast(1)).toInt().coerceAtMost(999)}%",
                    style = TextStyle(color = GlanceTheme.colors.primary, fontSize = 14.sp, fontWeight = FontWeight.Bold))
            }
            Spacer(GlanceModifier.width(12.dp))
            Column(GlanceModifier.defaultWeight()) {
                Text(format(data.steps), style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 24.sp, fontWeight = FontWeight.Bold))
                Text(
                    if (data.canCount) context.getString(R.string.widget_steps_of_goal, format(data.goal))
                    else context.getString(R.string.widget_steps_off),
                    style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp),
                )
                Spacer(GlanceModifier.height(6.dp))
                Text("${data.distance}  ·  ${context.getString(R.string.fitness_kcal, format(data.kcal))}",
                    style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Medium))
            }
        }
    }

    private fun format(v: Int) = NumberFormat.getIntegerInstance().format(v)

    companion object {
        val SMALL = DpSize(110.dp, 110.dp)
        val MEDIUM = DpSize(250.dp, 110.dp)
    }
}

class StepsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = StepsWidget()
}
