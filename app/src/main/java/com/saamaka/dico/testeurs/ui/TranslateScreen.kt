package com.saamaka.dico.testeurs.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.saamaka.dico.testeurs.AccessLevel
import com.saamaka.dico.testeurs.AppStrings
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import java.util.Locale

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
    val context = LocalContext.current
    var textToSpeechReady by remember { mutableStateOf(false) }
    val textToSpeech = remember {
        TextToSpeech(context) { status ->
            textToSpeechReady = status == TextToSpeech.SUCCESS
        }
    }

    DisposableEffect(textToSpeech) {
        onDispose {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
    }

    val forestGreen = Color(0xFF0B5D3B)
    val cream = Color(0xFFFFFBF3)
    val softCream = Color(0xFFF4EFE5)
    val gold = Color(0xFFF0C96A)
    val darkText = Color(0xFF16372A)

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
            shape = RoundedCornerShape(24.dp),
            color = forestGreen,
            shadowElevation = 3.dp
        ) {
            Column(
                modifier = Modifier.padding(18.dp)
            ) {

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White.copy(alpha = 0.13f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Translate,
                            contentDescription = null,
                            tint = gold,
                            modifier = Modifier.padding(10.dp).size(24.dp)
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    Column {
                        Text(
                            text = strings.translate,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            text = "Français ↔ Saamaka",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = gold
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    text = "Traduisez des phrases et des textes complets. Les mots et expressions du dictionnaire restent gratuits.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.84f)
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
                        containerColor = softCream
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0D8C9))
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp)
                    ) {

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lock, null, tint = forestGreen)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Créez votre compte gratuitement",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = darkText
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        Text("Profitez de 3 traductions de phrases ou textes complets offertes.")

                        Spacer(Modifier.height(14.dp))

                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = forestGreen),
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
                        containerColor = Color(0xFFFFEFC4)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp)
                    ) {

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.WorkspacePremium, null, tint = Color(0xFF8A6712))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Saamaka Premium",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = darkText
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = "Traductions illimitées, textes plus longs, apprentissage, quiz et fonctions avancées."
                        )

                        Spacer(Modifier.height(14.dp))

                        OutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = forestGreen),
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
                        containerColor = softCream
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0D8C9))
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp)
                    ) {

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CardGiftcard, null, tint = forestGreen)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "$remainingTrials traduction(s) restante(s)",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = darkText
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = "Votre compte gratuit permet d'essayer le traducteur Saamaka."
                        )

                        Spacer(Modifier.height(14.dp))

                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            enabled = remainingTrials > 0,
                            colors = ButtonDefaults.buttonColors(containerColor = forestGreen),
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
                    text = "Premium : traductions illimitées",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            AccessLevel.PREMIUM -> {

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFEFC4)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp)
                    ) {

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.WorkspacePremium, null, tint = Color(0xFF8A6712))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Premium",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = darkText
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = "Traductions de phrases et textes sans limite."
                        )

                        Spacer(Modifier.height(14.dp))

                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = forestGreen),
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
                        containerColor = softCream
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0D8C9))
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp)
                    ) {

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Science, null, tint = forestGreen)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Mode testeur",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = darkText
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = "Traducteur complet activé pour les tests."
                        )

                        Spacer(Modifier.height(14.dp))

                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = forestGreen),
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

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = cream),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0D8C9)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Translate, null, tint = forestGreen)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Traduire une phrase",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = darkText
                        )
                    }

                    Spacer(Modifier.height(14.dp))

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
                        Text("FR  →  SM")
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
                        Text("FR  →  SM")
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
                        Text("SM  →  FR")
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
                        Text("SM  →  FR")
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
                    focusedBorderColor = forestGreen,
                    unfocusedBorderColor =
                        MaterialTheme.colorScheme.outline.copy(
                            alpha = 0.5f
                        ),
                    focusedContainerColor =
                        cream,
                    unfocusedContainerColor =
                        cream,
                    cursorColor = forestGreen
                )
            )

            Spacer(Modifier.height(14.dp))

            Button(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                enabled = sourceText.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = forestGreen),
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
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null)
                Spacer(Modifier.width(8.dp))
                Text("Traduire", fontWeight = FontWeight.Bold)
            }
                }
            }

            // -------------------------------------------------
            // RÉSULTAT
            // -------------------------------------------------

            if (translationResult.isNotBlank()) {

                Spacer(Modifier.height(14.dp))

                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = forestGreen),
                    onClick = {
                        sourceText = ""
                        translationResult = ""
                    }
                ) {
                    Icon(Icons.Default.Refresh, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Nouvelle phrase")
                }

                Spacer(Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFEFC4)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, gold),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {

                    Column(
                        modifier = Modifier.padding(18.dp)
                    ) {

                        Surface(
                            shape = RoundedCornerShape(50),
                            color = cream
                        ) {

                            Text(
                                text = if (frenchToSaamaka) {
                                    "Résultat Saamaka"
                                } else {
                                    "Résultat français"
                                },
                                modifier = Modifier.padding(
                                    horizontal = 10.dp,
                                    vertical = 4.dp
                                ),
                                fontWeight = FontWeight.Bold,
                                color = forestGreen
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        Text(
                            text = translationResult,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = darkText
                        )

                        Spacer(Modifier.height(16.dp))

                        HorizontalDivider(color = Color(0xFFD8C58F))

                        Spacer(Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedButton(
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 10.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = forestGreen),
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(
                                        ClipData.newPlainText("Traduction Saamaka Dico", translationResult)
                                    )
                                    Toast.makeText(context, "Traduction copiée", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(17.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Copier", maxLines = 1)
                            }

                            OutlinedButton(
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 10.dp),
                                shape = RoundedCornerShape(14.dp),
                                enabled = textToSpeechReady,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = forestGreen),
                                onClick = {
                                    if (!frenchToSaamaka) {
                                        textToSpeech.language = Locale.FRENCH
                                    }
                                    textToSpeech.speak(
                                        translationResult,
                                        TextToSpeech.QUEUE_FLUSH,
                                        null,
                                        "translation_result"
                                    )
                                }
                            ) {
                                Icon(Icons.AutoMirrored.Filled.VolumeUp, null, modifier = Modifier.size(17.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Écouter", maxLines = 1)
                            }

                            OutlinedButton(
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 10.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = forestGreen),
                                onClick = {
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_SUBJECT, "Saamaka Dico")
                                        putExtra(Intent.EXTRA_TEXT, translationResult)
                                    }
                                    context.startActivity(Intent.createChooser(intent, "Partager la traduction"))
                                }
                            ) {
                                Icon(Icons.Default.Share, null, modifier = Modifier.size(17.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Partager", maxLines = 1)
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

