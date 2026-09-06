package com.saamaka.dico.testeurs

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.net.Uri
import android.util.Log
import java.util.zip.ZipFile
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.saamaka.dico.testeurs.database.DictionaryDatabase
import com.saamaka.dico.testeurs.billing.PremiumBillingManager
import com.saamaka.dico.testeurs.billing.PREMIUM_PRODUCT_ID
import com.saamaka.dico.testeurs.billing.PremiumEntitlementState
import com.saamaka.dico.testeurs.billing.PremiumPlan
import com.saamaka.dico.testeurs.billing.PremiumVerification
import com.saamaka.dico.testeurs.model.DictionaryEntry
import com.saamaka.dico.testeurs.model.PhraseTranslationKind
import com.saamaka.dico.testeurs.repository.CorrectionStore
import com.saamaka.dico.testeurs.repository.DeletionProposalStore
import com.saamaka.dico.testeurs.repository.NewEntryProposalStore
import com.saamaka.dico.testeurs.repository.FavoritesStore
import com.saamaka.dico.testeurs.repository.HistoryStore
import com.saamaka.dico.testeurs.ui.SearchScreen
import com.saamaka.dico.testeurs.ui.BottomNavigationLabel
import com.saamaka.dico.testeurs.ui.PremiumScreen
import com.saamaka.dico.testeurs.ui.SearchLanguageFilter
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.saamaka.dico.testeurs.UiLanguage
import java.io.File
import com.saamaka.dico.testeurs.ui.TranslateScreen
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.ui.Alignment
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID


private val LightColors = lightColorScheme(
    primary = Color(0xFF0F5A3C),          // vert forêt principal
    onPrimary = Color.White,

    primaryContainer = Color(0xFFDCEBDD), // vert très clair
    onPrimaryContainer = Color(0xFF123528),

    secondary = Color(0xFFC99A2E),        // touche dorée
    onSecondary = Color(0xFF2B2108),

    secondaryContainer = Color(0xFFF7EAC2),
    onSecondaryContainer = Color(0xFF3C2D06),

    background = Color(0xFFFFFBF3),       // crème très clair
    onBackground = Color(0xFF1F2B24),

    surface = Color(0xFFFFFBF3),
    onSurface = Color(0xFF1F2B24),

    surfaceVariant = Color(0xFFF3EDE2),
    onSurfaceVariant = Color(0xFF4C554F),

    outline = Color(0xFF8B948E)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFD1BCFF),
    onPrimary = Color(0xFF3B246B),
    primaryContainer = Color(0xFF52368A),
    onPrimaryContainer = Color(0xFFE9DFFF),
    background = Color(0xFF171217),
    surface = Color(0xFF171217),
    surfaceVariant = Color(0xFF4B454D)
)

class MainActivity : ComponentActivity() {
    private lateinit var premiumBillingManager: PremiumBillingManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        premiumBillingManager = PremiumBillingManager(applicationContext)
        premiumBillingManager.connect()
        setContent {
            MaterialTheme(
                colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    TesterApp(premiumBillingManager)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::premiumBillingManager.isInitialized) {
            premiumBillingManager.restorePurchases()
        }
    }

    override fun onDestroy() {
        if (::premiumBillingManager.isInitialized) {
            premiumBillingManager.close()
        }
        super.onDestroy()
    }
}

enum class MainTab {
    HOME,
    SEARCH,
    FAVORITES,
    LEARN,
    MORE,
    PREMIUM,

