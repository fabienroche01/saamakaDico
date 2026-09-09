package com.saamaka.dico.testeurs

/** Email registration never grants a paid entitlement or tester privileges. */
internal fun emailAccountAccess(
    testerMode: Boolean,
    premiumVerified: Boolean,
    emailVerified: Boolean
): AccessLevel = when {
    testerMode -> AccessLevel.TESTER
    premiumVerified -> AccessLevel.PREMIUM
    emailVerified -> AccessLevel.FREE_ACCOUNT
    else -> AccessLevel.GUEST
}
