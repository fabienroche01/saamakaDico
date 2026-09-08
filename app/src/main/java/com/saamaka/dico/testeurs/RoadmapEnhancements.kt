package com.saamaka.dico.testeurs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatAudioDuration(durationMs: Long): String {
    if (durationMs <= 0L) return "0 s"
    val totalSeconds = (durationMs / 1000L).coerceAtLeast(1L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return if (minutes > 0L) "%d:%02d".format(minutes, seconds) else "$seconds s"
}

fun formatExportTimestamp(timestamp: Long): String {
    if (timestamp <= 0L) return "Aucun export précédent"
    return "Dernier export : " + SimpleDateFormat(
        "dd/MM/yyyy HH:mm",
        Locale.getDefault()
    ).format(Date(timestamp))
}

@Composable
fun DictionaryQualitySummaryCard(audit: DictionaryQualityAudit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "Qualité de la base",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Doublons : ${audit.exactDuplicateGroups}")
                Text("Champs vides : ${audit.missingFields}")
            }
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Expressions longues : ${audit.longExpressions}")
                Text("Variantes catégories : ${audit.categoryVariantGroups}")
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Entrées à contrôler : ${audit.affectedEntryCount}",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun TesterExportPreviewDialog(
    lastExportAtMillis: Long,
    validations: Int,
    corrections: Int,
    deletions: Int,
    newEntries: Int,
    audioFiles: Int,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val total = validations + corrections + deletions + newEntries
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Aperçu de l’export") },
        text = {
            Column {
                Text(formatExportTimestamp(lastExportAtMillis))
                Spacer(Modifier.height(10.dp))
                Text("Validations : $validations")
                Text("Corrections : $corrections")
                Text("Suppressions : $deletions")
                Text("Nouvelles entrées : $newEntries")
                Text("Audios nouveaux/modifiés : $audioFiles")
                Spacer(Modifier.height(10.dp))
                Text("Actions à envoyer : $total", fontWeight = FontWeight.Bold)
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) { Text("Générer le ZIP") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Annuler") }
        }
    )
}