    TRANSLATE,
    CATEGORIES,
    MISSION,
    HISTORY,
    CORRECTIONS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TesterApp(premiumBillingManager: PremiumBillingManager) {
    val context = LocalContext.current
    val uiLanguageStore = remember(context) { UiLanguageStore(context) }
    val settingsStore = remember(context) { SettingsStore(context) }
    var uiLanguage by remember { mutableStateOf(uiLanguageStore.load()) }
    fun selectUiLanguage(language: UiLanguage) {
        uiLanguage = language
        uiLanguageStore.save(language)
    }
    var languageMenuExpanded by remember {mutableStateOf(false)}
    var accessMenuExpanded by remember {
        mutableStateOf(false)
    }
    val appStrings = stringsFor(uiLanguage)
    var accessLevel by remember {
        mutableStateOf(
            if (
                TesterAccess.FORCE_TESTER_MODE_FOR_BETA ||
                settingsStore.testerModeEnabled()
            ) {
                AccessLevel.TESTER
            } else {
                AccessLevel.GUEST
            }
        )
    }
    var premiumState by remember(premiumBillingManager) {
        mutableStateOf(premiumBillingManager.state)
    }

    DisposableEffect(premiumBillingManager) {
        val listener: (PremiumEntitlementState) -> Unit = { premiumState = it }
        premiumBillingManager.addListener(listener)
        onDispose { premiumBillingManager.removeListener(listener) }
    }


    val database = remember { DictionaryDatabase(context) }
    val favoritesStore = remember { FavoritesStore(context) }
    val historyStore = remember { HistoryStore(context) }
    val correctionStore = remember { CorrectionStore(context) }
    val deletionProposalStore = remember { DeletionProposalStore(context) }
    var deletionProposals by remember { mutableStateOf(deletionProposalStore.all()) }
    val newEntryProposalStore = remember { NewEntryProposalStore(context) }
    var newEntryProposals by remember { mutableStateOf(newEntryProposalStore.all()) }
    val translationTrialStore = remember {
        TranslationTrialStore(context)
    }
    val phraseTranslationPipeline = remember(database, correctionStore, translationTrialStore) {
        PhraseTranslationPipeline(
            resolvePhrase = { text, frenchToSaamaka ->
                withContext(Dispatchers.IO) {
                    database.preparePhraseTranslationIndex()
                    database.translatePhrase(
                        text = text,
                        frenchToSaamaka = frenchToSaamaka,
                        localCorrections = correctionStore.all()
                    )
                }
            },
            resolveWordByWord = { text, frenchToSaamaka ->
                withContext(Dispatchers.IO) {
                    database.preparePhraseTranslationIndex()
                    database.translateWordByWordPhrase(
                        text = text,
                        frenchToSaamaka = frenchToSaamaka,
                        localCorrections = correctionStore.all()
                    )
                }
            },
            resolveGrammaticalPartial = { text, frenchToSaamaka ->
                withContext(Dispatchers.IO) {
                    database.preparePhraseTranslationIndex()
                    database.translatePartialGrammaticalPhrase(
                        text = text,
                        frenchToSaamaka = frenchToSaamaka,
                        localCorrections = correctionStore.all()
                    )
                }
            },
            remainingTrials = translationTrialStore::remainingTrials,
            consumeTrial = translationTrialStore::useTrial
        )
    }
    val learningTrialStore = remember {
        LearningTrialStore(context)
    }
    var remainingLearningTrials by remember(accessLevel) {
        mutableStateOf(
            LearningActivity.entries.associateWith { activity ->
                learningTrialStore.remainingTrials(accessLevel, activity)
            }
        )
    }
    val assignmentStore = remember { AssignmentStore(context) }
    var testerName by remember { mutableStateOf(correctionStore.testerName()) }
    var showTesterSetup by remember {
        mutableStateOf(accessLevel == AccessLevel.TESTER && testerName.isBlank())
    }
    var showTesterCodePrompt by remember { mutableStateOf(false) }
    var testerNumber by remember {
        mutableStateOf(assignmentStore.testerNumber())
    }

    var assignedCategory by remember {
        mutableStateOf(assignmentStore.assignedCategory())
    }
    val validationStore = remember { ValidationStore(context) }
    val reviewStore = remember { ReviewStore(context) }
    val audioStore = remember { AudioStore(context, appStrings) }
    audioStore.updateStrings(appStrings)

    var validatedCount by remember { mutableStateOf(validationStore.count()) }
    var activeTab by remember {
        mutableStateOf(MainTab.HOME)
    }

    LaunchedEffect(accessLevel) {
        if (
            accessLevel != AccessLevel.TESTER &&
            (
                    activeTab == MainTab.MISSION ||
                            activeTab == MainTab.CORRECTIONS
                    )
        ) {
            activeTab = MainTab.SEARCH
        }
    }

    var selectedEntry by remember { mutableStateOf<DictionaryEntry?>(null) }
    var preferredConsultationLanguage by remember { mutableStateOf(AppLanguage.FRENCH) }
    var correctionEntry by remember { mutableStateOf<DictionaryEntry?>(null) }
    var query by remember { mutableStateOf("") }
    var homeExactCompleteMatch by remember { mutableStateOf<LocalExactMatch?>(null) }
    var homePhraseResult by remember { mutableStateOf<PhraseTranslationPipelineResult?>(null) }
    var homePhraseSubmissionRunning by remember { mutableStateOf(false) }
    var pendingPhraseText by remember { mutableStateOf("") }
    var translatePendingPhraseImmediately by remember { mutableStateOf(false) }
    var selectedLanguage by remember { mutableStateOf(AppLanguage.FRENCH)    }
    var searchLanguageFilter by remember { mutableStateOf(SearchLanguageFilter.ALL) }
    var status by remember { mutableStateOf(appStrings.startSearching) }

    var searchResults by remember { mutableStateOf(emptyList<DictionaryEntry>()) }
    val favoriteResults = remember { mutableStateListOf<DictionaryEntry>() }
    val historyResults = remember { mutableStateListOf<DictionaryEntry>() }
    val total = remember { database.countEntries() }
    val allEntries: List<DictionaryEntry> = remember { database.allEntries() }
    var quizScore by remember { mutableStateOf(0) }
    var quizQuestionNumber by remember { mutableStateOf(1) }
    var learnSection by remember {
        mutableStateOf("")
    }

    fun consumeLearningTrialOrOpenPremium(activity: LearningActivity): Boolean {
        val canLaunch = learningTrialStore.useTrial(accessLevel, activity)
        if (accessLevel == AccessLevel.GUEST || accessLevel == AccessLevel.FREE_ACCOUNT) {
            remainingLearningTrials = remainingLearningTrials +
                (activity to learningTrialStore.remainingTrials(accessLevel, activity))
        }

        if (!canLaunch) {
            Toast.makeText(
                context,
                appStrings.ui(UiCopyKey.LEARNING_TRIALS_EXHAUSTED),
                Toast.LENGTH_LONG
            ).show()
            activeTab = MainTab.PREMIUM
        }
        return canLaunch
    }

    fun launchLearningActivity(section: String) {
        if (learnSection == section) return
        if (
            section == "GAMES" &&
            !consumeLearningTrialOrOpenPremium(LearningActivity.GAMES)
        ) return
        learnSection = section
    }
    val quizEntries = remember(allEntries) {
        allEntries.filter { entry ->

            val saamaka = entry.saamaka.trim()
            val french = entry.french.trim()

            val saamakaWordCount =
                saamaka.split(Regex("\\s+"))
                    .filter { it.isNotBlank() }
                    .size

            val frenchWordCount =
                french.split(Regex("\\s+"))
                    .filter { it.isNotBlank() }
                    .size

            val forbiddenChars = listOf(
                ".", ",", ";", ":", "?", "!",
                "(", ")", "[", "]", "{", "}",
                "/", "\\", "\""
            )

            saamaka.isNotBlank() &&
                    french.isNotBlank() &&

                    // Un seul mot Saamaka pour le quiz de base
                    saamakaWordCount == 1 &&

                    // Traduction courte
                    frenchWordCount in 1..3 &&

                    // Évite les entrées anormalement longues
                    saamaka.length in 2..20 &&
                    french.length in 2..30 &&

                    // Évite phrases, références et formulations complexes
                    forbiddenChars.none { saamaka.contains(it) } &&
                    forbiddenChars.none { french.contains(it) } &&

                    // Évite les traductions qui commencent comme une définition
                    !french.startsWith("le ", ignoreCase = true) &&
                    !french.startsWith("la ", ignoreCase = true) &&
                    !french.startsWith("les ", ignoreCase = true) &&
                    !french.startsWith("un ", ignoreCase = true) &&
                    !french.startsWith("une ", ignoreCase = true) &&

                    // Évite les entrées purement numériques
                    saamaka.any { it.isLetter() } &&
                    french.any { it.isLetter() }
        }
    }

    var quizEntry by remember {
        mutableStateOf(
            quizEntries.randomOrNull()
        )
    }

    var matchingGameKey by remember {
        mutableStateOf(0)
    }

    val matchingEntries = remember(
        quizEntries,
        matchingGameKey
    ) {
        quizEntries
            .shuffled()
            .take(4)
    }

    var selectedSaamakaMatch by remember {
        mutableStateOf<DictionaryEntry?>(null)
    }

    var selectedFrenchMatch by remember {
        mutableStateOf<DictionaryEntry?>(null)
    }

    val matchedEntryIds = remember {
        mutableStateListOf<Int>()
    }

    var matchingFeedback by remember {
        mutableStateOf<String?>(null)
    }

    var matchingErrors by remember {
        mutableStateOf(0)
    }



    var quizAnswers by remember {
        mutableStateOf<List<String>>(emptyList())
    }

    var selectedQuizAnswer by remember {
        mutableStateOf<String?>(null)
    }

    LaunchedEffect(quizEntry) {
        val current = quizEntry ?: return@LaunchedEffect

        val wrongAnswers = quizEntries
            .asSequence()
            .filter { it.id != current.id }
            .map { it.french.trim() }
            .filter { it.isNotBlank() }
            .filter { it != current.french.trim() }
            .distinct()
            .shuffled()
            .take(3)
            .toList()

        quizAnswers =
            (wrongAnswers + current.french.trim())
                .shuffled()

        selectedQuizAnswer = null
    }
    var remainingTranslationTrials by remember(accessLevel) {
        mutableStateOf(
            translationTrialStore.remainingTrials(accessLevel)
        )
    }

    fun applyCorrection(entry: DictionaryEntry): DictionaryEntry {
        val correction = correctionStore.latestForEntry(entry.id)
            ?: return entry

        return entry.copy(
            french = correction.frenchProposed.ifBlank { entry.french },
            saamaka = correction.saamakaProposed.ifBlank { entry.saamaka }
        )
    }

    suspend fun executeSearch(request: SearchRequest): SearchOutcome {
        val cleaned = request.text.trim()
        if (cleaned.isBlank()) return SearchOutcome(cleaned, emptyList(), null)

        return withContext(Dispatchers.IO) {
            val corrections = correctionStore.latestByEntry()
            val relatedCandidates = relatedCandidateSearchTerms(cleaned, request.languageCode)
                .flatMap { term -> database.search(term, request.languageCode, limit = 20) }
            val rawCorrectedResults = (database.search(cleaned, request.languageCode) + relatedCandidates)
                .distinctBy { it.id }
                .map { entry ->
                corrections[entry.id]?.let { correction ->
                    entry.copy(
                        french = correction.frenchProposed.ifBlank { entry.french },
                        saamaka = correction.saamakaProposed.ifBlank { entry.saamaka }
                    )
                } ?: entry
            }
            val correctedResults = if (normalizedInputWordCount(cleaned) > 1) {
                relevantRelatedExpressions(cleaned, rawCorrectedResults, request.languageCode)
            } else {
                rawCorrectedResults
            }
            val frenchToSaamaka = request.sourceLanguageCode == AppLanguage.FRENCH.code
            val supportsLocalExactMatch = request.sourceLanguageCode in setOf(
                AppLanguage.FRENCH.code,
                AppLanguage.SAAMAKA.code
            )
            val phraseTranslation: PhraseTranslationPipelineResult? = null
            var exactMatch = if (supportsLocalExactMatch && normalizedInputWordCount(cleaned) <= 2) {
            findAttestedPhraseRule(cleaned, frenchToSaamaka)?.let { translation ->
                val normalizedTranslation = normalizeAttestedPhraseKey(translation)
                val linkedEntry = allEntries.firstOrNull { entry ->
                    val translatedValue = if (frenchToSaamaka) entry.saamaka else entry.french
                    normalizeAttestedPhraseKey(translatedValue) == normalizedTranslation
                }
                LocalExactMatch(
                    cleaned,
                    translation,
                    LocalMatchProvenance.ATTESTED_EXPRESSION,
                    linkedEntry
                )
            }
            } else null
            if (exactMatch == null && supportsLocalExactMatch && normalizedInputWordCount(cleaned) <= 2) {
            correctedResults.firstOrNull { entry ->
                val source = if (frenchToSaamaka) entry.french else entry.saamaka
                normalizeAttestedPhraseKey(source) == normalizeAttestedPhraseKey(cleaned)
            }?.let { entry ->
                exactMatch = LocalExactMatch(
                    cleaned,
                    if (frenchToSaamaka) entry.saamaka else entry.french,
                    LocalMatchProvenance.DICTIONARY,
                    entry
                )
            }
            }
            SearchOutcome(
                text = cleaned,
                results = correctedResults,
                exactMatch = exactMatch,
                phraseTranslation = phraseTranslation
            )
        }
    }

    LaunchedEffect(appStrings) {
        snapshotFlow {
            SearchRequest(
                text = query,
                languageCode = searchLanguageFilter.language?.code,
                sourceLanguageCode = selectedLanguage.code,
                accessLevel = accessLevel
            )
        }
            .debouncedSearch(::executeSearch)
            .collectLatest { outcome ->
                searchResults = outcome.results
                val authorizedPhrase = outcome.phraseTranslation
                if (authorizedPhrase?.disposition == PhraseTranslationDisposition.PREMIUM_REQUIRED) {
                    activeTab = MainTab.PREMIUM
                }
                homeExactCompleteMatch = outcome.exactMatch
                    ?.takeIf { authorizedPhrase?.disposition != PhraseTranslationDisposition.PREMIUM_REQUIRED }
                status = when {
                    outcome.text.isBlank() -> appStrings.startSearching
                    outcome.results.isEmpty() -> appStrings.noResult
                    else -> "${outcome.results.size} ${appStrings.results}"
                }
            }
    }

    fun refreshFavorites() {
        favoriteResults.clear()
        favoriteResults.addAll(
            database.findByIds(favoritesStore.favoriteIds())
                .map { applyCorrection(it) }
        )
    }

    LaunchedEffect(premiumState.verification, accessLevel) {
        if (accessLevel != AccessLevel.TESTER) {
            accessLevel = when (premiumState.verification) {
                PremiumVerification.VERIFIED_ACTIVE -> AccessLevel.PREMIUM
                PremiumVerification.CHECKING,
                PremiumVerification.PENDING,
                PremiumVerification.VERIFIED_INACTIVE,
                PremiumVerification.UNAVAILABLE -> if (accessLevel == AccessLevel.PREMIUM) {
                    AccessLevel.FREE_ACCOUNT
                } else {
                    accessLevel
                }
            }
        }
    }

    fun updateFavorite(id: Int, favorite: Boolean) {
        favoritesStore.setFavorite(id, favorite)
        refreshFavorites()
    }

    fun openFavorites() {
        refreshFavorites()
        activeTab = MainTab.FAVORITES
    }

    fun refreshHistory() {
        historyResults.clear()
        historyResults.addAll(
            database.findByOrderedIds(historyStore.ids())
                .map { applyCorrection(it) }
        )
    }

    fun openHistory() {
        refreshHistory()
        activeTab = MainTab.HISTORY
    }

    fun preferredDetailLanguage(entry: DictionaryEntry): AppLanguage {
        return preferredTranslationLanguage(entry, uiLanguage)
    }

    fun openEntry(entry: DictionaryEntry) {
        selectedEntry = applyCorrection(entry)
        historyStore.add(entry.id)
        refreshHistory()
    }

    fun openSearchEntry(entry: DictionaryEntry, matchedLanguage: AppLanguage) {
        preferredConsultationLanguage = if (searchLanguageFilter == SearchLanguageFilter.SAAMAKA) {
            preferredDetailLanguage(entry)
        } else {
            matchedLanguage
        }
        selectedEntry = applyCorrection(entry)
        historyStore.add(entry.id)
        refreshHistory()
    }
    fun openNextUnvalidated() {
        val validatedIds = validationStore.ids()
        val availableEntries = allEntries.filter { entry ->
            entry.id !in validatedIds &&
                    entry.french.isNotBlank() &&
                    entry.saamaka.isNotBlank() &&
                    !entry.french.equals("#NAME?", ignoreCase = true) &&
                    !entry.saamaka.equals("#NAME?", ignoreCase = true)
        }

        if (availableEntries.isEmpty()) {
            selectedEntry = null
            Toast.makeText(context, appStrings.ui(UiCopyKey.NO_OTHER_WORD), Toast.LENGTH_SHORT).show()
            return
        }

        val currentId = selectedEntry?.id
        val currentIndex = availableEntries.indexOfFirst { it.id == currentId }
        val nextEntry = when {
            currentIndex == -1 -> availableEntries.first()
            currentIndex < availableEntries.lastIndex -> availableEntries[currentIndex + 1]
            else -> availableEntries.first()
        }
        openEntry(nextEntry)
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    fun submitHomePhrase() {
        if (!shouldAnalyzeAsPhrase(query) || homePhraseSubmissionRunning) return
        homePhraseSubmissionRunning = true
        coroutineScope.launch {
            try {
                val result = phraseTranslationPipeline.translate(
                    text = query,
                    frenchToSaamaka = true,
                    accessLevel = accessLevel
                )
                if (result.trialConsumed) {
                    remainingTranslationTrials = result.remainingTrials
                }
                if (result.disposition == PhraseTranslationDisposition.PREMIUM_REQUIRED) {
                    activeTab = MainTab.PREMIUM
                } else if (result.wordByWordTranslation != null ||
                    normalizedInputWordCount(query) >= 3
                ) {
                    homePhraseResult = result
                }
            } finally {
                homePhraseSubmissionRunning = false
            }
        }
    }

    if (showTesterSetup) {
        TesterNameSetupScreen(
            initialName = testerName,
            strings = appStrings,
            onCancel = { showTesterSetup = false },
            onSave = { name ->

                correctionStore.setTesterName(name)
                testerName = correctionStore.testerName()
                settingsStore.setTesterModeEnabled(true)
                accessLevel = AccessLevel.TESTER
                showTesterSetup = false

                when {
                    name.equals("Fucia", ignoreCase = true) -> {
                        assignmentStore.selectTester(1)
                        testerNumber = 1
                        assignedCategory = "Religion"
                    }

                    name.equals("Testeur2", ignoreCase = true) -> {
                        assignmentStore.selectTester(2)
                        testerNumber = 2
                        assignedCategory = assignmentStore.assignedCategory()
                    }
                }
            }
        )
        return
    }

    if (showTesterCodePrompt) {
        TesterAccessCodeDialog(
            strings = appStrings,
            onDismiss = { showTesterCodePrompt = false },
            onAccessGranted = {
                showTesterCodePrompt = false
                showTesterSetup = true
            }
        )
    }

    Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                TopAppBar(
                    title = {
                        Column {

                            Text(
                                text = when {
                                    correctionEntry != null -> appStrings.proposeCorrection
                                    selectedEntry != null -> appStrings.wordDetails
                                    else -> appStrings.dictionaryTitle
                                },
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF16372A)
                            )

                            if (correctionEntry == null && selectedEntry == null) {

                                val context = LocalContext.current
                                val versionName =
                                    context.packageManager
                                        .getPackageInfo(context.packageName, 0)
                                        .versionName
                                        ?: appStrings.notSpecified

                                Spacer(Modifier.height(1.dp))

                                Text(
                                    text = "${appStrings.ui(UiCopyKey.ENTRIES, total)} • ${appStrings.ui(UiCopyKey.VERSION, versionName)}",
                                    fontSize = 10.sp,
                                    color = Color(0xFF68736C)
                                )

                                Text(
                                    text = when (accessLevel) {
                                        AccessLevel.GUEST -> "👤 ${appStrings.guest}"
                                        AccessLevel.FREE_ACCOUNT -> "🔐 ${appStrings.freeAccount}"
                                        AccessLevel.PREMIUM -> "👑 ${appStrings.premium}"
                                        AccessLevel.TESTER -> "🧪 ${appStrings.tester}"
                                    },
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF0B5D3B)
                                )
                            }
                        }
                    },

                    actions = {

                    // -------------------------
                    // LANGUE DE L'APPLICATION
                    // -------------------------
                    Box {
                        IconButton(
                            modifier = Modifier.semantics {
                                contentDescription = appStrings.ui(UiCopyKey.LANGUAGE_MENU)
                            },
                            onClick = { languageMenuExpanded = true }
                        ) {
                            Text(
                                text = "🌐",
                                style = MaterialTheme.typography.titleLarge
                            )
                        }

                        DropdownMenu(
                            expanded = languageMenuExpanded,
                            onDismissRequest = {
                                languageMenuExpanded = false
                            }
                        ) {
                            DropdownMenuItem(
                                text = { Text("🇸🇷 ${appStrings.saamaka}") },
                                onClick = {
                                    selectUiLanguage(UiLanguage.SAAMAKA)
                                    languageMenuExpanded = false
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("🇫🇷 ${appStrings.french}") },
                                onClick = {
                                    selectUiLanguage(UiLanguage.FRENCH)
                                    languageMenuExpanded = false
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("🇬🇧 ${appStrings.english}") },
                                onClick = {
                                    selectUiLanguage(UiLanguage.ENGLISH)
                                    languageMenuExpanded = false
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("🇳🇱 ${appStrings.dutch}") },
                                onClick = {
                                    selectUiLanguage(UiLanguage.DUTCH)
                                    languageMenuExpanded = false
                                }
                            )
                        }
                    }

                    // -------------------------
                    // MODE D'ACCÈS - TEST V12
                    // -------------------------
                    Box {
                        IconButton(
                            modifier = Modifier.semantics {
                                contentDescription = appStrings.ui(UiCopyKey.ACCESS_MENU)
                            },
                            onClick = { accessMenuExpanded = true }
                        ) {
                            Text(
                                text = when (accessLevel) {
                                    AccessLevel.GUEST -> "👤"
                                    AccessLevel.FREE_ACCOUNT -> "🔐"
                                    AccessLevel.PREMIUM -> "👑"
                                    AccessLevel.TESTER -> "🧪"
                                },
                                style = MaterialTheme.typography.titleLarge
                            )
                        }

                        DropdownMenu(
                            expanded = accessMenuExpanded,
                            onDismissRequest = {
                                accessMenuExpanded = false
                            }
                        ) {
                            DropdownMenuItem(
                                text = { Text("👤 ${appStrings.guest}") },
                                onClick = {
                                    settingsStore.setTesterModeEnabled(false)
                                    accessLevel = AccessLevel.GUEST
                                    accessMenuExpanded = false
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("🔐 ${appStrings.freeAccount}") },
                                onClick = {
                                    settingsStore.setTesterModeEnabled(false)
                                    accessLevel = AccessLevel.FREE_ACCOUNT
                                    accessMenuExpanded = false
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("👑 ${appStrings.premium}") },
                                onClick = {
                                    activeTab = MainTab.PREMIUM
                                    accessMenuExpanded = false
                                }
                            )

                        }
                    }
                }
            )
        },
        bottomBar = {
            if (selectedEntry == null && correctionEntry == null) {

                NavigationBar(
                    containerColor = Color(0xFFFFFBF3),
                    contentColor = Color(0xFF234437),
                    tonalElevation = 4.dp
                ) {

                    val navigationItemColors =
                        NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF0B5D3B),
                            selectedTextColor = Color(0xFF0B5D3B),

                            indicatorColor = Color(0xFFDCEEE2),

                            unselectedIconColor = Color(0xFF68736C),
                            unselectedTextColor = Color(0xFF68736C)
                        )

                    // Recherche : tout le monde
                    // Accueil
                    NavigationBarItem(
                        modifier = Modifier.weight(1f),
                        selected = activeTab == MainTab.HOME ||
                            activeTab == MainTab.CATEGORIES,
                        onClick = {
                            activeTab = MainTab.HOME
                        },
                        colors = navigationItemColors,
                        icon = {
                            Icon(
                                Icons.Default.Home,
                                contentDescription = appStrings.ui(UiCopyKey.HOME_DESCRIPTION)
                            )
                        },
                        label = {
                            BottomNavigationLabel(appStrings.ui(UiCopyKey.HOME))
                        }
                    )

                    // Recherche
                    NavigationBarItem(
                        modifier = Modifier.weight(1f),
                        selected = activeTab == MainTab.SEARCH,
                        onClick = {
                            activeTab = MainTab.SEARCH
                        },
                        colors = navigationItemColors,
                        icon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = appStrings.ui(UiCopyKey.SEARCH_DESCRIPTION)
                            )
                        },
                        label = {
                            BottomNavigationLabel(appStrings.search)
                        }
                    )

                    // Favoris
                    NavigationBarItem(
                        modifier = Modifier.weight(1f),
                        selected = activeTab == MainTab.FAVORITES,
                        onClick = {
                            openFavorites()
                        },
                        colors = navigationItemColors,
                        icon = {
                            Icon(
                                Icons.Default.Favorite,
                                contentDescription = appStrings.ui(UiCopyKey.FAVORITES_DESCRIPTION)
                            )
                        },
                        label = {
                            BottomNavigationLabel(appStrings.favorites)
                        }
                    )

                    // Apprendre
                    NavigationBarItem(
                        modifier = Modifier.weight(1f),
                        selected = activeTab == MainTab.LEARN,
                        onClick = {
                            activeTab = MainTab.LEARN
                        },
                        colors = navigationItemColors,
                        icon = {
                            Icon(
                                Icons.Default.School,
                                contentDescription = appStrings.ui(UiCopyKey.LEARN_DESCRIPTION)
                            )
                        },
                        label = {
                            BottomNavigationLabel(appStrings.ui(UiCopyKey.LEARN))
                        }
                    )

                    // Plus
                    NavigationBarItem(
                        modifier = Modifier.weight(1f),
                        selected = activeTab == MainTab.MORE ||
                            activeTab == MainTab.PREMIUM ||
                            activeTab == MainTab.TRANSLATE ||
                            activeTab == MainTab.HISTORY ||
                            activeTab == MainTab.MISSION ||
                            activeTab == MainTab.CORRECTIONS,
                        onClick = {
                            activeTab = MainTab.MORE
                        },
                        colors = navigationItemColors,
                        icon = {
                            Icon(
                                Icons.Default.MoreHoriz,
                                contentDescription = appStrings.ui(UiCopyKey.MORE_DESCRIPTION)
                            )
                        },
                        label = {
                            BottomNavigationLabel(appStrings.ui(UiCopyKey.MORE))
                        }
                    )



                }
            }
        }
        ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .padding(horizontal = 16.dp)
        ) {
            when {
                correctionEntry != null -> {
                    val entry = correctionEntry!!
                    CorrectionForm(
                        entry = entry,
                        defaultTesterName = correctionStore.testerName(),
                        strings = appStrings,
                        onSave = { proposal ->
                            correctionStore.setTesterName(proposal.testerName)
                            correctionStore.save(proposal)
                            selectedEntry = selectedEntry?.copy(
                                french = proposal.frenchProposed.ifBlank { proposal.frenchCurrent },
                                saamaka = proposal.saamakaProposed.ifBlank { proposal.saamakaCurrent }
                            )
                            reviewStore.save(
                                ReviewAction(
                                    id = System.currentTimeMillis(),
                                    entryId = proposal.entryId,
                                    sourceLanguage = "fr",
                                    targetLanguage = "srm",
                                    frenchCurrent = proposal.frenchCurrent,
                                    saamakaCurrent = proposal.saamakaCurrent,
                                    action = ReviewActionType.CORRECTED,
                                    frenchProposed = proposal.frenchProposed,
                                    saamakaProposed = proposal.saamakaProposed,
                                    comment = proposal.comment,
                                    reviewer = proposal.testerName,
                                    createdAt = proposal.createdAt
                                )
                            )
                            Toast.makeText(context, appStrings.correctionSaved, Toast.LENGTH_SHORT).show()
                            correctionEntry = null
                        },
                        onCancel = {
                            correctionEntry = null
                            selectedEntry = entry
                        }
                    )
                }

                selectedEntry != null -> {
                    val entry = selectedEntry!!
                    DetailScreen(
                        entry = entry,
                        accessLevel = accessLevel,
                        preferredTranslationLanguage = preferredConsultationLanguage,
                        onPreferredTranslationLanguageChange = {
                            preferredConsultationLanguage = it
                        },
                        audioStore = audioStore,
                        testerName = testerName,
                        initiallyFavorite = favoritesStore.isFavorite(entry.id),
                        classification = classifyMissionEntry(
                            entry = entry,
                            hasLocalValidation = validationStore.isValidated(entry.id)
                        ),
                        initialDeletionProposal = deletionProposalStore.proposalFor(entry.id),
                        onDeletionProposal = { reason, comment ->
                            deletionProposalStore.save(
                                DeletionProposal(
                                    entry.id, entry.saamaka, entry.french, entry.english, entry.dutch,
                                    reason, comment, testerName, System.currentTimeMillis()
                                )
                            )
                            deletionProposals = deletionProposalStore.all()
                        },
                        onCancelDeletionProposal = {
                            deletionProposalStore.cancel(entry.id)
                            deletionProposals = deletionProposalStore.all()
                        },
                        onValidate = validate@{
                            if (
                                deletionProposalStore.proposalFor(entry.id) != null ||
                                entry.french.isBlank() ||
                                entry.saamaka.isBlank() ||
                                entry.valide.trim().equals("O", ignoreCase = true) ||
                                validationStore.isValidated(entry.id)
                            ) {
                                return@validate
                            }
                            val reviewerName = correctionStore
                                .testerName()
                                .ifBlank { "Fucia" }

                            reviewStore.save(
                                ReviewAction(
                                    id = System.currentTimeMillis(),
                                    entryId = entry.id,
                                    sourceLanguage = "fr",
                                    targetLanguage = "srm",
                                    frenchCurrent = entry.french,
                                    saamakaCurrent = entry.saamaka,
                                    action = ReviewActionType.VALIDATED,
                                    frenchProposed = null,
                                    saamakaProposed = null,
                                    comment = "",
                                    reviewer = reviewerName,
                                    createdAt = System.currentTimeMillis()
                                )
                            )

                            validationStore.validate(entry.id)
                            validatedCount = validationStore.count()

                            Toast.makeText(
                                context,
                                appStrings.ui(UiCopyKey.VALIDATED_BY, reviewerName),
                                Toast.LENGTH_SHORT
                            ).show()

                            openNextUnvalidated()
                        },
                        onNext = { openNextUnvalidated() },
                        onFavoriteChange = { favorite ->
                            updateFavorite(entry.id, favorite)
                        },
                        onCopy = {
                            copyEntry(
                                context,
                                entry,
                                appStrings.wordCopied
                            )
                        },
                        onShare = {
                            shareEntry(
                                context,
                                entry,
                                appStrings.shareChooser
                            )
                        },
                        onCorrection = {
                            if (deletionProposalStore.proposalFor(entry.id) == null) {
                                correctionEntry = entry
                            }
                        },
                        strings = appStrings,
                        onBack = { selectedEntry = null }
                    )
                }

                else -> when (activeTab) {
                    MainTab.PREMIUM -> PremiumScreen(
                        strings = appStrings,
                        state = premiumState,
                        onSubscribe = { plan: PremiumPlan ->
                            val activity = context as? Activity
                            val result = activity?.let {
                                premiumBillingManager.launchSubscription(it, plan)
                            }
                            if (activity == null || result == null) {
                                Toast.makeText(
                                    context,
                                    appStrings.ui(UiCopyKey.PLAN_UNAVAILABLE),
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        },
                        onRestorePurchases = premiumBillingManager::restorePurchases,
                        onRetryBilling = premiumBillingManager::refreshProductDetails,
                        onManageSubscription = { openPremiumSubscriptionManagement(context, appStrings) },
                        onBack = { activeTab = MainTab.MORE }
                    )

                    MainTab.TRANSLATE -> {
                        TranslateScreen(
                            strings = appStrings,
                            initialText = pendingPhraseText,
                            translateInitialTextImmediately = translatePendingPhraseImmediately,
                            onInitialTextHandled = {
                                pendingPhraseText = ""
                                translatePendingPhraseImmediately = false
                            },
                            accessLevel = accessLevel,
                            remainingTrials = remainingTranslationTrials,
                            onTrialsChanged = { remainingTranslationTrials = it },
                            onPremiumRequired = { activeTab = MainTab.PREMIUM },
                            onLocalSearch = { text, frenchToSaamaka ->
                                withContext(Dispatchers.IO) {
                                    database.preparePhraseTranslationIndex()
                                    val corrections = correctionStore.all()
                                    val normalizedInput = normalizeAttestedPhraseKey(text)

                                    fun corrected(entry: DictionaryEntry): DictionaryEntry {
                                        val correction = corrections.firstOrNull { it.entryId == entry.id }
                                            ?: return entry
                                        return entry.copy(
                                            french = correction.frenchProposed.ifBlank { entry.french },
                                            saamaka = correction.saamakaProposed.ifBlank { entry.saamaka }
                                        )
                                    }

                                    val dictionaryEntry = database.exactLocalEntry(text, frenchToSaamaka)
                                    val correctedDictionaryEntry = dictionaryEntry?.let(::corrected)
                                    val dictionaryCorrection = dictionaryEntry?.let { entry ->
                                        corrections.firstOrNull { it.entryId == entry.id }
                                    }
                                    val standaloneCorrection = corrections.firstOrNull { correction ->
                                        val source = if (frenchToSaamaka) {
                                            correction.frenchProposed.ifBlank { correction.frenchCurrent }
                                        } else {
                                            correction.saamakaProposed.ifBlank { correction.saamakaCurrent }
                                        }
                                        normalizeAttestedPhraseKey(source) == normalizedInput
                                    }

                                    val exactMatch = if (normalizedInputWordCount(text) >= 3) {
                                        null
                                    } else when {
                                        correctedDictionaryEntry != null -> {
                                            val translation = if (frenchToSaamaka) {
                                                correctedDictionaryEntry.saamaka
                                            } else {
                                                correctedDictionaryEntry.french
                                            }
                                            LocalExactMatch(
                                                source = text.trim(),
                                                translation = translation,
                                                provenance = if (dictionaryCorrection != null) {
                                                    LocalMatchProvenance.LOCAL_CORRECTION
                                                } else {
                                                    LocalMatchProvenance.DICTIONARY
                                                }
                                            )
                                        }
                                        standaloneCorrection != null -> LocalExactMatch(
                                            source = text.trim(),
                                            translation = if (frenchToSaamaka) {
                                                standaloneCorrection.saamakaProposed.ifBlank {
                                                    standaloneCorrection.saamakaCurrent
                                                }
                                            } else {
                                                standaloneCorrection.frenchProposed.ifBlank {
                                                    standaloneCorrection.frenchCurrent
                                                }
                                            },
                                            provenance = LocalMatchProvenance.LOCAL_CORRECTION
                                        )
                                        else -> findAttestedPhraseRule(text, frenchToSaamaka)?.let {
                                            LocalExactMatch(
                                                source = text.trim(),
                                                translation = it,
                                                provenance = LocalMatchProvenance.ATTESTED_EXPRESSION
                                            )
                                        }
                                    }?.takeIf { it.translation.isNotBlank() }

                                    val languageCode = if (frenchToSaamaka) "fr" else "srm"
                                    val usefulEntries = if (exactMatch == null) {
                                        val wholeTextMatches = database.search(text, languageCode, limit = 20)
                                        val attestedSemanticMatches = relatedCandidateSearchTerms(text, languageCode)
                                            .flatMap { term -> database.search(term, languageCode, limit = 20) }
                                        val segmentMatches = cleanPhraseInput(text)
                                            .split(Regex("\\s+"))
                                            .asSequence()
                                            .filter { it.isNotBlank() }
                                            .mapNotNull { database.exactLocalEntry(it, frenchToSaamaka) }
                                            .toList()
                                        relevantRelatedExpressions(
                                            input = text,
                                            candidates = (wholeTextMatches + attestedSemanticMatches + segmentMatches)
                                                .distinctBy { it.id }
                                                .map(::corrected),
                                            languageCode = languageCode,
                                            limit = 20
                                        )
                                    } else {
                                        emptyList()
                                    }
                                    UnifiedLocalSearchResult(exactMatch, usefulEntries)
                                }
                            },
                            onTranslate = { text, frenchToSaamaka ->
                                phraseTranslationPipeline.translate(
                                    text = text,
                                    frenchToSaamaka = frenchToSaamaka,
                                    accessLevel = accessLevel
                                )
                            },
                            onOpenEntry = ::openEntry
                        )
                    }

                    MainTab.CATEGORIES -> {
                        CategoriesScreen(
                            strings = appStrings,
                            database = database,
                            onBack = {
                                activeTab = MainTab.SEARCH
                            },
                            onOpen = ::openEntry
                        )
                    }

                    MainTab.HOME -> {

                        val homeCategories = remember {
                            database.categories()
                                .filter { it.isNotBlank() }
                                .sortedBy { it.lowercase() }
                        }

                        val wordsOfDay = remember(allEntries) {
                            allEntries
                                .filter { entry ->
                                    entry.saamaka.isNotBlank() &&
                                        entry.french.isNotBlank()
                                }
                                .sortedBy { it.id }
                        }

                        val calendar = java.util.Calendar.getInstance()

                        val dayNumber =
                            calendar.get(java.util.Calendar.DAY_OF_YEAR)

                        val year =
                            calendar.get(java.util.Calendar.YEAR)

                        val wordOfDay =
                            if (wordsOfDay.isNotEmpty()) {
                                wordsOfDay[
                                    ((year * 366L + dayNumber) % wordsOfDay.size)
                                        .toInt()
                                ]
                            } else {
                                null
                            }

                        SearchScreen(
                            categoryCount = homeCategories.size,
                            wordOfDay = wordOfDay,
                            total = total,
                            officiallyValidated = 76,
                            toReview = 990,
                            waiting = (total - 76 - 990).coerceAtLeast(0),
                            query = query,
                            status = status,
                            entries = searchResults,
                            isValidated = validationStore::isValidated,
                            selectedLanguage = selectedLanguage,
                            onLanguageChange = { selectedLanguage = it },

                            onLearnClick = {
                                activeTab = MainTab.LEARN
                            },

                            onQueryChange = {
                                query = it
                                homePhraseResult = null
                            },
                            onClear = {
                                query = ""
                                searchResults = emptyList()
                                homeExactCompleteMatch = null
                                homePhraseResult = null
                                status = appStrings.startSearching
                            },
                            strings = appStrings,
                            onOpen = ::openSearchEntry,
                            onOpenExactDictionaryMatch = ::openSearchEntry,
                            onTranslateClick = ::submitHomePhrase,
                            onPhraseSubmit = ::submitHomePhrase,
                            phraseResult = homePhraseResult,
                            exactCompleteMatch = homeExactCompleteMatch,
                            onFavoritesClick = {
                                openFavorites()
                            },
                            onCategoriesClick = {
                                activeTab = MainTab.CATEGORIES
                            },
                            onHistoryClick = {
                                openHistory()
                            },
                            onWordOfDayClick = {
                                wordOfDay?.let(::openEntry)
                            },
                            searchLanguageFilter = searchLanguageFilter,
                            onSearchLanguageFilterChange = { filter ->
                                searchLanguageFilter = filter
                                selectedLanguage = filter.language ?: AppLanguage.FRENCH
                            },
                            hasAudio = { entry ->
                                audioStore.hasOfficialAudio(entry.id) ||
                                    (testerName.isNotBlank() && audioStore.hasAudio(entry.id, testerName))
                            },
                            onPlayAudio = { entry ->
                                if (audioStore.hasOfficialAudio(entry.id)) {
                                    audioStore.playOfficialAudio(entry.id)
                                } else if (testerName.isNotBlank()) {
                                    audioStore.playAudio(entry.id, testerName)
                                }
                            },
                            isFavorite = { favoritesStore.isFavorite(it.id) },
                            onToggleFavorite = { entry ->
                                updateFavorite(entry.id, !favoritesStore.isFavorite(entry.id))
                            },
                            showHomeContent = true
                        )
                    }

                    MainTab.SEARCH -> {

                        SearchScreen(
                            categoryCount = 0,
                            wordOfDay = null,
                            total = total,
                            officiallyValidated = 76,
                            toReview = 990,
                            waiting = (total - 76 - 990).coerceAtLeast(0),
                            query = query,
                            status = status,
                            entries = searchResults,
                            isValidated = validationStore::isValidated,
                            selectedLanguage = selectedLanguage,
                            onLanguageChange = { selectedLanguage = it },

                            onQueryChange = {
                                query = it
                                homePhraseResult = null
                            },

                            onClear = {
                                query = ""
                                searchResults = emptyList()
                                homeExactCompleteMatch = null
                                homePhraseResult = null
                                status = appStrings.startSearching
                            },

                            strings = appStrings,
                            onOpen = ::openSearchEntry,

                            onTranslateClick = ::submitHomePhrase,
                            onPhraseSubmit = ::submitHomePhrase,
                            phraseResult = homePhraseResult,
                            exactCompleteMatch = homeExactCompleteMatch,

                            onFavoritesClick = {
                                openFavorites()
                            },

                            onCategoriesClick = {
                                activeTab = MainTab.CATEGORIES
                            },

                            onHistoryClick = {
                                openHistory()
                            },

                            onLearnClick = {
                                activeTab = MainTab.LEARN
                            },

                            onWordOfDayClick = { },
                            searchLanguageFilter = searchLanguageFilter,
                            onSearchLanguageFilterChange = { filter ->
                                searchLanguageFilter = filter
                                selectedLanguage = filter.language ?: AppLanguage.FRENCH
                            },
                            hasAudio = { entry ->
                                audioStore.hasOfficialAudio(entry.id) ||
                                    (testerName.isNotBlank() && audioStore.hasAudio(entry.id, testerName))
                            },
                            onPlayAudio = { entry ->
                                if (audioStore.hasOfficialAudio(entry.id)) {
                                    audioStore.playOfficialAudio(entry.id)
                                } else if (testerName.isNotBlank()) {
                                    audioStore.playAudio(entry.id, testerName)
                                }
                            },
                            isFavorite = { favoritesStore.isFavorite(it.id) },
                            onToggleFavorite = { entry ->
                                updateFavorite(entry.id, !favoritesStore.isFavorite(entry.id))
                            },
                            showHomeContent = false
                        )
                    }

                    MainTab.MISSION -> {

                        if (accessLevel != AccessLevel.TESTER) {

                            LaunchedEffect(Unit) {
                                activeTab = MainTab.SEARCH
                            }

                        } else {

                            val categories = remember {
                                database.categories()
                            }

                            var selectedMissionCategory by remember {
                                mutableStateOf(
                                    assignmentStore.assignedCategory()
                                        .ifBlank { categories.firstOrNull().orEmpty() }
                                )
                            }

                            var expandedCategory by remember {
                                mutableStateOf(false)
                            }

                            var missionFilter by remember {
                                mutableStateOf(MissionFilter.ALL)
                            }

                            var missionQuery by remember {
                                mutableStateOf("")
                            }

                            val missionEntries = remember(selectedMissionCategory) {
                                if (selectedMissionCategory.isBlank()) {
                                    emptyList()
                                } else {
                                    database.missionByCategory(
                                        category = selectedMissionCategory,
                                        limit = 50
                                    ).sortedBy {
                                        it.french.trim().contains(" ")
                                    }
                                }
                            }

                            val localCorrections = remember(correctionStore.all().size) {
                                correctionStore.all().associateBy { it.entryId }
                            }

                            val missionClassifications = remember(
                                validatedCount,
                                missionEntries,
                                localCorrections
                            ) {
                                classifyMissionEntries(
                                    entries = missionEntries,
                                    completedIds = validationStore.ids()
                                )
                            }

                            val completedMissionCount = missionClassifications.values
                                .count { it.isCompleted }

                            val filteredMissionEntries = remember(
                                missionEntries,
                                missionFilter,
                                missionQuery,
                                missionClassifications,
                                localCorrections
                            ) {
                                filterMissionEntries(
                                    entries = missionEntries,
                                    filter = missionFilter,
                                    query = missionQuery,
                                    classifications = missionClassifications,
                                    localCorrections = localCorrections
                                )
                            }

                            LazyColumn(
                                contentPadding = PaddingValues(bottom = 28.dp)
                            ) {

                                item {

                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(22.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = Color(0xFF0B5D3B)
                                        ),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(18.dp)
                                        ) {

                                            Text(
                                            text = appStrings.ui(UiCopyKey.MISSION_OF, testerName),
                                                style = MaterialTheme.typography.headlineSmall,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color.White
                                            )

                                            Spacer(
                                                modifier = Modifier.height(4.dp)
                                            )

                                            Text(
                                            text = appStrings.ui(UiCopyKey.MISSION_GUIDANCE),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = Color.White.copy(alpha = 0.82f)
                                            )

                                            Spacer(
                                                modifier = Modifier.height(14.dp)
                                            )

                                            Text(
                                                text = appStrings.ui(UiCopyKey.WORD_COUNT, missionEntries.size),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )

                                            Spacer(modifier = Modifier.height(8.dp))

                                            LinearProgressIndicator(
                                                progress = {
                                                    if (missionEntries.isEmpty()) 0f
                                                    else completedMissionCount.toFloat() / missionEntries.size
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                color = Color(0xFFF0C96A),
                                                trackColor = Color.White.copy(alpha = 0.25f)
                                            )

                                            Spacer(modifier = Modifier.height(6.dp))

                                            Text(
                                            text = appStrings.ui(UiCopyKey.COMPLETED_COUNT, completedMissionCount, missionEntries.size),
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = Color.White
                                            )

                                            Spacer(modifier = Modifier.height(14.dp))

                                            Box(
                                                modifier = Modifier.fillMaxWidth()
                                            ) {

                                                Button(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    shape = RoundedCornerShape(14.dp),
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = Color(0xFFF0C96A),
                                                        contentColor = Color(0xFF16372A)
                                                    ),
                                                    onClick = {
                                                        expandedCategory = true
                                                    }
                                                ) {
                                                    Text(
                                                        if (selectedMissionCategory.isBlank()) {
                                                            appStrings.ui(UiCopyKey.CHOOSE_CATEGORY)
                                                        } else {
                                                            appStrings.ui(UiCopyKey.CATEGORY_VALUE, selectedMissionCategory)
                                                        }
                                                    )
                                                }

                                                DropdownMenu(
                                                    expanded = expandedCategory,
                                                    onDismissRequest = {
                                                        expandedCategory = false
                                                    }
                                                ) {

                                                    categories.forEach { category ->

                                                        DropdownMenuItem(
                                                            text = {
                                                                Text(category)
                                                            },
                                                            onClick = {
                                                                selectedMissionCategory = category
                                                                assignmentStore.selectCategory(category)
                                                                expandedCategory = false
                                                            }

                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Spacer(
                                        modifier = Modifier.height(8.dp)
                                    )

                                    val doubtfulCount = missionClassifications.values
                                        .count { it.visualStatus.isDoubtful }

                                    val toCompleteCount = missionClassifications.values
                                        .count { it.visualStatus.isToComplete }

                                    val filters = listOf(
                                        MissionFilter.ALL to missionEntries.size,
                                        MissionFilter.DOUBTFUL to doubtfulCount,
                                        MissionFilter.TO_COMPLETE to toCompleteCount,
                                        MissionFilter.COMPLETED to completedMissionCount
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        filters.forEach { (filter, count) ->
                                            val selected = missionFilter == filter
                                            Card(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clickable { missionFilter = filter },
                                                shape = RoundedCornerShape(16.dp),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = if (selected) {
                                                        Color(0xFF0B5D3B)
                                                    } else {
                                                        Color(0xFFF4EFE5)
                                                    }
                                                ),
                                                border = BorderStroke(
                                                    if (selected) 2.dp else 1.dp,
                                                    if (selected) Color(0xFFF0C96A) else Color(0xFFE0D8C9)
                                                )
                                            ) {
                                                Column(modifier = Modifier.padding(10.dp)) {
                                                    Text(
                                                        text = "$count",
                                                        style = MaterialTheme.typography.titleLarge,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (selected) Color.White else Color(0xFF16372A)
                                                    )
                                                    Text(
                                                        text = appStrings.missionFilterLabel(filter),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        maxLines = 1,
                                                        color = if (selected) Color.White else Color(0xFF4C554F)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Spacer(
                                        modifier = Modifier.height(14.dp)
                                    )

                                    OutlinedTextField(
                                        value = missionQuery,
                                        onValueChange = { missionQuery = it },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        label = { Text(appStrings.ui(UiCopyKey.MISSION_SEARCH)) },
                                        leadingIcon = {
                                            Icon(Icons.Default.Search, contentDescription = null)
                                        }
                                    )

                                    Spacer(modifier = Modifier.height(8.dp))
                                }

                                if (filteredMissionEntries.isEmpty()) {
                                    item {
                                        LibraryEmptyState(
                                            icon = Icons.Default.CheckCircle,
                                            title = if (missionEntries.isEmpty()) appStrings.ui(UiCopyKey.EMPTY_MISSION) else appStrings.ui(UiCopyKey.NO_FILTER_RESULT),
                                            message = if (missionEntries.isEmpty()) {
                                                appStrings.ui(UiCopyKey.NO_CATEGORY_WORD)
                                            } else {
                                                appStrings.ui(UiCopyKey.NO_SEARCH_FILTER_RESULT)
                                            }
                                        )
                                    }
                                }

                                items(filteredMissionEntries, key = { it.id }) { entry ->

                                    val classification = missionClassifications.getValue(entry.id)

                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 5.dp)
                                            .clickable {
                                                openEntry(entry)
                                            },
                                        shape = RoundedCornerShape(18.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = Color(0xFFFFFBF3)
                                        ),
                                        border = BorderStroke(1.dp, Color(0xFFE0D8C9)),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                    ) {

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(14.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {

                                            Column(
                                                modifier = Modifier.weight(1f)
                                            ) {

                                                Text(
                                                    text = entry.french,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF16372A)
                                                )

                                                Spacer(
                                                    modifier = Modifier.height(4.dp)
                                                )

                                                Text(
                                                    text = if (entry.saamaka.isBlank()) {
                                                        appStrings.ui(UiCopyKey.SAAMAKA_TO_COMPLETE)
                                                    } else {
                                                        "Saamaka : ${entry.saamaka}"
                                                    },
                                                    style = MaterialTheme.typography.bodyMedium
                                                )

                                                Spacer(
                                                    modifier = Modifier.height(6.dp)
                                                )

                                                Text(
                                                    text = appStrings.missionStatusLabel(classification.visualStatus),
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = when {
                                                        classification.visualStatus.isDoubtful ->
                                                            Color(0xFF8A6712)
                                                        classification.visualStatus.isToComplete ->
                                                            Color(0xFF8B2F2F)
                                                        else -> Color(0xFF0B5D3B)
                                                    }
                                                )
                                            }

                                            Icon(
                                                imageVector = Icons.Default.ChevronRight,
                                                contentDescription = null,
                                                tint = Color(0xFF0B5D3B)
                                            )
                                        }
                                    }

                                }
                            }
                        }
                    }

                    MainTab.FAVORITES -> SavedScreen(
                        strings = appStrings,
                        title = if (favoriteResults.isEmpty()) {
                            appStrings.ui(UiCopyKey.NO_FAVORITE)
                        } else {
                            appStrings.ui(UiCopyKey.FAVORITES_COUNT, favoriteResults.size)
                        },
                        entries = favoriteResults,
                        onOpen = ::openEntry,
                        onRemove = { entry ->
                            updateFavorite(entry.id, false)

                            coroutineScope.launch {
                                val result = snackbarHostState.showSnackbar(
                                    message = appStrings.ui(UiCopyKey.REMOVED_FROM_FAVORITES),
                                    actionLabel = appStrings.cancel,
                                    withDismissAction = true,
                                    duration = SnackbarDuration.Long
                                )

                                if (result == SnackbarResult.ActionPerformed) {
                                    updateFavorite(entry.id, true)
                                }
                            }
                        },
                        onSearch = {
                            activeTab = MainTab.SEARCH
                        }
                    )

                    MainTab.HISTORY -> HistoryScreen(
                        strings = appStrings,
                        entries = historyResults,
                        onClear = {
                            historyStore.clear()
                            refreshHistory()
                        },
                        onOpen = ::openEntry,
                        onRemove = { entry ->
                            val previousIndex = historyStore.remove(entry.id)

                            if (previousIndex != null) {
                                refreshHistory()

                                coroutineScope.launch {
                                    val result = snackbarHostState.showSnackbar(
                                        message = appStrings.ui(UiCopyKey.REMOVED_FROM_HISTORY),
                                        actionLabel = appStrings.cancel,
                                        withDismissAction = true,
                                        duration = SnackbarDuration.Long
                                    )

                                    if (result == SnackbarResult.ActionPerformed) {
                                        historyStore.restore(entry.id, previousIndex)
                                        refreshHistory()
                                    }
                                }
                            }
                        },
                        onSearch = {
                            activeTab = MainTab.SEARCH
                        }
                    )

                    MainTab.LEARN -> {

                        val learningPrefs = remember {
                            context.getSharedPreferences(
                                "learning_progress",
                                android.content.Context.MODE_PRIVATE
                            )
                        }

                        var matchingGamesPlayed by remember {
                            mutableStateOf(
                                learningPrefs.getInt("matching_games_played", 0)
                            )
                        }

                        var matchingTotalCorrect by remember {
                            mutableStateOf(
                                learningPrefs.getInt("matching_total_correct", 0)
                            )
                        }

                        var matchingTotalErrors by remember {
                            mutableStateOf(
                                learningPrefs.getInt("matching_total_errors", 0)
                            )
                        }

                        var matchingPerfectGames by remember {
                            mutableStateOf(
                                learningPrefs.getInt("matching_perfect_games", 0)
                            )
                        }
                        var matchingCompletedGameRecorded by remember {
                            mutableStateOf(false)
                        }

                        val knownWordIds = remember {
                            mutableStateListOf<Int>().apply {
                                addAll(
                                    learningPrefs
                                        .getStringSet("known_word_ids", emptySet())
                                        .orEmpty()
                                        .mapNotNull { it.toIntOrNull() }
                                )
                            }
                        }

                        val reviewWordIds = remember {
                            mutableStateListOf<Int>().apply {
                                addAll(
                                    learningPrefs
                                        .getStringSet("review_word_ids", emptySet())
                                        .orEmpty()
                                        .mapNotNull { it.toIntOrNull() }
                                )
                            }
                        }

                        var wordReviewEntry by remember {
                            mutableStateOf(
                                quizEntries.randomOrNull()
                            )
                        }

                        val phraseEntries = remember(allEntries) {
                            allEntries.filter { entry ->

                                val saamaka = entry.saamaka.trim()
                                val french = entry.french.trim()

                                val saamakaWordCount =
                                    saamaka.split(Regex("\\s+"))
                                        .filter { it.isNotBlank() }
                                        .size

                                saamaka.isNotBlank() &&
                                        french.isNotBlank() &&
                                        saamakaWordCount in 2..5 &&
                                        saamaka.length <= 60 &&
                                        french.length <= 80
                            }
                        }

                        var phraseReviewEntry by remember {
                            mutableStateOf(
                                phraseEntries.randomOrNull()
                            )
                        }

                        val phraseKnownIds = remember {
                            mutableStateListOf<Int>().apply {
                                addAll(
                                    learningPrefs
                                        .getStringSet("known_phrase_ids", emptySet())
                                        .orEmpty()
                                        .mapNotNull { it.toIntOrNull() }
                                )
                            }
                        }

                        val phraseReviewIds = remember {
                            mutableStateListOf<Int>().apply {
                                addAll(
                                    learningPrefs
                                        .getStringSet("review_phrase_ids", emptySet())
                                        .orEmpty()
                                        .mapNotNull { it.toIntOrNull() }
                                )
                            }
                        }

                        var knownWordsCount by remember {
                            mutableStateOf(0)
                        }

                        var reviewWordsCount by remember {
                            mutableStateOf(0)
                        }

                        val learnedItemCount =
                            knownWordIds.size + phraseKnownIds.size + matchingTotalCorrect
                        val learningItemTarget =
                            (quizEntries.size + phraseEntries.size).coerceAtLeast(1)
                        val globalLearningProgress =
                            (learnedItemCount.toFloat() / learningItemTarget).coerceIn(0f, 1f)
                        val continueSection =
                            if (reviewWordIds.isNotEmpty()) {
                                "WORDS"
                            } else {
                                learnSection.ifBlank { "QUIZ" }
                            }
                        val continueLabel = when (continueSection) {
                            "WORDS" -> appStrings.ui(UiCopyKey.WORD_REVIEW)
                            "PHRASES" -> "Phrases"
                            "GAMES" -> "Jeu d’association"
                            else -> "Quiz du jour"
                        }

                        val learnScrollState = rememberScrollState()

                        LaunchedEffect(
                            learnSection,
                            matchedEntryIds.size,
                            matchingEntries.size
                        ) {
                            if (
                                learnSection == "GAMES" &&
                                matchingEntries.isNotEmpty() &&
                                matchedEntryIds.size == matchingEntries.size
                            ) {
                                withFrameNanos { }
                                learnScrollState.animateScrollTo(
                                    learnScrollState.maxValue
                                )
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(learnScrollState)
                                .padding(top = 12.dp, bottom = 32.dp)
                        ) {

                            Text(
                                text = appStrings.ui(UiCopyKey.LEARN),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF16372A)
                            )

                            Text(
                                text = appStrings.ui(UiCopyKey.LEARN_AT_YOUR_PACE),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF68736C)
                            )

                            val selectedLearningActivity = when (learnSection) {
                                "QUIZ" -> LearningActivity.QUIZ
                                "WORDS" -> LearningActivity.REVIEW
                                "PHRASES" -> LearningActivity.PHRASES
                                "GAMES" -> LearningActivity.GAMES
                                else -> null
                            }
                            if (
                                (accessLevel == AccessLevel.GUEST ||
                                    accessLevel == AccessLevel.FREE_ACCOUNT) &&
                                selectedLearningActivity != null
                            ) {
                                Text(
                                    text = appStrings.ui(
                                        UiCopyKey.REMAINING_LEARNING_TRIALS,
                                        remainingLearningTrials[selectedLearningActivity] ?: 0
                                    ),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFF68736C)
                                )
                            }

                            Spacer(Modifier.height(12.dp))

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(22.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF0B5D3B)
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(18.dp)
                                ) {

                                    Text(
                                        text = appStrings.ui(UiCopyKey.MY_PROGRESS),
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )

                                    Spacer(Modifier.height(4.dp))

                                    Text(
                                        text = appStrings.ui(UiCopyKey.LEARNING_PATH),
                                        fontSize = 12.sp,
                                        color = Color.White.copy(alpha = 0.78f)
                                    )

                                    Spacer(Modifier.height(8.dp))

                                    LinearProgressIndicator(
                                        progress = { globalLearningProgress },
                                        modifier = Modifier.fillMaxWidth(),
                                        color = Color(0xFFF0C96A),
                                        trackColor = Color.White.copy(alpha = 0.22f)
                                    )

                                    Spacer(Modifier.height(6.dp))

                                    Text(
                                        text = appStrings.ui(UiCopyKey.PATH_PERCENT, (globalLearningProgress * 100).toInt()),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White.copy(alpha = 0.86f)
                                    )

                                    Spacer(Modifier.height(12.dp))

                                    val progressStats = listOf(
                                        knownWordIds.size to appStrings.ui(UiCopyKey.LEARNED_WORDS),
                                        reviewWordIds.size to appStrings.ui(UiCopyKey.REVIEW),
                                        quizQuestionNumber to appStrings.ui(UiCopyKey.TODAY),
                                        matchingPerfectGames to appStrings.ui(UiCopyKey.PERFECT_STREAK)
                                    )

                                    BoxWithConstraints(
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        val useSingleRow =
                                            maxWidth >= 340.dp &&
                                                LocalDensity.current.fontScale <= 1.15f

                                        if (useSingleRow) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                progressStats.forEach { (value, label) ->
                                                    LearnProgressStat(
                                                        modifier = Modifier.weight(1f),
                                                        value = value,
                                                        label = label
                                                    )
                                                }
                                            }
                                        } else {
                                            Column(
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                progressStats.chunked(2).forEach { rowStats ->
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        rowStats.forEach { (value, label) ->
                                                            LearnProgressStat(
                                                                modifier = Modifier.weight(1f),
                                                                value = value,
                                                                label = label
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(12.dp))

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { launchLearningActivity(continueSection) },
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEFC4))
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(appStrings.ui(UiCopyKey.CONTINUE_LEARNING), fontWeight = FontWeight.Bold, color = Color(0xFF16372A))
                                        Text(continueLabel, fontSize = 13.sp, color = Color(0xFF705A1D))
                                    }
                                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF0B5D3B))
                                }
                            }

                            Spacer(Modifier.height(14.dp))

                            Text(appStrings.ui(UiCopyKey.CHOOSE_ACTIVITY), fontWeight = FontWeight.Bold, color = Color(0xFF16372A))
                            Spacer(Modifier.height(8.dp))

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    LearnAccessCard(appStrings.ui(UiCopyKey.QUIZ), appStrings.ui(UiCopyKey.QUICK_QUESTIONS), "QUIZ", learnSection, Modifier.weight(1f), ::launchLearningActivity)
                                    LearnAccessCard(appStrings.ui(UiCopyKey.REVIEW_ACTIVITY), appStrings.ui(UiCopyKey.REVIEW_WORDS), "WORDS", learnSection, Modifier.weight(1f), ::launchLearningActivity)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    LearnAccessCard(appStrings.ui(UiCopyKey.PHRASES), appStrings.ui(UiCopyKey.USEFUL_EXPRESSIONS), "PHRASES", learnSection, Modifier.weight(1f), ::launchLearningActivity)
                                    LearnAccessCard(appStrings.ui(UiCopyKey.GAMES), appStrings.ui(UiCopyKey.MATCH_WORDS), "GAMES", learnSection, Modifier.weight(1f), ::launchLearningActivity)
                                }
                            }

                            Spacer(Modifier.height(18.dp))

                            when (learnSection) {

                                "QUIZ" -> {

                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(22.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = Color(0xFFFFFBF3)
                                        ),
                                        border = BorderStroke(1.dp, Color(0xFFE0D8C9)),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(18.dp)
                                        ) {

                                            Text(
                                                text = appStrings.ui(UiCopyKey.TODAY_QUIZ),
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF16372A)
                                            )

                                            Spacer(Modifier.height(6.dp))

                                            Text(
                                                text = appStrings.ui(UiCopyKey.QUESTION_NUMBER, quizQuestionNumber),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFF0B5D3B)
                                            )

                                            Surface(
                                                shape = RoundedCornerShape(50),
                                                color = Color(0xFFFFEFC4)
                                            ) {
                                                Text(
                                                    text = appStrings.ui(UiCopyKey.SCORE_VALUE, quizScore),
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF705A1D)
                                                )
                                            }

                                            Text(
                                                text = appStrings.ui(UiCopyKey.TEST_KNOWLEDGE),
                                                fontSize = 13.sp,
                                                color = Color(0xFF68736C)
                                            )

                                            Spacer(Modifier.height(20.dp))

                                            Surface(
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(18.dp),
                                                color = Color(0xFFDCEEE2)
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(16.dp)
                                                ) {

                                                    Text(
                                                        text = appStrings.ui(UiCopyKey.WHAT_DOES_WORD_MEAN),
                                                        fontSize = 12.sp,
                                                        color = Color(0xFF68736C)
                                                    )

                                                    Spacer(Modifier.height(10.dp))

                                                    Text(
                                                        text = quizEntry?.saamaka ?: "—",
                                                        fontSize = 28.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF0B5D3B)
                                                    )
                                                }
                                            }

                                            Spacer(Modifier.height(16.dp))

                                            quizAnswers.forEach { answer ->

                                                val isSelected =
                                                    selectedQuizAnswer == answer

                                                val isCorrect =
                                                    quizEntry?.french?.trim() == answer

                                                val hasAnswered =
                                                    selectedQuizAnswer != null

                                                val buttonContainerColor = when {
                                                    !hasAnswered ->
                                                        Color.Transparent

                                                    isCorrect ->
                                                        Color(0xFFDCEEE2)

                                                    isSelected && !isCorrect ->
                                                        Color(0xFFF8DDDD)

                                                    else ->
                                                        Color.Transparent
                                                }

                                                val buttonBorderColor = when {
                                                    !hasAnswered ->
                                                        Color(0xFFB7BDB8)

                                                    isCorrect ->
                                                        Color(0xFF0B5D3B)

                                                    isSelected && !isCorrect ->
                                                        Color(0xFF8B2F2F)

                                                    else ->
                                                        Color(0xFFB7BDB8)
                                                }

                                                val buttonTextColor = when {
                                                    isCorrect && hasAnswered ->
                                                        Color(0xFF0B5D3B)

                                                    isSelected && !isCorrect ->
                                                        Color(0xFF8B2F2F)

                                                    else ->
                                                        Color(0xFF2E332F)
                                                }

                                                OutlinedButton(
                                                    onClick = {
                                                        if (selectedQuizAnswer == null) {
                                                            if (!consumeLearningTrialOrOpenPremium(LearningActivity.QUIZ)) {
                                                                return@OutlinedButton
                                                            }

                                                            selectedQuizAnswer = answer

                                                            if (
                                                                answer ==
                                                                quizEntry?.french?.trim()
                                                            ) {
                                                                quizScore++
                                                            }
                                                        }
                                                    },
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .heightIn(min = 52.dp),
                                                    shape = RoundedCornerShape(14.dp),
                                                    colors =
                                                        ButtonDefaults.outlinedButtonColors(
                                                            containerColor =
                                                                buttonContainerColor,
                                                            contentColor =
                                                                buttonTextColor
                                                        ),
                                                    border = BorderStroke(
                                                        width = 1.dp,
                                                        color = buttonBorderColor
                                                    )
                                                ) {
                                                    Text(
                                                        text = answer,
                                                        modifier =
                                                            Modifier.fillMaxWidth(),
                                                        textAlign =
                                                            TextAlign.Start,
                                                        fontWeight =
                                                            if (
                                                                hasAnswered &&
                                                                (isCorrect || isSelected)
                                                            ) {
                                                                FontWeight.SemiBold
                                                            } else {
                                                                FontWeight.Normal
                                                            }
                                                    )
                                                }

                                                Spacer(Modifier.height(8.dp))
                                            }

                                            selectedQuizAnswer?.let {

                                                val correctAnswer =
                                                    quizEntry
                                                        ?.french
                                                        ?.trim()
                                                        .orEmpty()

                                                if (
                                                    selectedQuizAnswer ==
                                                    correctAnswer
                                                ) {

                                                    Text(
                                                        text = appStrings.ui(UiCopyKey.GOOD_ANSWER),
                                                        fontSize = 13.sp,
                                                        fontWeight =
                                                            FontWeight.SemiBold,
                                                        color =
                                                            Color(0xFF0B5D3B)
                                                    )

                                                } else {

                                                    Text(
                                                        text = appStrings.ui(UiCopyKey.WRONG_ANSWER),
                                                        fontSize = 13.sp,
                                                        fontWeight =
                                                            FontWeight.SemiBold,
                                                        color =
                                                            Color(0xFF8B2F2F)
                                                    )

                                                    Spacer(
                                                        Modifier.height(3.dp)
                                                    )

                                                    Text(
                                                        text =
                                                        appStrings.ui(UiCopyKey.CORRECT_ANSWER, correctAnswer),
                                                        fontSize = 13.sp,
                                                        fontWeight =
                                                            FontWeight.SemiBold,
                                                        color =
                                                            Color(0xFF0B5D3B)
                                                    )
                                                }

                                                Spacer(
                                                    Modifier.height(12.dp)
                                                )

                                                Button(
                                                    onClick = {

                                                        quizEntry =
                                                            quizEntries
                                                                .filter {
                                                                    it.id !=
                                                                            quizEntry?.id
                                                                }
                                                                .randomOrNull()

                                                        quizQuestionNumber++
                                                    },
                                                    modifier =
                                                        Modifier.fillMaxWidth(),
                                                    shape =
                                                        RoundedCornerShape(14.dp),
                                                    colors =
                                                        ButtonDefaults.buttonColors(
                                                            containerColor =
                                                                Color(0xFF0B5D3B)
                                                        )
                                                ) {
                                                    Text(
                                                        text = appStrings.ui(UiCopyKey.NEXT),
                                                        color = Color.White
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                "WORDS" -> {

                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(22.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = Color(0xFFFFFBF3)
                                        ),
                                        border = BorderStroke(1.dp, Color(0xFFE0D8C9)),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(18.dp)
                                        ) {

                                            Text(
                                                text = appStrings.ui(UiCopyKey.WORD_REVIEW),
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF16372A)
                                            )

                                            Spacer(Modifier.height(6.dp))

                                            Text(
                                                text = appStrings.ui(UiCopyKey.REVIEW_VOCABULARY),
                                                fontSize = 13.sp,
                                                color = Color(0xFF68736C)
                                            )

                                            Spacer(Modifier.height(8.dp))

                                            Text(
                                                text = appStrings.ui(UiCopyKey.I_KNOW_REVIEW, knownWordsCount, reviewWordsCount),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFF0B5D3B)
                                            )

                                            Spacer(Modifier.height(22.dp))

                                            Surface(
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(18.dp),
                                                color = Color(0xFFDCEEE2)
                                            ) {
                                                Column(
                                                    modifier =
                                                        Modifier.padding(24.dp)
                                                ) {

                                                    Text(
                                                        text =
                                                            wordReviewEntry
                                                                ?.saamaka
                                                                ?: "—",
                                                        fontSize = 36.sp,
                                                        fontWeight =
                                                            FontWeight.Bold,
                                                        color =
                                                            Color(0xFF0B5D3B)
                                                    )

                                                    Spacer(
                                                        Modifier.height(8.dp)
                                                    )

                                                    Text(
                                                        text =
                                                            wordReviewEntry
                                                                ?.french
                                                                ?: "",
                                                        fontSize = 16.sp,
                                                        fontWeight =
                                                            FontWeight.Medium,
                                                        color =
                                                            Color(0xFF2E332F)
                                                    )

                                                    val reviewAudioEntry = wordReviewEntry
                                                    if (
                                                        reviewAudioEntry != null &&
                                                        audioStore.hasOfficialAudio(reviewAudioEntry.id)
                                                    ) {
                                                        Spacer(Modifier.height(12.dp))
                                                        OutlinedButton(
                                                            onClick = { audioStore.playOfficialAudio(reviewAudioEntry.id) }
                                                        ) {
                                                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                                                            Spacer(Modifier.width(6.dp))
                                                    Text(appStrings.ui(UiCopyKey.LISTEN))
                                                        }
                                                    }
                                                }
                                            }

                                            Spacer(Modifier.height(18.dp))

                                            Row(
                                                modifier =
                                                    Modifier.fillMaxWidth(),
                                                horizontalArrangement =
                                                    Arrangement.spacedBy(10.dp)
                                            ) {

                                                OutlinedButton(
                                                    onClick = {
                                                        if (!consumeLearningTrialOrOpenPremium(LearningActivity.REVIEW)) {
                                                            return@OutlinedButton
                                                        }

                                                        reviewWordsCount++

                                                        wordReviewEntry?.id?.let { id ->
                                                            if (id !in reviewWordIds) {
                                                                reviewWordIds.add(id)
                                                            }

                                                            knownWordIds.remove(id)
                                                            learningPrefs.edit()
                                                                .putStringSet(
                                                                    "known_word_ids",
                                                                    knownWordIds.map { it.toString() }.toSet()
                                                                )
                                                                .putStringSet(
                                                                    "review_word_ids",
                                                                    reviewWordIds.map { it.toString() }.toSet()
                                                                )
                                                                .apply()
                                                        }

                                                        val reviewCandidates = quizEntries.filter {
                                                            it.id in reviewWordIds &&
                                                                    it.id != wordReviewEntry?.id
                                                        }

                                                        val unknownCandidates = quizEntries.filter {
                                                            it.id !in knownWordIds &&
                                                                    it.id !in reviewWordIds &&
                                                                    it.id != wordReviewEntry?.id
                                                        }

                                                        val knownCandidates = quizEntries.filter {
                                                            it.id in knownWordIds &&
                                                                    it.id != wordReviewEntry?.id
                                                        }

                                                        wordReviewEntry = when {
                                                            reviewCandidates.isNotEmpty() &&
                                                                    (0..99).random() < 60 -> {
                                                                reviewCandidates.random()
                                                            }

                                                            unknownCandidates.isNotEmpty() -> {
                                                                unknownCandidates.random()
                                                            }

                                                            reviewCandidates.isNotEmpty() -> {
                                                                reviewCandidates.random()
                                                            }

                                                            knownCandidates.isNotEmpty() -> {
                                                                knownCandidates.random()
                                                            }

                                                            else -> {
                                                                quizEntries
                                                                    .filter { it.id != wordReviewEntry?.id }
                                                                    .randomOrNull()
                                                            }
                                                        }
                                                    },
                                                    modifier =
                                                        Modifier.weight(1f),
                                                    shape =
                                                        RoundedCornerShape(14.dp)
                                                ) {
                                                    Text(appStrings.ui(UiCopyKey.REVIEW))
                                                }

                                                Button(
                                                    onClick = {
                                                        if (!consumeLearningTrialOrOpenPremium(LearningActivity.REVIEW)) {
                                                            return@Button
                                                        }

                                                        knownWordsCount++

                                                        wordReviewEntry?.id?.let { id ->
                                                            if (id !in knownWordIds) {
                                                                knownWordIds.add(id)
                                                            }

                                                            reviewWordIds.remove(id)

                                                            learningPrefs.edit()
                                                                .putStringSet(
                                                                    "known_word_ids",
                                                                    knownWordIds.map { it.toString() }.toSet()
                                                                )
                                                                .putStringSet(
                                                                    "review_word_ids",
                                                                    reviewWordIds.map { it.toString() }.toSet()
                                                                )
                                                                .apply()
                                                        }

                                                        val reviewCandidates = quizEntries.filter {
                                                            it.id in reviewWordIds &&
                                                                    it.id != wordReviewEntry?.id
                                                        }

                                                        val unknownCandidates = quizEntries.filter {
                                                            it.id !in knownWordIds &&
                                                                    it.id !in reviewWordIds &&
                                                                    it.id != wordReviewEntry?.id
                                                        }

                                                        val knownCandidates = quizEntries.filter {
                                                            it.id in knownWordIds &&
                                                                    it.id != wordReviewEntry?.id
                                                        }

                                                        wordReviewEntry = when {
                                                            reviewCandidates.isNotEmpty() &&
                                                                    (0..99).random() < 60 -> {
                                                                reviewCandidates.random()
                                                            }

                                                            unknownCandidates.isNotEmpty() -> {
                                                                unknownCandidates.random()
                                                            }

                                                            reviewCandidates.isNotEmpty() -> {
                                                                reviewCandidates.random()
                                                            }

                                                            knownCandidates.isNotEmpty() -> {
                                                                knownCandidates.random()
                                                            }

                                                            else -> {
                                                                quizEntries
                                                                    .filter { it.id != wordReviewEntry?.id }
                                                                    .randomOrNull()
                                                            }
                                                        }
                                                    },
                                                    modifier =
                                                        Modifier.weight(1f),
                                                    shape =
                                                        RoundedCornerShape(14.dp),
                                                    colors =
                                                        ButtonDefaults.buttonColors(
                                                            containerColor =
                                                                Color(0xFF0B5D3B)
                                                        )
                                                ) {
                                                    Text(
                                                        text = appStrings.ui(UiCopyKey.I_KNOW),
                                                        color = Color.White
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                "PHRASES" -> {

                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(22.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = Color(0xFFFFFBF3)
                                        ),
                                        border = BorderStroke(1.dp, Color(0xFFE0D8C9)),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(18.dp)
                                        ) {

                                            Text(
                                                text = appStrings.ui(UiCopyKey.PHRASES),
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF16372A)
                                            )

                                            Spacer(Modifier.height(6.dp))

                                            Text(
                                                text = appStrings.ui(UiCopyKey.SHORT_EXPRESSIONS),
                                                fontSize = 13.sp,
                                                color = Color(0xFF68736C)
                                            )

                                            Spacer(Modifier.height(8.dp))

                                            Text(
                                                text = appStrings.ui(UiCopyKey.I_KNOW_REVIEW, phraseKnownIds.size, phraseReviewIds.size),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFF0B5D3B)
                                            )

                                            Spacer(Modifier.height(22.dp))

                                            Surface(
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(18.dp),
                                                color = Color(0xFFDCEEE2)
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(18.dp)
                                                ) {

                                                    Text(
                                                        text = phraseReviewEntry?.saamaka ?: "—",
                                                        fontSize = 24.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF0B5D3B)
                                                    )

                                                    Spacer(Modifier.height(10.dp))

                                                    Text(
                                                        text = phraseReviewEntry?.french ?: "",
                                                        fontSize = 16.sp,
                                                        fontWeight = FontWeight.Medium,
                                                        color = Color(0xFF2E332F)
                                                    )
                                                }
                                            }

                                            Spacer(Modifier.height(18.dp))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {

                                                OutlinedButton(
                                                    onClick = {
                                                        if (!consumeLearningTrialOrOpenPremium(LearningActivity.PHRASES)) {
                                                            return@OutlinedButton
                                                        }

                                                        phraseReviewEntry?.id?.let { id ->
                                                            if (id !in phraseReviewIds) {
                                                                phraseReviewIds.add(id)
                                                            }

                                                            phraseKnownIds.remove(id)

                                                            learningPrefs.edit()
                                                                .putStringSet(
                                                                    "known_phrase_ids",
                                                                    phraseKnownIds.map { it.toString() }.toSet()
                                                                )
                                                                .putStringSet(
                                                                    "review_phrase_ids",
                                                                    phraseReviewIds.map { it.toString() }.toSet()
                                                                )
                                                                .apply()
                                                        }

                                                        val reviewCandidates = phraseEntries.filter {
                                                            it.id in phraseReviewIds &&
                                                                    it.id != phraseReviewEntry?.id
                                                        }

                                                        val unknownCandidates = phraseEntries.filter {
                                                            it.id !in phraseKnownIds &&
                                                                    it.id !in phraseReviewIds &&
                                                                    it.id != phraseReviewEntry?.id
                                                        }

                                                        phraseReviewEntry = when {
                                                            reviewCandidates.isNotEmpty() &&
                                                                    (0..99).random() < 60 -> {
                                                                reviewCandidates.random()
                                                            }

                                                            unknownCandidates.isNotEmpty() -> {
                                                                unknownCandidates.random()
                                                            }

                                                            reviewCandidates.isNotEmpty() -> {
                                                                reviewCandidates.random()
                                                            }

                                                            else -> {
                                                                phraseEntries
                                                                    .filter { it.id != phraseReviewEntry?.id }
                                                                    .randomOrNull()
                                                            }
                                                        }
                                                    },
                                                    modifier = Modifier.weight(1f),
                                                    shape = RoundedCornerShape(14.dp)
                                                ) {
                                                    Text(appStrings.ui(UiCopyKey.REVIEW))
                                                }

                                                Button(
                                                    onClick = {
                                                        if (!consumeLearningTrialOrOpenPremium(LearningActivity.PHRASES)) {
                                                            return@Button
                                                        }

                                                        phraseReviewEntry?.id?.let { id ->
                                                            if (id !in phraseKnownIds) {
                                                                phraseKnownIds.add(id)
                                                            }

                                                            phraseReviewIds.remove(id)

                                                            learningPrefs.edit()
                                                                .putStringSet(
                                                                    "known_phrase_ids",
                                                                    phraseKnownIds.map { it.toString() }.toSet()
                                                                )
                                                                .putStringSet(
                                                                    "review_phrase_ids",
                                                                    phraseReviewIds.map { it.toString() }.toSet()
                                                                )
                                                                .apply()
                                                        }

                                                        val reviewCandidates = phraseEntries.filter {
                                                            it.id in phraseReviewIds &&
                                                                    it.id != phraseReviewEntry?.id
                                                        }

                                                        val unknownCandidates = phraseEntries.filter {
                                                            it.id !in phraseKnownIds &&
                                                                    it.id !in phraseReviewIds &&
                                                                    it.id != phraseReviewEntry?.id
                                                        }

                                                        phraseReviewEntry = when {
                                                            reviewCandidates.isNotEmpty() &&
                                                                    (0..99).random() < 60 -> {
                                                                reviewCandidates.random()
                                                            }

                                                            unknownCandidates.isNotEmpty() -> {
                                                                unknownCandidates.random()
                                                            }

                                                            reviewCandidates.isNotEmpty() -> {
                                                                reviewCandidates.random()
                                                            }

                                                            else -> {
                                                                phraseEntries
                                                                    .filter { it.id != phraseReviewEntry?.id }
                                                                    .randomOrNull()
                                                            }
                                                        }
                                                    },
                                                    modifier = Modifier.weight(1f),
                                                    shape = RoundedCornerShape(14.dp),
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = Color(0xFF0B5D3B)
                                                    )
                                                ) {
                                                    Text(
                                                        text = appStrings.ui(UiCopyKey.I_KNOW),
                                                        color = Color.White
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                "GAMES" -> {

                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(22.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = Color(0xFFF4EFE5)
                                        ),
                                        border = BorderStroke(1.dp, Color(0xFFE0D8C9)),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp)
                                        ) {

                                            Text(
                                                text = appStrings.ui(UiCopyKey.MATCH_WORDS_TITLE),
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF16372A)
                                            )

                                            Spacer(Modifier.height(10.dp))

                                            Surface(
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(14.dp),
                                                color = Color(0xFFDCEEE2)
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(12.dp)
                                                ) {

                                                    Text(
                                                        text = appStrings.ui(UiCopyKey.MY_STATISTICS),
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF16372A)
                                                    )

                                                    Spacer(Modifier.height(6.dp))

                                                    Text(
                                                        text = appStrings.ui(UiCopyKey.GAMES_COUNT, matchingGamesPlayed),
                                                        fontSize = 12.sp,
                                                        color = Color(0xFF2E332F)
                                                    )

                                                    Text(
                                                        text = appStrings.ui(UiCopyKey.CORRECT_MATCHES, matchingTotalCorrect),
                                                        fontSize = 12.sp,
                                                        color = Color(0xFF2E332F)
                                                    )

                                                    Text(
                                                        text = appStrings.ui(UiCopyKey.ERRORS_STAT, matchingTotalErrors),
                                                        fontSize = 12.sp,
                                                        color = Color(0xFF2E332F)
                                                    )

                                                    Text(
                                                        text = appStrings.ui(UiCopyKey.PERFECT_GAMES, matchingPerfectGames),
                                                        fontSize = 12.sp,
                                                        color = Color(0xFF2E332F)
                                                    )
                                                }
                                            }

                                            Spacer(Modifier.height(4.dp))

                                            Text(
                                                text = appStrings.ui(UiCopyKey.CHOOSE_SAAMAKA_THEN_TRANSLATION),
                                                fontSize = 12.sp,
                                                color = Color(0xFF68736C)
                                            )

                                            Spacer(Modifier.height(8.dp))

                                            Text(
                                                text = appStrings.ui(UiCopyKey.GAME_SCORE, matchedEntryIds.size, matchingEntries.size, matchingErrors),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFF0B5D3B)
                                            )

                                            Spacer(Modifier.height(8.dp))

                                            LinearProgressIndicator(
                                                progress = {
                                                    if (matchingEntries.isEmpty()) 0f
                                                    else matchedEntryIds.size.toFloat() / matchingEntries.size
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                color = Color(0xFF0B5D3B),
                                                trackColor = Color(0xFFDCEEE2)
                                            )

                                            Spacer(Modifier.height(16.dp))

                                            val shuffledFrenchEntries = remember(
                                                matchingEntries,
                                                matchingGameKey
                                            ) {
                                                matchingEntries.shuffled()
                                            }

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {

                                                // Colonne Saamaka
                                                Column(
                                                    modifier = Modifier.weight(1f)
                                                ) {

                                                    Text(
                                                        text = appStrings.saamaka,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF16372A)
                                                    )

                                                    Spacer(Modifier.height(6.dp))

                                                    matchingEntries.forEach { entry ->

                                                        val isMatched = entry.id in matchedEntryIds
                                                        val isSelected =
                                                            selectedSaamakaMatch?.id == entry.id

                                                        OutlinedButton(
                                                            onClick = {
                                                                if (!isMatched) {
                                                                    selectedSaamakaMatch = entry
                                                                    selectedFrenchMatch = null
                                                                    matchingFeedback = null
                                                                }
                                                            },
                                                            enabled = !isMatched,
                                                            modifier = Modifier.fillMaxWidth(),
                                                            shape = RoundedCornerShape(14.dp),
                                                            contentPadding = PaddingValues(
                                                                horizontal = 10.dp,
                                                                vertical = 8.dp
                                                            ),
                                                            colors = ButtonDefaults.outlinedButtonColors(
                                                                containerColor = when {
                                                                    isMatched -> Color(0xFFDCEEE2)
                                                                    isSelected -> Color(0xFFDCEEE2)
                                                                    else -> Color.Transparent
                                                                }
                                                            )
                                                        ) {
                                                            Text(
                                                                text = if (isMatched) {
                                                                    "✅ ${entry.saamaka}"
                                                                } else {
                                                                    entry.saamaka
                                                                },
                                                                modifier = Modifier.fillMaxWidth(),
                                                                textAlign = TextAlign.Start,
                                                                fontSize = 12.sp,
                                                                maxLines = 2,
                                                                fontWeight = if (isMatched || isSelected) {
                                                                    FontWeight.Bold
                                                                } else {
                                                                    FontWeight.Normal
                                                                }
                                                            )
                                                        }

                                                        Spacer(Modifier.height(6.dp))
                                                    }
                                                }

                                                // Colonne Français
                                                Column(
                                                    modifier = Modifier.weight(1f)
                                                ) {

                                                    Text(
                                                        text = appStrings.french,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF16372A)
                                                    )

                                                    Spacer(Modifier.height(6.dp))

                                                    shuffledFrenchEntries.forEach { entry ->

                                                        val isMatched = entry.id in matchedEntryIds

                                                        Button(
                                                            onClick = {
                                                                if (!isMatched) {

                                                                    selectedFrenchMatch = entry

                                                                    val saamakaSelection =
                                                                        selectedSaamakaMatch

                                                                    if (saamakaSelection == null) {

                                                                        matchingFeedback =
                                                                appStrings.ui(UiCopyKey.CHOOSE_SAAMAKA_FIRST)

                                                                    } else if (
                                                                        saamakaSelection.id == entry.id
                                                                    ) {

                                                                        if (entry.id !in matchedEntryIds) {
                                                                            matchedEntryIds.add(entry.id)
                                                                        }

                                                                        matchingFeedback =
                                                                            "✅ Bonne association"

                                                                        selectedSaamakaMatch = null
                                                                        selectedFrenchMatch = null

                                                                    } else {

                                                                        matchingErrors++

                                                                        matchingFeedback =
                                                                            "❌ Mauvaise association"
                                                                    }
                                                                }
                                                            },
                                                            enabled = !isMatched,
                                                            modifier = Modifier.fillMaxWidth(),
                                                            shape = RoundedCornerShape(14.dp),
                                                            contentPadding = PaddingValues(
                                                                horizontal = 10.dp,
                                                                vertical = 8.dp
                                                            ),
                                                            colors = ButtonDefaults.buttonColors(
                                                                containerColor = if (isMatched) {
                                                                    Color(0xFFDCEEE2)
                                                                } else {
                                                                    Color(0xFFDCEEE2)
                                                                },
                                                                contentColor = Color(0xFF16372A)
                                                            )
                                                        ) {
                                                            Text(
                                                                text = if (isMatched) {
                                                                    "✅ ${entry.french}"
                                                                } else {
                                                                    entry.french
                                                                },
                                                                modifier = Modifier.fillMaxWidth(),
                                                                textAlign = TextAlign.Start,
                                                                fontSize = 12.sp,
                                                                maxLines = 2,
                                                                fontWeight = if (isMatched) {
                                                                    FontWeight.Bold
                                                                } else {
                                                                    FontWeight.Normal
                                                                }
                                                            )
                                                        }

                                                        Spacer(Modifier.height(6.dp))
                                                    }
                                                }
                                            }

                                            matchingFeedback?.let { feedback ->

                                                Spacer(Modifier.height(8.dp))

                                                Text(
                                                    text = feedback,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = when {
                                                        feedback.startsWith("✅") ->
                                                            Color(0xFF0B5D3B)

                                                        feedback.startsWith("❌") ->
                                                            Color(0xFF8B2F2F)

                                                        else ->
                                                            Color(0xFF68736C)
                                                    }
                                                )
                                            }

                                            if (
                                                matchingEntries.isNotEmpty() &&
                                                matchedEntryIds.size == matchingEntries.size
                                            ) {

                                                Spacer(Modifier.height(12.dp))

                                                Surface(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    shape = RoundedCornerShape(16.dp),
                                                    color = Color(0xFFFFEFC4)
                                                ) {
                                                    Column(
                                                        modifier = Modifier.padding(14.dp)
                                                    ) {

                                                    Text(
                                                        text = appStrings.ui(UiCopyKey.GAME_FINISHED),
                                                        fontSize = 15.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF0B5D3B)
                                                    )

                                                    Spacer(Modifier.height(4.dp))

                                                    Text(
                                                        text = appStrings.ui(UiCopyKey.SUCCESSFUL_MATCHES, matchedEntryIds.size, matchingEntries.size),
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = Color(0xFF0B5D3B)
                                                    )

                                                    Spacer(Modifier.height(2.dp))

                                                    Text(
                                                        text = appStrings.ui(UiCopyKey.ERRORS_COUNT, matchingErrors),
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = if (matchingErrors == 0) {
                                                            Color(0xFF0B5D3B)
                                                        } else {
                                                            Color(0xFF8B2F2F)
                                                        }
                                                    )

                                                    Spacer(Modifier.height(4.dp))

                                                    Text(
                                                        text = when {
                                                            matchingErrors == 0 ->
                                                                appStrings.ui(UiCopyKey.EXCELLENT_NO_ERROR)

                                                            matchingErrors <= 2 ->
                                                                appStrings.ui(UiCopyKey.VERY_GOOD)

                                                            else ->
                                                                appStrings.ui(UiCopyKey.WELL_DONE)
                                                        },
                                                        fontSize = 12.sp,
                                                        color = Color(0xFF68736C)
                                                    )
                                                    }
                                                }

                                                Spacer(Modifier.height(10.dp))

                                                Button(
                                                    onClick = {

                                                        if (!matchingCompletedGameRecorded) {
                                                            matchingGamesPlayed++
                                                            matchingTotalCorrect += matchedEntryIds.size
                                                            matchingTotalErrors += matchingErrors

                                                            if (matchingErrors == 0) {
                                                                matchingPerfectGames++
                                                            }

                                                            learningPrefs.edit()
                                                                .putInt(
                                                                    "matching_games_played",
                                                                    matchingGamesPlayed
                                                                )
                                                                .putInt(
                                                                    "matching_total_correct",
                                                                    matchingTotalCorrect
                                                                )
                                                                .putInt(
                                                                    "matching_total_errors",
                                                                    matchingTotalErrors
                                                                )
                                                                .putInt(
                                                                    "matching_perfect_games",
                                                                    matchingPerfectGames
                                                                )
                                                                .apply()
                                                            matchingCompletedGameRecorded = true
                                                        }

                                                        if (!consumeLearningTrialOrOpenPremium(LearningActivity.GAMES)) {
                                                            return@Button
                                                        }

                                                        matchedEntryIds.clear()
                                                        selectedSaamakaMatch = null
                                                        selectedFrenchMatch = null
                                                        matchingFeedback = null
                                                        matchingErrors = 0
                                                        matchingGameKey++
                                                        matchingCompletedGameRecorded = false
                                                    },
                                                    modifier = Modifier.fillMaxWidth(),
                                                    shape = RoundedCornerShape(14.dp),
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = Color(0xFF0B5D3B)
                                                    )
                                                ) {
                                                    Text(
                                                        text = appStrings.ui(UiCopyKey.NEW_GAME),
                                                        color = Color.White
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    MainTab.MORE -> {
                        val historyCount = historyStore.ids().size
                        val correctionsCount = reviewStore.all().size
                        val installedVersionName = remember(context) {
                            context.packageManager
                                .getPackageInfo(context.packageName, 0)
                                .versionName
                        }
                        val versionName = installedVersionName ?: appStrings.notSpecified
                        var showAboutDialog by remember {
                            mutableStateOf(false)
                        }

                        if (showAboutDialog) {
                            AlertDialog(
                                onDismissRequest = { showAboutDialog = false },
                                icon = {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = Color(0xFF0B5D3B)
                                    )
                                },
                                title = {
                                    Text(
                                        text = appStrings.dictionaryTitle,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                text = {
                                    Column {
                                        Text(
                                            text = appStrings.ui(UiCopyKey.VERSION, versionName),
                                            style = MaterialTheme.typography.bodyMedium
                                        )

                                        Spacer(Modifier.height(12.dp))

                                        Text(
                                        text = appStrings.ui(UiCopyKey.COPYRIGHT),
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                },
                                confirmButton = {
                                    TextButton(
                                        onClick = { showAboutDialog = false }
                                    ) {
                                        Text(appStrings.ui(UiCopyKey.CLOSE))
                                    }
                                }
                            )
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(top = 12.dp, bottom = 32.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = appStrings.ui(UiCopyKey.MORE),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF16372A)
                            )

                            Text(
                                text = appStrings.ui(UiCopyKey.MORE_SUBTITLE),
                                fontSize = 13.sp,
                                color = Color(0xFF68736C)
                            )

                            Spacer(Modifier.height(4.dp))

                            Text(
                                text = appStrings.ui(UiCopyKey.EXPLORE),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF16372A)
                            )

                            MoreAccessCard(
                                icon = Icons.Default.Translate,
                                title = appStrings.translate,
                                description = appStrings.ui(UiCopyKey.TRANSLATE_SUBTITLE),
                                onClick = { activeTab = MainTab.TRANSLATE }
                            )

                            MoreAccessCard(
                                icon = Icons.AutoMirrored.Filled.MenuBook,
                                title = appStrings.ui(UiCopyKey.CATEGORIES),
                                description = appStrings.ui(UiCopyKey.CATEGORIES_SUBTITLE),
                                onClick = { activeTab = MainTab.CATEGORIES }
                            )

                            MoreAccessCard(
                                icon = Icons.Default.History,
                                title = appStrings.history,
                                description = appStrings.ui(UiCopyKey.HISTORY_SUBTITLE),
                                badge = historyCount.toString(),
                                onClick = ::openHistory
                            )

                            MoreAccessCard(
                                icon = Icons.Default.Favorite,
                                title = appStrings.ui(UiCopyKey.PREMIUM_TITLE),
                                description = if (premiumState.isPremium) {
                                    appStrings.ui(UiCopyKey.SUBSCRIPTION_ACTIVE)
                                } else {
                                    appStrings.ui(UiCopyKey.MONTHLY_OR_ANNUAL)
                                },
                                onClick = { activeTab = MainTab.PREMIUM }
                            )

                            if (accessLevel == AccessLevel.TESTER) {
                                Spacer(Modifier.height(4.dp))

                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(22.dp),
                                    color = Color(0xFFDCEEE2),
                                    border = BorderStroke(1.dp, Color(0xFFBFD8C6))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = appStrings.ui(UiCopyKey.TESTER_SPACE),
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF16372A)
                                            )

                                            Spacer(Modifier.width(8.dp))

                                            Surface(
                                                shape = RoundedCornerShape(50),
                                                color = Color(0xFFFFEFC4)
                                            ) {
                                                Text(
                                                    text = appStrings.tester.uppercase(Locale.ROOT),
                                                    modifier = Modifier.padding(
                                                        horizontal = 8.dp,
                                                        vertical = 3.dp
                                                    ),
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF6D5312)
                                                )
                                            }
                                        }

                                        MoreAccessCard(
                                            icon = Icons.Default.CheckCircle,
                                            title = appStrings.ui(UiCopyKey.TESTER_MISSION),
                                            description = appStrings.ui(UiCopyKey.TESTER_MISSION_SUBTITLE),
                                            containerColor = Color(0xFFFFFBF3),
                                            onClick = { activeTab = MainTab.MISSION }
                                        )

                                        MoreAccessCard(
                                            icon = Icons.Default.Settings,
                                            title = appStrings.corrections,
                                            description = appStrings.ui(UiCopyKey.TESTER_EXPORTS),
                                            badge = correctionsCount.toString(),
                                            containerColor = Color(0xFFFFFBF3),
                                            onClick = { activeTab = MainTab.CORRECTIONS }
                                        )

                                        MoreAccessCard(
                                            icon = Icons.Default.Settings,
                                            title = appStrings.ui(UiCopyKey.DEACTIVATE_TESTER_MODE),
                                            description = appStrings.ui(UiCopyKey.TESTER_MODE),
                                            containerColor = Color(0xFFFFFBF3),
                                            onClick = {
                                                settingsStore.setTesterModeEnabled(false)
                                                accessLevel = if (premiumState.isPremium) {
                                                    AccessLevel.PREMIUM
                                                } else {
                                                    AccessLevel.GUEST
                                                }
                                            }
                                        )
                                    }
                                }
                            } else {
                                MoreAccessCard(
                                    icon = Icons.Default.Settings,
                                    title = appStrings.ui(UiCopyKey.ACTIVATE_TESTER_MODE),
                                    description = appStrings.ui(UiCopyKey.TESTER_MODE),
                                    onClick = {
                                        if (TesterAccess.REQUIRE_TESTER_ACCESS_CODE) {
                                            showTesterCodePrompt = true
                                        } else {
                                            showTesterSetup = true
                                        }
                                    }
                                )
                            }

                            Spacer(Modifier.height(4.dp))

                            MoreAccessCard(
                                icon = Icons.Default.Info,
                                title = appStrings.ui(UiCopyKey.ABOUT),
                                description = "${appStrings.dictionaryTitle} • ${appStrings.ui(UiCopyKey.VERSION, versionName)}",
                                onClick = { showAboutDialog = true }
                            )
                        }
                    }
                    MainTab.CORRECTIONS -> CorrectionsScreen(
                        strings = appStrings,
                        testerName = correctionStore.testerName(),
                        correctionCount = reviewStore.all().size + deletionProposals.size + newEntryProposals.size,
                        deletionProposals = deletionProposals,
                        newEntryProposals = newEntryProposals,
                        categories = remember { database.categories() },
                        audioStore = audioStore,
                        existingEntries = allEntries,
                        validatedCount = validatedCount,
                        total = total,

                        validatedReviewCount = reviewStore.validatedCount(),
                        correctedReviewCount = reviewStore.correctedCount(),
                        onOpenDeletionProposal = { proposal ->
                            selectedEntry = database.findByIds(setOf(proposal.entryId)).firstOrNull()
                        },
                        onCancelDeletionProposal = { proposal ->
                            deletionProposalStore.cancel(proposal.entryId)
                            deletionProposals = deletionProposalStore.all()
                        },
                        onSaveNewEntryProposal = { proposal ->
                            newEntryProposalStore.save(proposal)
                            newEntryProposals = newEntryProposalStore.all()
                        },
                        onCancelNewEntryProposal = { proposal ->
                            newEntryProposalStore.cancel(proposal.localId)
                            audioStore.deleteProposedEntryAudio(proposal.localId, proposal.testerName)
                            newEntryProposals = newEntryProposalStore.all()
                        },

                        onTesterNameChange = { name ->
                            correctionStore.setTesterName(name)
                            testerName = name

                            if (name.equals("Fucia", ignoreCase = true)) {
                                assignmentStore.selectTester(1)
                                testerNumber = 1
                                assignedCategory = "Religion"
                            }
                        },
                        onExportCorrections = {
                            val exportText = buildString {
                                appendLine(reviewStore.exportText())
                                appendLine()
                                appendLine(deletionProposalStore.exportText())
                                appendLine()
                                appendLine(newEntryProposalStore.exportText())
                                appendLine()
                                appendLine(audioStore.exportAudioSummary())
                            }

                            val zipFile = audioStore.createTesterExportZip(
                                testerName = testerName,
                                exportText = exportText
                            )

                            val exportHistoryStore = ExportHistoryStore(context)

                            exportHistoryStore.markCreated(
                                testerName = testerName,
                                file = zipFile
                            )

                            shareFile(
                                strings = appStrings,
                                context = context,
                                file = zipFile,
                                subject = "Travail testeur Saamaka Dico",
                                testerName = testerName,
                                exportHistoryStore = exportHistoryStore

                            )
                        },
                        onClearCorrections = {
                            correctionStore.clear()
                            Toast.makeText(
                                context,
                                appStrings.ui(UiCopyKey.LOCAL_CORRECTIONS_CLEARED),
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        onClearValidations = {
                            validationStore.clear()
                            validatedCount = 0
                            Toast.makeText(
                                context,
                                appStrings.ui(UiCopyKey.LOCAL_VALIDATIONS_CLEARED),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun TesterNameSetupScreen(
    initialName: String,
    strings: AppStrings,
    onCancel: () -> Unit,
    onSave: (String) -> Unit
) {
    var name by remember(initialName) {
        mutableStateOf(initialName)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            strings.ui(UiCopyKey.WELCOME),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(12.dp))

        Text(
            strings.ui(UiCopyKey.ENTER_NAME_INTRO)
        )

        Spacer(Modifier.height(20.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text(strings.testerSpeakerName)
            },
            singleLine = true
        )

        Spacer(Modifier.height(16.dp))

        Button(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            enabled = name.trim().isNotEmpty(),
            onClick = {
                onSave(name.trim())
            }
        ){
            Text(strings.continueText)
        }

        Spacer(Modifier.height(8.dp))

        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = onCancel
        ) {
            Text(strings.cancel)
        }
    }
}

@Composable
private fun SavedScreen(
    strings: AppStrings,
    title: String,
    entries: List<DictionaryEntry>,
    onOpen: (DictionaryEntry) -> Unit,
    onRemove: (DictionaryEntry) -> Unit,
    onSearch: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
    ) {

        Spacer(Modifier.height(14.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0B5D3B)),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White.copy(alpha = 0.13f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = Color(0xFFF0C96A),
                        modifier = Modifier.padding(9.dp).size(22.dp)
                    )
                }

                Spacer(Modifier.width(12.dp))

                Column {
                    Text(
                        text = strings.ui(UiCopyKey.MY_FAVORITES),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        if (entries.isEmpty()) {
            LibraryEmptyState(
                icon = Icons.Default.FavoriteBorder,
                title = strings.ui(UiCopyKey.NO_FAVORITE),
                message = strings.ui(UiCopyKey.FAVORITES_EMPTY_HELP),
                actionLabel = strings.searchPlaceholder,
                onAction = onSearch
            )
        } else {
            FavoritesList(
                strings = strings,
                entries = entries,
                onOpen = onOpen,
                onRemove = onRemove
            )
        }
    }
}


@Composable
private fun HistoryScreen(
    strings: AppStrings,
    entries: List<DictionaryEntry>,
    onClear: () -> Unit,
    onOpen: (DictionaryEntry) -> Unit,
    onRemove: (DictionaryEntry) -> Unit,
    onSearch: () -> Unit
) {
    var showClearConfirmation by remember { mutableStateOf(false) }

    if (showClearConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearConfirmation = false },
            title = { Text(strings.ui(UiCopyKey.CLEAR_HISTORY_TITLE)) },
            text = { Text(strings.ui(UiCopyKey.CLEAR_HISTORY_MESSAGE)) },
            dismissButton = {
                TextButton(onClick = { showClearConfirmation = false }) {
                    Text(strings.cancel)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearConfirmation = false
                        onClear()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(strings.ui(UiCopyKey.CLEAR_ALL))
                }
            }
        )
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {

        Spacer(Modifier.height(14.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0B5D3B)),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = strings.history,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )

                    Spacer(Modifier.height(3.dp))

                    Text(
                        text = if (entries.isEmpty()) {
                            strings.ui(UiCopyKey.NO_RECENT_WORDS)
                        } else {
                            strings.ui(UiCopyKey.RECENT_WORDS, entries.size)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }

                TextButton(
                    onClick = { showClearConfirmation = true },
                    enabled = entries.isNotEmpty(),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color(0xFFF0C96A),
                        disabledContentColor = Color.White.copy(alpha = 0.35f)
                    )
                ) {
                    Text(text = strings.ui(UiCopyKey.CLEAR_ALL), fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        if (entries.isEmpty()) {
            LibraryEmptyState(
                icon = Icons.Default.History,
                title = strings.history,
                message = strings.ui(UiCopyKey.EMPTY_HISTORY_HELP),
                actionLabel = strings.searchPlaceholder,
                onAction = onSearch
            )
        } else {
            HistoryList(
                strings = strings,
                entries = entries,
                onOpen = onOpen,
                onRemove = onRemove
            )
        }
    }
}


@Composable
private fun CorrectionsScreen(
    testerName: String,
    strings: AppStrings,
    correctionCount: Int,
    deletionProposals: List<DeletionProposal>,
    newEntryProposals: List<NewEntryProposal>,
    categories: List<String>,
    audioStore: AudioStore,
    existingEntries: List<DictionaryEntry>,
    validatedCount: Int,
    total: Int,
    onTesterNameChange: (String) -> Unit,
    onExportCorrections: () -> Unit,
    onClearCorrections: () -> Unit,
    onClearValidations: () -> Unit,
    validatedReviewCount: Int,
    correctedReviewCount: Int,
    onOpenDeletionProposal: (DeletionProposal) -> Unit,
    onCancelDeletionProposal: (DeletionProposal) -> Unit,
    onSaveNewEntryProposal: (NewEntryProposal) -> Unit,
    onCancelNewEntryProposal: (NewEntryProposal) -> Unit
) {
    var pendingCancellation by remember { mutableStateOf<DeletionProposal?>(null) }
    var editedNewEntry by remember { mutableStateOf<NewEntryProposal?>(null) }
    var showNewEntryForm by remember { mutableStateOf(false) }
    var pendingNewEntryCancellation by remember { mutableStateOf<NewEntryProposal?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {

        item {

            Spacer(Modifier.height(14.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0B5D3B)),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = strings.ui(UiCopyKey.CORRECTIONS_SPACE),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = strings.ui(UiCopyKey.TESTER_WORK_GUIDANCE),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.82f)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Identité V13 verrouillée
            OutlinedTextField(
                value = testerName,
                onValueChange = { },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text(strings.tester)
                },
                supportingText = {
                    Text(strings.ui(UiCopyKey.SAVED_IDENTITY))
                },
                singleLine = true,
                readOnly = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF0B5D3B),
                    unfocusedBorderColor = Color(0xFFD2CCC0),
                    focusedContainerColor = Color(0xFFFFFBF3),
                    unfocusedContainerColor = Color(0xFFFFFBF3)
                )
            )

            Spacer(Modifier.height(14.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF4EFE5)),
                border = BorderStroke(1.dp, Color(0xFFE0D8C9)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {

                Column(
                    modifier = Modifier.padding(16.dp)
                ) {

                    Text(
                        text = strings.ui(UiCopyKey.WORK_SUMMARY),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF16372A)
                    )

                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {

                        StatCard(
                            modifier = Modifier.weight(1f),
                            value = "$total",
                            label = strings.ui(UiCopyKey.TESTER_WORDS)
                        )

                        StatCard(
                            modifier = Modifier.weight(1f),
                            value = "$validatedReviewCount",
                            label = strings.ui(UiCopyKey.TESTER_VALIDATIONS)
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {

                        StatCard(
                            modifier = Modifier.weight(1f),
                            value = "$correctedReviewCount",
                            label = strings.ui(UiCopyKey.TESTER_CORRECTIONS)
                        )

                        StatCard(
                            modifier = Modifier.weight(1f),
                            value = "$correctionCount",
                            label = strings.ui(UiCopyKey.TESTER_ACTIONS)
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = strings.ui(UiCopyKey.LOCAL_VALIDATIONS_COUNT, validatedCount),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF68736C)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            Button(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                onClick = {
                    editedNewEntry = null
                    showNewEntryForm = true
                }
            ) {
                Text(strings.proposeNewEntry)
            }

            Spacer(Modifier.height(14.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F7F3)),
                border = BorderStroke(1.dp, Color(0xFFB8D5C3))
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        strings.proposedNewEntries,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0B5D3B)
                    )
                    Text("${newEntryProposals.size} ${strings.newEntriesCount}")
                    if (newEntryProposals.isEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        Text(strings.noProposedNewEntry)
                    } else {
                        newEntryProposals.forEach { proposal ->
                            Spacer(Modifier.height(12.dp))
                            NewEntryProposalCard(
                                proposal = proposal,
                                strings = strings,
                                canEdit = proposal.testerName.trim().equals(testerName.trim(), true),
                                onEdit = {
                                    editedNewEntry = proposal
                                    showNewEntryForm = true
                                },
                                onCancel = { pendingNewEntryCancellation = proposal }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF4F4)),
                border = BorderStroke(1.dp, Color(0xFFE9BDBD))
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        strings.proposedDeletionsSection,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF8B2F2F)
                    )
                    Text(
                        "${deletionProposals.size} ${strings.proposedDeletionsCount}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    if (deletionProposals.isEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        Text(strings.noProposedDeletion)
                    } else {
                        deletionProposals.forEach { proposal ->
                            Spacer(Modifier.height(12.dp))
                            DeletionProposalCard(
                                proposal = proposal,
                                strings = strings,
                                canCancel = proposal.testerName.trim().equals(
                                    testerName.trim(),
                                    ignoreCase = true
                                ),
                                onOpen = { onOpenDeletionProposal(proposal) },
                                onCancel = { pendingCancellation = proposal }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            Button(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                enabled = correctionCount > 0,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0B5D3B)),
                onClick = onExportCorrections
            ) {
                Icon(
                    Icons.Default.Share,
                    contentDescription = null
                )

                Spacer(Modifier.width(8.dp))

                Text(strings.ui(UiCopyKey.EXPORT_MY_WORK))
            }

            Spacer(Modifier.height(8.dp))

            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                onClick = onClearCorrections
            ) {
                Text(strings.ui(UiCopyKey.CLEAR_LOCAL_CORRECTIONS))
            }

            Spacer(Modifier.height(8.dp))

            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                onClick = onClearValidations
            ) {
                Text(strings.ui(UiCopyKey.CLEAR_LOCAL_VALIDATIONS))
            }

            Spacer(Modifier.height(18.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEFC4)),
                border = BorderStroke(1.dp, Color(0xFFE2CC8B))
            ) {

                Column(
                    modifier = Modifier.padding(16.dp)
                ) {

                    Text(
                        text = strings.ui(UiCopyKey.INSTRUCTIONS),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF16372A)
                    )

                    Spacer(Modifier.height(8.dp))

                    Text("• ${strings.instructionSearch}")
                    Spacer(Modifier.height(4.dp))

                    Text("• ${strings.instructionValidate}")
                    Spacer(Modifier.height(4.dp))

                    Text("• ${strings.instructionCorrect}")
                    Spacer(Modifier.height(4.dp))

                    Text("• ${strings.instructionExport}")
                }
            }

            Spacer(Modifier.height(20.dp))
        }
    }

    pendingCancellation?.let { proposal ->
        AlertDialog(
            onDismissRequest = { pendingCancellation = null },
            title = { Text(strings.cancelDeletionProposal) },
            text = { Text(strings.cancelDeletionConfirmation) },
            confirmButton = {
                Button(onClick = {
                    onCancelDeletionProposal(proposal)
                    pendingCancellation = null
                }) { Text(strings.confirm) }
            },
            dismissButton = {
                TextButton(onClick = { pendingCancellation = null }) {
                    Text(strings.cancel)
                }
            }
        )
    }

    if (showNewEntryForm) {
        NewEntryProposalDialog(
            strings = strings,
            testerName = testerName,
            categories = categories,
            original = editedNewEntry,
            audioStore = audioStore,
            isDuplicate = { localId, saamaka ->
                val normalized = normalizeEntryText(saamaka)
                existingEntries.any { normalizeEntryText(it.saamaka) == normalized } ||
                    newEntryProposals.any {
                        it.localId != localId && normalizeEntryText(it.saamaka) == normalized
                    }
            },
            onDismiss = { showNewEntryForm = false },
            onSave = { proposal ->
                onSaveNewEntryProposal(proposal)
                showNewEntryForm = false
            }
        )
    }

    pendingNewEntryCancellation?.let { proposal ->
        AlertDialog(
            onDismissRequest = { pendingNewEntryCancellation = null },
            title = { Text(strings.cancelProposal) },
            text = { Text(strings.cancelNewEntryConfirmation) },
            confirmButton = {
                Button(onClick = {
                    onCancelNewEntryProposal(proposal)
                    pendingNewEntryCancellation = null
                }) { Text(strings.confirm) }
            },
            dismissButton = {
                TextButton(onClick = { pendingNewEntryCancellation = null }) {
                    Text(strings.cancel)
                }
            }
        )
    }
}

