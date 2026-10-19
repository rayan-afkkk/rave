package com.rave.app.ui.pro

import android.app.Activity
import com.rave.app.ui.components.SmoothCornerShape
import android.content.Context
import android.content.ContextWrapper
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.DownloadForOffline
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Healing
import androidx.compose.material.icons.rounded.HideImage
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
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
    val benefits = benefits()
    // The benefit the paywall was opened for goes first and starts expanded.
    val focus = benefitFor(viewModel.reason)
    val ordered = remember(benefits, focus) { benefits.sortedBy { if (it.key == focus) 0 else 1 } }
    var expanded by rememberSaveable { mutableStateOf(focus) }

    Column(
        Modifier.fillMaxSize().background(ink.container).statusBarsPadding().navigationBarsPadding()
            .padding(start = 18.dp, end = 18.dp, top = 4.dp, bottom = 12.dp),
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

        // Scrolling part: headline + every Pro benefit. Plans and the CTA stay pinned below.
        Box(Modifier.weight(1f).fillMaxWidth()) {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item(key = "headline") {
                    Column {
                        Headline(Modifier.padding(top = 6.dp))
                        if (viewModel.reason != ProReason.GENERAL) {
                            Text(
                                stringResource(headline(viewModel.reason)),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = RaveTheme.colors.highlight, modifier = Modifier.padding(top = 10.dp),
                            )
                        }
                        Text(
                            stringResource(R.string.pb_section_title),
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, fontWeight = FontWeight.ExtraBold),
                            color = ink.content, modifier = Modifier.padding(top = 18.dp),
                        )
                        Text(
                            stringResource(R.string.pb_section_sub),
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp), color = RaveTheme.colors.navContent,
                            modifier = Modifier.padding(top = 2.dp, bottom = 4.dp),
                        )
                    }
                }
                items(ordered, key = { it.key }) { b ->
                    BenefitRow(b, expanded = expanded == b.key, onToggle = { expanded = if (expanded == b.key) null else b.key })
                }
            }
            // Soft fade so the list slides under the pinned plans.
            Box(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(20.dp)
                    .background(Brush.verticalGradient(listOf(ink.container.copy(alpha = 0f), ink.container))),
            )
        }

        if (isPro) {
            Text(
                stringResource(R.string.pro_already), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = RaveTheme.colors.highlight, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            )
        } else {
            val lifetime = offers.first { it.plan == ProPlan.LIFETIME }
            Column(Modifier.padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp), height = 50.dp,
            )
            Text(
                stringResource(if (offer.plan == ProPlan.LIFETIME) R.string.pro_fine_print_once else R.string.bplan_fine_print),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp), color = RaveTheme.colors.navContent,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            com.rave.app.ui.legal.LegalAgreementText(
                Modifier.fillMaxWidth().padding(top = 4.dp), lead = R.string.legal_agree_purchase,
                color = RaveTheme.colors.navContent, linkColor = ink.content,
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

/** One Pro benefit: icon tile, title, subtitle and a chevron; tap to expand the free vs Pro details. */
@Composable
private fun BenefitRow(b: Benefit, expanded: Boolean, onToggle: () -> Unit) {
    val c = RaveTheme.colors
    val on = c.tileInk.content
    val rotation by animateFloatAsState(if (expanded) 90f else 0f, label = "chevron")
    val toggleLabel = stringResource(if (expanded) R.string.pb_hide_details else R.string.pb_show_details)
    Column(
        Modifier.fillMaxWidth().clip(BentoTileShape).background(c.inkRaised)
            .clickable(onClickLabel = toggleLabel, onClick = onToggle)
            .padding(horizontal = 12.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(42.dp).clip(SmoothCornerShape(12.dp)).background(b.tile.container),
                contentAlignment = Alignment.Center,
            ) { Icon(b.icon, null, tint = b.tile.content, modifier = Modifier.size(22.dp)) }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(
                    stringResource(b.title),
                    style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 19.sp),
                    color = on,
                )
                Text(
                    stringResource(b.subtitle), maxLines = 1, overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp), color = c.navContent,
                )
            }
            Icon(
                Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = on,
                modifier = Modifier.size(24.dp).rotate(rotation),
            )
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(tween(260)) + fadeIn(tween(260)),
            exit = shrinkVertically(tween(220)) + fadeOut(tween(160)),
        ) {
            Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    stringResource(b.body),
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 20.sp), color = on.copy(alpha = 0.85f),
                )
                CompareLine(stringResource(R.string.pb_label_free), stringResource(b.free), on.copy(alpha = 0.12f), on)
                CompareLine(stringResource(R.string.pb_label_pro), stringResource(b.pro), c.highlight, c.onHighlight)
            }
        }
    }
}

