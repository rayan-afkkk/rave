package com.rave.app.ui.settings

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.rave.app.R
import com.rave.app.ui.components.IconTile
import com.rave.app.ui.components.PillButton
import com.rave.app.ui.components.RaveScreen
import com.rave.app.ui.components.SectionCard
import com.rave.app.ui.components.TonalPillButton
import com.rave.app.ui.theme.Dimens
import com.rave.app.ui.theme.RaveTheme

/** Phone brands whose battery savers are known to stop background step counting. */
enum class PhoneBrand(val titleRes: Int, val stepsRes: Int) {
    XIAOMI(R.string.battery_brand_xiaomi, R.string.battery_steps_xiaomi),
    SAMSUNG(R.string.battery_brand_samsung, R.string.battery_steps_samsung),
    OPPO(R.string.battery_brand_oppo, R.string.battery_steps_oppo),
    VIVO(R.string.battery_brand_vivo, R.string.battery_steps_vivo),
    TRANSSION(R.string.battery_brand_transsion, R.string.battery_steps_transsion),
    HUAWEI(R.string.battery_brand_huawei, R.string.battery_steps_huawei),
    OTHER(R.string.battery_brand_other, R.string.battery_steps_other);

    companion object {
        fun current(): PhoneBrand {
            val m = (Build.MANUFACTURER + " " + Build.BRAND).lowercase()
            return when {
                listOf("xiaomi", "redmi", "poco").any { it in m } -> XIAOMI
                "samsung" in m -> SAMSUNG
                listOf("oppo", "realme", "oneplus").any { it in m } -> OPPO
                listOf("vivo", "iqoo").any { it in m } -> VIVO
                listOf("infinix", "tecno", "itel").any { it in m } -> TRANSSION
                listOf("huawei", "honor").any { it in m } -> HUAWEI
                else -> OTHER
            }
        }
    }
}

fun isIgnoringBatteryOptimizations(context: Context): Boolean {
    val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    return pm.isIgnoringBatteryOptimizations(context.packageName)
}

private fun Context.tryStart(vararg intents: Intent): Boolean = intents.any { intent ->
    runCatching { startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess
}

/**
 * Budget phones (Xiaomi, Infinix/Tecno, Oppo, Vivo…) aggressively stop background apps, which
 * makes step counts and reminders miss. This screen detects the brand, shows the exact
 * settings to change, and opens them.
 */
@Composable
fun BatteryGuideScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val brand = remember { PhoneBrand.current() }
    var unrestricted by remember { mutableStateOf(isIgnoringBatteryOptimizations(context)) }
    // Re-check when the user comes back from system settings.
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) unrestricted = isIgnoringBatteryOptimizations(context) }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    val appDetails = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))

    RaveScreen(title = stringResource(R.string.battery_title), onBack = onBack) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = Dimens.ScreenPadding, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(Dimens.SectionGap),
        ) {
            Text(stringResource(R.string.battery_intro), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)

            val tile = if (unrestricted) RaveTheme.colors.tileGreen else RaveTheme.colors.tilePeach
            SectionCard(Modifier.fillMaxWidth(), color = tile.container) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconTile(if (unrestricted) Icons.Rounded.CheckCircle else Icons.Rounded.BatteryAlert, tile)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(if (unrestricted) R.string.battery_ok_title else R.string.battery_bad_title),
                            style = MaterialTheme.typography.titleSmall, color = tile.content)
                        Text(stringResource(if (unrestricted) R.string.battery_ok_body else R.string.battery_bad_body),
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (!unrestricted) {
                    Spacer(Modifier.padding(top = 10.dp))
                    PillButton(stringResource(R.string.battery_open_battery), modifier = Modifier.fillMaxWidth(), onClick = {
                        context.tryStart(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS), appDetails)
                    })
                }
            }

            SectionCard(Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.battery_your_phone, stringResource(brand.titleRes)), style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.padding(top = 6.dp))
                Text(stringResource(brand.stepsRes), style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.padding(top = 12.dp))
                TonalPillButton(stringResource(R.string.battery_open_app_settings), modifier = Modifier.fillMaxWidth(), onClick = {
                    context.tryStart(appDetails)
                })
                if (brand == PhoneBrand.XIAOMI) {
                    Spacer(Modifier.padding(top = 8.dp))
                    TonalPillButton(stringResource(R.string.battery_open_autostart), modifier = Modifier.fillMaxWidth(), onClick = {
                        context.tryStart(
                            Intent().setComponent(ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")),
                            appDetails,
                        )
                    })
                }
            }

            Text(stringResource(R.string.battery_footer), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.padding(bottom = 16.dp))
        }
    }
}
