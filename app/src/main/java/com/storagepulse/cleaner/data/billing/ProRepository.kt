package com.storagepulse.cleaner.data.billing

import android.app.Activity
import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.storagepulse.cleaner.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

private val Context.proStore by preferencesDataStore(name = "pro")

sealed interface ProEvent {
    data object Purchased : ProEvent
    data object Pending : ProEvent
    data object Cancelled : ProEvent
    data object Restored : ProEvent
    data object NothingToRestore : ProEvent
    /** Play Billing is not reachable or the product is not published for this build. */
    data object Unavailable : ProEvent
    data class Error(val code: Int) : ProEvent
}

/**
 * StoragePulse Pro: one-time "storagepulse_pro_lifetime" purchase through Google Play Billing.
 * The last known state is cached in DataStore so Pro works offline and widgets can read it.
 * Debug builds also have a local override switch for testing (never in release).
 */
@Singleton
class ProRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) : PurchasesUpdatedListener {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val purchasedKey = booleanPreferencesKey("pro_purchased")
    private val debugKey = booleanPreferencesKey("pro_debug_override")

    val purchased: Flow<Boolean> = context.proStore.data.map { it[purchasedKey] ?: false }
    val debugOverride: Flow<Boolean> = context.proStore.data.map { BuildConfig.DEBUG && (it[debugKey] ?: false) }
    val isPro: Flow<Boolean> = combine(purchased, debugOverride) { p, d -> p || d }

    private val _price = MutableStateFlow<String?>(null)
    /** Localised price from Play, or null until loaded (the UI then shows $2.99). */
    val price: StateFlow<String?> = _price.asStateFlow()

    private val _events = MutableSharedFlow<ProEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<ProEvent> = _events.asSharedFlow()

    private val client: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .build()

    private var details: ProductDetails? = null

    suspend fun setDebugOverride(on: Boolean) {
        if (!BuildConfig.DEBUG) return
        context.proStore.edit { it[debugKey] = on }
    }

    /** Loads the price and re-checks owned purchases. Quiet: no events. */
    fun refreshQuietly() {
        scope.launch {
            if (!connect()) return@launch
            loadDetails()
            syncPurchases()
        }
    }

    /** "Restore Previous Purchase". */
    fun restore() {
        scope.launch {
            if (!connect()) {
                _events.emit(ProEvent.Unavailable)
                return@launch
            }
            _events.emit(
                when (syncPurchases()) {
                    true -> ProEvent.Restored
                    false -> ProEvent.NothingToRestore
                    null -> ProEvent.Unavailable
                },
            )
        }
    }

    /** Opens the Play purchase sheet. */
    suspend fun launchPurchase(activity: Activity) {
        if (!connect()) {
            _events.emit(ProEvent.Unavailable)
            return
        }
        val product = details ?: loadDetails()
        if (product == null) {
            _events.emit(ProEvent.Unavailable)
            return
        }
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(product).build()),
            )
            .build()
        val result = client.launchBillingFlow(activity, params)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) emitFailure(result)
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        scope.launch {
            when (result.responseCode) {
                BillingClient.BillingResponseCode.OK -> {
                    val ours = purchases.orEmpty().filter { PRODUCT_ID in it.products }
                    when {
                        ours.any { it.purchaseState == Purchase.PurchaseState.PURCHASED } -> {
                            ours.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }.forEach { acknowledge(it) }
                            setPurchased(true)
                            _events.emit(ProEvent.Purchased)
                        }
                        ours.any { it.purchaseState == Purchase.PurchaseState.PENDING } -> _events.emit(ProEvent.Pending)
                    }
                }
                BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                    syncPurchases()
                    _events.emit(ProEvent.Restored)
                }
                else -> emitFailure(result)
            }
        }
    }

    // ------------------------------------------------------------------ internals

    private suspend fun emitFailure(result: BillingResult) {
        _events.emit(
            when (result.responseCode) {
                BillingClient.BillingResponseCode.USER_CANCELED -> ProEvent.Cancelled
                BillingClient.BillingResponseCode.BILLING_UNAVAILABLE,
                BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE,
                BillingClient.BillingResponseCode.ITEM_UNAVAILABLE,
                BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED,
                -> ProEvent.Unavailable
                else -> ProEvent.Error(result.responseCode)
            },
        )
    }

    private suspend fun connect(): Boolean {
        if (client.isReady) return true
        return withTimeoutOrNull(10_000) {
            suspendCancellableCoroutine { cont ->
                client.startConnection(object : BillingClientStateListener {
                    override fun onBillingSetupFinished(result: BillingResult) {
                        if (cont.isActive) cont.resume(result.responseCode == BillingClient.BillingResponseCode.OK)
                    }

                    override fun onBillingServiceDisconnected() {
                        if (cont.isActive) cont.resume(false)
                    }
                })
            }
        } ?: false
    }

    private suspend fun loadDetails(): ProductDetails? {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PRODUCT_ID)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build(),
                ),
            )
            .build()
        val found = suspendCancellableCoroutine<ProductDetails?> { cont ->
            client.queryProductDetailsAsync(params) { result, list ->
                val d = if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    list.productDetailsList.firstOrNull { it.productId == PRODUCT_ID }
                } else {
                    null
                }
                if (cont.isActive) cont.resume(d)
            }
        }
        details = found
        found?.oneTimePurchaseOfferDetails?.formattedPrice?.let { _price.value = it }
        return found
    }

    /** true = owns Pro, false = Play says no purchase, null = could not ask Play. */
    private suspend fun syncPurchases(): Boolean? {
        val params = QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.INAPP).build()
        val owned = suspendCancellableCoroutine<List<Purchase>?> { cont ->
            client.queryPurchasesAsync(params) { result, purchases ->
                val r = if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    purchases.filter { PRODUCT_ID in it.products && it.purchaseState == Purchase.PurchaseState.PURCHASED }
                } else {
                    null
                }
                if (cont.isActive) cont.resume(r)
            }
        } ?: return null
        owned.forEach { acknowledge(it) }
        // Only Play's own answer changes the cached state (also handles refunds).
        setPurchased(owned.isNotEmpty())
        return owned.isNotEmpty()
    }

    /** Unacknowledged purchases are refunded by Play after 3 days. */
    private suspend fun acknowledge(purchase: Purchase) {
        if (purchase.isAcknowledged) return
        val params = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
        suspendCancellableCoroutine { cont ->
            client.acknowledgePurchase(params) { if (cont.isActive) cont.resume(Unit) }
        }
    }

    private suspend fun setPurchased(on: Boolean) {
        if (context.proStore.data.first()[purchasedKey] == on) return
        context.proStore.edit { it[purchasedKey] = on }
    }

    companion object {
        const val PRODUCT_ID = "storagepulse_pro_lifetime"
    }
}
