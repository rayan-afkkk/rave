package com.rave.app.ui.routes

import androidx.compose.foundation.background
import com.rave.app.ui.theme.Dimens
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rave.app.R
import com.rave.app.data.local.entity.ActivityEntity
import com.rave.app.domain.model.DistanceUnit
import com.rave.app.domain.model.LatLngPoint
import com.rave.app.domain.util.Format
import com.rave.app.ui.activityTypeIcon
import com.rave.app.ui.components.EmptyState
import com.rave.app.ui.components.LoadingState
import com.rave.app.ui.components.PillChip
import com.rave.app.ui.components.RouteCanvas
import com.rave.app.ui.components.StatItem
import com.rave.app.ui.theme.RaveTheme
import com.rave.app.ui.unitLabel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun RoutesScreen(
    contentPadding: PaddingValues,
    onOpenActivity: (Long) -> Unit,
    onOpenWorkouts: () -> Unit,
    onBack: (() -> Unit)? = null,
    viewModel: RoutesViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    if (state.loading) {
        LoadingState()
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding() + 12.dp,
            bottom = contentPadding.calculateBottomPadding() + 16.dp,
            start = Dimens.ScreenPadding, end = Dimens.ScreenPadding,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onBack != null) {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.action_back)) }
                }
                Text(stringResource(R.string.nav_routes), style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.weight(1f).semantics { heading() })
                PillChip(false, stringResource(R.string.workouts_title), onOpenWorkouts, icon = Icons.Rounded.FitnessCenter)
            }
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val filters = listOf(
                    RouteFilter.ALL to R.string.filter_all, RouteFilter.WALKS to R.string.filter_walks,
                    RouteFilter.RUNS to R.string.filter_runs, RouteFilter.FAVORITES to R.string.filter_favorites,
                )
                items(filters) { (f, label) -> PillChip(state.filter == f, stringResource(label), { viewModel.setFilter(f) }) }
            }
        }
        item { WeekCard(state) }
        if (state.activities.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Rounded.Map,
                    title = stringResource(if (state.hasAny) R.string.routes_empty_filter_title else R.string.routes_empty_title),
                    message = stringResource(if (state.hasAny) R.string.routes_empty_filter_message else R.string.routes_empty_message),
                    tile = RaveTheme.colors.tileBlue,
                )
            }
        } else {
            items(state.activities, key = { it.id }) { a ->
                ActivityRow(a, state.unit, viewModel, onClick = { onOpenActivity(a.id) }, modifier = Modifier.animateItem())
            }
        }
    }
}

@Composable
private fun WeekCard(state: RoutesState) {
    val c = RaveTheme.colors
    Column(Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large).background(c.accentCard).padding(20.dp)) {
        Text(stringResource(R.string.routes_this_week), style = MaterialTheme.typography.labelLarge, color = c.onAccentCard.copy(alpha = 0.8f))
        Spacer(Modifier.size(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            WeekStat(Format.distance(state.week.distanceM, state.unit, 1), unitLabel(state.unit))
            WeekStat(state.week.count.toString(), stringResource(R.string.routes_activities))
            WeekStat(Format.duration(state.week.movingTimeS), stringResource(R.string.stat_moving_time))
        }
    }
}

@Composable
private fun WeekStat(value: String, label: String) {
    val c = RaveTheme.colors
    Column {
        Text(value, style = MaterialTheme.typography.headlineSmall, color = c.onAccentCard)
        Text(label, style = MaterialTheme.typography.bodySmall, color = c.onAccentCard.copy(alpha = 0.75f))
    }
}

@Composable
private fun ActivityRow(a: ActivityEntity, unit: DistanceUnit, viewModel: RoutesViewModel, onClick: () -> Unit, modifier: Modifier) {
    val segments by produceState(initialValue = emptyList<List<LatLngPoint>>(), a.id) { value = viewModel.thumbnail(a.id) }
    val tile = if (a.type == com.rave.app.domain.model.ActivityType.RUN) RaveTheme.colors.tilePeach else RaveTheme.colors.tileGreen
    Row(
        modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .heightIn(min = 96.dp)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(72.dp).clip(MaterialTheme.shapes.medium).background(tile.container), contentAlignment = Alignment.Center) {
            if (segments.isEmpty()) Icon(activityTypeIcon(a.type), null, tint = tile.content)
            else RouteCanvas(segments, tile.content, Modifier.size(72.dp), strokeWidth = 3.dp)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(a.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false))
                if (a.isPB) {
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.Rounded.EmojiEvents, stringResource(R.string.tag_pb), tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                }
                if (a.autoDetected) {
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Rounded.AutoAwesome, stringResource(R.string.tag_auto_detected), tint = RaveTheme.colors.tileBlue.content,
                        modifier = Modifier.size(16.dp))
                }
            }
            Text(
                Instant.ofEpochMilli(a.startTime).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.size(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StatItem(Format.distance(a.distanceM, unit), "", unit = unitLabel(unit))
                StatItem(Format.duration(a.durationS), "")
                StatItem(Format.pace(a.avgPaceSPerKm, unit), "")
            }
        }
        IconButton(onClick = { viewModel.toggleFavorite(a) }) {
            Icon(
                if (a.favorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                contentDescription = stringResource(if (a.favorite) R.string.cd_unfavorite else R.string.cd_favorite),
                tint = if (a.favorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
