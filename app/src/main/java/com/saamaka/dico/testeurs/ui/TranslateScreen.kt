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
            .padding(20.dp)
    ) {
        Text(
            text = "✨ ${strings.translate}",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(10.dp))

        Text(
            text = "Traduisez des phrases et des textes complets entre le Français et le Saamaka. Les mots et expressions du dictionnaire restent gratuits.",
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(Modifier.height(20.dp))

        when (accessLevel) {

            AccessLevel.GUEST -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp)
                    ) {
                        Text(
                            text = "🔒 Créez votre compte gratuitement",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )

                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = "🎁 Créez votre compte et profitez de 3 traductions de phrases/Texte complètes offertes."
                        )

                        Spacer(Modifier.height(14.dp))

                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                // Compte réel plus tard
                            }
                        ) {
                            Text("Créer mon compte gratuitement")
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp)
                    ) {
                        Text(
                            text = "👑 Saamaka Premium",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )

                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = "Traductions illimitées, textes plus longs, apprentissage, quiz et autres fonctions avancées."
                        )

                        Spacer(Modifier.height(14.dp))

                        OutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
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
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp)
                    ) {
                        Text(
                            text = "🎁 $remainingTrials phrase(s) complète(s) restante(s)",
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
                            enabled = accessLevel != AccessLevel.FREE_ACCOUNT || remainingTrials > 0,
                            onClick = {
                                if (accessLevel != AccessLevel.FREE_ACCOUNT || remainingTrials > 0) {
                                   showTranslator = true
                                }
                            }
                        ) {
                            Text("Commencer une traduction")
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "👑 Premium : traductions illimitées",
                    fontWeight = FontWeight.Bold
                )
            }

            AccessLevel.PREMIUM -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp)
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
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp)
                    ) {
                        Text(
                            text = "🧪 Mode testeur",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )

                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = "Traducteur complet activé pour les tests."
                        )

                        Spacer(Modifier.height(14.dp))

                        Button(
                            modifier = Modifier.fillMaxWidth(),
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
        if (showTranslator && (accessLevel != AccessLevel.FREE_ACCOUNT ||
                remainingTrials > 0)
        ) {

            Spacer(Modifier.height(20.dp))

            HorizontalDivider()

            Spacer(Modifier.height(20.dp))

            Text(
                text = "✨ Traduire une phrase",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        frenchToSaamaka = true
                        translationResult = ""
                    }
                ) {
                    Text("🇫🇷 Français → 🇸🇷 Saamaka")
                }

                OutlinedButton(
                    modifier = Modifier.weight(1f),
                    onClick = {
                        frenchToSaamaka = false
                        translationResult = ""
                    }
                ) {
                    Text("🇸🇷 Saamaka → 🇫🇷 Français")
                }
            }

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = sourceText,
                onValueChange = {
                    sourceText = it
                    translationResult = ""
                },
                modifier = Modifier.fillMaxWidth(),
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
                minLines = 3
            )

            Spacer(Modifier.height(12.dp))

            Button(
                modifier = Modifier.fillMaxWidth(),
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

            if (translationResult.isNotBlank()) {

                Spacer(Modifier.height(12.dp))

                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
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
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp)
                    ) {
                        Text(
                            text = if (frenchToSaamaka) {
                                "🇸🇷 Résultat Saamaka"
                            } else {
                                "🇫🇷 Résultat français"
                            },
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = translationResult,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        }

    }



}