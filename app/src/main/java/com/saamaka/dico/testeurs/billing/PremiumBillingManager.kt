package com.saamaka.dico.testeurs.billing

import android.app.Activity
import android.content.Context
import android.util.Log
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
        Log.d(TAG, "connect ready=${billingClient.isReady} state=${billingClient.connectionState}")
        if (billingClient.isReady) {
            queryPremiumProduct()
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
                logResult("setup", result)
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
                Log.w(TAG, "service disconnected; automatic reconnection enabled")
                publish(
                    state.copy(
                        isPremium = false,
                        verification = PremiumVerification.UNAVAILABLE,
                        isBillingConnected = false,
                        isLoadingPlans = false,
                        plansMessage = if (state.wasPremiumLastKnown) {
                            "Connexion à Google Play interrompue. Reconnectez-vous pour vérifier votre abonnement Premium."
                        } else {
                            "Connexion à Google Play interrompue."
                        },
                        message = if (state.wasPremiumLastKnown) {
                            "Impossible de vérifier Premium hors ligne. Reconnectez-vous à Google Play."
                        } else {
                            "Connexion à Google Play interrompue"
                        }
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

    fun refreshProductDetails() {
        productDetails = null
        availableOffers = emptyList()
        publish(
            state.copy(
                availablePlans = emptyMap(),
                isLoadingPlans = true,
                plansMessage = null
            )
        )
        if (billingClient.isReady) queryPremiumProduct() else connect()
    }

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
        if (!billingClient.isReady) {
            Log.w(TAG, "product query deferred: BillingClient not ready")
            publish(
                state.copy(
                    isLoadingPlans = false,
                    plansMessage = "Google Play Billing n’est pas connecté."
                )
            )
            return
        }
        publish(state.copy(isLoadingPlans = true, plansMessage = null))
        Log.d(TAG, "query product=$PREMIUM_PRODUCT_ID type=SUBS")
        val product = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(PREMIUM_PRODUCT_ID)
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        billingClient.queryProductDetailsAsync(
            QueryProductDetailsParams.newBuilder()
                .setProductList(listOf(product))
                .build()
        ) { result, detailsResult ->
            logResult("queryProductDetails", result)
            Log.d(
                TAG,
                "queryProductDetails products=${detailsResult.productDetailsList.size} " +
                    "unfetched=${detailsResult.unfetchedProductList.size}"
            )
            detailsResult.unfetchedProductList.forEach { unfetched ->
                Log.w(
                    TAG,
                    "unfetched product=${unfetched.productId} type=${unfetched.productType} " +
                        "status=${unfetched.statusCode}"
                )
            }
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                productDetails = detailsResult.productDetailsList
                    .firstOrNull { it.productId == PREMIUM_PRODUCT_ID }
                productDetails?.let { details ->
                    Log.d(
                        TAG,
                        "product returned id=${details.productId} type=${details.productType} " +
                            "offers=${details.subscriptionOfferDetails.orEmpty().size}"
                    )
                    details.subscriptionOfferDetails.orEmpty().forEach { offer ->
                        val phases = offer.pricingPhases.pricingPhaseList.joinToString { phase ->
                            "${phase.billingPeriod}:${phase.formattedPrice}:${phase.priceAmountMicros}"
                        }
                        Log.d(
                            TAG,
                            "offer basePlan=${offer.basePlanId} offerId=${offer.offerId ?: "base"} " +
                                "tokenPresent=${offer.offerToken.isNotBlank()} phases=[$phases]"
                        )
                    }
                }
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
                val plans = PremiumOfferSelector.planDetails(availableOffers)
                val selectedMonthly = PremiumOfferSelector.select(PremiumPlan.MONTHLY, availableOffers)
                val selectedAnnual = PremiumOfferSelector.select(PremiumPlan.ANNUAL, availableOffers)
                Log.d(
                    TAG,
                    "selected monthlyBase=${selectedMonthly?.basePlanId} " +
                        "monthlyToken=${selectedMonthly?.offerToken?.isNotBlank() == true} " +
                        "annualBase=${selectedAnnual?.basePlanId} annualOffer=${selectedAnnual?.offerId ?: "base"} " +
                        "annualToken=${selectedAnnual?.offerToken?.isNotBlank() == true}"
                )
                val missingPlans = PremiumPlan.entries.filterNot(plans::containsKey)
                val unfetched = detailsResult.unfetchedProductList.firstOrNull {
                    it.productId == PREMIUM_PRODUCT_ID
                }
                val plansMessage = when {
                    productDetails == null && unfetched != null ->
                        "Google Play n’a pas pu récupérer l’abonnement (code ${unfetched.statusCode})."
                    productDetails == null ->
                        "L’abonnement DicoSaam Premium n’est pas disponible pour ce compte Google Play."
                    missingPlans.isNotEmpty() ->
                        "Forfait${if (missingPlans.size > 1) "s" else ""} Google Play indisponible${if (missingPlans.size > 1) "s" else ""} : " +
                            missingPlans.joinToString { if (it == PremiumPlan.MONTHLY) "mensuel" else "annuel" } + "."
                    else -> null
                }
                publish(
                    state.copy(
                        availablePlans = plans,
                        isLoadingPlans = false,
                        plansMessage = plansMessage
                    )
                )
            } else {
                publish(
                    state.copy(
                        availablePlans = emptyMap(),
                        isLoadingPlans = false,
                        plansMessage = result.debugMessage.ifBlank {
                            "Google Play Billing indisponible (${result.responseCode})."
                        }
                    )
                )
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
        ).copy(
            availablePlans = state.availablePlans,
            isLoadingPlans = state.isLoadingPlans,
            plansMessage = state.plansMessage
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
        val unavailableMessage = result.debugMessage.ifBlank {
            "Google Play Billing indisponible (${result.responseCode})"
        }
        publish(
            PremiumEntitlementPolicy.unavailable(
                previous = state,
                message = unavailableMessage
            ).copy(isLoadingPlans = false, plansMessage = unavailableMessage)
        )
    }

    private fun publish(newState: PremiumEntitlementState) {
        state = newState
        listeners.forEach { it(newState) }
    }

    private fun logResult(operation: String, result: BillingResult) {
        Log.d(TAG, "$operation response=${result.responseCode} message=${result.debugMessage}")
    }

    override fun close() {
        connectionStarted = false
        productDetails = null
        availableOffers = emptyList()
        billingClient.endConnection()
        publish(state.copy(isBillingConnected = false))
        listeners.clear()
    }

    private companion object {
        const val TAG = "DicoSaamBilling"
    }
}
