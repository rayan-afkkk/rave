package com.rave.app.ui.progress

import androidx.compose.foundation.layout.Arrangement
import com.rave.app.ui.theme.Dimens
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rave.app.R
import com.rave.app.ui.activityTypeRes
import com.rave.app.ui.components.BarChart
import com.rave.app.ui.components.EmptyState
import com.rave.app.ui.components.LoadingState
import com.rave.app.ui.components.MonthHeatmap
import com.rave.app.ui.components.PillBarRow
import com.rave.app.ui.components.PillChip
import com.rave.app.ui.components.PillProgress
import com.rave.app.ui.components.RaveScreen
import com.rave.app.ui.components.SectionCard
import com.rave.app.ui.components.StatItem
import com.rave.app.ui.components.tile
import com.rave.app.ui.theme.RaveTheme
import com.rave.app.ui.unitLabel
import java.time.DayOfWeek
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun ProgressScreen(onBack: () -> Unit, viewModel: ProgressViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val pro = com.rave.app.ui.pro.rememberPro()
    RaveScreen(title = stringResource(R.string.progress_title), onBack = onBack) {
        if (state.loading) {
            LoadingState()
            return@RaveScreen
        }
        val chart = RaveTheme.colors.chart
        Column(
            Modifier.verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = Dimens.ScreenPadding, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(Dimens.SectionGap),
        ) {
            // Points + level
            SectionCard(color = RaveTheme.colors.accentCard) {
                val on = RaveTheme.colors.onAccentCard
                Text(stringResource(R.string.progress_points_week), style = MaterialTheme.typography.labelLarge, color = on.copy(alpha = 0.8f))
                Text(state.stats.pointsThisWeek.toString(), style = MaterialTheme.typography.displaySmall, color = on)
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.profile_level, state.stats.level), style = MaterialTheme.typography.labelMedium, color = on)
                Spacer(Modifier.height(6.dp))
                PillProgress(state.stats.levelProgress)
            }

            // Weekly pill bars
            SectionCard {
                Text(stringResource(R.string.progress_this_week), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                if (state.weekRows.isEmpty()) {
                    EmptyState(Icons.Rounded.Insights, stringResource(R.string.progress_empty_title), stringResource(R.string.progress_empty_body))
                }
                state.weekRows.forEachIndexed { i, row ->
                    val color = row.color?.tile()?.content ?: chart[(i % chart.size)]
                    val label = row.type?.let { stringResource(activityTypeRes(it)) } ?: row.label
                    Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(6.dp))
                    PillBarRow(row.values, state.weekLabels, color, description = label)
                    Spacer(Modifier.height(14.dp))
                }
            }

            // Month heatmap
            SectionCard {
                Text(
                    "${state.month.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${state.month.year}",
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(12.dp))
                MonthHeatmap(
                    values = state.monthValues,
                    leadingBlanks = state.monthLeadingBlanks,
                    weekdayLabels = DayOfWeek.entries.map { it.getDisplayName(TextStyle.NARROW, Locale.getDefault()) },
                )
            }

            // Distance chart
            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.progress_distance, unitLabel(state.unit)), style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f))
                    PillChip(state.chartMode == ChartMode.WEEKLY, stringResource(R.string.progress_weekly), { viewModel.setMode(ChartMode.WEEKLY) })
                    Spacer(Modifier.width(6.dp))
                    PillChip(state.chartMode == ChartMode.MONTHLY, stringResource(R.string.progress_monthly),
                        { pro.gate(com.rave.app.data.billing.ProReason.STATS) { viewModel.setMode(ChartMode.MONTHLY) } })
                    Spacer(Modifier.width(6.dp))
                    PillChip(state.chartMode == ChartMode.YEARLY, stringResource(R.string.progress_yearly),
                        { pro.gate(com.rave.app.data.billing.ProReason.STATS) { viewModel.setMode(ChartMode.YEARLY) } })
                }
                Spacer(Modifier.height(12.dp))
                BarChart(
                    state.chartValues, state.chartLabels, MaterialTheme.colorScheme.primary,
                    description = state.chartLabels.zip(state.chartValues).joinToString { (l, v) -> "$l: ${"%.1f".format(v)}" },
                )
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth()) {
                    StatItem("%.1f".format(state.stats.totalDistanceM / state.unit.meters), stringResource(R.string.profile_total_distance),
                        Modifier.weight(1f), unit = unitLabel(state.unit))
                    StatItem(state.stats.totalActivities.toString(), stringResource(R.string.profile_total_activities), Modifier.weight(1f))
                }
            }

            // Personal records (Pro)
            com.rave.app.ui.pro.ProLock(pro, com.rave.app.data.billing.ProReason.STATS, stringResource(R.string.progress_records_locked)) {
                RecordsCard(state)
            }

            // Best streaks
            if (state.streaks.isNotEmpty()) {
                SectionCard {
                    Text(stringResource(R.string.progress_best_streaks), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    state.streaks.forEach { s ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.LocalFireDepartment, null, tint = s.color.tile().content, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(s.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                            Text(stringResource(R.string.progress_streak_values, s.current, s.best), style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun RecordsCard(state: ProgressState) {
    SectionCard(Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.progress_records), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        if (state.records.isEmpty()) {
            Text(stringResource(R.string.progress_records_empty), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        val fmt = java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault())
        state.records.forEach { r ->
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.EmojiEvents, null, tint = RaveTheme.colors.tilePeach.content, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(com.rave.app.ui.pbTagRes(r.tag)), style = MaterialTheme.typography.bodyMedium)
                    Text("${r.name.ifBlank { "—" }} · ${r.date.format(fmt)}", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                val value = if (r.tag == com.rave.app.domain.model.PbTag.LONGEST_DISTANCE) {
                    "${com.rave.app.domain.util.Format.distance(r.distanceM, state.unit)} ${com.rave.app.ui.unitLabel(state.unit)}"
                } else {
                    "${com.rave.app.domain.util.Format.pace(r.paceSPerKm, state.unit)} ${com.rave.app.ui.paceUnitLabel(state.unit)}"
                }
                Text(value, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
