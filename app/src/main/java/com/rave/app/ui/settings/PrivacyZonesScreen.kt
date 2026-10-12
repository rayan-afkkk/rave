package com.rave.app.ui.settings

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState
import com.rave.app.BuildConfig
import com.rave.app.R
import com.rave.app.data.local.entity.PrivacyZoneEntity
import com.rave.app.data.preferences.SettingsRepository
import com.rave.app.data.repository.PrivacyZoneRepository
import com.rave.app.ui.components.PillButton
import com.rave.app.ui.components.RaveScreen
import com.rave.app.ui.components.SectionCard
import com.rave.app.ui.permissions.hasPermission
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import kotlin.math.roundToInt

@HiltViewModel
class PrivacyZonesViewModel @Inject constructor(
    private val zones: PrivacyZoneRepository,
    private val settings: SettingsRepository,
) : ViewModel() {
    val list: StateFlow<List<PrivacyZoneEntity>> = zones.observeZones().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val hideEnds: StateFlow<Boolean> = settings.settings.map { it.hideStartEnd }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    fun setHideEnds(on: Boolean) = viewModelScope.launch { settings.update { it.copy(hideStartEnd = on) } }
    fun add(lat: Double, lng: Double, radius: Double, label: String) = viewModelScope.launch {
        zones.save(PrivacyZoneEntity(lat = lat, lng = lng, radiusM = radius, label = label))
    }
    fun delete(id: Long) = viewModelScope.launch { zones.delete(id) }
}

@SuppressLint("MissingPermission")
private suspend fun currentLocation(context: Context): LatLng? {
    if (!context.hasPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) &&
        !context.hasPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION)) return null
    val loc = runCatching {
        LocationServices.getFusedLocationProviderClient(context).getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null).await()
    }.getOrNull() ?: return null
    return LatLng(loc.latitude, loc.longitude)
}

@Composable
fun PrivacyZonesScreen(onBack: () -> Unit, viewModel: PrivacyZonesViewModel = hiltViewModel()) {
    val zones by viewModel.list.collectAsStateWithLifecycle()
    val hideEnds by viewModel.hideEnds.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingLat by rememberSaveable { mutableStateOf<Double?>(null) }
    var pendingLng by rememberSaveable { mutableStateOf<Double?>(null) }
    var radius by rememberSaveable { mutableStateOf(300f) }
    val defaultLabel = stringResource(R.string.privacy_home)
    var label by rememberSaveable { mutableStateOf(defaultLabel) }
    val camera = rememberCameraPositionState { position = CameraPosition.fromLatLngZoom(LatLng(0.0, 0.0), 2f) }

    RaveScreen(title = stringResource(R.string.privacy_title), onBack = onBack) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.privacy_hide_ends), style = MaterialTheme.typography.titleSmall)
                        Text(stringResource(R.string.privacy_hide_ends_help), style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = hideEnds, onCheckedChange = { viewModel.setHideEnds(it) })
                }
            }

            Text(stringResource(R.string.privacy_add_help), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Box(Modifier.fillMaxWidth().height(260.dp).clip(MaterialTheme.shapes.large).background(MaterialTheme.colorScheme.surfaceVariant)) {
                if (BuildConfig.MAPS_API_KEY.isNotBlank()) {
                    val zoneColor = MaterialTheme.colorScheme.primary
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = camera,
                        uiSettings = MapUiSettings(zoomControlsEnabled = false, mapToolbarEnabled = false),
                        onMapClick = { pendingLat = it.latitude; pendingLng = it.longitude },
                    ) {
                        zones.forEach {
                            Circle(LatLng(it.lat, it.lng), radius = it.radiusM, fillColor = zoneColor.copy(alpha = 0.18f),
                                strokeColor = zoneColor, strokeWidth = 3f)
                        }
                        val lat = pendingLat
                        val lng = pendingLng
                        if (lat != null && lng != null) {
                            Circle(LatLng(lat, lng), radius = radius.toDouble(), fillColor = zoneColor.copy(alpha = 0.3f),
                                strokeColor = zoneColor, strokeWidth = 4f)
                        }
                    }
                } else {
                    Text(stringResource(R.string.tracking_no_maps_key), modifier = Modifier.align(Alignment.Center).padding(16.dp),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            TextButton(onClick = {
                scope.launch {
                    currentLocation(context)?.let { here ->
                        pendingLat = here.latitude
                        pendingLng = here.longitude
                        runCatching { camera.animate(CameraUpdateFactory.newLatLngZoom(here, 15f)) }
                    }
                }
            }) {
                Icon(Icons.Rounded.MyLocation, null)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.privacy_use_location))
            }

            if (pendingLat != null) {
                SectionCard {
                    OutlinedTextField(value = label, onValueChange = { label = it.take(24) }, label = { Text(stringResource(R.string.privacy_label)) },
                        singleLine = true, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium)
                    Spacer(Modifier.height(10.dp))
                    Text(stringResource(R.string.privacy_radius, radius.roundToInt()), style = MaterialTheme.typography.labelLarge)
                    Slider(value = radius, onValueChange = { radius = it }, valueRange = 100f..1000f, steps = 8)
                    PillButton(stringResource(R.string.privacy_save_zone), onClick = {
                        viewModel.add(pendingLat!!, pendingLng!!, radius.toDouble(), label)
                        pendingLat = null
                        pendingLng = null
                    }, modifier = Modifier.fillMaxWidth())
                }
            }

            zones.forEach { z ->
                SectionCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Shield, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(z.label, style = MaterialTheme.typography.titleSmall)
                            Text(stringResource(R.string.privacy_radius, z.radiusM.roundToInt()), style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { viewModel.delete(z.id) }) {
                            Icon(Icons.Rounded.Delete, contentDescription = stringResource(R.string.action_delete))
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

