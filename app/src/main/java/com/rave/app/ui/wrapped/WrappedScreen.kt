package com.rave.app.ui.wrapped

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.rave.app.R
import com.rave.app.data.preferences.SettingsRepository
import com.rave.app.data.repository.WrappedRepository
import com.rave.app.domain.fitness.FitnessMath
import com.rave.app.domain.wrapped.WrappedPersona
import com.rave.app.domain.wrapped.WrappedSummary
import com.rave.app.ui.components.LoadingState
import com.rave.app.ui.components.RaveLogo
import com.rave.app.ui.navigation.WrappedRoute
import com.rave.app.ui.share.SharePager
import com.rave.app.ui.theme.RaveTheme
import com.rave.app.ui.theme.SharePalette
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

data class WrappedUi(val summary: WrappedSummary, val heightCm: Float)

@HiltViewModel
class WrappedViewModel @Inject constructor(
    savedState: SavedStateHandle,
    repo: WrappedRepository,
    settings: SettingsRepository,
) : ViewModel() {
    val year = savedState.toRoute<WrappedRoute>().year
    private val _ui = MutableStateFlow<WrappedUi?>(null)
    val ui: StateFlow<WrappedUi?> = _ui.asStateFlow()

    init {
        viewModelScope.launch { _ui.value = WrappedUi(repo.summary(year), settings.current().heightCm) }
    }
}

private enum class WrappedPage { INTRO, STEPS, MOVES, HABITS, STRENGTH, PEAK, SHARE, EMPTY }

/** "Rave Wrapped": story-style pages for the year, ending with a shareable card for Instagram. */
@Composable
fun WrappedScreen(onBack: () -> Unit, viewModel: WrappedViewModel = hiltViewModel()) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val colors = RaveTheme.colors
    Box(Modifier.fillMaxSize().background(colors.splashBackground)) {
        val u = ui
        if (u == null) {
            LoadingState()
        } else {
            val s = u.summary
            val pages = buildList {
                if (s.isEmpty) add(WrappedPage.EMPTY) else {
                    add(WrappedPage.INTRO)
                    if (s.totalSteps > 0) add(WrappedPage.STEPS)
                    if (s.activities > 0) add(WrappedPage.MOVES)
                    if (s.habitCompletions > 0) add(WrappedPage.HABITS)
                    if (s.workouts > 0) add(WrappedPage.STRENGTH)
                    if (s.peakMonth != null) add(WrappedPage.PEAK)
                    add(WrappedPage.SHARE)
                }
            }
            val pager = rememberPagerState { pages.size }
            HorizontalPager(pager, Modifier.fillMaxSize()) { index ->
                val active = pager.currentPage == index
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.radialGradient(listOf(colors.splashMark.copy(alpha = 0.28f), Color.Transparent), radius = 1400f),
                    ),
                ) {
                    when (pages[index]) {
                        WrappedPage.INTRO -> Intro(s.year, active)
                        WrappedPage.STEPS -> BigStat(
                            kicker = stringResource(R.string.wrapped_steps_kicker), value = s.totalSteps, active = active,
                            label = stringResource(R.string.wrapped_steps_label),
                            lines = listOfNotNull(
                                stringResource(R.string.wrapped_steps_km, "%.0f".format(FitnessMath.distanceM(s.totalSteps.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(), u.heightCm) / 1000)),
                                s.bestStepDay?.let { (d, n) -> stringResource(R.string.wrapped_best_day, fmt(n.toLong()), d.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))) },
                            ),
                        )
                        WrappedPage.MOVES -> BigStat(
                            kicker = stringResource(R.string.wrapped_moves_kicker), value = s.totalKm.toLong(), active = active,
                            label = stringResource(R.string.wrapped_km_label),
                            lines = listOfNotNull(
                                stringResource(R.string.wrapped_moves_count, s.activities, "%.0f".format(s.activeHours)),
                                s.longest?.let { stringResource(R.string.wrapped_longest, "%.1f".format(it.distanceM / 1000), it.name.ifBlank { "—" }) },
                            ),
                        )
                        WrappedPage.HABITS -> BigStat(
                            kicker = stringResource(R.string.wrapped_habits_kicker), value = s.habitCompletions.toLong(), active = active,
                            label = stringResource(R.string.wrapped_habits_label),
                            lines = listOfNotNull(
                                s.topHabit?.let { (name, n) -> stringResource(R.string.wrapped_top_habit, name, n) },
                                stringResource(R.string.wrapped_best_streak, s.bestStreak),
                            ),
                        )
                        WrappedPage.STRENGTH -> BigStat(
                            kicker = stringResource(R.string.wrapped_strength_kicker), value = s.workouts.toLong(), active = active,
                            label = stringResource(R.string.wrapped_workouts_label),
                            lines = listOf(stringResource(R.string.wrapped_lifted, fmt(s.liftedKg.toLong()))),
                        )
                        WrappedPage.PEAK -> Peak(s, active)
                        WrappedPage.SHARE -> ShareFinale(s)
                        WrappedPage.EMPTY -> Empty(s.year)
                    }
                }
            }
            // Story-style progress segments.
            Row(
                Modifier.statusBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                pages.indices.forEach { i ->
                    Box(
                        Modifier.weight(1f).height(3.dp).clip(CircleShape)
                            .background(if (i <= pager.currentPage) colors.splashText else colors.splashText.copy(alpha = 0.25f)),
                    )
                }
            }
        }
        IconButton(onClick = onBack, modifier = Modifier.statusBarsPadding().padding(top = 16.dp, start = 4.dp)) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.action_back), tint = colors.splashText)
        }
    }
}

