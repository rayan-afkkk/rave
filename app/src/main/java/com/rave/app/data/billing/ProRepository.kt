package com.rave.app.data.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import com.rave.app.core.ApplicationScope
import com.rave.app.data.preferences.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/** Rave Pro plans. Product ids must match the ones created in the Play Console. */
enum class ProPlan(val productId: String, val type: String, val fallbackPrice: String) {
    MONTHLY("rave_pro_monthly", ProductType.SUBS, "$1.99"),
    YEARLY("rave_pro_yearly", ProductType.SUBS, "$9.99"),
    LIFETIME("rave_pro_lifetime", ProductType.INAPP, "$25.99"),
}

/** A plan as the store sells it in the user's country (local currency, trial if eligible). */
data class PlanOffer(
    val plan: ProPlan,
    val price: String,
    val priceMicros: Long,
    val trialDays: Int,
    internal val details: ProductDetails?,
    internal val offerToken: String?,
)

/** What a paywall was opened for; shown as its headline. */
enum class ProReason { GENERAL, SCANS, AI_SCANS, MEAL_PLAN, STREAK, STATS, SHARE, OFFLINE_MAPS, COMPETITIONS }

/**
 * Rave Pro state from Google Play Billing. Pro = an active monthly/yearly subscription or the
 * lifetime purchase. The last known state is cached so Pro works offline and on cold start.
 */
