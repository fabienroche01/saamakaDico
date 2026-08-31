package com.saamaka.dico.testeurs.billing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PremiumEntitlementPolicyTest {
    @Test
    fun cachedPremiumIsNeverGrantedBeforeGooglePlayVerification() {
        val state = PremiumEntitlementPolicy.initialState(wasPremiumLastKnown = true)

        assertFalse(state.isPremium)
        assertTrue(state.wasPremiumLastKnown)
        assertEquals(PremiumVerification.CHECKING, state.verification)
    }

    @Test
    fun purchasedTargetSubscriptionGrantsPremium() {
        val state = PremiumEntitlementPolicy.fromGooglePlay(
            purchases = listOf(
                PremiumPurchase(setOf(PREMIUM_PRODUCT_ID), isPurchased = true)
            ),
            verifiedAtMillis = 123L
        )

        assertTrue(state.isPremium)
        assertEquals(PremiumVerification.VERIFIED_ACTIVE, state.verification)
        assertEquals(123L, state.lastVerifiedAtMillis)
    }

    @Test
    fun pendingOrUnrelatedPurchasesDoNotGrantPremium() {
        val state = PremiumEntitlementPolicy.fromGooglePlay(
            purchases = listOf(
                PremiumPurchase(setOf(PREMIUM_PRODUCT_ID), isPurchased = false),
                PremiumPurchase(setOf("another_product"), isPurchased = true)
            ),
            verifiedAtMillis = 456L
        )

        assertFalse(state.isPremium)
        assertEquals(PremiumVerification.VERIFIED_INACTIVE, state.verification)
    }

    @Test
    fun billingFailureRevokesUnverifiedAccessButKeepsLastKnownSignal() {
        val previouslyVerified = PremiumEntitlementPolicy.fromGooglePlay(
            purchases = listOf(
                PremiumPurchase(setOf(PREMIUM_PRODUCT_ID), isPurchased = true)
            ),
            verifiedAtMillis = 789L
        )

        val unavailable = PremiumEntitlementPolicy.unavailable(previouslyVerified, "offline")

        assertFalse(unavailable.isPremium)
        assertTrue(unavailable.wasPremiumLastKnown)
        assertEquals(PremiumVerification.UNAVAILABLE, unavailable.verification)
        assertEquals("offline", unavailable.message)
    }
}
