package com.rave.app.ui.pro

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.RestaurantMenu
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.toRoute
import com.rave.app.R
import com.rave.app.data.billing.PlanOffer
import com.rave.app.data.billing.ProPlan
import com.rave.app.data.billing.ProReason
import com.rave.app.data.billing.ProRepository
import com.rave.app.data.billing.PurchaseEvent
import com.rave.app.ui.components.AppBackground
import com.rave.app.ui.components.PillButton
import com.rave.app.ui.navigation.PaywallRoute
import com.rave.app.ui.theme.Dimens
import com.rave.app.ui.theme.RaveTheme
import com.rave.app.ui.theme.TileColor
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.math.roundToInt

@HiltViewModel
class PaywallViewModel @Inject constructor(savedState: SavedStateHandle, private val pro: ProRepository) : ViewModel() {
    val reason: ProReason = runCatching { ProReason.valueOf(savedState.toRoute<PaywallRoute>().reason) }.getOrDefault(ProReason.GENERAL)
    val offers = pro.offers
    val isPro = pro.isPro
    val events = pro.events
    fun buy(activity: Activity, offer: PlanOffer) = pro.purchase(activity, offer)
    fun restore() = pro.refresh()
}

@Composable
fun PaywallScreen(onClose: () -> Unit, viewModel: PaywallViewModel = hiltViewModel()) {
    val offers by viewModel.offers.collectAsStateWithLifecycle()
    val isPro by viewModel.isPro.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var selected by rememberSaveable { mutableStateOf(ProPlan.YEARLY) }
    val unavailable = stringResource(R.string.pro_store_unavailable)
    val pending = stringResource(R.string.pro_pending)
    val failed = stringResource(R.string.pro_failed)
    val welcome = stringResource(R.string.pro_welcome)

    LaunchedEffect(Unit) {
        viewModel.events.collect { e ->
            when (e) {
                PurchaseEvent.Success -> { Toast.makeText(context, welcome, Toast.LENGTH_LONG).show(); onClose() }
                PurchaseEvent.Pending -> Toast.makeText(context, pending, Toast.LENGTH_LONG).show()
                PurchaseEvent.Failed -> Toast.makeText(context, failed, Toast.LENGTH_LONG).show()
            }
        }
    }

    val offer = offers.first { it.plan == selected }
    val monthly = offers.first { it.plan == ProPlan.MONTHLY }
    val yearly = offers.first { it.plan == ProPlan.YEARLY }
    val savePercent = if (monthly.priceMicros > 0 && yearly.priceMicros > 0) {
        (100 - yearly.priceMicros * 100.0 / (monthly.priceMicros * 12)).roundToInt()
    } else DEFAULT_SAVE_PERCENT

    AppBackground {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, stringResource(R.string.action_close)) }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = viewModel::restore) { Text(stringResource(R.string.pro_restore)) }
            }
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = Dimens.ScreenPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(Modifier.size(72.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.WorkspacePremium, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(40.dp))
                }
                Spacer(Modifier.height(14.dp))
                Text(stringResource(headline(viewModel.reason)), style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center)
                Text(stringResource(R.string.pro_subtitle), style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
                Spacer(Modifier.height(18.dp))
                Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        features().forEach { (icon, tile, text) -> Feature(icon, tile, stringResource(text)) }
                    }
                }
                Spacer(Modifier.height(16.dp))
                if (isPro) {
                    Text(stringResource(R.string.pro_already), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                } else {
                    PlanCard(yearly, selected == ProPlan.YEARLY, stringResource(R.string.pro_plan_yearly),
                        stringResource(R.string.pro_per_year, yearly.price), stringResource(R.string.pro_save, savePercent)) { selected = ProPlan.YEARLY }
                    Spacer(Modifier.height(8.dp))
                    PlanCard(monthly, selected == ProPlan.MONTHLY, stringResource(R.string.pro_plan_monthly),
                        stringResource(R.string.pro_per_month, monthly.price), null) { selected = ProPlan.MONTHLY }
                    Spacer(Modifier.height(8.dp))
                    val lifetime = offers.first { it.plan == ProPlan.LIFETIME }
                    PlanCard(lifetime, selected == ProPlan.LIFETIME, stringResource(R.string.pro_plan_lifetime),
                        stringResource(R.string.pro_once, lifetime.price), stringResource(R.string.pro_no_subscription)) { selected = ProPlan.LIFETIME }
                }
                Spacer(Modifier.height(16.dp))
            }
            if (!isPro) {
                Column(Modifier.padding(horizontal = Dimens.ScreenPadding, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    PillButton(
                        if (offer.trialDays > 0) stringResource(R.string.pro_start_trial, offer.trialDays) else stringResource(R.string.pro_continue),
                        onClick = {
                            val activity = context.findActivity()
                            if (activity == null || !viewModel.buy(activity, offer)) Toast.makeText(context, unavailable, Toast.LENGTH_LONG).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        stringResource(if (offer.plan == ProPlan.LIFETIME) R.string.pro_fine_print_once else R.string.pro_fine_print),
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun Feature(icon: ImageVector, tile: TileColor, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(34.dp).clip(MaterialTheme.shapes.small).background(tile.container), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = tile.content, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun PlanCard(offer: PlanOffer, selected: Boolean, title: String, price: String, tag: String?, onClick: () -> Unit) {
    val border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface, border = border,
        modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.large).clickable(onClick = onClick)) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(22.dp).clip(CircleShape)
                    .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(price, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (offer.trialDays > 0) {
                    Text(stringResource(R.string.pro_trial_days, offer.trialDays), style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary)
                }
            }
            if (tag != null) {
                Text(
                    tag, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.primary).padding(horizontal = 8.dp, vertical = 3.dp),
                )
            }
        }
    }
}

@Composable
private fun features(): List<Triple<ImageVector, TileColor, Int>> {
    val c = RaveTheme.colors
    return listOf(
        Triple(Icons.Rounded.AutoAwesome, c.tilePink, R.string.pro_f_ai),
        Triple(Icons.Rounded.QrCodeScanner, c.tilePeach, R.string.pro_f_scans),
        Triple(Icons.Rounded.RestaurantMenu, c.tileGreen, R.string.pro_f_meals),
        Triple(Icons.Rounded.AcUnit, c.tileBlue, R.string.pro_f_streak),
        Triple(Icons.AutoMirrored.Rounded.ShowChart, c.tilePeach, R.string.pro_f_stats),
        Triple(Icons.Rounded.Palette, c.tilePink, R.string.pro_f_share),
        Triple(Icons.Rounded.Map, c.tileGreen, R.string.pro_f_maps),
        Triple(Icons.Rounded.EmojiEvents, c.tileBlue, R.string.pro_f_comps),
    )
}

private fun headline(reason: ProReason) = when (reason) {
    ProReason.GENERAL -> R.string.pro_h_general
    ProReason.SCANS -> R.string.pro_h_scans
    ProReason.AI_SCANS -> R.string.pro_h_ai
    ProReason.MEAL_PLAN -> R.string.pro_h_meals
    ProReason.STREAK -> R.string.pro_h_streak
    ProReason.STATS -> R.string.pro_h_stats
    ProReason.SHARE -> R.string.pro_h_share
    ProReason.OFFLINE_MAPS -> R.string.pro_h_maps
    ProReason.COMPETITIONS -> R.string.pro_h_comps
}

private const val DEFAULT_SAVE_PERCENT = 54

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
