package com.rave.app.ui.share

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.rave.app.R
import com.rave.app.data.local.entity.ActivityEntity
import com.rave.app.data.local.entity.StreakEntity
import com.rave.app.data.preferences.SettingsRepository
import com.rave.app.data.repository.ActivityRepository
import com.rave.app.data.repository.PrivacyZoneRepository
import com.rave.app.data.repository.StreakRepository
import com.rave.app.domain.model.DistanceUnit
import com.rave.app.domain.model.LatLngPoint
import com.rave.app.domain.util.Format
import com.rave.app.ui.components.LoadingState
import com.rave.app.ui.components.RaveScreen
import com.rave.app.ui.components.RouteCanvas
import com.rave.app.ui.navigation.ShareActivityRoute
import com.rave.app.ui.paceUnitLabel
import com.rave.app.ui.theme.Outfit
import com.rave.app.ui.theme.SharePalette
import com.rave.app.ui.unitLabel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import javax.inject.Inject

data class ShareActivityState(
    val activity: ActivityEntity? = null,
    /** Route with privacy rules applied (ends trimmed, zones removed). */
    val segments: List<List<LatLngPoint>> = emptyList(),
    /** True when the activity has GPS points but privacy zones hid all of them. */
    val routeHidden: Boolean = false,
    val streak: Int = 0,
    val unit: DistanceUnit = DistanceUnit.KM,
)

@HiltViewModel
class ShareActivityViewModel @Inject constructor(
    savedState: SavedStateHandle,
    activities: ActivityRepository,
    privacy: PrivacyZoneRepository,
    streaks: StreakRepository,
    settings: SettingsRepository,
) : ViewModel() {
    private val id = savedState.toRoute<ShareActivityRoute>().activityId
    private val _state = MutableStateFlow(ShareActivityState())
    val state: StateFlow<ShareActivityState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val activity = activities.get(id) ?: return@launch
            val points = activities.points(id)
            val segments = privacy.shareCardSegments(points)
            val streak = streaks.observeStreaks().first()[StreakEntity.OVERALL]?.current ?: 0
            _state.value = ShareActivityState(
                activity = activity,
                segments = segments,
                routeHidden = segments.isEmpty() && points.size >= 2,
                streak = streak,
                unit = settings.current().unit,
            )
        }
    }
}

@Composable
fun ShareActivityScreen(onBack: () -> Unit, viewModel: ShareActivityViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val pro = com.rave.app.ui.pro.rememberPro()
    RaveScreen(title = stringResource(R.string.share_title), onBack = onBack) {
        val a = state.activity
        if (a == null) {
            LoadingState()
            return@RaveScreen
        }
        val distance = Format.distance(a.distanceM, state.unit)
        val text = stringResource(R.string.share_activity_text, a.name, distance, unitLabel(state.unit), stringResource(R.string.app_name))
        SharePager(
            fileName = "activity-${a.id}", shareText = text, modifier = Modifier.fillMaxSize().padding(bottom = 8.dp),
            isPro = pro.isPro, onUpgrade = { pro.upgrade(com.rave.app.data.billing.ProReason.SHARE) }, routeColors = true,
        ) { _, palette ->
            ActivityCard(a, state, palette)
        }
    }
}

@Composable
private fun BoxScope.ActivityCard(a: ActivityEntity, state: ShareActivityState, p: SharePalette) {
    val shadow = if (p.shadow) Shadow(color = p.shadowColor, blurRadius = 8f) else null
    fun style(size: Int, weight: androidx.compose.ui.text.font.FontWeight) =
        TextStyle(fontFamily = Outfit, fontWeight = weight, fontSize = size.sp, shadow = shadow)
    val unit = state.unit
    Column(Modifier.fillMaxSize().padding(22.dp)) {
        Text(a.name, style = style(22, androidx.compose.ui.text.font.FontWeight.Bold), color = p.text, maxLines = 2)
        Text(
            Instant.ofEpochMilli(a.startTime).atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)),
            style = style(13, androidx.compose.ui.text.font.FontWeight.Medium), color = p.secondaryText,
        )
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            if (state.segments.isNotEmpty()) {
                RouteCanvas(state.segments, p.route, Modifier.fillMaxSize().padding(vertical = 12.dp), strokeWidth = 5.dp, padding = 12.dp)
            } else if (state.routeHidden) {
                Text(stringResource(R.string.share_route_hidden), style = style(13, androidx.compose.ui.text.font.FontWeight.Medium), color = p.secondaryText)
            }
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Text(Format.distance(a.distanceM, unit), style = style(52, androidx.compose.ui.text.font.FontWeight.ExtraBold), color = p.text)
            Spacer(Modifier.width(6.dp))
            Text(unitLabel(unit), style = style(18, androidx.compose.ui.text.font.FontWeight.SemiBold), color = p.secondaryText,
                modifier = Modifier.padding(bottom = 10.dp))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            CardStat(Format.duration(a.durationS), stringResource(R.string.stat_time), p, ::style)
            CardStat(Format.pace(a.avgPaceSPerKm, unit), paceUnitLabel(unit), p, ::style)
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.LocalFireDepartment, null, tint = p.accent, modifier = Modifier.size(18.dp))
                    Text(state.streak.toString(), style = style(18, androidx.compose.ui.text.font.FontWeight.Bold), color = p.text)
                }
                Text(stringResource(R.string.share_streak), style = style(11, androidx.compose.ui.text.font.FontWeight.Medium), color = p.secondaryText)
            }
        }
        if (p.watermark) {
            Spacer(Modifier.height(14.dp))
            Text(stringResource(R.string.share_made_with).uppercase(), style = style(12, androidx.compose.ui.text.font.FontWeight.Bold), color = p.accent)
        }
    }
}

@Composable
private fun CardStat(value: String, label: String, p: SharePalette, style: (Int, androidx.compose.ui.text.font.FontWeight) -> TextStyle) {
    Column {
        Text(value, style = style(18, androidx.compose.ui.text.font.FontWeight.Bold), color = p.text)
        Text(label, style = style(11, androidx.compose.ui.text.font.FontWeight.Medium), color = p.secondaryText)
    }
}
