package com.saamaka.dico.testeurs.billing

const val PREMIUM_MONTHLY_BASE_PLAN_ID = "mensuel"
const val PREMIUM_ANNUAL_BASE_PLAN_ID = "annuel"
const val PREMIUM_ANNUAL_TRIAL_OFFER_ID = "essai-7-jours"

enum class PremiumPlan {
    MONTHLY,
    ANNUAL
}

data class PremiumOffer(
    val productId: String,
    val basePlanId: String,
    val offerId: String?,
    val offerToken: String,
    val localizedPrice: String
)

data class PremiumPlanDetails(
    val plan: PremiumPlan,
    val localizedPrice: String,
    val hasSevenDayTrial: Boolean
)

object PremiumOfferSelector {
    fun select(plan: PremiumPlan, offers: List<PremiumOffer>): PremiumOffer? {
        val premiumOffers = offers.filter { it.productId == PREMIUM_PRODUCT_ID }
        return when (plan) {
            PremiumPlan.MONTHLY -> premiumOffers.firstOrNull {
                it.basePlanId == PREMIUM_MONTHLY_BASE_PLAN_ID && it.offerId == null
            }

            PremiumPlan.ANNUAL -> premiumOffers.firstOrNull {
                it.basePlanId == PREMIUM_ANNUAL_BASE_PLAN_ID &&
                    it.offerId == PREMIUM_ANNUAL_TRIAL_OFFER_ID
            } ?: premiumOffers.firstOrNull {
                it.basePlanId == PREMIUM_ANNUAL_BASE_PLAN_ID && it.offerId == null
            }
        }
    }

    fun planDetails(offers: List<PremiumOffer>): Map<PremiumPlan, PremiumPlanDetails> =
        PremiumPlan.entries.mapNotNull { plan ->
            val selected = select(plan, offers) ?: return@mapNotNull null
            selected.localizedPrice.takeIf { it.isNotBlank() }?.let { price ->
                plan to PremiumPlanDetails(
                    plan = plan,
                    localizedPrice = price,
                    hasSevenDayTrial = selected.offerId == PREMIUM_ANNUAL_TRIAL_OFFER_ID
                )
            }
        }.toMap()
}
