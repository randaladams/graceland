package com.adamselite.gracelandmiles

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import com.android.billingclient.api.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

class BillingManager(context: Context) : PurchasesUpdatedListener {

    companion object {
        const val PRO_ID = "pro_upgrade"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences("graceland_prefs", Context.MODE_PRIVATE)

    private val _isPro = MutableStateFlow(prefs.getBoolean("is_pro", false))
    val isPro: StateFlow<Boolean> = _isPro.asStateFlow()

    // One-off diagnostic messages, shown as Toasts from MainActivity
    private val _toastEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val toastEvents = _toastEvents.asSharedFlow()

    private var productDetails: ProductDetails? = null

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
        )
        .build()

    fun connect() {
        if (billingClient.isReady) {
            loadProduct()
            return
        }
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    _toastEvents.tryEmit("Billing connected OK")
                    loadProduct()
                    restore()
                } else {
                    _toastEvents.tryEmit(
                        "Billing setup FAILED: code=${result.responseCode} msg=${result.debugMessage}"
                    )
                }
            }

            override fun onBillingServiceDisconnected() {
                _toastEvents.tryEmit("Billing service disconnected")
            }
        })
    }

    fun loadProduct() {
        val product = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(PRO_ID)
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(listOf(product))
            .build()

        billingClient.queryProductDetailsAsync(params) { result, queryResult ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                _toastEvents.tryEmit(
                    "Product query FAILED: code=${result.responseCode} msg=${result.debugMessage}"
                )
                return@queryProductDetailsAsync
            }
            val list = queryResult.productDetailsList
            if (list.isEmpty()) {
                _toastEvents.tryEmit("Product query OK but list is EMPTY (product id '$PRO_ID' not found for this build/account)")
            } else {
                productDetails = list.firstOrNull()
                _toastEvents.tryEmit("Product loaded: ${productDetails?.productId} / ${productDetails?.oneTimePurchaseOfferDetails?.formattedPrice}")
            }
        }
    }

    fun launchUpgrade(activity: Activity) {
        val details = productDetails
        if (details == null) {
            _toastEvents.tryEmit("Tap ignored: product details not loaded yet")
            return
        }

        val offerToken = details.oneTimePurchaseOfferDetails?.offerToken
        if (offerToken == null) {
            _toastEvents.tryEmit("Tap ignored: no purchase offer token on product")
            return
        }

        val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
            .setOfferToken(offerToken)
            .build()

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productParams))
            .build()

        val result = billingClient.launchBillingFlow(activity, flowParams)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            _toastEvents.tryEmit(
                "launchBillingFlow FAILED: code=${result.responseCode} msg=${result.debugMessage}"
            )
        } else {
            _toastEvents.tryEmit("Purchase flow launched")
        }
    }

    fun restore() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        billingClient.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                purchases.forEach { handle(it) }
            }
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { handle(it) }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                _toastEvents.tryEmit("Purchase canceled by user")
            }
            else -> {
                _toastEvents.tryEmit(
                    "Purchase update error: code=${result.responseCode} msg=${result.debugMessage}"
                )
            }
        }
    }

    private fun handle(purchase: Purchase) {
        if (purchase.products.contains(PRO_ID) &&
            purchase.purchaseState == Purchase.PurchaseState.PURCHASED
        ) {
            _isPro.value = true
            prefs.edit().putBoolean("is_pro", true).apply()

            if (!purchase.isAcknowledged) {
                val ackParams = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                billingClient.acknowledgePurchase(ackParams) { ackResult ->
                    if (ackResult.responseCode == BillingClient.BillingResponseCode.OK) {
                        _toastEvents.tryEmit("Purchase acknowledged - Pro unlocked!")
                    } else {
                        _toastEvents.tryEmit("Acknowledge FAILED: ${ackResult.debugMessage}")
                    }
                }
            }
        }
    }
}
