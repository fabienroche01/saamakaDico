package com.saamaka.dico.testeurs.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
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
import com.saamaka.dico.testeurs.UiCopyKey
import com.saamaka.dico.testeurs.ui
import com.saamaka.dico.testeurs.provenanceLabel
import com.saamaka.dico.testeurs.reliabilityLabel
import com.saamaka.dico.testeurs.model.PhraseTranslationResult
import com.saamaka.dico.testeurs.model.PhraseTranslationKind
import com.saamaka.dico.testeurs.model.TranslationReliability
import com.saamaka.dico.testeurs.model.DictionaryEntry
import com.saamaka.dico.testeurs.UnifiedLocalSearchResult
import com.saamaka.dico.testeurs.PhraseTranslationDisposition
import com.saamaka.dico.testeurs.PhraseTranslationPipelineResult
import com.saamaka.dico.testeurs.unifiedSearchButtonLabel
import com.saamaka.dico.testeurs.shouldOfferPremiumTranslation
import com.saamaka.dico.testeurs.normalizedInputWordCount
import com.saamaka.dico.testeurs.relatedExpressionLabel
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

private fun localizedRelatedExpressionLabel(label: String, strings: AppStrings): String = when (label) {
    "Correspondance exacte du dictionnaire" -> strings.ui(UiCopyKey.EXACT_DICTIONARY_MATCH)
    "Expression proche — contient des mots supplémentaires" -> strings.ui(UiCopyKey.CLOSE_EXPRESSION_EXTRA)
    "Proposition incomplète" -> strings.ui(UiCopyKey.INCOMPLETE_SUGGESTION)
    else -> strings.ui(UiCopyKey.CLOSE_EXPRESSION)
}

private fun localizedShareableText(
    result: PhraseTranslationResult,
    strings: AppStrings
): String = if (result.reliability == TranslationReliability.HIGH) {
    result.translation
} else {
    buildString {
        appendLine(strings.ui(UiCopyKey.APPROXIMATE_REVIEW))
        appendLine(result.translation)
        result.recognizedSegments.mapNotNull { it.detail }.forEach(::appendLine)
        if (result.untranslatedSegments.isNotEmpty()) {
            append(
                strings.ui(
                    UiCopyKey.ITEMS_TO_REVIEW,
                    result.untranslatedSegments.joinToString(", ")
                )
            )
        }
    }
}

@Composable
fun TranslateScreen(
    strings: AppStrings,
    initialText: String = "",
    translateInitialTextImmediately: Boolean = false,
    onInitialTextHandled: () -> Unit = {},
    accessLevel: AccessLevel,
    remainingTrials: Int,
    onTrialsChanged: (Int) -> Unit,
    onLocalSearch: suspend (String, Boolean) -> UnifiedLocalSearchResult,
    onTranslate: suspend (String, Boolean) -> PhraseTranslationPipelineResult,
    onOpenEntry: (DictionaryEntry) -> Unit
) {
    UnifiedTranslateContent(
        strings = strings,
        initialText = initialText,
        translateInitialTextImmediately = translateInitialTextImmediately,
        onInitialTextHandled = onInitialTextHandled,
        accessLevel = accessLevel,
        remainingTrials = remainingTrials,
        onTrialsChanged = onTrialsChanged,
        onLocalSearch = onLocalSearch,
        onTranslate = onTranslate,
        onOpenEntry = onOpenEntry
    )
}