@Composable
private fun TesterAccessCodeDialog(
    strings: AppStrings,
    onDismiss: () -> Unit,
    onAccessGranted: () -> Unit
) {
    var code by remember { mutableStateOf("") }
    var showInvalidCode by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.ui(UiCopyKey.TESTER_CODE_TITLE)) },
        text = {
            Column {
                Text(strings.ui(UiCopyKey.TESTER_CODE_PROMPT))
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = code,
                    onValueChange = {
                        code = it
                        showInvalidCode = false
                    },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    isError = showInvalidCode,
                    supportingText = if (showInvalidCode) {
                        { Text(strings.ui(UiCopyKey.TESTER_CODE_INVALID)) }
                    } else {
                        null
                    }
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = code.isNotEmpty(),
                onClick = {
                    if (TesterAccess.isValid(code)) {
                        onAccessGranted()
                    } else {
                        showInvalidCode = true
                    }
                }
            ) {
                Text(strings.continueText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.cancel)
            }
        }
    )
}


@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    value: String,
    label: String
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFFFFBF3),
        border = BorderStroke(1.dp, Color(0xFFE0D8C9))
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0B5D3B)
            )

            Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF68736C)
            )
        }
    }
}

@Composable
private fun LibraryEmptyState(
    icon: ImageVector,
    title: String,
    message: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF4EFE5)),
        border = BorderStroke(1.dp, Color(0xFFE0D8C9))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = RoundedCornerShape(50),
                color = Color(0xFFFFEFC4)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFF0B5D3B),
                    modifier = Modifier.padding(12.dp).size(26.dp)
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color(0xFF16372A),
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = message,
                fontSize = 13.sp,
                color = Color(0xFF68736C),
                textAlign = TextAlign.Center
            )

            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = onAction,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0B5D3B)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(actionLabel)
                }
            }
        }
    }

}

