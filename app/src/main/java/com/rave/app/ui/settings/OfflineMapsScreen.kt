package com.rave.app.ui.settings

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.rave.app.R
import com.rave.app.data.billing.ProReason
import com.rave.app.data.preferences.SettingsRepository
import com.rave.app.ui.components.IconTile
import com.rave.app.ui.components.PillButton
import com.rave.app.ui.components.PillChip
import com.rave.app.ui.components.RaveScreen
import com.rave.app.ui.components.OfflineTiles
import com.rave.app.ui.components.SectionCard
import com.rave.app.ui.permissions.hasPermission
import com.rave.app.ui.pro.rememberPro
import com.rave.app.ui.theme.Dimens
import com.rave.app.ui.theme.RaveTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.DateFormat
import java.util.Date
import javax.inject.Inject

sealed interface OfflineUi {
    data object Idle : OfflineUi
    data class Saving(val done: Int, val total: Int) : OfflineUi
    data class Saved(val tiles: Int) : OfflineUi
    data object NoLocation : OfflineUi
    data object Failed : OfflineUi
}

@HiltViewModel
class OfflineMapsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: SettingsRepository,
) : ViewModel() {
    private val _ui = MutableStateFlow<OfflineUi>(OfflineUi.Idle)
    val ui: StateFlow<OfflineUi> = _ui.asStateFlow()
    private val _lastSaved = MutableStateFlow<Long?>(null)
    val lastSaved: StateFlow<Long?> = _lastSaved.asStateFlow()

    init {
        viewModelScope.launch { _lastSaved.value = settings.getString(KEY_LAST)?.toLongOrNull() }
    }

    fun save(radiusKm: Int, dark: Boolean) = viewModelScope.launch {
        if (_ui.value is OfflineUi.Saving) return@launch
        val here = currentLocation() ?: run { _ui.value = OfflineUi.NoLocation; return@launch }
        _ui.value = OfflineUi.Saving(0, 1)
        val saved = runCatching {
            OfflineTiles.download(context, here.first, here.second, radiusKm.toDouble(), dark) { done, total -> _ui.value = OfflineUi.Saving(done, total) }
        }.getOrDefault(0)
        if (saved == 0) {
            _ui.value = OfflineUi.Failed
        } else {
            val now = System.currentTimeMillis()
            settings.putString(KEY_LAST, now.toString())
            _lastSaved.value = now
            _ui.value = OfflineUi.Saved(saved)
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun currentLocation(): Pair<Double, Double>? {
        if (!context.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) && !context.hasPermission(Manifest.permission.ACCESS_COARSE_LOCATION)) return null
        val loc = runCatching {
            LocationServices.getFusedLocationProviderClient(context).getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null).await()
        }.getOrNull() ?: return null
        return loc.latitude to loc.longitude
    }

    private companion object {
        const val KEY_LAST = "offline_maps_saved_at"
    }
}

@Composable
fun OfflineMapsScreen(onBack: () -> Unit, viewModel: OfflineMapsViewModel = hiltViewModel()) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val lastSaved by viewModel.lastSaved.collectAsStateWithLifecycle()
    val pro = rememberPro()
    val context = LocalContext.current
    val dark = RaveTheme.isDark
    var radius by rememberSaveable { mutableIntStateOf(3) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok -> if (ok) viewModel.save(radius, dark) }

    RaveScreen(title = stringResource(R.string.offline_title), onBack = onBack) {
        Column(Modifier.padding(horizontal = Dimens.ScreenPadding), verticalArrangement = Arrangement.spacedBy(Dimens.SectionGap)) {
            SectionCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconTile(Icons.Rounded.Map, RaveTheme.colors.tileGreen)
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(R.string.offline_body), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                }
                Spacer(Modifier.height(14.dp))
                Text(stringResource(R.string.offline_radius), style = MaterialTheme.typography.labelLarge)
                Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(2, 3, 5).forEach { r -> PillChip(radius == r, stringResource(R.string.offline_km, r), { radius = r }) }
                }
                Spacer(Modifier.height(14.dp))
                when (val u = ui) {
                    is OfflineUi.Saving -> {
                        LinearProgressIndicator(progress = { u.done / u.total.coerceAtLeast(1).toFloat() }, modifier = Modifier.fillMaxWidth())
                        Text(stringResource(R.string.offline_saving, u.done, u.total), style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 6.dp))
                    }
                    else -> PillButton(
                        stringResource(R.string.offline_save), icon = Icons.Rounded.Download, modifier = Modifier.fillMaxWidth(),
                        enabled = OfflineTiles.supported,
                        onClick = {
                            pro.gate(ProReason.OFFLINE_MAPS) {
                                if (context.hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) viewModel.save(radius, dark)
                                else permission.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                            }
                        },
                    )
                }
                val note = when (val u = ui) {
                    is OfflineUi.Saved -> stringResource(R.string.offline_done, u.tiles)
                    OfflineUi.NoLocation -> stringResource(R.string.offline_no_location)
                    OfflineUi.Failed -> stringResource(R.string.offline_failed)
                    else -> if (!OfflineTiles.supported) stringResource(R.string.offline_unsupported) else null
                }
                note?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp)) }
            }
            lastSaved?.let {
                Text(stringResource(R.string.offline_last, DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(it))),
                    style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
