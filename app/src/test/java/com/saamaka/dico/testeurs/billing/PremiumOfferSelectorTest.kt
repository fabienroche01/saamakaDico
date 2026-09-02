package com.saamaka.dico.testeurs.billing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PremiumOfferSelectorTest {
    private val offers = listOf(
        PremiumOffer(PREMIUM_PRODUCT_ID, PREMIUM_MONTHLY_BASE_PLAN_ID, null, "monthly", "4,99 €"),
        PremiumOffer(PREMIUM_PRODUCT_ID, PREMIUM_MONTHLY_BASE_PLAN_ID, "promo", "wrong-monthly", "1,99 €"),
        PremiumOffer(PREMIUM_PRODUCT_ID, PREMIUM_ANNUAL_BASE_PLAN_ID, null, "annual", "49,99 €"),
        PremiumOffer(
            PREMIUM_PRODUCT_ID,
            PREMIUM_ANNUAL_BASE_PLAN_ID,
            PREMIUM_ANNUAL_TRIAL_OFFER_ID,
            "annual-trial",
            "49,99 €"
        )
    )

    @Test
    fun monthlyPlanUsesTheConfiguredMonthlyBasePlan() {
        assertEquals(
            "monthly",
            PremiumOfferSelector.select(PremiumPlan.MONTHLY, offers)?.offerToken
        )
    }

    @Test
    fun annualPlanPrefersTheSevenDayTrial() {
        val selected = PremiumOfferSelector.select(PremiumPlan.ANNUAL, offers)

        assertEquals("annual-trial", selected?.offerToken)
        assertEquals(PREMIUM_ANNUAL_TRIAL_OFFER_ID, selected?.offerId)
    }

    @Test
    fun annualPlanFallsBackToBasePlanWhenTrialIsNotEligible() {
        val withoutTrial = offers.filter { it.offerId != PREMIUM_ANNUAL_TRIAL_OFFER_ID }

        assertEquals(
            "annual",
            PremiumOfferSelector.select(PremiumPlan.ANNUAL, withoutTrial)?.offerToken
        )
    }

    @Test
    fun annualDetailsWithoutEligibleTrialUseTheLocalizedBasePlanPrice() {
        val withoutTrial = offers.filter { it.offerId != PREMIUM_ANNUAL_TRIAL_OFFER_ID }
        val details = PremiumOfferSelector.planDetails(withoutTrial).getValue(PremiumPlan.ANNUAL)

        assertEquals("49,99 €", details.localizedPrice)
        assertEquals(false, details.hasSevenDayTrial)
    }

    @Test
    fun monthlyDetailsNeverUseAPromotionalOffer() {
        val details = PremiumOfferSelector.planDetails(offers).getValue(PremiumPlan.MONTHLY)

        assertEquals("4,99 €", details.localizedPrice)
        assertEquals(false, details.hasSevenDayTrial)
        assertEquals("monthly", PremiumOfferSelector.select(PremiumPlan.MONTHLY, offers)?.offerToken)
    }

    @Test
    fun planDetailsContainOnlyGooglePlayLocalizedPrices() {
        val details = PremiumOfferSelector.planDetails(offers)

        assertEquals("4,99 €", details[PremiumPlan.MONTHLY]?.localizedPrice)
        assertEquals("49,99 €", details[PremiumPlan.ANNUAL]?.localizedPrice)
        assertEquals(true, details[PremiumPlan.ANNUAL]?.hasSevenDayTrial)
    }

    @Test
    fun unrelatedProductsAndOffersAreNeverSelected() {
        val unrelated = listOf(
            PremiumOffer("another_product", PREMIUM_MONTHLY_BASE_PLAN_ID, null, "other", "\$1"),
            PremiumOffer(PREMIUM_PRODUCT_ID, "lifetime", null, "wrong-plan", "\$10")
        )

        assertNull(PremiumOfferSelector.select(PremiumPlan.MONTHLY, unrelated))
        assertNull(PremiumOfferSelector.select(PremiumPlan.ANNUAL, unrelated))
    }
}