@Composable
private fun DeletionProposalCard(
    proposal: DeletionProposal,
    strings: AppStrings,
    canCancel: Boolean,
    onOpen: () -> Unit,
    onCancel: () -> Unit
) {
    val formattedDate = remember(proposal.createdAt) {
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(proposal.createdAt))
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE4D6D6))
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(
                proposal.saamaka,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF16372A)
            )
            if (proposal.french.isNotBlank()) {
                Text(proposal.french, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(8.dp))
            Text("${strings.deletionReasonLabel} : ${proposal.reason}")
            if (proposal.comment.isNotBlank()) {
                Text("${strings.deletionCommentLabel} : ${proposal.comment}")
            }
            Text("${strings.deletionTesterLabel} : ${proposal.testerName.ifBlank { strings.notSpecified }}")
            Text("${strings.deletionDateLabel} : $formattedDate")
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onOpen, modifier = Modifier.weight(1f)) {
                    Text(strings.openEntry)
                }
                if (canCancel) {
                    TextButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                        Text(strings.cancelDeletionProposal, color = Color(0xFF8B2F2F))
                    }
                }
            }
        }
    }

}

private fun normalizeEntryText(value: String): String =
    value.trim().replace(Regex("\\s+"), " ").lowercase(Locale.ROOT)

