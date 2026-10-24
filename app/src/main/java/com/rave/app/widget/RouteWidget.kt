package com.rave.app.widget

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
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
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.rave.app.MainActivity
import com.rave.app.R
import com.rave.app.domain.model.DistanceUnit
import com.rave.app.domain.tracking.RouteCleaner
import com.rave.app.domain.util.Format
import com.rave.app.ui.components.project
import com.rave.app.ui.theme.DarkColors
import com.rave.app.ui.theme.LightColors
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

private data class RouteData(
    val activityId: Long,
    val name: String,
    val date: String,
    val distance: String,
    val duration: String,
    val route: Bitmap?,
)

/**
 * Your latest walk or run on the home screen: the route line plus distance and time.
 * Small: route with the distance under it. Medium: route next to the stats.
 * Tapping opens that activity.
 */
class RouteWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(setOf(SMALL, MEDIUM))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val dark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val data = load(context, dark)
        provideContent {
            GlanceTheme(colors = WidgetColors) {
                when {
                    data == null -> Empty(context)
                    LocalSize.current.width < MEDIUM.width -> Small(context, data)
                    else -> Medium(context, data)
                }
            }
        }
    }

    private suspend fun load(context: Context, dark: Boolean): RouteData? {
        val ep = entryPoint(context)
        val activity = ep.activities().latestCompleted() ?: return null
        val unit = ep.settings().current().unit
        val segments = RouteCleaner.asTracked(ep.activities().points(activity.id))
        val color = (if (dark) DarkColors else LightColors).primary.toArgb()
        return RouteData(
            activityId = activity.id,
            name = activity.name.ifBlank { context.getString(R.string.widget_route_title) },
            date = Instant.ofEpochMilli(activity.startTime).atZone(ZoneId.systemDefault()).toLocalDate()
                .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)),
            distance = Format.distance(activity.distanceM, unit) + " " +
                context.getString(if (unit == DistanceUnit.KM) R.string.unit_km else R.string.unit_mi),
            duration = Format.duration(activity.durationS),
            route = routeBitmap(segments, color),
        )
    }

    private fun openActivity(context: Context, id: Long) =
        actionStartActivity(Intent(Intent.ACTION_VIEW, Uri.parse("rave://activity/$id"), context, MainActivity::class.java))

    @Composable
    private fun RouteImage(data: RouteData, modifier: GlanceModifier) {
        data.route?.let { Image(ImageProvider(it), contentDescription = null, modifier = modifier) }
    }

    @Composable
    private fun Small(context: Context, data: RouteData) {
        Column(
            GlanceModifier.fillMaxSize().background(GlanceTheme.colors.surface).cornerRadius(24.dp).padding(10.dp)
                .clickable(openActivity(context, data.activityId)),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            RouteImage(data, GlanceModifier.fillMaxWidth().defaultWeight())
            Text(data.distance, style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Bold))
        }
    }

    @Composable
    private fun Medium(context: Context, data: RouteData) {
        Row(
            GlanceModifier.fillMaxSize().background(GlanceTheme.colors.surface).cornerRadius(24.dp).padding(12.dp)
                .clickable(openActivity(context, data.activityId)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(GlanceModifier.width(110.dp).fillMaxHeight(), contentAlignment = Alignment.Center) {
                RouteImage(data, GlanceModifier.fillMaxSize())
            }
            Spacer(GlanceModifier.width(12.dp))
            Column(GlanceModifier.defaultWeight()) {
                Text(data.name, maxLines = 1, style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold))
                Text(data.date, style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 11.sp))
                Spacer(GlanceModifier.height(6.dp))
                Text(data.distance, style = TextStyle(color = GlanceTheme.colors.primary, fontSize = 22.sp, fontWeight = FontWeight.Bold))
                Text(data.duration, style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 12.sp))
            }
        }
    }

    @Composable
    private fun Empty(context: Context) {
        Column(
            GlanceModifier.fillMaxSize().background(GlanceTheme.colors.surface).cornerRadius(24.dp).padding(12.dp)
                .clickable(actionStartActivity(Intent(Intent.ACTION_VIEW, Uri.parse("rave://start/walk"), context, MainActivity::class.java))),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(context.getString(R.string.widget_route_empty), style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 13.sp))
            Spacer(GlanceModifier.height(6.dp))
            Text(context.getString(R.string.start_walk), style = TextStyle(color = GlanceTheme.colors.primary, fontSize = 14.sp, fontWeight = FontWeight.Bold))
        }
    }

    companion object {
        val SMALL = DpSize(110.dp, 110.dp)
        val MEDIUM = DpSize(250.dp, 110.dp)

        /** Glance can't draw paths, so the route is rendered to a bitmap (aspect kept, centred). */
        fun routeBitmap(segments: List<List<com.rave.app.domain.model.LatLngPoint>>, color: Int, sizePx: Int = 360): Bitmap? {
            val route = project(segments) ?: return null
            val bmp = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            val stroke = sizePx * 0.035f
            val pad = stroke * 2
            val avail = sizePx - pad * 2
            val scale = minOf(avail / route.width, avail / route.height)
            val ox = pad + (avail - route.width * scale) / 2
            val oy = pad + (avail - route.height * scale) / 2
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE; strokeWidth = stroke; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
                this.color = color
            }
            route.segments.filter { it.size >= 2 }.forEach { seg ->
                val path = android.graphics.Path()
                seg.forEachIndexed { i, o ->
                    val x = ox + (o.x - route.minX) * scale
                    val y = oy + (o.y - route.minY) * scale
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                canvas.drawPath(path, paint)
            }
            route.segments.firstOrNull()?.firstOrNull()?.let { o ->
                val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
                canvas.drawCircle(ox + (o.x - route.minX) * scale, oy + (o.y - route.minY) * scale, stroke * 1.3f, dot)
            }
            return bmp
        }
    }
}

class RouteWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = RouteWidget()
}
