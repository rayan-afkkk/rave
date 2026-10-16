package com.rave.app.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.rave.app.data.repository.RepairOffer
import com.rave.app.data.repository.StreakRepository
import com.rave.app.ui.components.IconTile
import com.rave.app.ui.components.PillButton
import com.rave.app.ui.components.SectionCard
import com.rave.app.ui.theme.RaveTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StreakRepairViewModel @Inject constructor(private val streaks: StreakRepository) : ViewModel() {
    val offer = streaks.repairOffer

    fun repair(offer: RepairOffer) = viewModelScope.launch { streaks.repair(offer) }
    fun dismiss() = streaks.dismissRepairOffer()
}

/** Shown on Home when the overall streak broke in the last two days: one free repair per month. */
@Composable
fun StreakRepairCard(modifier: Modifier = Modifier, viewModel: StreakRepairViewModel = hiltViewModel()) {
    val offer by viewModel.offer.collectAsStateWithLifecycle()
    val pro = com.rave.app.ui.pro.rememberPro()
    AnimatedVisibility(visible = offer != null, modifier = modifier) {
        val o = offer ?: return@AnimatedVisibility
        SectionCard(Modifier.fillMaxWidth(), color = RaveTheme.colors.tilePeach.container) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconTile(Icons.Rounded.Build, RaveTheme.colors.tilePeach)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.repair_title, o.lostStreak), style = MaterialTheme.typography.titleSmall,
                        color = RaveTheme.colors.tilePeach.content)
                    Text(stringResource(R.string.repair_body), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = viewModel::dismiss) { Text(stringResource(R.string.action_not_now)) }
                PillButton(stringResource(R.string.repair_action), onClick = { pro.gate(com.rave.app.data.billing.ProReason.STREAK) { viewModel.repair(o) } },
                    icon = if (pro.isPro) null else androidx.compose.material.icons.Icons.Rounded.WorkspacePremium)
            }
        }
    }
}