@Composable
private fun UnifiedTranslateContent(
    strings: AppStrings,
    initialText: String,
    translateInitialTextImmediately: Boolean,
    onInitialTextHandled: () -> Unit,
    accessLevel: AccessLevel,
    remainingTrials: Int,
    onTrialsChanged: (Int) -> Unit,
    onLocalSearch: suspend (String, Boolean) -> UnifiedLocalSearchResult,
    onTranslate: suspend (String, Boolean) -> PhraseTranslationPipelineResult,
    onOpenEntry: (DictionaryEntry) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val startupText = remember { initialText }
    val translateStartupText = remember { translateInitialTextImmediately }
    var input by remember { mutableStateOf(initialText) }
    var frenchToSaamaka by remember { mutableStateOf(true) }
    var localResult by remember { mutableStateOf<UnifiedLocalSearchResult?>(null) }
    var phraseResult by remember { mutableStateOf<PhraseTranslationResult?>(null) }
    var runningJob by remember { mutableStateOf<Job?>(null) }
    var requestId by remember { mutableStateOf(0L) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var showDetails by remember { mutableStateOf(false) }
    var ttsReady by remember { mutableStateOf(false) }
    val tts = remember {
        TextToSpeech(context) { ttsReady = it == TextToSpeech.SUCCESS }
    }

    DisposableEffect(tts) {
        onDispose {
            runningJob?.cancel()
            tts.stop()
            tts.shutdown()
        }
    }

    fun resetResults() {
        runningJob?.cancel()
        requestId++
        isLoading = false
        error = null
        localResult = null
        phraseResult = null
        showDetails = false
    }

    fun launchRequest(block: suspend () -> Unit) {
        runningJob?.cancel()
        val currentId = ++requestId
        runningJob = scope.launch {
            isLoading = true
            error = null
            try {
                block()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Throwable) {
                if (currentId == requestId) error = strings.ui(UiCopyKey.TRANSLATION_FAILED_RETRY)
            } finally {
                if (currentId == requestId) isLoading = false
            }
        }
    }

    val canUsePremium = accessLevel == AccessLevel.PREMIUM ||
        accessLevel == AccessLevel.TESTER ||
        (accessLevel == AccessLevel.FREE_ACCOUNT && remainingTrials > 0)

    suspend fun translateCurrentPhrase(): PhraseTranslationPipelineResult {
        val pipelineResult = onTranslate(input, frenchToSaamaka)
        if (pipelineResult.trialConsumed) {
            onTrialsChanged(pipelineResult.remainingTrials)
        }
        val result = pipelineResult.translation
            ?.takeIf { pipelineResult.disposition == PhraseTranslationDisposition.TRANSLATED }
        phraseResult = result
        return pipelineResult
    }

    LaunchedEffect(Unit) {
        if (startupText.isBlank()) return@LaunchedEffect
        launchRequest {
            val shouldTranslate = translateStartupText &&
                normalizedInputWordCount(startupText) >= 2
            if (shouldTranslate) {
                val decision = translateCurrentPhrase()
                localResult = if (decision.disposition == PhraseTranslationDisposition.TRANSLATED) {
                    null
                } else {
                    onLocalSearch(startupText, frenchToSaamaka).copy(exactMatch = null)
                }
            } else {
                localResult = onLocalSearch(startupText, frenchToSaamaka)
            }
            onInitialTextHandled()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = 12.dp, bottom = 120.dp)
    ) {
        Text(
            text = strings.translate,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF16372A)
        )
        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (frenchToSaamaka) {
                Button(modifier = Modifier.weight(1f), onClick = {}) { Text("FR → SM") }
            } else {
                OutlinedButton(modifier = Modifier.weight(1f), onClick = {
                    resetResults(); frenchToSaamaka = true
                }) { Text("FR → SM") }
            }
            if (!frenchToSaamaka) {
                Button(modifier = Modifier.weight(1f), onClick = {}) { Text("SM → FR") }
            } else {
                OutlinedButton(modifier = Modifier.weight(1f), onClick = {
                    resetResults(); frenchToSaamaka = false
                }) { Text("SM → FR") }
            }
        }

        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = input,
            onValueChange = { input = it; resetResults() },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(strings.ui(UiCopyKey.SEARCH_OR_TRANSLATE)) },
            placeholder = { Text(strings.ui(UiCopyKey.SEARCH_OR_TRANSLATE_HINT)) },
            minLines = 2,
            maxLines = 6,
            shape = RoundedCornerShape(18.dp)
        )

        Spacer(Modifier.height(12.dp))
        Button(
            modifier = Modifier.fillMaxWidth(),
            enabled = input.isNotBlank() && !isLoading,
            onClick = {
                phraseResult = null
                launchRequest {
                    if (normalizedInputWordCount(input) >= 2) {
                        val decision = translateCurrentPhrase()
                        localResult = if (decision.disposition == PhraseTranslationDisposition.TRANSLATED) {
                            null
                        } else {
                            onLocalSearch(input, frenchToSaamaka).copy(exactMatch = null)
                        }
                    } else {
                        localResult = onLocalSearch(input, frenchToSaamaka)
                    }
                }
            }
        ) {
            if (isLoading) {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
                Spacer(Modifier.width(8.dp))
            }
            Text(
                if (isLoading) strings.ui(UiCopyKey.SEARCHING)
                else if (normalizedInputWordCount(input) >= 2) strings.ui(UiCopyKey.SEARCH_OR_TRANSLATE)
                else strings.ui(UiCopyKey.SEARCH_ACTION)
            )
        }

        error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        localResult?.exactMatch?.let { match ->
            Spacer(Modifier.height(16.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(strings.provenanceLabel(match.provenance), fontWeight = FontWeight.Bold, color = Color(0xFF0B5D3B))
                    Spacer(Modifier.height(8.dp))
                    Text(match.translation, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(strings.ui(UiCopyKey.RELIABILITY_HIGH), style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(10.dp))
                    ResultActionRow(
                        strings = strings,
                        text = match.translation,
                        canListen = ttsReady,
                        onListen = {
                            tts.speak(match.translation, TextToSpeech.QUEUE_FLUSH, null, "local_exact")
                        }
                    )
                }
            }
        }

        val usefulEntries = localResult?.usefulEntries.orEmpty()
        if (usefulEntries.isNotEmpty() && localResult?.exactMatch == null) {
            Spacer(Modifier.height(16.dp))
            Text(strings.ui(UiCopyKey.DICTIONARY_RESULTS), fontWeight = FontWeight.Bold)
            usefulEntries.take(20).forEach { entry ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable {
                        onOpenEntry(entry)
                    }
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            localizedRelatedExpressionLabel(
                                relatedExpressionLabel(
                                    input,
                                    if (frenchToSaamaka) entry.french else entry.saamaka
                                ),
                                strings
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF8A6712)
                        )
                        Text(entry.french, fontWeight = FontWeight.Bold)
                        Text(entry.saamaka.ifBlank { strings.ui(UiCopyKey.MISSING_TRANSLATION) })
                    }
                }
            }
        }

        if (
            localResult != null &&
            localResult?.exactMatch == null &&
            usefulEntries.isEmpty() &&
            normalizedInputWordCount(input) <= 1
        ) {
            Spacer(Modifier.height(16.dp))
            Text(
                text = strings.noResult,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (shouldOfferPremiumTranslation(input, localResult) && phraseResult == null) {
            Spacer(Modifier.height(16.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = strings.ui(UiCopyKey.TRANSLATE_THIS_PHRASE),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color(0xFFFFEFC4)
                        ) {
                            Text(
                                text = strings.premium,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF6D5312)
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = strings.ui(UiCopyKey.NO_COMPLETE_EXPRESSION),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        enabled = canUsePremium && !isLoading,
                    onClick = {
                        launchRequest {
                            translateCurrentPhrase()
                        }
                    }
                    ) {
                        Icon(Icons.Default.WorkspacePremium, null)
                        Spacer(Modifier.width(8.dp))
                        Text(strings.ui(UiCopyKey.TRANSLATE_PHRASE))
                    }
                    if (!canUsePremium) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = when (accessLevel) {
                        AccessLevel.GUEST -> strings.ui(UiCopyKey.CREATE_ACCOUNT_FOR_TRIALS)
                        AccessLevel.FREE_ACCOUNT -> strings.ui(UiCopyKey.NO_TRIALS_LEFT)
                        else -> strings.ui(UiCopyKey.PREMIUM_TRANSLATION_UNAVAILABLE)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        phraseResult?.let { result ->
            Spacer(Modifier.height(16.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        when {
                            result.kind == PhraseTranslationKind.VALIDATED_RULE ->
                                strings.ui(UiCopyKey.VALIDATED_RULE_TRANSLATION)
                            result.kind == PhraseTranslationKind.GRAMMATICAL ->
                                strings.ui(UiCopyKey.GRAMMATICAL_TRANSLATION)
                            result.isComplete -> "Proposition locale"
                            else -> strings.ui(UiCopyKey.INCOMPLETE_SUGGESTION)
                        },
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0B5D3B)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(result.translation, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(strings.ui(UiCopyKey.RELIABILITY, strings.reliabilityLabel(result.reliability)))
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = { showDetails = !showDetails }) {
                        Text(if (showDetails) strings.ui(UiCopyKey.HIDE_DETAILS) else strings.ui(UiCopyKey.SHOW_DETAILS))
                    }
                    if (showDetails) {
                        result.recognizedSegments.forEach { segment ->
                            Text("${segment.source} → ${segment.matchedSource ?: segment.source} → ${segment.translation}")
                            segment.alternatives.takeIf { it.isNotEmpty() }?.let {
                                Text(strings.ui(UiCopyKey.OTHER_POSSIBILITIES, it.joinToString(", ")))
                            }
                        }
                        if (result.untranslatedSegments.isNotEmpty()) {
                            Text(strings.ui(UiCopyKey.ITEMS_TO_REVIEW, result.untranslatedSegments.joinToString(", ")))
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    ResultActionRow(
                        strings = strings,
                        text = localizedShareableText(result, strings),
                        canListen = ttsReady && result.isComplete,
                        onListen = {
                            tts.speak(result.translation, TextToSpeech.QUEUE_FLUSH, null, "phrase_result")
                        }
                    )
                }
            }
        }

        if (localResult != null || phraseResult != null) {
            Spacer(Modifier.height(12.dp))
            OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = {
                input = ""; resetResults()
            }) { Text(strings.ui(UiCopyKey.NEW_SEARCH)) }
        }
    }
}

@Composable
private fun ResultActionRow(
    strings: AppStrings,
    text: String,
    canListen: Boolean,
    onListen: () -> Unit
) {
    val context = LocalContext.current
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedButton(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
            onClick = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Saamaka Dico", text))
            }
        ) {
            Text(strings.ui(UiCopyKey.COPY_ACTION), maxLines = 1, softWrap = false)
        }
        OutlinedButton(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
            enabled = canListen,
            onClick = onListen
        ) {
            Text(strings.ui(UiCopyKey.LISTEN), maxLines = 1, softWrap = false)
        }
        OutlinedButton(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
            onClick = {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text)
                }
                context.startActivity(Intent.createChooser(intent, "Partager"))
            }
        ) {
            Text(strings.ui(UiCopyKey.SHARE_ACTION), maxLines = 1, softWrap = false)
        }
    }
}

