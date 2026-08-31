package com.saamaka.dico.testeurs.billing

const val PREMIUM_PRODUCT_ID = "dicosaam_premium"

enum class PremiumVerification {
    CHECKING,
    PENDING,
    VERIFIED_ACTIVE,
    VERIFIED_INACTIVE,
    UNAVAILABLE
}

data class PremiumEntitlementState(
    val isPremium: Boolean = false,
    val verification: PremiumVerification = PremiumVerification.CHECKING,
    val wasPremiumLastKnown: Boolean = false,
    val isBillingConnected: Boolean = false,
    val lastVerifiedAtMillis: Long? = null,
    val availablePlans: Map<PremiumPlan, PremiumPlanDetails> = emptyMap(),
    val message: String? = null
)

enum class PremiumPurchaseStatus {
    PURCHASED,
    PENDING,
    OTHER
}

data class PremiumPurchase(
    val productIds: Set<String>,
    val status: PremiumPurchaseStatus
)

object PremiumEntitlementPolicy {
    fun initialState(wasPremiumLastKnown: Boolean): PremiumEntitlementState =
        PremiumEntitlementState(
            isPremium = false,
            verification = PremiumVerification.CHECKING,
            wasPremiumLastKnown = wasPremiumLastKnown
        )

    fun fromGooglePlay(
        purchases: List<PremiumPurchase>,
        verifiedAtMillis: Long,
        isBillingConnected: Boolean = true
    ): PremiumEntitlementState {
        val active = purchases.any { purchase ->
            purchase.status == PremiumPurchaseStatus.PURCHASED &&
                PREMIUM_PRODUCT_ID in purchase.productIds
        }
        val pending = !active && purchases.any { purchase ->
            purchase.status == PremiumPurchaseStatus.PENDING &&
                PREMIUM_PRODUCT_ID in purchase.productIds
        }
        return PremiumEntitlementState(
            isPremium = active,
            verification = when {
                active -> PremiumVerification.VERIFIED_ACTIVE
                pending -> PremiumVerification.PENDING
                else -> PremiumVerification.VERIFIED_INACTIVE
            },
            wasPremiumLastKnown = active,
            isBillingConnected = isBillingConnected,
            lastVerifiedAtMillis = verifiedAtMillis,
            message = if (pending) "Achat en attente de confirmation" else null
        )
    }

    fun unavailable(
        previous: PremiumEntitlementState,
        message: String
    ): PremiumEntitlementState = previous.copy(
        isPremium = false,
        verification = PremiumVerification.UNAVAILABLE,
        isBillingConnected = false,
        message = message
    )
}