private fun cleanEntryText(value: String): String =
    value.trim().replace(Regex("\\s+"), " ")

@Composable
private fun NewEntryProposalCard(
    proposal: NewEntryProposal,
    strings: AppStrings,
    canEdit: Boolean,
    onEdit: () -> Unit,
    onCancel: () -> Unit
) {
    val formattedDate = remember(proposal.createdAt) {
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(proposal.createdAt))
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFC9DED0))
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(proposal.saamaka, fontWeight = FontWeight.ExtraBold, color = Color(0xFF16372A))
            if (proposal.french.isNotBlank()) Text("${strings.french} : ${proposal.french}")
            if (proposal.english.isNotBlank()) Text("${strings.english} : ${proposal.english}")
            if (proposal.dutch.isNotBlank()) Text("${strings.dutch} : ${proposal.dutch}")
            Text("${strings.category} : ${proposal.category}", style = MaterialTheme.typography.bodySmall)
            if (proposal.comment.isNotBlank()) Text("${strings.deletionCommentLabel} : ${proposal.comment}")
            Text("${strings.deletionTesterLabel} : ${proposal.testerName}")
            Text("${strings.deletionDateLabel} : $formattedDate")
            if (proposal.audioFileName != null) {
                Text(strings.proposalAudioRecorded, color = Color(0xFF0B5D3B))
            }
            if (canEdit) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f)) {
                        Text(strings.editProposal)
                    }
                    TextButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                        Text(strings.cancelProposal, color = Color(0xFF8B2F2F))
                    }
                }
            }
        }
    }

}

