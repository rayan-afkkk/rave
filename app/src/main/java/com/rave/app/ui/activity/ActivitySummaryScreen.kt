package com.rave.app.ui.activity

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rave.app.R
import com.rave.app.data.local.entity.ActivityEntity
import com.rave.app.domain.model.ActivityType
import com.rave.app.domain.model.DistanceUnit
import com.rave.app.domain.model.Split
import com.rave.app.domain.util.Format
import com.rave.app.ui.components.AppBackground
import com.rave.app.ui.components.BentoBig
import com.rave.app.ui.components.BentoChip
import com.rave.app.ui.components.BentoGap
import com.rave.app.ui.components.BentoKicker
import com.rave.app.ui.components.BentoPillShape
import com.rave.app.ui.components.BentoTile
import com.rave.app.ui.components.BentoTileShape
import com.rave.app.ui.components.ConfirmDialog
import com.rave.app.ui.components.EmptyState
import com.rave.app.ui.components.LoadingState
import com.rave.app.ui.pbTagRes
import com.rave.app.ui.theme.RaveTheme
import com.rave.app.ui.theme.TileColor
import com.rave.app.ui.tracking.BentoMiniStat
import com.rave.app.ui.tracking.BentoPillAction
import com.rave.app.ui.tracking.bentoPace
import com.rave.app.ui.unitLabel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/*
 * Bento Pop activity summary: close + "New PR" chip, big title, sand route map,
 * KM / TIME / PACE tiles, black splits card, then "Do again" / "Share".
 */

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ActivitySummaryScreen(
    onClose: () -> Unit,
    onShare: (Long) -> Unit,
    onRepeat: (ActivityType, Long) -> Unit,
    viewModel: ActivitySummaryViewModel = hiltViewModel(),
) {
    val pro = com.rave.app.ui.pro.rememberPro()
    val state by viewModel.state.collectAsStateWithLifecycle()
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(state.closed) { if (state.closed) onClose() }
    val close = { if (viewModel.isNew) viewModel.save() else { viewModel.saveSilently(); onClose() } }
    val c = RaveTheme.colors
    // New activities are saved on close (X or system back), as before.
    BackHandler(onBack = close)

    AppBackground {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            // Header: close X · favourite · New PR chip
            Row(
                Modifier.fillMaxWidth().padding(start = 6.dp, end = 18.dp, top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = close) {
                    Icon(Icons.Rounded.Close, stringResource(R.string.action_close), tint = MaterialTheme.colorScheme.onBackground)
                }
                Spacer(Modifier.weight(1f))
                val a = state.activity
                if (a != null) {
                    IconButton(onClick = viewModel::toggleFavorite) {
                        Icon(
                            if (state.favorite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                            contentDescription = stringResource(if (state.favorite) R.string.cd_unfavorite else R.string.cd_favorite),
                            tint = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                    if (state.pbTags.isNotEmpty() || a.isPB) {
                        BentoChip(stringResource(R.string.btrack_new_pr), container = c.highlight, content = c.onHighlight)
                    }
                }
            }

            when {
                state.loading -> LoadingState()
                state.missing -> EmptyState(Icons.Rounded.Flag, stringResource(R.string.summary_missing_title), stringResource(R.string.summary_missing_body))
                else -> {
                    val a = state.activity!!
                    val unit = state.unit
                    Column(
                        Modifier.verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 18.dp),
                    ) {
                        // Title + date · time
                        val start = Instant.ofEpochMilli(a.startTime).atZone(ZoneId.systemDefault())
                        BentoBig(
                            state.name.ifBlank { stringResource(defaultTitleRes(a)) },
                            MaterialTheme.colorScheme.onBackground,
                            Modifier.padding(top = 12.dp), size = 28.sp,
                        )
                        Text(
                            stringResource(
                                R.string.btrack_subtitle,
                                start.format(DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault())),
                                start.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT)),
                            ),
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
                        )

                        RouteMapCard(state.segments, Modifier.padding(top = 12.dp))

                        // KM · TIME · PACE
                        Row(Modifier.fillMaxWidth().padding(top = BentoGap), horizontalArrangement = Arrangement.spacedBy(BentoGap)) {
                            SummaryStat(Format.distance(a.distanceM, unit), unitLabel(unit), c.tileInk, Modifier.weight(1f))
                            SummaryStat(Format.duration(a.durationS), stringResource(R.string.stat_time), c.tilePink, Modifier.weight(1f))
                            SummaryStat(bentoPace(a.avgPaceSPerKm, unit), stringResource(R.string.stat_pace), c.tileBlue, Modifier.weight(1f))
                        }

                        if (state.splits.isNotEmpty()) {
                            com.rave.app.ui.pro.ProLock(
                                pro, com.rave.app.data.billing.ProReason.STATS, stringResource(R.string.summary_splits_locked),
                                Modifier.padding(top = BentoGap),
                            ) {
                                SplitsCard(state.splits, unit)
                            }
                        }

                        // Do again · Share
                        Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(BentoGap)) {
                            BentoPillAction(
                                stringResource(R.string.btrack_do_again),
                                container = c.tileSand.container, content = c.tileSand.content,
                                onClick = { viewModel.saveSilently(); onRepeat(a.type, a.id) },
                                leading = Icons.Rounded.Replay, modifier = Modifier.weight(1f),
                            )
                            BentoPillAction(
                                stringResource(R.string.action_share),
                                container = c.tileOrange.container, content = c.tileOrange.content,
                                onClick = { viewModel.saveSilently(); onShare(a.id) },
                                leading = Icons.Rounded.IosShare, modifier = Modifier.weight(1f),
                            )
                        }

                        // ---- More below the fold: badges, extra stats, name & note, delete ----
                        val hasTags = state.pbTags.isNotEmpty() || a.goalReached || a.autoDetected
                        if (hasTags) {
                            FlowRow(
                                Modifier.padding(top = 18.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                state.pbTags.forEach {
                                    BentoChip(stringResource(pbTagRes(it)), c.tilePeach.container, c.tilePeach.content, icon = Icons.Rounded.EmojiEvents)
                                }
                                if (a.goalReached) {
                                    BentoChip(stringResource(R.string.tag_goal_reached), c.tileGreen.container, c.tileGreen.content, icon = Icons.Rounded.Flag)
                                }
                                if (a.autoDetected) {
                                    BentoChip(stringResource(R.string.tag_auto_detected), c.tileBlue.container, c.tileBlue.content, icon = Icons.Rounded.AutoAwesome)
                                }
                            }
                        }

                        if (state.tickedHabits.isNotEmpty()) {
                            Banner(
                                Icons.Rounded.CheckCircle, c.tileGreen,
                                stringResource(R.string.summary_ticked, state.tickedHabits.joinToString(", ")),
                                Modifier.padding(top = BentoGap),
                            )
                        }

                        Row(Modifier.fillMaxWidth().padding(top = BentoGap), horizontalArrangement = Arrangement.spacedBy(BentoGap)) {
                            SummaryStat(a.calories.toString(), stringResource(R.string.unit_kcal), c.tilePeach, Modifier.weight(1f))
                            SummaryStat(
                                if (a.elevationGainM > 0) "%.0f".format(a.elevationGainM) + stringResource(R.string.unit_m) else "–",
                                stringResource(R.string.stat_elevation), c.tileYellow, Modifier.weight(1f),
                            )
                            SummaryStat(Format.duration(a.movingTimeS), stringResource(R.string.stat_moving_time), c.tileSand, Modifier.weight(1f))
                        }

                        BentoTile(c.tileSand.container, Modifier.fillMaxWidth().padding(top = BentoGap)) {
                            OutlinedTextField(
                                value = state.name, onValueChange = viewModel::setName,
                                label = { Text(stringResource(R.string.summary_name)) }, singleLine = true,
                                shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth(),
                                colors = sandFieldColors(),
                            )
                            Spacer(Modifier.height(10.dp))
                            OutlinedTextField(
                                value = state.note, onValueChange = viewModel::setNote,
                                label = { Text(stringResource(R.string.summary_note)) }, minLines = 2,
                                shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth(),
                                colors = sandFieldColors(),
                            )
                        }

                        TextButton(onClick = { confirmDelete = true }, modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp)) {
                            Icon(Icons.Rounded.Delete, null, tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                        }
                        Spacer(Modifier.height(16.dp))
                    }
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

/** "Evening walk"-style fallback title from the start hour. */
private fun defaultTitleRes(a: ActivityEntity): Int {
    val hour = Instant.ofEpochMilli(a.startTime).atZone(ZoneId.systemDefault()).hour
    val run = a.type == ActivityType.RUN
    return when (hour) {
        in 5..11 -> if (run) R.string.btrack_title_morning_run else R.string.btrack_title_morning_walk
        in 12..16 -> if (run) R.string.btrack_title_afternoon_run else R.string.btrack_title_afternoon_walk
        in 17..21 -> if (run) R.string.btrack_title_evening_run else R.string.btrack_title_evening_walk
        else -> if (run) R.string.btrack_title_night_run else R.string.btrack_title_night_walk
    }
}

@Composable
private fun SummaryStat(value: String, label: String, tile: TileColor, modifier: Modifier = Modifier) {
    BentoMiniStat(
        value, label, tile, modifier,
        radius = 28.dp, padding = 12.dp, valueSize = 22.sp, labelSize = 10.sp, labelSpacing = 1.2.sp, labelAlpha = 0.6f,
    )
}

@Composable
private fun sandFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = RaveTheme.colors.tileSand.content,
    unfocusedBorderColor = RaveTheme.colors.tileSand.content.copy(alpha = 0.3f),
    focusedLabelColor = RaveTheme.colors.tileSand.content,
    unfocusedLabelColor = RaveTheme.colors.tileSand.content.copy(alpha = 0.6f),
    focusedTextColor = RaveTheme.colors.tileSand.content,
    unfocusedTextColor = RaveTheme.colors.tileSand.content,
    cursorColor = RaveTheme.colors.tileSand.content,
)

@Composable
private fun SplitsCard(splits: List<Split>, unit: DistanceUnit) {
    val c = RaveTheme.colors
    val ink = c.tileInk
    BentoTile(ink.container, Modifier.fillMaxWidth()) {
        BentoKicker(stringResource(R.string.summary_splits), ink.content)
        val paces = splits.map { it.paceSecondsPer(unit.meters) }
        val fastest = paces.filter { it > 0 }.minOrNull() ?: 1.0
        val slowest = paces.maxOrNull() ?: 1.0
        splits.forEachIndexed { i, split ->
            val pace = paces[i]
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                val partial = split.distanceM < unit.meters - 1
                Text(
                    if (partial) Format.distance(split.distanceM, unit) else split.index.toString(),
                    color = ink.content, maxLines = 1,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontSize = if (partial) 11.sp else 15.sp, fontWeight = FontWeight.Bold,
                    ),
                    modifier = Modifier.width(if (partial) 36.dp else 20.dp),
                )
                // Faster splits get longer bars; the fastest is lime.
                val fraction = if (slowest <= fastest) 0.92f else (0.55f + 0.37f * ((slowest - pace) / (slowest - fastest)).toFloat())
                Box(
                    Modifier.weight(1f).padding(horizontal = 10.dp).height(12.dp).clip(BentoPillShape)
                        .background(ink.content.copy(alpha = 0.12f)),
                ) {
                    Box(
                        Modifier.fillMaxWidth(fraction.coerceIn(0.1f, 1f)).height(12.dp).clip(BentoPillShape)
                            .background(if (pace == fastest) c.highlight else c.navContent.copy(alpha = 0.75f)),
                    )
                }
                Text(
                    bentoPace(pace / unit.meters * 1000.0, unit),
                    color = ink.content, style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                )
            }
        }
    }
}

@Composable
private fun Banner(icon: ImageVector, tile: TileColor, text: String, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().clip(BentoTileShape).background(tile.container).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = tile.content)
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.titleSmall, color = tile.content)
    }
}
