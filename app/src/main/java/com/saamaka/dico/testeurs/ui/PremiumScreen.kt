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

@Composable
fun PremiumScreen(
    state: PremiumEntitlementState,
    onSubscribe: (PremiumPlan) -> Unit,
    onRestorePurchases: () -> Unit,
    onManageSubscription: () -> Unit,
    onBack: () -> Unit
) {
    var selectedPlan by remember { mutableStateOf(PremiumPlan.ANNUAL) }
    val selectedDetails = state.availablePlans[selectedPlan]

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TextButton(onClick = onBack) { Text("Retour") }
        Text(
            text = "DicoSaam Premium",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF16372A)
        )
        Text(
            text = "Choisissez votre formule. Les prix affichés sont fournis par Google Play.",
            style = MaterialTheme.typography.bodyMedium
        )

        PremiumPlanCard(
            title = "Mensuel",
            details = state.availablePlans[PremiumPlan.MONTHLY],
            selected = selectedPlan == PremiumPlan.MONTHLY,
            onSelect = { selectedPlan = PremiumPlan.MONTHLY }
        )
        PremiumPlanCard(
            title = "Annuel",
            details = state.availablePlans[PremiumPlan.ANNUAL],
            selected = selectedPlan == PremiumPlan.ANNUAL,
            onSelect = { selectedPlan = PremiumPlan.ANNUAL }
        )

        when (state.verification) {
            PremiumVerification.VERIFIED_ACTIVE -> Text(
                text = "Votre abonnement Premium est actif.",
                color = Color(0xFF0B5D3B),
                fontWeight = FontWeight.Bold
            )
            PremiumVerification.PENDING -> Text(
                text = "Achat en attente de confirmation par Google Play.",
                color = Color(0xFF8A6500)
            )
            PremiumVerification.CHECKING -> Text("Vérification des achats en cours…")
            PremiumVerification.UNAVAILABLE -> Text(
                text = state.message ?: "Google Play est temporairement indisponible.",
                color = MaterialTheme.colorScheme.error
            )
            PremiumVerification.VERIFIED_INACTIVE -> Unit
        }

        state.message
            ?.takeUnless { state.verification == PremiumVerification.UNAVAILABLE }
            ?.let { Text(it, style = MaterialTheme.typography.bodySmall) }

        Button(
            modifier = Modifier.fillMaxWidth(),
            enabled = selectedDetails != null && !state.isPremium,
            onClick = { onSubscribe(selectedPlan) }
        ) {
            Text(if (state.isPremium) "Premium actif" else "Continuer avec cette formule")
        }

        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = onRestorePurchases
        ) {
            Text("Restaurer les achats")
        }

        TextButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = onManageSubscription
        ) {
            Text("Gérer mon abonnement")
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun PremiumPlanCard(
    title: String,
    details: PremiumPlanDetails?,
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
                    Text("Prix indisponible", style = MaterialTheme.typography.bodyMedium)
                } else {
                    Text(
                        text = details.localizedPrice,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = planDisclosure(details),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

internal fun planDisclosure(details: PremiumPlanDetails): String = when (details.plan) {
    PremiumPlan.MONTHLY ->
        "${details.localizedPrice} par mois, renouvellement automatique, résiliable à tout moment."
    PremiumPlan.ANNUAL -> if (details.hasSevenDayTrial) {
        "7 jours gratuits, puis ${details.localizedPrice} par an, renouvellement automatique, résiliable à tout moment."
    } else {
        "${details.localizedPrice} par an, renouvellement automatique, résiliable à tout moment."
    }
}