@Composable
private fun NewEntryProposalDialog(
    strings: AppStrings,
    testerName: String,
    categories: List<String>,
    original: NewEntryProposal?,
    audioStore: AudioStore,
    isDuplicate: (String, String) -> Boolean,
    onDismiss: () -> Unit,
    onSave: (NewEntryProposal) -> Unit
) {
    val context = LocalContext.current
    val localId = remember(original?.localId) { original?.localId ?: UUID.randomUUID().toString() }
    var saamaka by remember(original) { mutableStateOf(original?.saamaka.orEmpty()) }
    var french by remember(original) { mutableStateOf(original?.french.orEmpty()) }
    var english by remember(original) { mutableStateOf(original?.english.orEmpty()) }
    var dutch by remember(original) { mutableStateOf(original?.dutch.orEmpty()) }
    var category by remember(original, categories) {
        mutableStateOf(original?.category ?: categories.firstOrNull().orEmpty())
    }
    var comment by remember(original) { mutableStateOf(original?.comment.orEmpty()) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isRecording by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    var hasAudio by remember(localId, testerName) {
        mutableStateOf(audioStore.hasProposedEntryAudio(localId, testerName))
    }

    DisposableEffect(localId) {
        onDispose { audioStore.stopPlayback() }
    }

    fun startRecording() {
        if (isPlaying) return
        audioStore.startProposedEntryRecording(localId, testerName)
        isRecording = true
    }

    fun dismiss() {
        if (isRecording) audioStore.stopRecording()
        audioStore.stopPlayback()
        isPlaying = false
        if (original == null) audioStore.deleteProposedEntryAudio(localId, testerName)
        onDismiss()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startRecording()
        else Toast.makeText(context, strings.ui(UiCopyKey.MICROPHONE_DENIED), Toast.LENGTH_SHORT).show()
    }

    AlertDialog(
        onDismissRequest = { dismiss() },
        title = { Text(strings.proposeNewEntry) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = saamaka,
                    onValueChange = { saamaka = it; errorMessage = null },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("${strings.saamaka} *") },
                    singleLine = true
                )
                OutlinedTextField(french, { french = it }, Modifier.fillMaxWidth(), label = { Text(strings.french) })
                OutlinedTextField(english, { english = it }, Modifier.fillMaxWidth(), label = { Text(strings.english) })
                OutlinedTextField(dutch, { dutch = it }, Modifier.fillMaxWidth(), label = { Text(strings.dutch) })
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { categoryExpanded = true }
                ) { Text("${strings.category} : ${category.ifBlank { strings.notSpecified }}") }
                DropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false }
                ) {
                    categories.forEach { item ->
                        DropdownMenuItem(
                            text = { Text(item) },
                            onClick = { category = item; categoryExpanded = false }
                        )
                    }
                }
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(strings.optionalComment) },
                    minLines = 2
                )
                Spacer(Modifier.height(10.dp))
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isPlaying,
                    onClick = {
                        if (isRecording) {
                            audioStore.stopRecording()
                            isRecording = false
                            hasAudio = audioStore.hasProposedEntryAudio(localId, testerName)
                        } else {
                            val granted = ContextCompat.checkSelfPermission(
                                context, Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED
                            if (granted) startRecording()
                            else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                ) {
                    Text(if (isRecording) strings.stopProposalAudio else strings.recordProposalAudio)
                }
                if (hasAudio && !isRecording) {
                    Text(strings.proposalAudioRecorded, color = Color(0xFF0B5D3B))
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            if (isPlaying) {
                                audioStore.stopPlayback()
                                isPlaying = false
                            } else {
                                isPlaying = true
                                audioStore.playProposedEntryAudio(localId, testerName) {
                                    isPlaying = false
                                }
                            }
                        }
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (isPlaying) strings.stopListening else strings.listen.replace("▶", "").trim())
                    }
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isPlaying,
                        onClick = {
                            audioStore.stopPlayback()
                            audioStore.deleteProposedEntryAudio(localId, testerName)
                            hasAudio = false
                        }
                    ) { Text(strings.deleteRedo) }
                }
                errorMessage?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !isRecording,
                onClick = {
                    val cleanSaamaka = cleanEntryText(saamaka)
                    val cleanFrench = cleanEntryText(french)
                    val cleanEnglish = cleanEntryText(english)
                    val cleanDutch = cleanEntryText(dutch)
                    when {
                        cleanSaamaka.isBlank() -> errorMessage = strings.saamakaRequired
                        listOf(cleanFrench, cleanEnglish, cleanDutch).all { it.isBlank() } ->
                            errorMessage = strings.atLeastOneTranslation
                        category.isBlank() -> errorMessage = strings.category
                        isDuplicate(localId, cleanSaamaka) -> errorMessage = strings.duplicateEntryWarning
                        else -> {
                            audioStore.stopPlayback()
                            isPlaying = false
                            onSave(NewEntryProposal(
                                localId = localId,
                                saamaka = cleanSaamaka,
                                french = cleanFrench,
                                english = cleanEnglish,
                                dutch = cleanDutch,
                                category = cleanEntryText(category),
                                comment = cleanEntryText(comment),
                                testerName = testerName,
                                createdAt = original?.createdAt ?: System.currentTimeMillis(),
                                audioFileName = audioStore.proposedEntryAudioFile(localId, testerName)
                                    .takeIf { it.exists() && it.length() > 0L }?.name
                            ))
                        }
                    }
                }
            ) { Text(strings.saveProposal) }
        },
        dismissButton = { TextButton(onClick = { dismiss() }) { Text(strings.cancel) } }
    )
}