@Singleton
class ProRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: SettingsRepository,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private val _isPro = MutableStateFlow(false)
    val isPro: StateFlow<Boolean> = _isPro.asStateFlow()

    private val _offers = MutableStateFlow(ProPlan.entries.map { PlanOffer(it, it.fallbackPrice, 0, 0, null, null) })
    val offers: StateFlow<List<PlanOffer>> = _offers.asStateFlow()

    /** Ask the app to show the paywall (collected in RaveApp). */
    private val _paywall = MutableSharedFlow<ProReason>(extraBufferCapacity = 1)
    val paywallRequests: SharedFlow<ProReason> = _paywall.asSharedFlow()

    /** One-off purchase results for the paywall to react to. */
    private val _events = MutableSharedFlow<PurchaseEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<PurchaseEvent> = _events.asSharedFlow()

    private val client: BillingClient = BillingClient.newBuilder(context)
        .setListener { result, purchases -> scope.launch { onPurchases(result, purchases) } }
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .build()

    init {
        scope.launch {
            _isPro.value = settings.getString(KEY_CACHED) == "1"
            connect()
        }
    }

    fun requestPaywall(reason: ProReason) { _paywall.tryEmit(reason) }

    /** Opens Google Play's purchase sheet for [offer]. */
    fun purchase(activity: Activity, offer: PlanOffer): Boolean {
        val details = offer.details ?: return false
        val product = BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(details)
            .apply { offer.offerToken?.let { setOfferToken(it) } }.build()
        val params = BillingFlowParams.newBuilder().setProductDetailsParamsList(listOf(product)).build()
        return client.launchBillingFlow(activity, params).responseCode == BillingClient.BillingResponseCode.OK
    }

    /** Re-reads purchases from Play (the "Restore purchases" button). */
    fun refresh() = scope.launch { if (client.isReady) syncPurchases() else connect() }

    private fun connect() {
        if (client.isReady) return
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) scope.launch {
                    loadOffers()
                    syncPurchases()
                }
            }

            override fun onBillingServiceDisconnected() { /* reconnects on the next refresh() */ }
        })
    }

    private suspend fun loadOffers() {
        val loaded = mutableMapOf<ProPlan, PlanOffer>()
        for (type in listOf(ProductType.SUBS, ProductType.INAPP)) {
            val plans = ProPlan.entries.filter { it.type == type }
            val params = QueryProductDetailsParams.newBuilder().setProductList(plans.map {
                QueryProductDetailsParams.Product.newBuilder().setProductId(it.productId).setProductType(type).build()
            }).build()
            val result = client.queryProductDetails(params)
            for (details in result.productDetailsList.orEmpty()) {
                val plan = plans.firstOrNull { it.productId == details.productId } ?: continue
                loaded[plan] = toOffer(plan, details)
            }
        }
        _offers.value = ProPlan.entries.map { loaded[it] ?: PlanOffer(it, it.fallbackPrice, 0, 0, null, null) }
    }

    private fun toOffer(plan: ProPlan, details: ProductDetails): PlanOffer {
        if (plan.type == ProductType.INAPP) {
            val o = details.oneTimePurchaseOfferDetails
            return PlanOffer(plan, o?.formattedPrice ?: plan.fallbackPrice, o?.priceAmountMicros ?: 0, 0, details, null)
        }
        // Play only returns offers the user is eligible for; prefer one with a free trial.
        val offers = details.subscriptionOfferDetails.orEmpty()
        val best = offers.maxByOrNull { o -> o.pricingPhases.pricingPhaseList.count { it.priceAmountMicros == 0L } } ?: offers.firstOrNull()
        val phases = best?.pricingPhases?.pricingPhaseList.orEmpty()
        val base = phases.lastOrNull()
        val trial = phases.firstOrNull { it.priceAmountMicros == 0L }
        return PlanOffer(
            plan, base?.formattedPrice ?: plan.fallbackPrice, base?.priceAmountMicros ?: 0,
            trial?.let { trialDays(it.billingPeriod) } ?: 0, details, best?.offerToken,
        )
    }

    private suspend fun syncPurchases() {
        val subs = client.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(ProductType.SUBS).build())
        val inapp = client.queryPurchasesAsync(QueryPurchasesParams.newBuilder().setProductType(ProductType.INAPP).build())
        if (subs.billingResult.responseCode != BillingClient.BillingResponseCode.OK ||
            inapp.billingResult.responseCode != BillingClient.BillingResponseCode.OK
        ) return // keep the cached state rather than locking someone out on a Play hiccup
        val all = subs.purchasesList + inapp.purchasesList
        for (p in all) acknowledge(p)
        setPro(all.any { it.isPro() })
    }

    private suspend fun onPurchases(result: BillingResult, purchases: List<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                for (p in purchases.orEmpty()) acknowledge(p)
                if (purchases.orEmpty().any { it.isPro() }) {
                    setPro(true)
                    _events.tryEmit(PurchaseEvent.Success)
                } else if (purchases.orEmpty().any { it.purchaseState == Purchase.PurchaseState.PENDING }) {
                    _events.tryEmit(PurchaseEvent.Pending)
                }
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> syncPurchases().also { _events.tryEmit(PurchaseEvent.Success) }
            BillingClient.BillingResponseCode.USER_CANCELED -> Unit
            else -> _events.tryEmit(PurchaseEvent.Failed)
        }
    }

    private suspend fun acknowledge(p: Purchase) {
        if (p.purchaseState == Purchase.PurchaseState.PURCHASED && !p.isAcknowledged) {
            client.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(p.purchaseToken).build())
        }
    }

    private fun Purchase.isPro() = purchaseState == Purchase.PurchaseState.PURCHASED &&
        products.any { id -> ProPlan.entries.any { it.productId == id } }

    private suspend fun setPro(pro: Boolean) {
        _isPro.value = pro
        settings.putString(KEY_CACHED, if (pro) "1" else null)
    }

    private companion object {
        const val KEY_CACHED = "pro_cached"

        /** ISO-8601 period like P7D / P1W / P1M → days. */
        fun trialDays(period: String): Int {
            val m = Regex("P(\\d+)([DWM])").find(period) ?: return 0
            val n = m.groupValues[1].toInt()
            return when (m.groupValues[2]) { "W" -> n * 7; "M" -> n * 30; else -> n }
        }
    }
}

sealed interface PurchaseEvent {
    data object Success : PurchaseEvent
    data object Pending : PurchaseEvent
    data object Failed : PurchaseEvent
}