private fun fmt(n: Long): String = NumberFormat.getIntegerInstance().format(n)

@Composable
private fun countUp(target: Long, active: Boolean): Long {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(active, target) {
        if (active) {
            anim.snapTo(0f)
            anim.animateTo(1f, tween(1400, easing = FastOutSlowInEasing))
        }
    }
    return (target * anim.value).toLong()
}

@Composable
private fun BoxScope.Centered(content: @Composable () -> Unit) {
    Column(
        Modifier.align(Alignment.Center).fillMaxWidth().padding(horizontal = 28.dp).navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) { content() }
}

@Composable
private fun BoxScope.Intro(year: Int, active: Boolean) {
    val colors = RaveTheme.colors
    val p = remember { Animatable(0f) }
    LaunchedEffect(active) { if (active) { p.snapTo(0f); p.animateTo(1f, tween(900)) } }
    Centered {
        RaveLogo(color = colors.splashMark, progress = p.value, modifier = Modifier.size(96.dp))
        Spacer(Modifier.height(18.dp))
        Text(stringResource(R.string.wrapped_kicker), color = colors.splashMark, style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 4.sp))
        Text(stringResource(R.string.wrapped_your_year, year), color = colors.splashText,
            style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.ExtraBold), textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Text(stringResource(R.string.wrapped_swipe), color = colors.splashText.copy(alpha = 0.7f), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun BoxScope.BigStat(kicker: String, value: Long, active: Boolean, label: String, lines: List<String>) {
    val colors = RaveTheme.colors
    val shown = countUp(value, active)
    Centered {
        Text(kicker, color = colors.splashText.copy(alpha = 0.8f), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(fmt(shown), color = colors.splashMark, style = MaterialTheme.typography.displayLarge.copy(fontSize = 64.sp, fontWeight = FontWeight.ExtraBold))
        Text(label, color = colors.splashText, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
        Spacer(Modifier.height(22.dp))
        lines.forEach {
            Text(it, color = colors.splashText.copy(alpha = 0.8f), style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 3.dp))
        }
    }
}

@Composable
private fun BoxScope.Peak(s: WrappedSummary, active: Boolean) {
    val colors = RaveTheme.colors
    val a = remember { Animatable(0f) }
    LaunchedEffect(active) { if (active) { a.snapTo(0f); a.animateTo(1f, tween(800)) } }
    Centered {
        Text(stringResource(R.string.wrapped_peak_kicker), color = colors.splashText.copy(alpha = 0.8f), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(10.dp))
        Text(
            s.peakMonth?.getDisplayName(TextStyle.FULL, Locale.getDefault()).orEmpty(),
            color = colors.splashMark,
            style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.ExtraBold, fontSize = (40 + 16 * a.value).sp),
        )
        Spacer(Modifier.height(10.dp))
        Text(stringResource(R.string.wrapped_peak_body), color = colors.splashText.copy(alpha = 0.8f), style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center)
    }
}

@Composable
private fun personaName(p: WrappedPersona) = stringResource(
    when (p) {
        WrappedPersona.ROAD_WARRIOR -> R.string.persona_road
        WrappedPersona.IRON_LIFTER -> R.string.persona_iron
        WrappedPersona.HABIT_HERO -> R.string.persona_habit
        WrappedPersona.STEP_MACHINE -> R.string.persona_steps
        WrappedPersona.ALL_ROUNDER -> R.string.persona_all
        WrappedPersona.FRESH_START -> R.string.persona_fresh
    },
)

@Composable
private fun BoxScope.ShareFinale(s: WrappedSummary) {
    val colors = RaveTheme.colors
    val persona = personaName(s.persona)
    val text = stringResource(R.string.wrapped_share_text, s.year, persona, stringResource(R.string.app_name))
    Column(Modifier.fillMaxSize().statusBarsPadding().padding(top = 36.dp)) {
        Text(stringResource(R.string.wrapped_you_are), color = colors.splashText.copy(alpha = 0.8f), style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.align(Alignment.CenterHorizontally))
        Text(persona, color = colors.splashMark, style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
            modifier = Modifier.align(Alignment.CenterHorizontally))
        SharePager(fileName = "rave-wrapped-${s.year}", shareText = text, modifier = Modifier.weight(1f).padding(top = 8.dp)) { _, palette ->
            WrappedCard(s, persona, palette)
        }
    }
}

@Composable
private fun BoxScope.WrappedCard(s: WrappedSummary, persona: String, p: SharePalette) {
    Column(Modifier.fillMaxSize().padding(22.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RaveLogo(color = p.accent, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.wrapped_card_title, s.year), color = p.text, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
        Spacer(Modifier.height(4.dp))
        Text(persona, color = p.accent, fontWeight = FontWeight.ExtraBold, fontSize = 30.sp)
        Spacer(Modifier.weight(1f))
        if (s.totalSteps > 0) WrappedRow(p, fmt(s.totalSteps), stringResource(R.string.wrapped_steps_label))
        if (s.totalKm > 0) WrappedRow(p, "%.0f".format(s.totalKm), stringResource(R.string.wrapped_km_label))
        if (s.habitCompletions > 0) WrappedRow(p, fmt(s.habitCompletions.toLong()), stringResource(R.string.wrapped_habits_label))
        if (s.workouts > 0) WrappedRow(p, fmt(s.workouts.toLong()), stringResource(R.string.wrapped_workouts_label))
        WrappedRow(p, "${s.bestStreak}", stringResource(R.string.wrapped_streak_label))
        Spacer(Modifier.height(10.dp))
        Text(stringResource(R.string.app_name).lowercase(), color = p.accent, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
    }
}

@Composable
private fun WrappedRow(p: SharePalette, value: String, label: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.Bottom) {
        Text(value, color = p.text, fontWeight = FontWeight.ExtraBold, fontSize = 30.sp)
        Spacer(Modifier.width(8.dp))
        Text(label, color = p.secondaryText, fontSize = 14.sp, modifier = Modifier.padding(bottom = 5.dp))
    }
}

@Composable
private fun BoxScope.Empty(year: Int) {
    val colors = RaveTheme.colors
    Centered {
        RaveLogo(color = colors.splashMark, modifier = Modifier.size(80.dp))
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.wrapped_empty_title, year + 1), color = colors.splashText,
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold), textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.wrapped_empty_body), color = colors.splashText.copy(alpha = 0.75f),
            style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
    }
}
