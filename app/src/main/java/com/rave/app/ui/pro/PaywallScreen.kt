package com.rave.app.ui.pro

import android.app.Activity
import com.rave.app.ui.components.SmoothCornerShape
import android.content.Context
import android.content.ContextWrapper
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.RestaurantMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.sp
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
import com.rave.app.ui.components.BentoButton
import com.rave.app.ui.components.BentoChip
import com.rave.app.ui.components.BentoGap
import com.rave.app.ui.components.BentoPillShape
import com.rave.app.ui.components.BentoTileShape
import com.rave.app.ui.navigation.PaywallRoute
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

    val ink = RaveTheme.colors.tileInk
    Column(
        Modifier.fillMaxSize().background(ink.container).statusBarsPadding().navigationBarsPadding()
            .verticalScroll(rememberScrollState()).padding(start = 18.dp, end = 18.dp, top = 4.dp, bottom = 16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose, modifier = Modifier.offset(x = (-11).dp)) {
                Icon(Icons.Rounded.Close, stringResource(R.string.action_close), tint = ink.content, modifier = Modifier.size(26.dp))
            }
            Spacer(Modifier.weight(1f))
            Text(
                stringResource(R.string.pro_restore),
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
                color = RaveTheme.colors.navContent,
                modifier = Modifier.clip(BentoPillShape).clickable(onClick = viewModel::restore).padding(vertical = 8.dp, horizontal = 4.dp),
            )
        }
        Headline(Modifier.padding(top = 14.dp))
        if (viewModel.reason != ProReason.GENERAL) {
            Text(
                stringResource(headline(viewModel.reason)),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = RaveTheme.colors.highlight, modifier = Modifier.padding(top = 10.dp),
            )
        }
        FeatureGrid(Modifier.padding(top = 16.dp))
        Spacer(Modifier.height(14.dp))
        if (isPro) {
            Text(
                stringResource(R.string.pro_already), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = RaveTheme.colors.highlight, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            )
        } else {
            val lifetime = offers.first { it.plan == ProPlan.LIFETIME }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PlanCard(yearly, selected == ProPlan.YEARLY, stringResource(R.string.pro_plan_yearly),
                    stringResource(R.string.bplan_per_year, yearly.price), stringResource(R.string.pro_save, savePercent), tagHighlight = true) { selected = ProPlan.YEARLY }
                PlanCard(monthly, selected == ProPlan.MONTHLY, stringResource(R.string.pro_plan_monthly),
                    stringResource(R.string.bplan_per_month, monthly.price), null, tagHighlight = false) { selected = ProPlan.MONTHLY }
                PlanCard(lifetime, selected == ProPlan.LIFETIME, stringResource(R.string.pro_plan_lifetime),
                    stringResource(R.string.pro_once, lifetime.price), stringResource(R.string.bplan_forever), tagHighlight = false) { selected = ProPlan.LIFETIME }
            }
            BentoButton(
                if (offer.trialDays > 0) stringResource(R.string.bplan_start_trial) else stringResource(R.string.pro_continue),
                container = RaveTheme.colors.highlight, content = RaveTheme.colors.onHighlight,
                onClick = {
                    val activity = context.findActivity()
                    if (activity == null || !viewModel.buy(activity, offer)) Toast.makeText(context, unavailable, Toast.LENGTH_LONG).show()
                },
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp), height = 50.dp,
            )
            Text(
                stringResource(if (offer.plan == ProPlan.LIFETIME) R.string.pro_fine_print_once else R.string.bplan_fine_print),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp), color = RaveTheme.colors.navContent,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
        }
    }
}

/** "Go [Pro] / go further." with "Pro" in a lime rounded box. */
@Composable
private fun Headline(modifier: Modifier = Modifier) {
    val ink = RaveTheme.colors.tileInk
    val style = MaterialTheme.typography.displayMedium.copy(
        fontSize = 36.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-1.5).sp, lineHeight = 38.sp,
    )
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.bplan_pro_go), style = style, color = ink.content, maxLines = 1)
            Spacer(Modifier.width(12.dp))
            Text(
                stringResource(R.string.bplan_pro_word), style = style, color = RaveTheme.colors.onHighlight, maxLines = 1,
                modifier = Modifier.clip(SmoothCornerShape(12.dp)).background(RaveTheme.colors.highlight).padding(horizontal = 10.dp),
            )
        }
        Text(stringResource(R.string.bplan_pro_further), style = style, color = ink.content)
    }
}

/** 2-column grid of colour feature tiles. */
@Composable
private fun FeatureGrid(modifier: Modifier = Modifier) {
    val items = features()
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(BentoGap)) {
        items.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(BentoGap)) {
                row.forEach { (icon, tile, text) ->
                    Column(
                        Modifier.weight(1f).fillMaxHeight().clip(BentoTileShape).background(tile.container).padding(12.dp),
                    ) {
                        Icon(icon, null, tint = tile.content, modifier = Modifier.size(26.dp))
                        Spacer(Modifier.weight(1f))
                        Text(
                            stringResource(text),
                            style = MaterialTheme.typography.titleSmall.copy(fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 17.sp),
                            color = tile.content, modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                }
            }
        }
    }
}

/** One plan row: white with a lime border when selected, raised ink otherwise. */
@Composable
private fun PlanCard(offer: PlanOffer, selected: Boolean, title: String, price: String, tag: String?, tagHighlight: Boolean, onClick: () -> Unit) {
    val c = RaveTheme.colors
    val bg = if (selected) c.tileInk.content else c.inkRaised
    val fg = if (selected) c.tileInk.container else c.tileInk.content
    Row(
        Modifier.fillMaxWidth().clip(BentoTileShape).background(bg)
            .then(if (selected) Modifier.border(3.dp, c.highlight, BentoTileShape) else Modifier)
            .clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, fontWeight = FontWeight.ExtraBold), color = fg)
            Text(
                if (offer.trialDays > 0) stringResource(R.string.bplan_with_trial, price, offer.trialDays) else price,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp), color = fg.copy(alpha = 0.7f),
            )
        }
        if (tag != null) {
            Spacer(Modifier.width(8.dp))
            BentoChip(
                tag,
                container = if (tagHighlight) c.highlight else fg.copy(alpha = 0.1f),
                content = if (tagHighlight) c.onHighlight else fg,
            )
        }
    }
}

@Composable
private fun features(): List<Triple<ImageVector, TileColor, Int>> {
    val c = RaveTheme.colors
    return listOf(
        Triple(Icons.Rounded.AutoAwesome, c.tilePink, R.string.bplan_f_ai),
        Triple(Icons.Rounded.RestaurantMenu, c.tileGreen, R.string.bplan_f_meals),
        Triple(Icons.Rounded.AcUnit, c.tileBlue, R.string.bplan_f_streak),
        Triple(Icons.Rounded.Insights, c.tilePeach, R.string.bplan_f_stats),
        Triple(Icons.Rounded.Palette, c.tileOrange, R.string.bplan_f_share),
        Triple(Icons.Rounded.Map, c.tileYellow, R.string.bplan_f_maps),
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
