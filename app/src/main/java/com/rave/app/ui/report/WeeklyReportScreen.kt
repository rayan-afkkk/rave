package com.rave.app.ui.report

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.rave.app.R
import com.rave.app.data.preferences.SettingsRepository
import com.rave.app.data.repository.WeeklyReportRepository
import com.rave.app.domain.model.DistanceUnit
import com.rave.app.domain.notify.WeeklyReport
import com.rave.app.domain.util.Format
import com.rave.app.ui.components.EmptyState
import com.rave.app.ui.components.LoadingState
import com.rave.app.ui.components.RaveScreen
import com.rave.app.ui.pro.rememberPro
import com.rave.app.ui.share.SharePager
import com.rave.app.ui.theme.Outfit
import com.rave.app.ui.theme.SharePalette
import com.rave.app.ui.unitLabel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class WeeklyReportViewModel @Inject constructor(reports: WeeklyReportRepository, settings: SettingsRepository) : ViewModel() {
    private val _report = MutableStateFlow<Pair<WeeklyReport, DistanceUnit>?>(null)
    val report: StateFlow<Pair<WeeklyReport, DistanceUnit>?> = _report.asStateFlow()

    init {
        viewModelScope.launch { _report.value = reports.build() to settings.current().unit }
    }
}

/** "Your week": the Sunday report as a shareable card. */
@Composable
fun WeeklyReportScreen(onBack: () -> Unit, viewModel: WeeklyReportViewModel = hiltViewModel()) {
    val data by viewModel.report.collectAsStateWithLifecycle()
    val pro = rememberPro()
    RaveScreen(title = stringResource(R.string.weekly_screen_title), onBack = onBack) {
        val (r, unit) = data ?: run { LoadingState(); return@RaveScreen }
        if (r.isEmpty) {
            EmptyState(Icons.Rounded.CalendarMonth, stringResource(R.string.weekly_screen_title), stringResource(R.string.weekly_empty))
            return@RaveScreen
        }
        val n = NumberFormat.getIntegerInstance()
        val text = stringResource(R.string.weekly_share_text, stringResource(R.string.app_name), n.format(r.steps))
        SharePager(
            fileName = "rave-week-${r.weekStart}", shareText = text, modifier = Modifier.fillMaxSize().padding(bottom = 8.dp),
            isPro = pro.isPro, onUpgrade = { pro.upgrade(com.rave.app.data.billing.ProReason.SHARE) },
        ) { _, palette -> WeekCard(r, unit, palette) }
    }
}

@Composable
private fun BoxScope.WeekCard(r: WeeklyReport, unit: DistanceUnit, p: SharePalette) {
    val n = NumberFormat.getIntegerInstance()
    fun style(size: Int, weight: FontWeight) = TextStyle(fontFamily = Outfit, fontWeight = weight, fontSize = size.sp)
    val range = "${r.weekStart.format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()))} – " +
        r.weekStart.plusDays(6).format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()))
    Column(Modifier.fillMaxSize().padding(22.dp)) {
        Text(stringResource(R.string.weekly_screen_title), style = style(22, FontWeight.Bold), color = p.text)
        Text(range, style = style(13, FontWeight.Medium), color = p.secondaryText)
        Spacer(Modifier.weight(1f))
        if (r.leadsWithDistance) {
            Text("${Format.distance(r.distanceM, unit, 1)} ${unitLabel(unit)}", style = style(48, FontWeight.ExtraBold), color = p.text)
            r.distanceChange?.let { Change(it, p, ::style) }
        } else {
            Text(n.format(r.steps), style = style(48, FontWeight.ExtraBold), color = p.text)
            Text(stringResource(R.string.weekly_steps), style = style(14, FontWeight.SemiBold), color = p.secondaryText)
            r.stepsChange?.let { Change(it, p, ::style) }
        }
        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            if (r.leadsWithDistance) Stat(n.format(r.steps), stringResource(R.string.weekly_steps), p, ::style)
            Stat(r.activities.toString(), stringResource(R.string.weekly_activities), p, ::style)
            r.habitRate?.let { Stat("$it%", stringResource(R.string.weekly_habits), p, ::style) }
            Stat(n.format(r.bestDaySteps), stringResource(R.string.weekly_best_day), p, ::style)
        }
        if (p.watermark) {
            Spacer(Modifier.height(14.dp))
            Text(stringResource(R.string.share_made_with).uppercase(), style = style(12, FontWeight.Bold), color = p.accent)
        }
    }
}

@Composable
private fun Change(pct: Int, p: SharePalette, style: (Int, FontWeight) -> TextStyle) {
    val sign = if (pct >= 0) "▲ $pct%" else "▼ ${-pct}%"
    Text(stringResource(R.string.weekly_vs_last, sign), style = style(15, FontWeight.Bold), color = p.accent)
}

@Composable
private fun Stat(value: String, label: String, p: SharePalette, style: (Int, FontWeight) -> TextStyle) {
    Column {
        Text(value, style = style(18, FontWeight.Bold), color = p.text)
        Text(label, style = style(11, FontWeight.Medium), color = p.secondaryText)
    }
}
