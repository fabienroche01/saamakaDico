package com.saamaka.dico.testeurs.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.saamaka.dico.testeurs.AccessLevel
import com.saamaka.dico.testeurs.AppStrings
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface

@Composable
fun TranslateScreen(
    strings: AppStrings,
    accessLevel: AccessLevel,
    remainingTrials: Int,
    onUseTrial: () -> Unit,
    onTranslate: (String, Boolean) -> String?
) {
    var showTranslator by remember { mutableStateOf(false) }
    var sourceText by remember { mutableStateOf("") }
    var translationResult by remember { mutableStateOf("") }
    var frenchToSaamaka by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(
                start = 20.dp,
                top = 20.dp,
                end = 20.dp,
                bottom = 120.dp
            )
    ) {

        // -------------------------------------------------
        // EN-TÊTE
        // -------------------------------------------------

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Column(
                modifier = Modifier.padding(18.dp)
            ) {

                Text(
                    text = "✨ ${strings.translate}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = "Français ↔ Saamaka",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = "Traduisez des phrases et des textes complets. Les mots et expressions du dictionnaire restent gratuits.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        // -------------------------------------------------
        // ACCÈS
        // -------------------------------------------------

        when (accessLevel) {

            AccessLevel.GUEST -> {

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp)
                    ) {

                        Text(
                            text = "🔒 Créez votre compte gratuitement",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = "🎁 Profitez de 3 traductions de phrases ou textes complets offertes."
                        )

                        Spacer(Modifier.height(14.dp))

                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            onClick = {
                                // Compte réel plus tard
                            }
                        ) {
                            Text("Créer mon compte gratuitement")
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp)
                    ) {

                        Text(
                            text = "👑 Saamaka Premium",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )

                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = "Traductions illimitées, textes plus longs, apprentissage, quiz et fonctions avancées."
                        )

                        Spacer(Modifier.height(14.dp))

                        OutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            onClick = {
                                // Écran Premium plus tard
                            }
                        ) {
                            Text("Découvrir Premium")
                        }
                    }
                }
            }

            AccessLevel.FREE_ACCOUNT -> {

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp)
                    ) {

                        Text(
                            text = "🎁 $remainingTrials traduction(s) restante(s)",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )

                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = "Votre compte gratuit permet d'essayer le traducteur Saamaka."
                        )

                        Spacer(Modifier.height(14.dp))

                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            enabled = remainingTrials > 0,
                            onClick = {
                                if (remainingTrials > 0) {
                                    showTranslator = true
                                }
                            }
                        ) {
                            Text("Commencer une traduction")
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    text = "👑 Premium : traductions illimitées",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            AccessLevel.PREMIUM -> {

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp)
                    ) {

                        Text(
                            text = "👑 Premium",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )

                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = "Traductions de phrases et textes sans limite."
                        )

                        Spacer(Modifier.height(14.dp))

                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            onClick = {
                                showTranslator = true
                            }
                        ) {
                            Text("Traduire maintenant")
                        }
                    }
                }
            }

            AccessLevel.TESTER -> {

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp)
                    ) {

                        Text(
                            text = "🧪 Mode testeur",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = "Traducteur complet activé pour les tests."
                        )

                        Spacer(Modifier.height(14.dp))

                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            onClick = {
                                showTranslator = true
                            }
                        ) {
                            Text("Tester une traduction")
                        }
                    }
                }
            }
        }

        // -------------------------------------------------
        // TRADUCTEUR
        // -------------------------------------------------

        if (
            showTranslator &&
            (
                    accessLevel != AccessLevel.FREE_ACCOUNT ||
                            remainingTrials > 0
                    )
        ) {

            Spacer(Modifier.height(20.dp))

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(
                    alpha = 0.25f
                )
            )

            Spacer(Modifier.height(20.dp))

            Text(
                text = "✨ Traduire une phrase",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.height(12.dp))

            // Sens de traduction
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                val frSelected = frenchToSaamaka

                if (frSelected) {
                    Button(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        onClick = {
                            frenchToSaamaka = true
                            translationResult = ""
                        }
                    ) {
                        Text("🇫🇷 → 🇸🇷")
                    }
                } else {
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        onClick = {
                            frenchToSaamaka = true
                            translationResult = ""
                        }
                    ) {
                        Text("🇫🇷 → 🇸🇷")
                    }
                }

                if (!frSelected) {
                    Button(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        onClick = {
                            frenchToSaamaka = false
                            translationResult = ""
                        }
                    ) {
                        Text("🇸🇷 → 🇫🇷")
                    }
                } else {
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        onClick = {
                            frenchToSaamaka = false
                            translationResult = ""
                        }
                    ) {
                        Text("🇸🇷 → 🇫🇷")
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Zone de saisie
            OutlinedTextField(
                value = sourceText,
                onValueChange = {
                    sourceText = it
                    translationResult = ""
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                label = {
                    Text(
                        if (frenchToSaamaka) {
                            "Phrase en français"
                        } else {
                            "Phrase en Saamaka"
                        }
                    )
                },
                placeholder = {
                    Text("Écrivez votre phrase ici…")
                },
                minLines = 4,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor =
                        MaterialTheme.colorScheme.outline.copy(
                            alpha = 0.5f
                        ),
                    focusedContainerColor =
                        MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor =
                        MaterialTheme.colorScheme.surface
                )
            )

            Spacer(Modifier.height(14.dp))

            Button(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                enabled = sourceText.isNotBlank(),
                onClick = {

                    val result = onTranslate(
                        sourceText,
                        frenchToSaamaka
                    )

                    if (!result.isNullOrBlank()) {

                        translationResult = result

                        if (
                            accessLevel == AccessLevel.FREE_ACCOUNT &&
                            remainingTrials > 0
                        ) {
                            onUseTrial()
                        }
                    }
                }
            ) {
                Text("✨ Traduire")
            }

            // -------------------------------------------------
            // RÉSULTAT
            // -------------------------------------------------

            if (translationResult.isNotBlank()) {

                Spacer(Modifier.height(14.dp))

                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    onClick = {
                        sourceText = ""
                        translationResult = ""
                    }
                ) {
                    Text("🔄 Nouvelle phrase")
                }

                Spacer(Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor =
                            MaterialTheme.colorScheme.primaryContainer
                    )
                ) {

                    Column(
                        modifier = Modifier.padding(18.dp)
                    ) {

                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.surface
                        ) {

                            Text(
                                text = if (frenchToSaamaka) {
                                    "🇸🇷 Résultat Saamaka"
                                } else {
                                    "🇫🇷 Résultat français"
                                },
                                modifier = Modifier.padding(
                                    horizontal = 10.dp,
                                    vertical = 4.dp
                                ),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        Text(
                            text = translationResult,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color =
                                MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

