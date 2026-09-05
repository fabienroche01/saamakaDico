package com.saamaka.dico.testeurs

private const val GUEST_TRANSLATION_TRIAL_LIMIT = 3
private const val FREE_ACCOUNT_TRANSLATION_TRIAL_LIMIT = 5

internal fun translationTrialLimit(accessLevel: AccessLevel): Int? = when (accessLevel) {
    AccessLevel.GUEST -> GUEST_TRANSLATION_TRIAL_LIMIT
    AccessLevel.FREE_ACCOUNT -> FREE_ACCOUNT_TRANSLATION_TRIAL_LIMIT
    AccessLevel.PREMIUM, AccessLevel.TESTER -> null
}
