package com.rave.app.ui.activity

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rave.app.R
import com.rave.app.domain.model.ActivityType
import com.rave.app.domain.model.DistanceUnit
import com.rave.app.domain.model.Split
import com.rave.app.domain.util.Format
import com.rave.app.ui.components.ConfirmDialog
import com.rave.app.ui.components.EmptyState
import com.rave.app.ui.components.LoadingState
import com.rave.app.ui.components.PillButton
import com.rave.app.ui.components.RaveScreen
import com.rave.app.ui.components.SectionCard
import com.rave.app.ui.components.StatItem
import com.rave.app.ui.components.TonalPillButton
import com.rave.app.ui.paceUnitLabel
import com.rave.app.ui.pbTagRes
import com.rave.app.ui.theme.PillShape
import com.rave.app.ui.theme.RaveTheme
import com.rave.app.ui.theme.TileColor
import com.rave.app.ui.unitLabel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ActivitySummaryScreen(
    onClose: () -> Unit,
    onShare: (Long) -> Unit,
    onRepeat: (ActivityType, Long) -> Unit,
    viewModel: ActivitySummaryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(state.closed) { if (state.closed) onClose() }

    RaveScreen(
        title = stringResource(if (viewModel.isNew) R.string.summary_title_new else R.string.summary_title),
        onBack = { if (viewModel.isNew) viewModel.save() else { viewModel.saveSilently(); onClose() } },
        actions = {
            if (state.activity != null) {
                IconButton(onClick = viewModel::toggleFavorite) {
                    Icon(
                        if (state.favorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                        contentDescription = stringResource(if (state.favorite) R.string.cd_unfavorite else R.string.cd_favorite),
                        tint = if (state.favorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
    ) {
        when {
            state.loading -> LoadingState()
            state.missing -> EmptyState(Icons.Rounded.Flag, stringResource(R.string.summary_missing_title), stringResource(R.string.summary_missing_body))
            else -> {
                val a = state.activity!!
                val unit = state.unit
                Column(
                    Modifier.verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    RouteMapCard(state.segments)

                    if (state.tickedHabits.isNotEmpty()) {
                        Banner(Icons.Rounded.CheckCircle, RaveTheme.colors.tileGreen,
                            stringResource(R.string.summary_ticked, state.tickedHabits.joinToString(", ")))
                    }

                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        state.pbTags.forEach { Tag(Icons.Rounded.EmojiEvents, stringResource(pbTagRes(it)), RaveTheme.colors.tilePeach) }
                        if (a.goalReached) Tag(Icons.Rounded.Flag, stringResource(R.string.tag_goal_reached), RaveTheme.colors.tileGreen)
                        if (a.autoDetected) Tag(Icons.Rounded.AutoAwesome, stringResource(R.string.tag_auto_detected), RaveTheme.colors.tileBlue)
                    }

                    OutlinedTextField(
                        value = state.name, onValueChange = viewModel::setName,
                        label = { Text(stringResource(R.string.summary_name)) }, singleLine = true,
                        shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth(),
                    )

                    SectionCard {
                        Text(
                            Instant.ofEpochMilli(a.startTime).atZone(ZoneId.systemDefault())
                                .format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)),
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(Modifier.fillMaxWidth()) {
                            StatItem(Format.distance(a.distanceM, unit), stringResource(R.string.stat_distance), Modifier.weight(1f), unit = unitLabel(unit))
                            StatItem(Format.duration(a.durationS), stringResource(R.string.stat_time), Modifier.weight(1f))
                            StatItem(Format.pace(a.avgPaceSPerKm, unit), stringResource(R.string.stat_avg_pace), Modifier.weight(1f), unit = paceUnitLabel(unit))
                        }
                        Spacer(Modifier.height(14.dp))
                        Row(Modifier.fillMaxWidth()) {
                            StatItem(a.calories.toString(), stringResource(R.string.stat_calories), Modifier.weight(1f), unit = stringResource(R.string.unit_kcal))
                            StatItem(
                                if (a.elevationGainM > 0) "%.0f".format(a.elevationGainM) else "–",
                                stringResource(R.string.stat_elevation), Modifier.weight(1f),
                                unit = if (a.elevationGainM > 0) stringResource(R.string.unit_m) else null,
                            )
                            StatItem(Format.duration(a.movingTimeS), stringResource(R.string.stat_moving_time), Modifier.weight(1f))
                        }
                    }

                    if (state.splits.isNotEmpty()) SplitsCard(state.splits, unit)

                    OutlinedTextField(
                        value = state.note, onValueChange = viewModel::setNote,
                        label = { Text(stringResource(R.string.summary_note)) }, minLines = 2,
                        shape = MaterialTheme.shapes.medium, modifier = Modifier.fillMaxWidth(),
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TonalPillButton(stringResource(R.string.action_share), onClick = { viewModel.saveSilently(); onShare(a.id) },
                            icon = Icons.Rounded.Share, modifier = Modifier.weight(1f))
                        if (viewModel.isNew) {
                            PillButton(stringResource(R.string.action_save), onClick = viewModel::save, modifier = Modifier.weight(1f))
                        } else if (state.favorite) {
                            PillButton(stringResource(R.string.summary_repeat), onClick = { onRepeat(a.type, a.id) },
                                icon = Icons.Rounded.Repeat, modifier = Modifier.weight(1f))
                        }
                    }
                    TextButton(onClick = { confirmDelete = true }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                        Icon(Icons.Rounded.Delete, null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(R.string.summary_delete_title),
            message = stringResource(R.string.summary_delete_body),
            confirmLabel = stringResource(R.string.action_delete),
            destructive = true,
            onConfirm = { confirmDelete = false; viewModel.delete() },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun SplitsCard(splits: List<Split>, unit: DistanceUnit) {
    SectionCard {
        Text(stringResource(R.string.summary_splits), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(10.dp))
        val paces = splits.map { it.paceSecondsPer(unit.meters) }
        val fastest = paces.filter { it > 0 }.minOrNull() ?: 1.0
        val slowest = paces.maxOrNull() ?: 1.0
        splits.forEachIndexed { i, split ->
            val pace = paces[i]
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (split.distanceM < unit.meters - 1) Format.distance(split.distanceM, unit) else split.index.toString(),
                    style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(44.dp),
                )
                // Faster splits get longer bars.
                val fraction = if (slowest <= fastest) 1f else (0.35f + 0.65f * ((slowest - pace) / (slowest - fastest)).toFloat())
                Box(Modifier.weight(1f).height(10.dp)) {
                    Box(
                        Modifier.fillMaxWidth(fraction.coerceIn(0.1f, 1f)).height(10.dp).clip(PillShape)
                            .background(if (pace == fastest) MaterialTheme.colorScheme.primary else RaveTheme.colors.chart[1].copy(alpha = 0.55f)),
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text("${Format.pace(pace / unit.meters * 1000.0, unit)} ${paceUnitLabel(unit)}", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun Tag(icon: ImageVector, label: String, tile: TileColor) {
    Surface(shape = PillShape, color = tile.container) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = tile.content, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, color = tile.content)
        }
    }
}

@Composable
private fun Banner(icon: ImageVector, tile: TileColor, text: String) {
    Row(
        Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).background(tile.container).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = tile.content)
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.titleSmall, color = tile.content)
    }
}

