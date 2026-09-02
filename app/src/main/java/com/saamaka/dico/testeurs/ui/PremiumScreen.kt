package com.saamaka.dico.testeurs.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.saamaka.dico.testeurs.billing.PremiumEntitlementState
import com.saamaka.dico.testeurs.billing.PremiumPlan
import com.saamaka.dico.testeurs.billing.PremiumPlanDetails
import com.saamaka.dico.testeurs.billing.PremiumVerification
import com.saamaka.dico.testeurs.AppStrings
import com.saamaka.dico.testeurs.UiCopyKey
import com.saamaka.dico.testeurs.UiLanguage
import com.saamaka.dico.testeurs.stringsFor
import com.saamaka.dico.testeurs.ui

@Composable
fun PremiumScreen(
    strings: AppStrings,
    state: PremiumEntitlementState,
    onSubscribe: (PremiumPlan) -> Unit,
    onRestorePurchases: () -> Unit,
    onRetryBilling: () -> Unit,
    onManageSubscription: () -> Unit,
    onBack: () -> Unit
) {
    var selectedPlan by remember { mutableStateOf(PremiumPlan.ANNUAL) }
    val selectedDetails = state.availablePlans[selectedPlan]
    LaunchedEffect(Unit) { onRetryBilling() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TextButton(onClick = onBack) { Text(strings.back) }
        Text(
            text = strings.ui(UiCopyKey.PREMIUM_TITLE),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF16372A)
        )
        Text(
            text = strings.ui(UiCopyKey.PREMIUM_INTRO),
            style = MaterialTheme.typography.bodyMedium
        )

        if (state.isLoadingPlans) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator()
                Text(strings.ui(UiCopyKey.LOADING_PLAY_PRICES), Modifier.padding(start = 12.dp))
            }
        }

        PremiumPlanCard(
            title = strings.ui(UiCopyKey.MONTHLY),
            strings = strings,
            details = state.availablePlans[PremiumPlan.MONTHLY],
            loading = state.isLoadingPlans,
            selected = selectedPlan == PremiumPlan.MONTHLY,
            onSelect = { selectedPlan = PremiumPlan.MONTHLY }
        )
        PremiumPlanCard(
            title = strings.ui(UiCopyKey.ANNUAL),
            strings = strings,
            details = state.availablePlans[PremiumPlan.ANNUAL],
            loading = state.isLoadingPlans,
            selected = selectedPlan == PremiumPlan.ANNUAL,
            onSelect = { selectedPlan = PremiumPlan.ANNUAL }
        )

        when (state.verification) {
            PremiumVerification.VERIFIED_ACTIVE -> Text(
                text = strings.ui(UiCopyKey.PREMIUM_ACTIVE_MESSAGE),
                color = Color(0xFF0B5D3B),
                fontWeight = FontWeight.Bold
            )
            PremiumVerification.PENDING -> Text(
                text = strings.ui(UiCopyKey.PURCHASE_PENDING),
                color = Color(0xFF8A6500)
            )
            PremiumVerification.CHECKING -> Text(strings.ui(UiCopyKey.CHECKING_PURCHASES))
            PremiumVerification.UNAVAILABLE -> Text(
                text = localizedBillingMessage(state.message, strings)
                    ?: strings.ui(UiCopyKey.PLAY_UNAVAILABLE),
                color = MaterialTheme.colorScheme.error
            )
            PremiumVerification.VERIFIED_INACTIVE -> Unit
        }

        state.message
            ?.takeUnless { state.verification == PremiumVerification.UNAVAILABLE }
            ?.let { Text(localizedBillingMessage(it, strings) ?: it, style = MaterialTheme.typography.bodySmall) }

        state.plansMessage?.let { error ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        localizedBillingMessage(error, strings) ?: error,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    OutlinedButton(onClick = onRetryBilling, enabled = !state.isLoadingPlans) {
                        Text(strings.ui(UiCopyKey.RETRY))
                    }
                }
            }
        }

        Button(
            modifier = Modifier.fillMaxWidth(),
            enabled = selectedDetails != null && !state.isPremium && !state.isLoadingPlans,
            onClick = { onSubscribe(selectedPlan) }
        ) {
            Text(if (state.isPremium) strings.ui(UiCopyKey.PREMIUM_ACTIVE) else strings.ui(UiCopyKey.CONTINUE_PLAN))
        }

        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = onRestorePurchases
        ) {
            Text(strings.ui(UiCopyKey.RESTORE_PURCHASES))
        }

        TextButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = onManageSubscription
        ) {
            Text(strings.ui(UiCopyKey.MANAGE_SUBSCRIPTION))
        }

        Spacer(Modifier.height(24.dp))
    }
}

internal fun localizedBillingMessage(message: String?, strings: AppStrings): String? = when {
    message == null -> null
    message == "Google Play Billing n’est pas connecté." -> strings.ui(UiCopyKey.BILLING_DISCONNECTED)
    message == "L’abonnement DicoSaam Premium n’est pas disponible pour ce compte Google Play." ->
        strings.ui(UiCopyKey.SUBSCRIPTION_UNAVAILABLE)
    message.startsWith("Google Play n’a pas pu récupérer l’abonnement") ->
        strings.ui(UiCopyKey.BILLING_PRODUCT_ERROR)
    message == "Achat annulé" -> strings.ui(UiCopyKey.PURCHASE_CANCELED)
    message == "Achat non acquitté" -> strings.ui(UiCopyKey.PURCHASE_NOT_ACKNOWLEDGED)
    else -> message
}

@Composable
private fun PremiumPlanCard(
    title: String,
    strings: AppStrings,
    details: PremiumPlanDetails?,
    loading: Boolean,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = details != null, onClick = onSelect),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) Color(0xFFDCEEE2) else Color(0xFFFFFBF3)
        ),
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) Color(0xFF0B5D3B) else Color(0xFFD7D0C4)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            RadioButton(
                selected = selected,
                enabled = details != null,
                onClick = onSelect
            )
            Column(modifier = Modifier.padding(start = 8.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                if (details == null) {
                    Text(
                        if (loading) strings.ui(UiCopyKey.LOADING_PRICE) else strings.ui(UiCopyKey.PRICE_UNAVAILABLE),
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    Text(
                        text = details.localizedPrice,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = planDisclosure(details, strings),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

internal fun planDisclosure(
    details: PremiumPlanDetails,
    strings: AppStrings = stringsFor(UiLanguage.FRENCH)
): String = when (details.plan) {
    PremiumPlan.MONTHLY ->
        strings.ui(UiCopyKey.MONTHLY_DISCLOSURE, details.localizedPrice)
    PremiumPlan.ANNUAL -> if (details.hasSevenDayTrial) {
        strings.ui(UiCopyKey.ANNUAL_TRIAL_DISCLOSURE, details.localizedPrice)
    } else {
        strings.ui(UiCopyKey.ANNUAL_DISCLOSURE, details.localizedPrice)
    }
}