@Composable
private fun FavoritesList(
    strings: AppStrings,
    entries: List<DictionaryEntry>,
    onOpen: (DictionaryEntry) -> Unit,
    onRemove: (DictionaryEntry) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(
            items = entries,
            key = { it.id }
        ) { entry ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpen(entry) },
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFFBF3)
                ),
                border = BorderStroke(1.dp, Color(0xFFE0D8C9)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = strings.french,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = entry.french.ifBlank { strings.ui(UiCopyKey.TO_COMPLETE) },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF16372A),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = strings.saamaka,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = entry.saamaka.ifBlank { strings.ui(UiCopyKey.TO_COMPLETE) },
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color(0xFF0B5D3B),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = { onRemove(entry) },
                        modifier = Modifier.padding(start = 12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = strings.removeFavorite,
                            tint = Color(0xFF0B5D3B)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryList(
    strings: AppStrings,
    entries: List<DictionaryEntry>,
    onOpen: (DictionaryEntry) -> Unit,
    onRemove: (DictionaryEntry) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(
            items = entries,
            key = { it.id }
        ) { entry ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpen(entry) },
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFFBF3)
                ),
                border = BorderStroke(1.dp, Color(0xFFE0D8C9)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = strings.french,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = entry.french.ifBlank { strings.ui(UiCopyKey.TO_COMPLETE) },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF16372A),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = strings.saamaka,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = entry.saamaka.ifBlank { strings.ui(UiCopyKey.TO_COMPLETE) },
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color(0xFF0B5D3B),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(
                        onClick = { onRemove(entry) },
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = strings.ui(UiCopyKey.REMOVE_FROM_HISTORY),
                            tint = Color(0xFF68736C),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = Color(0xFF0B5D3B),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun EntryList(
    strings: AppStrings,
    entries: List<DictionaryEntry>,
    selectedLanguage: AppLanguage,
    onOpen: (DictionaryEntry) -> Unit,
    isValidated: (Int) -> Boolean = { false }
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(
            items = entries,
            key = { it.id }
        ) { entry ->

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpen(entry) },
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFFBF3)
                ),
                border = BorderStroke(1.dp, Color(0xFFE0D8C9)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {

                        val sourceText = when (selectedLanguage) {
                            AppLanguage.FRENCH -> entry.french
                            AppLanguage.SAAMAKA -> entry.saamaka
                            AppLanguage.ENGLISH -> entry.english
                            AppLanguage.DUTCH -> entry.dutch
                        }

                        Text(
                            text = sourceText,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF16372A)
                        )

                        Spacer(
                            modifier = Modifier.height(4.dp)
                        )

                        Text(
                            text = entry.saamaka,
                            color = Color(0xFF0B5D3B),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    if (isValidated(entry.id)) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = strings.verified,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = Color(0xFF0B5D3B)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailScreen(
    strings: AppStrings,
    accessLevel: AccessLevel,
    entry: DictionaryEntry,
    preferredTranslationLanguage: AppLanguage,
    onPreferredTranslationLanguageChange: (AppLanguage) -> Unit,
    audioStore: AudioStore,
    testerName: String,
    initiallyFavorite: Boolean,
    classification: MissionEntryClassification,
    initialDeletionProposal: DeletionProposal?,
    onDeletionProposal: (String, String) -> Unit,
    onCancelDeletionProposal: () -> Unit,
    onValidate: () -> Unit,
    onNext: () -> Unit,
    onFavoriteChange: (Boolean) -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onCorrection: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    var favorite by remember(
        entry.id,
        initiallyFavorite
    ) {
        mutableStateOf(initiallyFavorite)
    }
    val preferredDetailLanguage = preferredTranslationLanguage.takeUnless {
        it == AppLanguage.SAAMAKA
    } ?: AppLanguage.FRENCH
    var displayedLanguage by remember(entry.id, preferredDetailLanguage) {
        mutableStateOf(effectiveTranslationLanguage(entry, preferredDetailLanguage))
    }
    var deletionProposal by remember(entry.id, initialDeletionProposal) {
        mutableStateOf(initialDeletionProposal)
    }
    var showDeletionDialog by remember(entry.id) { mutableStateOf(false) }

    // Toujours faux à l'ouverture d'une nouvelle fiche.
    // Il passe à true uniquement après appui sur
    // "Enregistrer la prononciation".
    var isRecording by remember(entry.id) {
        mutableStateOf(false)
    }

    // Vérifie si un audio existe déjà pour ce mot et ce testeur.
    var hasAudio by remember(
        entry.id,
        testerName
    ) {
        mutableStateOf(
            audioStore.hasAudio(
                entry.id,
                testerName
            )
        )
    }

    // Gestion de la permission micro :
    // on conserve ici le comportement V12.
    val permissionLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { granted ->

            if (!granted) {
                Toast.makeText(
                    context,
                    strings.ui(UiCopyKey.MICROPHONE_DENIED),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    val hasOfficialAudio = remember(entry.id) {
        audioStore.hasOfficialAudio(entry.id)
    }
    val availableLanguages = AppLanguage.entries.filter {
        it != AppLanguage.SAAMAKA &&
        searchTextForLanguage(entry, it.code).isNotBlank()
    }

    if (showDeletionDialog) {
        DeletionProposalDialog(
            strings = strings,
            onDismiss = { showDeletionDialog = false },
            onConfirm = { reasonIndex, comment ->
                val proposal = DeletionProposal(
                    entry.id, entry.saamaka, entry.french, entry.english, entry.dutch,
                    DELETION_REASONS_FR[reasonIndex], comment, testerName, System.currentTimeMillis()
                )
                onDeletionProposal(proposal.reason, proposal.comment)
                deletionProposal = proposal
                showDeletionDialog = false
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        item {
            Spacer(Modifier.height(8.dp))

            TextButton(
                onClick = onBack
            ) {
                Text(strings.back)
            }

            Surface(
                shape = RoundedCornerShape(50),
                color = if (deletionProposal != null) Color(0xFFFFE1E1) else when (classification.visualStatus) {
                    MissionVisualStatus.DOUBTFUL -> Color(0xFFFFEFC4)
                    MissionVisualStatus.TO_COMPLETE -> Color(0xFFFFE1E1)
                    MissionVisualStatus.ALREADY_VALIDATED -> Color(0xFFDCEEE2)
                    MissionVisualStatus.NEW -> MaterialTheme.colorScheme.surfaceVariant
                }
            ) {
                Text(
                    text = deletionProposal?.let { strings.deletionProposed }
                        ?: strings.missionStatusLabel(classification.visualStatus),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (deletionProposal != null) Color(0xFF8B2F2F) else when (classification.visualStatus) {
                        MissionVisualStatus.DOUBTFUL -> Color(0xFF8A6712)
                        MissionVisualStatus.TO_COMPLETE -> Color(0xFF8B2F2F)
                        else -> Color(0xFF0B5D3B)
                    }
                )
            }

            Spacer(Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {

                    // Langue source


                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp)
                        ) {

                            // LANGUE SOURCE
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = strings.saamaka,
                                    modifier = Modifier.padding(
                                        horizontal = 10.dp,
                                        vertical = 4.dp
                                    ),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Spacer(Modifier.height(8.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = entry.saamaka.ifBlank { strings.ui(UiCopyKey.TO_COMPLETE) },
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                IconButton(
                                    onClick = {
                                        favorite = !favorite
                                        onFavoriteChange(favorite)
                                    }
                                ) {
                                    Icon(
                                        if (favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                        contentDescription = if (favorite) strings.removeFavorite else strings.addFavorite,
                                        tint = Color(0xFFC99A2E)
                                    )
                                }
                            }

                            Spacer(Modifier.height(16.dp))

                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outline.copy(
                                    alpha = 0.20f
                                )
                            )

                            Spacer(Modifier.height(16.dp))

                            // LANGUE TRADUITE
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    text = displayedLanguage.label,
                                    modifier = Modifier.padding(
                                        horizontal = 10.dp,
                                        vertical = 4.dp
                                    ),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Spacer(Modifier.height(8.dp))

                            Text(
                                text = searchTextForLanguage(entry, displayedLanguage.code),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    Text(
                        text = strings.ui(UiCopyKey.AVAILABLE_LANGUAGES),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF16372A)
                    )
                    Spacer(Modifier.height(7.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        availableLanguages.forEach { language ->
                            Surface(
                                modifier = Modifier.clickable {
                                    displayedLanguage = language
                                    onPreferredTranslationLanguageChange(language)
                                },
                                shape = RoundedCornerShape(50),
                                color = if (language == displayedLanguage) {
                                    Color(0xFFDCEEE2)
                                } else {
                                    Color(0xFFF4EFE5)
                                }
                            ) {
                                Text(
                                    text = language.label,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = if (language == displayedLanguage) {
                                        Color(0xFF0B5D3B)
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    fontWeight = if (language == displayedLanguage) {
                                        FontWeight.Bold
                                    } else {
                                        FontWeight.Normal
                                    }
                                )
                            }
                        }
                    }

                    if (entry.categorie.isNotBlank()) {
                        Spacer(Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(strings.category, fontWeight = FontWeight.Bold, color = Color(0xFF16372A))
                            Spacer(Modifier.width(8.dp))
                            Surface(shape = RoundedCornerShape(50), color = Color(0xFFFFEFC4)) {
                                Text(
                                    entry.categorie,
                                    Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFF705A1D)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    if (accessLevel == AccessLevel.TESTER) {
                        if (deletionProposal == null) {
                            MissionValidationCard(
                                strings = strings,
                                entry = entry,
                                classification = classification,
                                onValidate = onValidate,
                                onCorrection = onCorrection
                            )

                            Spacer(Modifier.height(20.dp))
                        }

                        if (deletionProposal == null) {
                            OutlinedButton(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                onClick = { showDeletionDialog = true },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF8B2F2F))
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(strings.reportDeletion)
                            }
                        } else {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFFFFE1E1)
                            ) {
                                Column(Modifier.padding(16.dp)) {
                                    Text(strings.deletionProposed, fontWeight = FontWeight.Bold, color = Color(0xFF8B2F2F))
                                    Text(deletionProposal!!.reason)
                                    TextButton(onClick = {
                                        onCancelDeletionProposal()
                                        deletionProposal = null
                                    }) { Text(strings.cancelDeletionProposal) }
                                }
                            }
                        }

                        Spacer(Modifier.height(20.dp))
                    }

                    // Prononciation
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(
                            alpha = 0.55f
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {

                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {

                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.primary
                                ) {
                                    Text(
                                        text = "🎙",
                                        modifier = Modifier.padding(8.dp),
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }

                                Spacer(Modifier.width(10.dp))

                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {

                                    Text(
                                        text = strings.pronunciationSaamaka,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    Spacer(Modifier.height(2.dp))

                                    Text(
                                        text = strings.ui(UiCopyKey.SPEAKER_VALUE, testerName),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    if (hasOfficialAudio) {
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            onClick = { audioStore.playOfficialAudio(entry.id) }
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(strings.ui(UiCopyKey.LISTEN_PRONUNCIATION))
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    if (accessLevel == AccessLevel.TESTER) {
                        if (!isRecording) {
                            Button(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    if (testerName.isBlank()) {
                                        Toast.makeText(
                                            context,
                                            strings.enterTesterNameFirst,
                                            Toast.LENGTH_SHORT
                                        ).show()

                                        return@Button
                                    }

                                    val granted =
                                        ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.RECORD_AUDIO
                                        ) == PackageManager.PERMISSION_GRANTED

                                    if (granted) {
                                        audioStore.startRecording(
                                            entry.id,
                                            testerName
                                        )
                                        isRecording = true
                                    } else {
                                        permissionLauncher.launch(
                                            Manifest.permission.RECORD_AUDIO
                                        )
                                    }
                                }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Spacer(Modifier.width(8.dp))
                                    Text(strings.recordPronunciation)
                                }
                            }
                        } else {
                            Button(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                onClick = {
                                    audioStore.stopRecording()
                                    isRecording = false

                                    hasAudio = audioStore.hasAudio(
                                        entry.id,
                                        testerName
                                    )
                                }
                            ) {
                                Text(strings.stopAndSave)
                            }
                        }
                    }

                    if (accessLevel == AccessLevel.TESTER) {
                        if (
                            hasAudio &&
                            !isRecording &&
                            accessLevel != AccessLevel.GUEST
                        ) {
                            Spacer(Modifier.height(8.dp))

                            OutlinedButton(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                onClick = {
                                    audioStore.playAudio(
                                        entry.id,
                                        testerName
                                    )
                                }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("▶")
                                    Spacer(Modifier.width(8.dp))
                                    Text(strings.listen.replace("▶", "").trim())
                                }
                            }

                            Spacer(Modifier.height(8.dp))
                        }

                        OutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            onClick = {
                                audioStore.stopPlayback()

                                audioStore.deleteAudio(
                                    entry.id,
                                    testerName
                                )

                                hasAudio = false
                            }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("↻")
                                Spacer(Modifier.width(8.dp))
                                Text(strings.deleteRedo)
                            }
                        }
                    }


                    Spacer(Modifier.height(20.dp))

                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        onClick = onNext
                    ) {
                        Icon(
                            Icons.Default.SkipNext,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )

                        Spacer(Modifier.width(8.dp))

                        Text(strings.nextWord)
                    }

                    Spacer(Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {

                        OutlinedButton(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            onClick = onCopy
                        ) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )

                            Spacer(Modifier.width(6.dp))

                            Text(strings.copy)
                        }

                        OutlinedButton(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            onClick = onShare
                        ) {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )

                            Spacer(Modifier.width(6.dp))

                            Text(strings.share)
                        }
                    }

                    Spacer(Modifier.height(18.dp))
                    }
                }
            }
        }
    }

private val DELETION_REASONS_FR = listOf(
    "Expression impossible ou incompréhensible",
    "Doublon",
    "Mot ou traduction incorrecte",
    "Expression trop longue",
    "Ne correspond pas au saamaka",
    "Autre motif"
)

@Composable
private fun DeletionProposalDialog(
    strings: AppStrings,
    onDismiss: () -> Unit,
    onConfirm: (Int, String) -> Unit
) {
    var selectedReason by remember { mutableStateOf<Int?>(null) }
    var comment by remember { mutableStateOf("") }
    var asksConfirmation by remember { mutableStateOf(false) }
    val isOther = selectedReason == strings.deletionReasons.lastIndex
    val isValid = selectedReason != null && (!isOther || comment.isNotBlank())

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (asksConfirmation) strings.confirm else strings.deletionReasonTitle) },
        text = {
            if (asksConfirmation) {
                Text(strings.deletionConfirmation)
            } else {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    strings.deletionReasons.forEachIndexed { index, reason ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { selectedReason = index },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedReason == index,
                                onClick = { selectedReason = index }
                            )
                            Text(reason, modifier = Modifier.weight(1f))
                        }
                    }
                    if (isOther) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = comment,
                            onValueChange = { comment = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(strings.deletionOtherExplanation) },
                            minLines = 3
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = asksConfirmation || isValid,
                onClick = {
                    if (asksConfirmation) onConfirm(selectedReason!!, comment.trim())
                    else asksConfirmation = true
                }
            ) { Text(if (asksConfirmation) strings.confirm else strings.continueText) }
        },
        dismissButton = {
            TextButton(onClick = {
                if (asksConfirmation) asksConfirmation = false else onDismiss()
            }) { Text(if (asksConfirmation) strings.back else strings.cancel) }
        }
    )
}

@Composable
private fun MissionValidationCard(
    strings: AppStrings,
    entry: DictionaryEntry,
    classification: MissionEntryClassification,
    onValidate: () -> Unit,
    onCorrection: () -> Unit
) {
    val isValidated = classification.isCompleted
    val policy = missionValidationPolicy(entry, classification)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "✓",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = strings.linguisticValidation,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.height(6.dp))

            Text(
                text = when {
                    isValidated -> strings.ui(UiCopyKey.ENTRY_ALREADY_VALIDATED)
                    policy.missingMessage != null -> strings.ui(
                        UiCopyKey.MISSING_BEFORE_VALIDATE,
                        strings.missionMissingMessage(policy.missingMessage)
                    )
                    else -> strings.validationExplanation
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (policy.correctionIsPrimary) {
                    Color(0xFF8B2F2F)
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )

            Spacer(Modifier.height(14.dp))

            if (policy.correctionIsPrimary) {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    onClick = onCorrection
                ) {
                    Text(strings.correct)
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        enabled = policy.canValidate,
                        onClick = onValidate
                    ) {
                        Text(if (isValidated) strings.validatedO else strings.validateO)
                    }

                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        onClick = onCorrection
                    ) {
                        Text(strings.correct)
                    }
                }
            }
        }
    }
}

