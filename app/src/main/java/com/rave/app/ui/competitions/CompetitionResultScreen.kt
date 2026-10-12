package com.rave.app.ui.competitions

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.rave.app.R
import com.rave.app.data.cloud.AuthRepository
import com.rave.app.data.cloud.CompetitionRepository
import com.rave.app.data.preferences.SettingsRepository
import com.rave.app.data.repository.StatsRepository
import com.rave.app.domain.competition.Competition
import com.rave.app.domain.competition.CompetitionRanking
import com.rave.app.domain.competition.RankedParticipant
import com.rave.app.domain.model.DistanceUnit
import com.rave.app.ui.components.Avatar
import com.rave.app.ui.components.LoadingState
import com.rave.app.ui.components.RaveScreen
import com.rave.app.ui.navigation.CompetitionResultRoute
import com.rave.app.ui.share.SharePager
import com.rave.app.ui.theme.Outfit
import com.rave.app.ui.theme.RaveTheme
import com.rave.app.ui.theme.SharePalette
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ResultState(
    val loading: Boolean = true,
    val competition: Competition? = null,
    val ranked: List<RankedParticipant> = emptyList(),
    val me: String = "",
    val unit: DistanceUnit = DistanceUnit.KM,
)

@HiltViewModel
class CompetitionResultViewModel @Inject constructor(
    savedState: SavedStateHandle,
    repo: CompetitionRepository,
    settings: SettingsRepository,
    private val stats: StatsRepository,
    auth: AuthRepository,
) : ViewModel() {
    private val id = savedState.toRoute<CompetitionResultRoute>().id
    private val me = auth.currentUser.value?.uid.orEmpty()

    val state: StateFlow<ResultState> = combine(repo.observe(id), repo.observeParticipants(id), settings.settings) { c, rows, prefs ->
        ResultState(false, c, if (c == null) emptyList() else CompetitionRanking.rank(rows.map { it.score }, c.type), me, prefs.unit)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ResultState())

    /** Awards the winner badge locally (+50 points) the first time the winner sees the results. */
    fun recordWinIfMine(s: ResultState) {
        val c = s.competition ?: return
        if (s.ranked.firstOrNull()?.score?.uid == me && me.isNotEmpty()) viewModelScope.launch { stats.recordCompetitionWin(c.id, c.name) }
    }
}

@Composable
fun CompetitionResultScreen(onBack: () -> Unit, viewModel: CompetitionResultViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.ranked) { viewModel.recordWinIfMine(state) }
    RaveScreen(title = stringResource(R.string.comp_results), onBack = onBack) {
        val c = state.competition
        if (state.loading || c == null) {
            LoadingState()
            return@RaveScreen
        }
        val winner = state.ranked.firstOrNull()
        val iWon = winner?.score?.uid == state.me
        val mine = state.ranked.firstOrNull { it.score.uid == state.me }
        val text = stringResource(R.string.comp_result_share, c.name, mine?.rank ?: 0, stringResource(R.string.app_name))
        Column(Modifier.fillMaxSize()) {
            Text(
                if (iWon) stringResource(R.string.comp_you_won) else winner?.let { stringResource(R.string.comp_winner, it.score.name) } ?: "",
                style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            )
            if (iWon) {
                Text(stringResource(R.string.comp_badge_awarded), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.primary, modifier = Modifier.fillMaxWidth())
            }
            Spacer(Modifier.height(8.dp))
            SharePager(fileName = "competition-${c.id}", shareText = text, modifier = Modifier.weight(1f)) { _, palette ->
                ResultCard(c, state, palette)
            }
        }
    }
}

/** Podium: 2nd · 1st · 3rd. Used both on screen (inside the share card) and in the exported image. */
@Composable
private fun BoxScope.ResultCard(c: Competition, state: ResultState, p: SharePalette) {
    val podium = state.ranked.take(3)
    fun style(size: Int, weight: FontWeight) = TextStyle(fontFamily = Outfit, fontWeight = weight, fontSize = size.sp)
    Column(Modifier.fillMaxSize().padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Rounded.EmojiEvents, null, tint = p.accent, modifier = Modifier.size(40.dp))
        Text(c.name, style = style(22, FontWeight.Bold), color = p.text, textAlign = TextAlign.Center, maxLines = 2)
        Text(stringResource(competitionTypeRes(c.type)), style = style(13, FontWeight.Medium), color = p.secondaryText)
        Spacer(Modifier.weight(1f))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.Bottom) {
            podium.getOrNull(1)?.let { Step(it, 2, 90.dp, c, state.unit, p, RaveTheme.colors.chart[1]) }
            podium.getOrNull(0)?.let { Step(it, 1, 130.dp, c, state.unit, p, p.accent) }
            podium.getOrNull(2)?.let { Step(it, 3, 64.dp, c, state.unit, p, RaveTheme.colors.chart[2]) }
        }
        Spacer(Modifier.height(18.dp))
        c.stake?.let { Text(stringResource(R.string.competition_stake, it), style = style(13, FontWeight.SemiBold), color = p.secondaryText) }
        Spacer(Modifier.height(10.dp))
        Text(stringResource(R.string.app_name).uppercase(), style = style(12, FontWeight.Bold), color = p.accent)
    }
}

@Composable
private fun Step(r: RankedParticipant, place: Int, height: Dp, c: Competition, unit: DistanceUnit, p: SharePalette, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(88.dp)) {
        Avatar(r.score.name, size = if (place == 1) 52.dp else 42.dp)
        Spacer(Modifier.height(4.dp))
        Text(r.score.name, color = p.text, style = MaterialTheme.typography.labelLarge, maxLines = 1, textAlign = TextAlign.Center)
        Text(competitionValue(c, r.score.value, unit), color = p.secondaryText, style = MaterialTheme.typography.labelSmall)
        Spacer(Modifier.height(6.dp))
        Box(Modifier.fillMaxWidth().height(height).clip(MaterialTheme.shapes.medium.copy(bottomEnd = androidx.compose.foundation.shape.CornerSize(0.dp),
            bottomStart = androidx.compose.foundation.shape.CornerSize(0.dp))).background(color), contentAlignment = Alignment.TopCenter) {
            Text("$place", color = p.text, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 6.dp))
        }
    }
}

