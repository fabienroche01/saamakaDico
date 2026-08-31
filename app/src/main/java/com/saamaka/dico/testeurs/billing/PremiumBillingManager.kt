package com.saamaka.dico.testeurs.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.Purchase
import java.util.concurrent.CopyOnWriteArraySet

class PremiumBillingManager(context: Context) : AutoCloseable {
    private val store = PremiumEntitlementStore(context.applicationContext)
    private val listeners = CopyOnWriteArraySet<(PremiumEntitlementState) -> Unit>()

    @Volatile
    var state: PremiumEntitlementState =
        PremiumEntitlementPolicy.initialState(store.lastKnownActive())
        private set

    @Volatile
    private var productDetails: ProductDetails? = null

    @Volatile
    private var connectionStarted = false

    private val billingClient = BillingClient.newBuilder(context.applicationContext)
        .setListener { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                processPurchases(purchases.orEmpty())
            } else if (billingResult.responseCode != BillingClient.BillingResponseCode.USER_CANCELED) {
                publishUnavailable(billingResult)
            }
        }
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .enablePrepaidPlans()
                .build()
        )
        .enableAutoServiceReconnection()
        .build()

    fun addListener(listener: (PremiumEntitlementState) -> Unit) {
        listeners += listener
        listener(state)
    }

    fun removeListener(listener: (PremiumEntitlementState) -> Unit) {
        listeners -= listener
    }

    @Synchronized
    fun connect() {
        if (billingClient.isReady) {
            restorePurchases()
            return
        }
        if (connectionStarted) return
        connectionStarted = true
        publish(state.copy(verification = PremiumVerification.CHECKING, message = null))
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                connectionStarted = false
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    publish(state.copy(isBillingConnected = true, message = null))
                    queryPremiumProduct()
                    restorePurchases()
                } else {
                    publishUnavailable(result)
                }
            }

            override fun onBillingServiceDisconnected() {
                connectionStarted = false
                publish(state.copy(isBillingConnected = false))
                // Billing 9 reconnecte automatiquement lors du prochain appel API.
            }
        })
    }

    fun restorePurchases() {
        if (!billingClient.isReady) {
            connect()
            return
        }
        publish(state.copy(verification = PremiumVerification.CHECKING, message = null))
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        billingClient.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                processPurchases(purchases)
            } else {
                publishUnavailable(result)
            }
        }
    }

    fun launchSubscription(activity: Activity): BillingResult? {
        val details = productDetails ?: run {
            queryPremiumProduct()
            return null
        }
        val offerToken = details.subscriptionOfferDetails
            ?.firstOrNull()
            ?.offerToken
            ?: return null
        val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
            .setOfferToken(offerToken)
            .build()
        return billingClient.launchBillingFlow(
            activity,
            BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(listOf(productParams))
                .build()
        )
    }

    private fun queryPremiumProduct() {
        if (!billingClient.isReady) return
        val product = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(PREMIUM_PRODUCT_ID)
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        billingClient.queryProductDetailsAsync(
            QueryProductDetailsParams.newBuilder()
                .setProductList(listOf(product))
                .build()
        ) { result, detailsResult ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                productDetails = detailsResult.productDetailsList
                    .firstOrNull { it.productId == PREMIUM_PRODUCT_ID }
            }
        }
    }

    private fun processPurchases(purchases: List<Purchase>) {
        val premiumPurchases = purchases.filter { PREMIUM_PRODUCT_ID in it.products }
        val verified = PremiumEntitlementPolicy.fromGooglePlay(
            purchases = premiumPurchases.map { purchase ->
                PremiumPurchase(
                    productIds = purchase.products.toSet(),
                    isPurchased = purchase.purchaseState == Purchase.PurchaseState.PURCHASED
                )
            },
            verifiedAtMillis = System.currentTimeMillis()
        )
        publish(verified)
        store.saveVerified(verified)

        premiumPurchases
            .filter {
                it.purchaseState == Purchase.PurchaseState.PURCHASED && !it.isAcknowledged
            }
            .forEach(::acknowledge)
    }

    private fun acknowledge(purchase: Purchase) {
        val params = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        billingClient.acknowledgePurchase(params) { result ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                publish(state.copy(message = result.debugMessage.ifBlank { "Achat non acquitté" }))
            }
        }
    }

    private fun publishUnavailable(result: BillingResult) {
        publish(
            PremiumEntitlementPolicy.unavailable(
                previous = state,
                message = result.debugMessage.ifBlank {
                    "Google Play Billing indisponible (${result.responseCode})"
                }
            )
        )
    }

    private fun publish(newState: PremiumEntitlementState) {
        state = newState
        listeners.forEach { it(newState) }
    }

    override fun close() {
        connectionStarted = false
        billingClient.endConnection()
        publish(state.copy(isBillingConnected = false))
        listeners.clear()
    }
}
