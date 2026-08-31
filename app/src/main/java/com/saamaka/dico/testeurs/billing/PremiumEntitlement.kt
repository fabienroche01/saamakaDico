package com.saamaka.dico.testeurs.billing

const val PREMIUM_PRODUCT_ID = "dicosaam_premium"

enum class PremiumVerification {
    CHECKING,
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
    val message: String? = null
)

data class PremiumPurchase(
    val productIds: Set<String>,
    val isPurchased: Boolean
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
            purchase.isPurchased && PREMIUM_PRODUCT_ID in purchase.productIds
        }
        return PremiumEntitlementState(
            isPremium = active,
            verification = if (active) {
                PremiumVerification.VERIFIED_ACTIVE
            } else {
                PremiumVerification.VERIFIED_INACTIVE
            },
            wasPremiumLastKnown = active,
            isBillingConnected = isBillingConnected,
            lastVerifiedAtMillis = verifiedAtMillis
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
