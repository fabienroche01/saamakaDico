package com.saamaka.dico.testeurs

import org.junit.Assert.assertEquals
import org.junit.Test

class EmailAccountAccessTest {
    @Test fun unverifiedRegistrationHasOnlyGuestAccess() {
        assertEquals(AccessLevel.GUEST, emailAccountAccess(false, false, false))
    }

    @Test fun verifiedEmailGrantsFreeButNeverPremium() {
        assertEquals(AccessLevel.FREE_ACCOUNT, emailAccountAccess(false, false, true))
    }

    @Test fun googlePlayPurchaseDoesNotRequireAnEmailAccount() {
        assertEquals(AccessLevel.PREMIUM, emailAccountAccess(false, true, false))
    }

    @Test fun signingOutDoesNotCancelVerifiedGooglePlayEntitlement() {
        assertEquals(AccessLevel.PREMIUM, emailAccountAccess(false, true, true))
        assertEquals(AccessLevel.PREMIUM, emailAccountAccess(false, true, false))
    }

    @Test fun losingPremiumFallsBackToActualEmailIdentity() {
        assertEquals(AccessLevel.FREE_ACCOUNT, emailAccountAccess(false, false, true))
        assertEquals(AccessLevel.GUEST, emailAccountAccess(false, false, false))
    }

    @Test fun testerModeIsIndependentOfRegistrationAndBilling() {
        for (premium in listOf(false, true)) {
            for (verified in listOf(false, true)) {
                assertEquals(AccessLevel.TESTER, emailAccountAccess(true, premium, verified))
            }
        }
    }
}
