package com.rave.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.rave.app.R
import com.rave.app.data.cloud.AuthRepository
import com.rave.app.data.cloud.LeagueRepository
import com.rave.app.data.cloud.LeagueStandings
import com.rave.app.ui.components.IconTile
import com.rave.app.ui.components.SectionCard
import com.rave.app.ui.theme.RaveTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.NumberFormat
import javax.inject.Inject

sealed interface LeagueUi {
    data object Loading : LeagueUi
    data object SignedOut : LeagueUi
    data object Unavailable : LeagueUi
    data class Ready(val standings: LeagueStandings) : LeagueUi
}

@HiltViewModel
class LeagueViewModel @Inject constructor(
    private val league: LeagueRepository,
    private val auth: AuthRepository,
) : ViewModel() {
    private val _ui = MutableStateFlow<LeagueUi>(LeagueUi.Loading)
    val ui: StateFlow<LeagueUi> = _ui.asStateFlow()

    fun refresh() = viewModelScope.launch {
        if (!auth.isAvailable || auth.currentUser.value == null) {
            _ui.value = LeagueUi.SignedOut
            return@launch
        }
        _ui.value = runCatching { league.standings() }.fold({ LeagueUi.Ready(it) }, { LeagueUi.Unavailable })
    }
}

/** Weekly steps leaderboard with your friends; resets every Monday. */
@Composable
fun LeagueCard(modifier: Modifier = Modifier, viewModel: LeagueViewModel = hiltViewModel()) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.refresh() }
    SectionCard(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconTile(Icons.Rounded.EmojiEvents, RaveTheme.colors.tilePeach, size = 32.dp, iconSize = 18.dp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.league_title), style = MaterialTheme.typography.titleMedium)
                val sub = when (val u = ui) {
                    is LeagueUi.Ready -> if (u.standings.daysLeft == 0) stringResource(R.string.league_last_day)
                        else stringResource(R.string.league_days_left, u.standings.daysLeft)
                    else -> stringResource(R.string.league_subtitle)
                }
                Text(sub, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(10.dp))
        when (val u = ui) {
            LeagueUi.Loading -> Text(stringResource(R.string.league_loading), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            LeagueUi.SignedOut -> Text(stringResource(R.string.league_signed_out), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            LeagueUi.Unavailable -> Text(stringResource(R.string.league_unavailable), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            is LeagueUi.Ready -> {
                if (!u.standings.hasFriends) {
                    Text(stringResource(R.string.league_no_friends), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                val leader = u.standings.entries.firstOrNull()?.steps?.coerceAtLeast(1) ?: 1
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    u.standings.entries.take(5).forEachIndexed { i, e ->
                        LeagueRow(i + 1, e.name, e.steps, e.steps / leader.toFloat(), e.isMe)
                    }
                    // Always show yourself, even outside the top 5.
                    val meIndex = u.standings.entries.indexOfFirst { it.isMe }
                    if (meIndex >= 5) {
                        val me = u.standings.entries[meIndex]
                        LeagueRow(meIndex + 1, me.name, me.steps, me.steps / leader.toFloat(), true)
                    }
                }
            }
        }
    }
}

@Composable
private fun LeagueRow(rank: Int, name: String, steps: Int, fraction: Float, isMe: Boolean) {
    val medal = when (rank) {
        1 -> RaveTheme.colors.tilePeach
        2 -> RaveTheme.colors.tileBlue
        3 -> RaveTheme.colors.tileGreen
        else -> null
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(26.dp).clip(CircleShape)
                .background(medal?.container ?: MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Text("$rank", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = medal?.content ?: MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Row {
                Text(
                    if (isMe) stringResource(R.string.league_you, name) else name,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = if (isMe) FontWeight.Bold else FontWeight.Medium),
                    maxLines = 1, modifier = Modifier.weight(1f),
                )
                Text(NumberFormat.getIntegerInstance().format(steps), style = MaterialTheme.typography.labelLarge,
                    color = if (isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
            }
            Box(Modifier.fillMaxWidth().padding(top = 4.dp).height(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)) {
                Box(
                    Modifier.fillMaxWidth(fraction.coerceIn(0.02f, 1f)).height(6.dp).clip(CircleShape)
                        .background(if (isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
                )
            }
        }
    }
}
