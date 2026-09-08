package com.saamaka.dico.testeurs

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp

private const val COURSE_QUOTE_RECIPIENT = "sebrosa25@gmail.com"

@Composable
fun CourseQuoteScreen() {
    val context = LocalContext.current
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var level by remember { mutableStateOf("") }
    var availability by remember { mutableStateOf("") }
    var frequency by remember { mutableStateOf("") }
    var project by remember { mutableStateOf("") }

    val requiredReady = fullName.isNotBlank() && email.isNotBlank() && project.isNotBlank()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Demande de devis",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Cours individuel de Saamaka en visioconférence avec un professionnel Saamaka.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Nom et prénom *") },
                singleLine = true
            )
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Email *") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true
            )
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Téléphone (facultatif)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                singleLine = true
            )
            OutlinedTextField(
                value = level,
                onValueChange = { level = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Votre niveau actuel") },
                placeholder = { Text("Débutant, intermédiaire ou avancé") },
                singleLine = true
            )
            OutlinedTextField(
                value = availability,
                onValueChange = { availability = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Vos disponibilités") },
                placeholder = { Text("Ex. mardi soir, samedi matin…") },
                minLines = 2
            )
            OutlinedTextField(
                value = frequency,
                onValueChange = { frequency = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Fréquence souhaitée") },
                placeholder = { Text("Ex. 1 fois par semaine") },
                singleLine = true
            )

            Spacer(Modifier.height(2.dp))
            Text(
                text = "Quel est votre projet pour apprendre le Saamaka ? *",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Expliquez votre objectif : famille, origines, voyage, transmission aux enfants, conversation, projet professionnel…",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = project,
                onValueChange = { project = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Votre projet") },
                minLines = 5
            )

            Button(
                onClick = {
                    val subject = "Demande de devis – cours individuel de Saamaka"
                    val body = buildString {
                        appendLine("DEMANDE DE DEVIS – COURS INDIVIDUEL DE SAAMAKA EN VISIO")
                        appendLine()
                        appendLine("Nom et prénom : ${fullName.trim()}")
                        appendLine("Email : ${email.trim()}")
                        appendLine("Téléphone : ${phone.trim().ifBlank { "Non renseigné" }}")
                        appendLine("Niveau actuel : ${level.trim().ifBlank { "Non renseigné" }}")
                        appendLine("Disponibilités : ${availability.trim().ifBlank { "Non renseignées" }}")
                        appendLine("Fréquence souhaitée : ${frequency.trim().ifBlank { "Non renseignée" }}")
                        appendLine()
                        appendLine("Projet pour apprendre le Saamaka :")
                        appendLine(project.trim())
                        appendLine()
                        appendLine("Format demandé : cours individuel, exclusivement en visioconférence.")
                    }
                    val mailUri = Uri.parse(
                        "mailto:$COURSE_QUOTE_RECIPIENT" +
                            "?subject=${Uri.encode(subject)}" +
                            "&body=${Uri.encode(body)}"
                    )
                    val intent = Intent(Intent.ACTION_SENDTO, mailUri)
                    try {
                        context.startActivity(intent)
                    } catch (_: ActivityNotFoundException) {
                        Toast.makeText(
                            context,
                            "Aucune application email n’est disponible sur cet appareil.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                },
                enabled = requiredReady,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Envoyer ma demande de devis")
            }

            Text(
                text = "La demande sera préparée pour $COURSE_QUOTE_RECIPIENT. Vous pourrez la vérifier avant l’envoi dans votre application email.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
