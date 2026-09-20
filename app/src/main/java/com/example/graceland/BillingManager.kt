package com.example.graceland

import android.app.Activity
import android.content.Context
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** One-time in-app purchase "pro_upgrade" that removes ads. Create it in Play Console. */
class BillingManager(context: Context) : PurchasesUpdatedListener {

    companion object { const val PRO_ID = "pro_upgrade" }

    private val prefs = context.getSharedPreferences("graceland", Context.MODE_PRIVATE)
    private val _isPro = MutableStateFlow(prefs.getBoolean("pro", false))
    val isPro: StateFlow<Boolean> = _isPro.asStateFlow()

    private var productDetails: ProductDetails? = null

    private val client: BillingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
        )
        .build()

    init { connect() }

    private fun connect() {
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    loadProduct()
                    restore()
                }
            }
            override fun onBillingServiceDisconnected() {}
        })
    }

    private fun loadProduct() {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PRO_ID)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                )
            ).build()
        client.queryProductDetailsAsync(params) { _, list ->
            productDetails = list.firstOrNull()
        }
    }

    /** Re-checks existing purchases (restores Pro on a new phone or reinstall). */
    fun restore() {
        client.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP).build()
        ) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                val owned = purchases.any {
                    it.products.contains(PRO_ID) && it.purchaseState == Purchase.PurchaseState.PURCHASED
                }
                purchases.forEach(::handle)
                if (!owned) setPro(false)
            }
        }
    }

    fun launchUpgrade(activity: Activity) {
        val details = productDetails ?: run { connect(); return }
        val flow = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(details).build()
                )
            ).build()
        client.launchBillingFlow(activity, flow)
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        if (result.responseCode == BillingClient.BillingResponseCode.OK) {
            purchases?.forEach(::handle)
        }
    }

    private fun handle(purchase: Purchase) {
        if (!purchase.products.contains(PRO_ID)) return
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return
        if (!purchase.isAcknowledged) {
            client.acknowledgePurchase(
                AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken).build()
            ) { }
        }
        setPro(true)
    }

    private fun setPro(value: Boolean) {
        prefs.edit().putBoolean("pro", value).apply()
        _isPro.value = value
    }
}
