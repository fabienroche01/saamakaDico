package com.saamaka.dico.testeurs.billing

import android.content.Context

class PremiumEntitlementStore(context: Context) {
    private val preferences = context.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE
    )

    fun lastKnownActive(): Boolean = preferences.getBoolean(KEY_ACTIVE, false)

    fun saveVerified(state: PremiumEntitlementState) {
        if (state.verification != PremiumVerification.VERIFIED_ACTIVE &&
            state.verification != PremiumVerification.VERIFIED_INACTIVE
        ) return

        preferences.edit()
            .putBoolean(KEY_ACTIVE, state.isPremium)
            .putLong(KEY_VERIFIED_AT, state.lastVerifiedAtMillis ?: 0L)
            .apply()
    }

    private companion object {
        const val PREFERENCES_NAME = "dicosaam_premium_entitlement"
        const val KEY_ACTIVE = "last_known_active"
        const val KEY_VERIFIED_AT = "last_verified_at"
    }
}