@Composable
private fun LearnAccessCard(
    title: String,
    description: String,
    section: String,
    selectedSection: String,
    modifier: Modifier = Modifier,
    onSelect: (String) -> Unit
) {
    val selected = section == selectedSection
    Surface(
        modifier = modifier.clickable { onSelect(section) },
        shape = RoundedCornerShape(18.dp),
        color = if (selected) Color(0xFF0B5D3B) else Color(0xFFF4EFE5),
        border = BorderStroke(
            1.dp,
            if (selected) Color(0xFF0B5D3B) else Color(0xFFD2CCC0)
        )
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                color = if (selected) Color.White else Color(0xFF16372A)
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = description,
                fontSize = 11.sp,
                color = if (selected) Color.White.copy(alpha = 0.78f) else Color(0xFF68736C)
            )
        }
    }
}

@Composable
private fun LearnProgressStat(
    modifier: Modifier = Modifier,
    value: Int,
    label: String
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = 0.12f)
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp)) {
            Text(
                text = value.toString(),
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFFF0C96A)
            )
            Text(
                text = label,
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.82f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun learnSectionChipColors() =
    FilterChipDefaults.filterChipColors(
        selectedContainerColor = Color(0xFF0B5D3B),
        selectedLabelColor = Color.White,
        containerColor = Color(0xFFF4EFE5),
        labelColor = Color(0xFF3F4842)
    )

@Composable
private fun learnSectionChipBorder(selected: Boolean) =
    FilterChipDefaults.filterChipBorder(
        enabled = true,
        selected = selected,
        borderColor = Color(0xFFD2CCC0),
        selectedBorderColor = Color(0xFF0B5D3B)
    )

@Composable
private fun MoreAccessCard(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
    badge: String? = null,
    containerColor: Color = Color(0xFFF4EFE5)
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, Color(0xFFE0D8C9)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFDCEEE2)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFF0B5D3B),
                    modifier = Modifier.padding(9.dp).size(20.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF16372A)
                )

                Spacer(Modifier.height(2.dp))

                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF68736C),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (badge != null) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color(0xFFFFEFC4)
                ) {
                    Text(
                        text = badge,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF6D5312)
                    )
                }

                Spacer(Modifier.width(8.dp))
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color(0xFF0B5D3B),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun CategoriesScreen(
    strings: AppStrings,
    database: DictionaryDatabase,
    onBack: () -> Unit,
    onOpen: (DictionaryEntry) -> Unit
) {

    var selectedCategory by remember {
        mutableStateOf<String?>(null)
    }

    val categories = remember {
        database.categories()
    }

    val allEntries = remember {
        database.allEntries()
    }

    val categoryCounts = remember(categories, allEntries) {
        categories.associateWith { category ->
            allEntries.count { entry ->
                entry.categorie.trim().equals(category, ignoreCase = true)
            }
        }
    }

    val forestGreen = Color(0xFF0B5D3B)
    val secondaryGreen = Color(0xFF174C36)
    val cream = Color(0xFFFFFBF3)
    val softCream = Color(0xFFF4EFE5)
    val gold = Color(0xFFF0C96A)
    val darkText = Color(0xFF16372A)

    // ============================================
    // LISTE DES MOTS D'UNE CATÉGORIE
    // ============================================

    if (selectedCategory != null) {

        val category = selectedCategory!!

        val categoryEntries = remember(category) {
            allEntries
                .filter { entry ->
                    entry.categorie
                        ?.trim()
                        ?.equals(
                            category,
                            ignoreCase = true
                        ) == true
                }
                .sortedBy {
                    it.french.lowercase()
                }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            item {

                Spacer(Modifier.height(12.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = forestGreen),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        TextButton(
                            onClick = { selectedCategory = null },
                            contentPadding = PaddingValues(0.dp),
                            colors = ButtonDefaults.textButtonColors(contentColor = gold)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(strings.ui(UiCopyKey.CATEGORIES), fontWeight = FontWeight.SemiBold)
                        }

                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = category,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )

                        Spacer(Modifier.height(4.dp))

                        Text(
                            text = strings.ui(UiCopyKey.WORDS_TO_DISCOVER, categoryEntries.size),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.82f)
                        )
                    }
                }

                Spacer(Modifier.height(6.dp))
            }

            items(categoryEntries) { entry ->

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onOpen(entry)
                        },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = cream
                    ),
                    border = BorderStroke(1.dp, Color(0xFFE4DED2)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                horizontal = 14.dp,
                                vertical = 12.dp
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFDCEEE2)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                contentDescription = null,
                                tint = forestGreen,
                                modifier = Modifier.padding(9.dp).size(18.dp)
                            )
                        }

                        Spacer(Modifier.width(12.dp))

                        Column(
                            modifier = Modifier.weight(1f)
                        ) {

                            Text(
                                text = entry.french,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyLarge,
                                color = darkText
                            )

                            if (entry.saamaka.isNotBlank()) {

                                Spacer(Modifier.height(3.dp))

                                Text(
                                    text = entry.saamaka,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = forestGreen
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = forestGreen
                        )
                    }
                }
            }

            item {
                Spacer(Modifier.height(100.dp))
            }
        }

        return
    }


    // ============================================
    // LISTE DES CATÉGORIES
    // ============================================

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        item {

            Spacer(Modifier.height(12.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = forestGreen),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    TextButton(
                        onClick = onBack,
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.textButtonColors(contentColor = gold)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(strings.back.removePrefix("← "), fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = strings.ui(UiCopyKey.CATEGORIES),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = strings.ui(UiCopyKey.EXPLORE_VOCABULARY),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.84f)
                    )

                    Spacer(Modifier.height(12.dp))

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color.White.copy(alpha = 0.13f)
                    ) {
                        Text(
                            text = strings.ui(UiCopyKey.THEMES_AND_WORDS, categories.size, allEntries.size),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = gold
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))
        }

        items(categories) { category ->

            val count = categoryCounts[category] ?: 0

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        selectedCategory = category
                    },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = softCream
                ),
                border = BorderStroke(1.dp, Color(0xFFE0D8C9)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 14.dp,
                            vertical = 13.dp
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = gold
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = null,
                            tint = secondaryGreen,
                            modifier = Modifier.padding(10.dp).size(20.dp)
                        )
                    }

                    Spacer(Modifier.width(12.dp))

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {

                        Text(
                            text = category,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = darkText
                        )

                        Spacer(Modifier.height(2.dp))

                        Text(
                            text = strings.ui(UiCopyKey.WORD_COUNT, count),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF68736C)
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = forestGreen
                    )
                }
            }
        }

        item {
            Spacer(Modifier.height(100.dp))
        }
    }
}

@Composable
private fun CorrectionForm(

    entry: DictionaryEntry,
    defaultTesterName: String,
    strings: AppStrings,
    onSave: (CorrectionProposal) -> Unit,
    onCancel: () -> Unit
) {
    var testerName by remember { mutableStateOf(defaultTesterName) }
    var frenchProposed by remember(entry.id, entry.french) { mutableStateOf(entry.french) }
    var saamakaProposed by remember(entry.id, entry.saamaka) { mutableStateOf(entry.saamaka) }
    var comment by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {

            Spacer(Modifier.height(14.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = strings.proposeCorrection,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = strings.ui(UiCopyKey.EDIT_CORRECTION_HELP),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Identité du testeur : affichée mais protégée
            OutlinedTextField(
                value = testerName,
                onValueChange = { },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text(strings.correctorName)
                },
                supportingText = {
                    Text(strings.ui(UiCopyKey.TESTER_IDENTITY_SAVED))
                },
                readOnly = true,
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = strings.french,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = entry.french.ifBlank { strings.notSpecified },
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(Modifier.height(6.dp))

            OutlinedTextField(
                value = frenchProposed,
                onValueChange = {
                    frenchProposed = it
                },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text(strings.proposedFrench)
                },
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(Modifier.height(10.dp))

            Text(
                text = strings.saamaka,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = entry.saamaka.ifBlank { strings.notSpecified },
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(Modifier.height(6.dp))

            OutlinedTextField(
                value = saamakaProposed,
                onValueChange = {
                    saamakaProposed = it
                },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text(strings.proposedSaamaka)
                },
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = comment,
                onValueChange = {
                    comment = it
                },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text(strings.commentExplanation)
                },
                placeholder = {
                    Text(strings.ui(UiCopyKey.CORRECTION_COMMENT_HINT))
                },
                minLines = 3,
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(Modifier.height(16.dp))

            Button(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                enabled = testerName.isNotBlank(),
                onClick = {
                    onSave(
                        CorrectionProposal(
                            id = System.currentTimeMillis(),
                            entryId = entry.id,
                            frenchCurrent = entry.french,
                            saamakaCurrent = entry.saamaka,
                            frenchProposed = frenchProposed.trim(),
                            saamakaProposed = saamakaProposed.trim(),
                            comment = comment.trim(),
                            testerName = testerName.trim(),
                            createdAt = System.currentTimeMillis()
                        )
                    )
                }
            ) {
                Text(strings.saveCorrection)
            }

            Spacer(Modifier.height(8.dp))

            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                onClick = onCancel
            ) {
                Text(strings.cancel)
            }

            Spacer(Modifier.height(80.dp))
        }
    }
}

private fun entryText(entry: DictionaryEntry): String =
    "${entry.french}\nSaamaka : ${entry.saamaka}"

private fun copyEntry(
    context: Context,
    entry: DictionaryEntry,
    copiedMessage: String
) {
    val clipboard =
        context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    clipboard.setPrimaryClip(
        ClipData.newPlainText("Saamaka Dico", entryText(entry))
    )

    Toast.makeText(
        context,
        copiedMessage,
        Toast.LENGTH_SHORT
    ).show()
}

private fun shareEntry(
    context: Context,
    entry: DictionaryEntry,
    chooserTitle: String
) {
    shareText(
        context,
        "Saamaka Dico",
        entryText(entry),
        chooserTitle
    )
}

private fun isValidTesterExportZip(file: File): Boolean {
    return try {
        ZipFile(file).use { zip ->

            val exportEntry = zip.getEntry("export.txt")
                ?: return false

            if (exportEntry.isDirectory) {
                return false
            }

            val exportContent = zip.getInputStream(exportEntry)
                .bufferedReader(Charsets.UTF_8)
                .use { it.readText() }

            if (exportContent.isBlank()) {
                return false
            }

            true
        }
    } catch (e: Exception) {
        false
    }
}
private fun shareFile(
    strings: AppStrings,
    context: Context,
    file: File,
    subject: String,
    testerName: String,
    exportHistoryStore: ExportHistoryStore
) {

    if (!file.exists()) {
        Toast.makeText(
            context,
            strings.ui(UiCopyKey.EXPORT_NOT_FOUND),
            Toast.LENGTH_LONG
        ).show()
        return
    }

    if (!file.isFile || file.length() <= 0L) {
        Toast.makeText(
            context,
            strings.ui(UiCopyKey.EXPORT_INVALID),
            Toast.LENGTH_LONG
        ).show()
        return
    }

    if (!file.name.endsWith(".zip", ignoreCase = true)) {
        Toast.makeText(
            context,
            strings.ui(UiCopyKey.EXPORT_NOT_ZIP),
            Toast.LENGTH_LONG
        ).show()
        return
    }

    if (!isValidTesterExportZip(file)) {
        Toast.makeText(
            context,
            strings.ui(UiCopyKey.EXPORT_CORRUPT),
            Toast.LENGTH_LONG
        ).show()
        return
    }

    val uri = try {
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    } catch (e: Exception) {
        Toast.makeText(
            context,
            strings.ui(UiCopyKey.SHARE_PREPARATION_FAILED),
            Toast.LENGTH_LONG
        ).show()
        return
    }

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/zip"
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    if (exportHistoryStore.hasSameExportAlreadyBeenShared(file)) {
        Toast.makeText(
            context,
            strings.ui(UiCopyKey.SHARE_RETRY_ALLOWED),
            Toast.LENGTH_LONG
        ).show()
    }

    // Ouverture du partage Android
    try {
        context.startActivity(
            Intent.createChooser(
                intent,
                strings.ui(UiCopyKey.SHARE_TESTER_WORK)
            )
        )
    } catch (e: Exception) {
        Toast.makeText(
            context,
            strings.ui(UiCopyKey.OPEN_SHARE_FAILED),
            Toast.LENGTH_LONG
        ).show()
        return
    }

    // Journalisation séparée :
    // un problème d'historique ne doit pas empêcher le partage.
    try {
        exportHistoryStore.markShareLaunched(
            testerName = testerName,
            file = file
        )
    } catch (e: Exception) {
        Toast.makeText(
            context,
            strings.ui(UiCopyKey.SHARE_HISTORY_FAILED),
            Toast.LENGTH_LONG
        ).show()
    }
}

private fun shareText(
    context: Context,
    subject: String,
    text: String,
    chooserTitle: String
) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_TEXT, text)
    }


    context.startActivity(
        Intent.createChooser(intent, chooserTitle)
    )
}

private fun openPremiumSubscriptionManagement(context: Context, strings: AppStrings) {
    val uri = Uri.parse(
        "https://play.google.com/store/account/subscriptions" +
            "?sku=$PREMIUM_PRODUCT_ID&package=${context.packageName}"
    )
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
    } catch (_: Exception) {
        Toast.makeText(
            context,
            strings.ui(UiCopyKey.MANAGE_SUBSCRIPTION_FAILED),
            Toast.LENGTH_LONG
        ).show()
    }
}