@Composable
private fun PhraseTranslateContent(
    strings: AppStrings,
    accessLevel: AccessLevel,
    remainingTrials: Int,
    onUseTrial: () -> Unit,
    onTranslate: suspend (String, Boolean) -> PhraseTranslationResult?
) {
    var showTranslator by remember { mutableStateOf(false) }
    var sourceText by remember { mutableStateOf("") }
    var translationResult by remember { mutableStateOf<PhraseTranslationResult?>(null) }
    var showTranslationDetails by remember { mutableStateOf(false) }
    var translationJob by remember { mutableStateOf<Job?>(null) }
    var translationRequestId by remember { mutableStateOf(0L) }
    var isTranslating by remember { mutableStateOf(false) }
    var translationError by remember { mutableStateOf<String?>(null) }
    val translationScope = rememberCoroutineScope()
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
            translationJob?.cancel()
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
                            text = "${strings.french} ↔ ${strings.saamaka}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = gold
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    text = strings.ui(UiCopyKey.TRANSLATOR_INTRO),
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
                                text = strings.ui(UiCopyKey.CREATE_FREE_ACCOUNT),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = darkText
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        Text(strings.ui(UiCopyKey.FREE_TRANSLATION_TRIALS))

                        Spacer(Modifier.height(14.dp))

                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = forestGreen),
                            onClick = {
                                // Compte réel plus tard
                            }
                        ) {
                            Text(strings.ui(UiCopyKey.CREATE_MY_FREE_ACCOUNT))
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
                                text = strings.ui(UiCopyKey.PREMIUM_TITLE),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = darkText
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = strings.ui(UiCopyKey.PREMIUM_BENEFITS)
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
                            Text(strings.ui(UiCopyKey.DISCOVER_PREMIUM))
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
                                text = strings.ui(UiCopyKey.REMAINING_TRANSLATIONS, remainingTrials),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = darkText
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = strings.ui(UiCopyKey.FREE_ACCOUNT_TRIAL_INFO)
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
                            Text(strings.ui(UiCopyKey.START_TRANSLATION))
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                Text(
                    text = strings.ui(UiCopyKey.PREMIUM_UNLIMITED),
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
                                text = strings.premium,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = darkText
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = strings.ui(UiCopyKey.UNLIMITED_SENTENCE_TRANSLATIONS)
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
                            Text(strings.ui(UiCopyKey.TRANSLATE_NOW))
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
                                text = strings.ui(UiCopyKey.TESTER_MODE),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = darkText
                            )
                        }

                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = strings.ui(UiCopyKey.TEST_TRANSLATOR_ENABLED)
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
                            Text(strings.ui(UiCopyKey.TEST_TRANSLATION))
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
                            text = strings.ui(UiCopyKey.TRANSLATE_A_PHRASE),
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
                            translationJob?.cancel()
                            translationRequestId++
                            isTranslating = false
                            frenchToSaamaka = true
                            translationResult = null
                            showTranslationDetails = false
                        }
                    ) {
                        Text("FR  →  SM")
                    }
                } else {
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        onClick = {
                            translationJob?.cancel()
                            translationRequestId++
                            isTranslating = false
                            frenchToSaamaka = true
                            translationResult = null
                            showTranslationDetails = false
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
                            translationJob?.cancel()
                            translationRequestId++
                            isTranslating = false
                            frenchToSaamaka = false
                            translationResult = null
                            showTranslationDetails = false
                        }
                    ) {
                        Text("SM  →  FR")
                    }
                } else {
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        onClick = {
                            translationJob?.cancel()
                            translationRequestId++
                            isTranslating = false
                            frenchToSaamaka = false
                            translationResult = null
                            showTranslationDetails = false
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
                    translationJob?.cancel()
                    translationRequestId++
                    isTranslating = false
                    translationError = null
                    sourceText = it
                    translationResult = null
                    showTranslationDetails = false
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                label = {
                    Text(
                        if (frenchToSaamaka) {
                            strings.ui(UiCopyKey.FRENCH_PHRASE)
                        } else {
                            "Phrase en Saamaka"
                        }
                    )
                },
                placeholder = {
                    Text(strings.ui(UiCopyKey.WRITE_PHRASE_HINT))
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
                enabled = sourceText.isNotBlank() && !isTranslating,
                colors = ButtonDefaults.buttonColors(containerColor = forestGreen),
                onClick = {

                    translationJob?.cancel()
                    val requestId = ++translationRequestId
                    translationJob = translationScope.launch {
                        isTranslating = true
                        translationError = null
                        try {
                            val result = onTranslate(sourceText, frenchToSaamaka)
                            if (requestId == translationRequestId && result != null) {
                                translationResult = result
                                showTranslationDetails = false

                                if (
                                    accessLevel == AccessLevel.FREE_ACCOUNT &&
                                    remainingTrials > 0
                                ) {
                                    onUseTrial()
                                }
                            } else if (requestId == translationRequestId) {
                                translationError = strings.ui(UiCopyKey.NO_RESULT_RETRY)
                            }
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (_: Throwable) {
                            if (requestId == translationRequestId) {
                                translationError = strings.ui(UiCopyKey.TRANSLATION_FAILED_RETRY)
                            }
                        } finally {
                            if (requestId == translationRequestId) {
                                isTranslating = false
                            }
                        }
                    }
                }
            ) {
                if (isTranslating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(strings.ui(UiCopyKey.TRANSLATING), fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null)
                    Spacer(Modifier.width(8.dp))
                    Text(strings.translate, fontWeight = FontWeight.Bold)
                }
            }

            translationError?.let { error ->
                Spacer(Modifier.height(8.dp))
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
                }
            }

            // -------------------------------------------------
            // RÉSULTAT
            // -------------------------------------------------

            translationResult?.let { result ->

                Spacer(Modifier.height(14.dp))

                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = forestGreen),
                    onClick = {
                        sourceText = ""
                        translationResult = null
                        showTranslationDetails = false
                    }
                ) {
                    Icon(Icons.Default.Refresh, null)
                    Spacer(Modifier.width(8.dp))
                    Text(strings.ui(UiCopyKey.NEW_PHRASE))
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
                                text = if (result.kind == PhraseTranslationKind.VALIDATED_RULE) {
                                    strings.ui(UiCopyKey.VALIDATED_RULE_TRANSLATION)
                                } else if (result.kind == PhraseTranslationKind.GRAMMATICAL) {
                                    strings.ui(UiCopyKey.GRAMMATICAL_TRANSLATION)
                                } else if (frenchToSaamaka) {
                                    if (result.isComplete) {
                                        "Proposition Saamaka"
                                    } else {
                                        strings.ui(UiCopyKey.LOCAL_INCOMPLETE_SUGGESTION)
                                    }
                                } else {
                                    if (result.isComplete) {
                                        strings.ui(UiCopyKey.FRENCH_SUGGESTION)
                                    } else {
                                        strings.ui(UiCopyKey.LOCAL_INCOMPLETE_SUGGESTION)
                                    }
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
                            text = result.translation,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = darkText
                        )

                        Spacer(Modifier.height(6.dp))

                        Text(
                            text = strings.ui(UiCopyKey.RELIABILITY, strings.reliabilityLabel(result.reliability)),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (result.reliability == TranslationReliability.LOW) {
                                Color(0xFF8B2F2F)
                            } else {
                                forestGreen
                            }
                        )

                        Spacer(Modifier.height(12.dp))

                        if (result.kind != PhraseTranslationKind.VALIDATED_RULE) Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = cream
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = strings.toReview,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF8A6712)
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = strings.ui(UiCopyKey.LOCAL_DICTIONARY_SUGGESTION),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = darkText
                                )
                            }
                        }

                        val hasDetails = result.untranslatedSegments.isNotEmpty() ||
                            result.recognizedSegments.isNotEmpty()

                        if (hasDetails) {
                            Spacer(Modifier.height(10.dp))
                            OutlinedButton(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { showTranslationDetails = !showTranslationDetails }
                            ) {
                                Text(if (showTranslationDetails) strings.ui(UiCopyKey.HIDE_DETAILS) else strings.ui(UiCopyKey.SHOW_DETAILS))
                            }

                            if (showTranslationDetails) {
                                Spacer(Modifier.height(10.dp))
                                result.recognizedSegments.forEach { segment ->
                                    val matchedSource = segment.matchedSource ?: segment.source
                                    Text(
                                        text = "${segment.source} → $matchedSource → ${segment.translation}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = darkText
                                    )

                                    segment.detail?.let { detail ->
                                        Text(
                                            text = detail,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = darkText
                                        )
                                    }

                                    if (segment.alternatives.isNotEmpty()) {
                                        Text(
                                            text = strings.ui(UiCopyKey.OTHER_POSSIBILITIES, segment.alternatives.joinToString(", ")),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Spacer(Modifier.height(8.dp))
                                }

                                if (result.untranslatedSegments.isNotEmpty()) {
                                    Spacer(Modifier.height(10.dp))
                                    Text(strings.toReview, fontWeight = FontWeight.Bold, color = darkText)
                                    Text(
                                        text = result.untranslatedSegments.joinToString(", "),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFF8B2F2F)
                                    )
                                }
                            }
                        }

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
                                    val copyText = localizedShareableText(result, strings)
                                    clipboard.setPrimaryClip(
                                        ClipData.newPlainText("Traduction Saamaka Dico", copyText)
                                    )
                                    Toast.makeText(context, strings.ui(UiCopyKey.TRANSLATION_COPIED), Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(17.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(strings.ui(UiCopyKey.COPY_ACTION), maxLines = 1)
                            }

                            OutlinedButton(
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 10.dp),
                                shape = RoundedCornerShape(14.dp),
                                enabled = textToSpeechReady && result.isComplete,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = forestGreen),
                                onClick = {
                                    if (!frenchToSaamaka) {
                                        textToSpeech.language = Locale.FRENCH
                                    }
                                    textToSpeech.speak(
                                        result.translation,
                                        TextToSpeech.QUEUE_FLUSH,
                                        null,
                                        "translation_result"
                                    )
                                }
                            ) {
                                Icon(Icons.AutoMirrored.Filled.VolumeUp, null, modifier = Modifier.size(17.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(strings.ui(UiCopyKey.LISTEN), maxLines = 1)
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
                                        putExtra(Intent.EXTRA_TEXT, localizedShareableText(result, strings))
                                    }
                                    context.startActivity(Intent.createChooser(intent, strings.ui(UiCopyKey.SHARE_TRANSLATION)))
                                }
                            ) {
                                Icon(Icons.Default.Share, null, modifier = Modifier.size(17.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(strings.ui(UiCopyKey.SHARE_ACTION), maxLines = 1)
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

