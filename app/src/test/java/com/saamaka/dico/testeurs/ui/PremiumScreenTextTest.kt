package com.saamaka.dico.testeurs.ui

import com.saamaka.dico.testeurs.billing.PremiumPlan
import com.saamaka.dico.testeurs.billing.PremiumPlanDetails
import org.junit.Assert.assertEquals
import org.junit.Test

class PremiumScreenTextTest {
    @Test
    fun monthlyDisclosureUsesOnlyTheLocalizedGooglePlayPrice() {
        assertEquals(
            "4,99 € par mois, renouvellement automatique, résiliable à tout moment.",
            planDisclosure(PremiumPlanDetails(PremiumPlan.MONTHLY, "4,99 €", false))
        )
    }

    @Test
    fun eligibleAnnualOfferShowsSevenFreeDaysThenLocalizedPrice() {
        assertEquals(
            "7 jours gratuits, puis 49,99 € par an, renouvellement automatique, résiliable à tout moment.",
            planDisclosure(PremiumPlanDetails(PremiumPlan.ANNUAL, "49,99 €", true))
        )
    }

    @Test
    fun annualBasePlanWithoutTrialShowsOnlyLocalizedPrice() {
        assertEquals(
            "49,99 € par an, renouvellement automatique, résiliable à tout moment.",
            planDisclosure(PremiumPlanDetails(PremiumPlan.ANNUAL, "49,99 €", false))
        )
    }
}