@Composable
private fun CompareLine(label: String, text: String, chip: Color, onChip: Color) {
    Row(verticalAlignment = Alignment.Top) {
        BentoChip(label, container = chip, content = onChip, modifier = Modifier.width(60.dp))
        Text(
            text, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 19.sp),
            color = RaveTheme.colors.tileInk.content, modifier = Modifier.weight(1f).padding(start = 10.dp, top = 4.dp),
        )
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

@Immutable
private data class Benefit(
    val key: String,
    val icon: ImageVector,
    val tile: TileColor,
    @StringRes val title: Int,
    @StringRes val subtitle: Int,
    @StringRes val body: Int,
    @StringRes val free: Int,
    @StringRes val pro: Int,
)

/** Every Rave Pro benefit, matching the gates in the app. */
@Composable
private fun benefits(): List<Benefit> {
    val c = RaveTheme.colors
    return remember(c) {
        listOf(
            Benefit(B_LIVE_MAP, Icons.Rounded.Map, c.tileYellow, R.string.pb_live_map_title, R.string.pb_live_map_sub, R.string.pb_live_map_body, R.string.pb_live_map_free, R.string.pb_live_map_pro),
            Benefit(B_BARCODE, Icons.Rounded.QrCodeScanner, c.tileGreen, R.string.pb_barcode_title, R.string.pb_barcode_sub, R.string.pb_barcode_body, R.string.pb_barcode_free, R.string.pb_barcode_pro),
            Benefit(B_AI, Icons.Rounded.AutoAwesome, c.tilePink, R.string.pb_ai_title, R.string.pb_ai_sub, R.string.pb_ai_body, R.string.pb_ai_free, R.string.pb_ai_pro),
            Benefit(B_MEALS, Icons.Rounded.RestaurantMenu, c.tilePeach, R.string.pb_meals_title, R.string.pb_meals_sub, R.string.pb_meals_body, R.string.pb_meals_free, R.string.pb_meals_pro),
            Benefit(B_FREEZE, Icons.Rounded.AcUnit, c.tileBlue, R.string.pb_freeze_title, R.string.pb_freeze_sub, R.string.pb_freeze_body, R.string.pb_freeze_free, R.string.pb_freeze_pro),
            Benefit(B_REPAIR, Icons.Rounded.Healing, c.tileOrange, R.string.pb_repair_title, R.string.pb_repair_sub, R.string.pb_repair_body, R.string.pb_repair_free, R.string.pb_repair_pro),
            Benefit(B_TRENDS, Icons.Rounded.Insights, c.tileGreen, R.string.pb_trends_title, R.string.pb_trends_sub, R.string.pb_trends_body, R.string.pb_trends_free, R.string.pb_trends_pro),
            Benefit(B_RECORDS, Icons.Rounded.EmojiEvents, c.tileYellow, R.string.pb_records_title, R.string.pb_records_sub, R.string.pb_records_body, R.string.pb_records_free, R.string.pb_records_pro),
            Benefit(B_SPLITS, Icons.Rounded.Timer, c.tileBlue, R.string.pb_splits_title, R.string.pb_splits_sub, R.string.pb_splits_body, R.string.pb_splits_free, R.string.pb_splits_pro),
            Benefit(B_TEMPLATES, Icons.Rounded.Palette, c.tilePink, R.string.pb_templates_title, R.string.pb_templates_sub, R.string.pb_templates_body, R.string.pb_templates_free, R.string.pb_templates_pro),
            Benefit(B_COLOURS, Icons.Rounded.ColorLens, c.tilePeach, R.string.pb_colours_title, R.string.pb_colours_sub, R.string.pb_colours_body, R.string.pb_colours_free, R.string.pb_colours_pro),
            Benefit(B_WATERMARK, Icons.Rounded.HideImage, c.tileSand, R.string.pb_watermark_title, R.string.pb_watermark_sub, R.string.pb_watermark_body, R.string.pb_watermark_free, R.string.pb_watermark_pro),
            Benefit(B_COMPS, Icons.Rounded.Groups, c.tileOrange, R.string.pb_comps_title, R.string.pb_comps_sub, R.string.pb_comps_body, R.string.pb_comps_free, R.string.pb_comps_pro),
            Benefit(B_OFFLINE, Icons.Rounded.DownloadForOffline, c.tileGreen, R.string.pb_offline_title, R.string.pb_offline_sub, R.string.pb_offline_body, R.string.pb_offline_free, R.string.pb_offline_pro),
        )
    }
}

/** Which benefit a paywall opened for [reason] should lead with (null = none). */
private fun benefitFor(reason: ProReason): String? = when (reason) {
    ProReason.GENERAL -> null
    ProReason.SCANS -> B_BARCODE
    ProReason.AI_SCANS -> B_AI
    ProReason.MEAL_PLAN -> B_MEALS
    ProReason.STREAK -> B_REPAIR
    ProReason.STATS -> B_TRENDS
    ProReason.SHARE -> B_TEMPLATES
    ProReason.OFFLINE_MAPS -> B_OFFLINE
    ProReason.COMPETITIONS -> B_COMPS
    ProReason.LIVE_MAP -> B_LIVE_MAP
}

private const val B_LIVE_MAP = "live_map"
private const val B_BARCODE = "barcode"
private const val B_AI = "ai"
private const val B_MEALS = "meals"
private const val B_FREEZE = "freeze"
private const val B_REPAIR = "repair"
private const val B_TRENDS = "trends"
private const val B_RECORDS = "records"
private const val B_SPLITS = "splits"
private const val B_TEMPLATES = "templates"
private const val B_COLOURS = "colours"
private const val B_WATERMARK = "watermark"
private const val B_COMPS = "comps"
private const val B_OFFLINE = "offline"

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
    ProReason.LIVE_MAP -> R.string.pb_h_live_map
}

private const val DEFAULT_SAVE_PERCENT = 42

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
