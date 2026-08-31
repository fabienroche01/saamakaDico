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
    private var availableOffers: List<PremiumOffer> = emptyList()

    @Volatile
    private var connectionStarted = false

    private val billingClient = BillingClient.newBuilder(context.applicationContext)
        .setListener { billingResult, purchases ->
            when (billingResult.responseCode) {
                BillingClient.BillingResponseCode.OK -> processPurchases(purchases.orEmpty())
                BillingClient.BillingResponseCode.USER_CANCELED ->
                    publish(state.copy(message = "Achat annulé"))
                BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> restorePurchases()
                else -> publishUnavailable(billingResult)
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
        publish(
            state.copy(
                isPremium = false,
                verification = PremiumVerification.CHECKING,
                message = null
            )
        )
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
                publish(
                    state.copy(
                        isPremium = false,
                        verification = PremiumVerification.UNAVAILABLE,
                        isBillingConnected = false,
                        message = "Connexion à Google Play interrompue"
                    )
                )
                // Billing 9 reconnecte automatiquement lors du prochain appel API.
            }
        })
    }

    fun restorePurchases() {
        if (!billingClient.isReady) {
            connect()
            return
        }
        publish(
            state.copy(
                isPremium = false,
                verification = PremiumVerification.CHECKING,
                message = null
            )
        )
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

    fun availablePlans(): Set<PremiumPlan> = state.availablePlans.keys

    fun launchSubscription(activity: Activity, plan: PremiumPlan): BillingResult? {
        val details = productDetails ?: run {
            queryPremiumProduct()
            return null
        }
        val offer = PremiumOfferSelector.select(plan, availableOffers)
            ?: return null
        val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
            .setOfferToken(offer.offerToken)
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
                availableOffers = productDetails
                    ?.subscriptionOfferDetails
                    .orEmpty()
                    .map { offer ->
                        PremiumOffer(
                            productId = PREMIUM_PRODUCT_ID,
                            basePlanId = offer.basePlanId,
                            offerId = offer.offerId,
                            offerToken = offer.offerToken,
                            localizedPrice = offer.pricingPhases.pricingPhaseList
                                .lastOrNull { it.priceAmountMicros > 0L }
                                ?.formattedPrice
                                .orEmpty()
                        )
                    }
                publish(
                    state.copy(
                        availablePlans = PremiumOfferSelector.planDetails(availableOffers),
                        message = null
                    )
                )
            } else {
                publish(state.copy(message = result.debugMessage))
            }
        }
    }

    private fun processPurchases(purchases: List<Purchase>) {
        val premiumPurchases = purchases.filter { PREMIUM_PRODUCT_ID in it.products }
        val verified = PremiumEntitlementPolicy.fromGooglePlay(
            purchases = premiumPurchases.map { purchase ->
                PremiumPurchase(
                    productIds = purchase.products.toSet(),
                    status = when (purchase.purchaseState) {
                        Purchase.PurchaseState.PURCHASED -> PremiumPurchaseStatus.PURCHASED
                        Purchase.PurchaseState.PENDING -> PremiumPurchaseStatus.PENDING
                        else -> PremiumPurchaseStatus.OTHER
                    }
                )
            },
            verifiedAtMillis = System.currentTimeMillis()
        ).copy(availablePlans = state.availablePlans)
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
        productDetails = null
        availableOffers = emptyList()
        billingClient.endConnection()
        publish(state.copy(isBillingConnected = false))
        listeners.clear()
    }
}
