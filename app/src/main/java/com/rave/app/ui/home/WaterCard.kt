package com.rave.app.ui.home

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.rave.app.R
import com.rave.app.data.preferences.SettingsRepository
import com.rave.app.data.repository.WaterRepository
import com.rave.app.ui.components.IconTile
import com.rave.app.ui.components.SectionCard
import com.rave.app.ui.theme.RaveTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WaterState(val glasses: Int = 0, val goal: Int = 8)

@HiltViewModel
class WaterViewModel @Inject constructor(private val water: WaterRepository, settings: SettingsRepository) : ViewModel() {
    val state: StateFlow<WaterState> = combine(water.observeToday(), settings.settings) { g, s -> WaterState(g, s.waterGoalGlasses) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), WaterState())

    fun add(delta: Int) = viewModelScope.launch { water.add(delta) }
}

/** Home: glasses of water today, one tap to add. */
@Composable
fun WaterCard(modifier: Modifier = Modifier, viewModel: WaterViewModel = hiltViewModel()) {
    val s by viewModel.state.collectAsStateWithLifecycle()
    val blue = RaveTheme.colors.tileBlue
    SectionCard(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconTile(Icons.Rounded.WaterDrop, blue)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.water_title), style = MaterialTheme.typography.titleSmall)
                Text(
                    if (s.glasses >= s.goal) stringResource(R.string.water_done) else stringResource(R.string.water_count, s.glasses, s.goal),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = { viewModel.add(-1) }, enabled = s.glasses > 0) {
                Icon(Icons.Rounded.Remove, stringResource(R.string.water_remove))
            }
            FilledTonalIconButton(onClick = { viewModel.add(1) }) { Icon(Icons.Rounded.Add, stringResource(R.string.water_add)) }
        }
        // One drop per glass of the goal; filled as you drink.
        Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            repeat(s.goal.coerceIn(1, 12)) { i ->
                val filled = i < s.glasses
                val tint by animateColorAsState(if (filled) blue.content else MaterialTheme.colorScheme.surfaceVariant, label = "drop")
                Icon(Icons.Rounded.WaterDrop, null, tint = tint, modifier = Modifier.size(22.dp))
            }
        }
    }
}
