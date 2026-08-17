package com.saamaka.dico.testeurs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.saamaka.dico.testeurs.database.DictionaryDatabase
import com.saamaka.dico.testeurs.model.DictionaryEntry
import com.saamaka.dico.testeurs.repository.CorrectionStore
import com.saamaka.dico.testeurs.repository.FavoritesStore
import com.saamaka.dico.testeurs.repository.HistoryStore
import com.saamaka.dico.testeurs.ui.SearchScreen
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

private val LightColors = lightColorScheme(
    primary = Color(0xFF6B4BAE),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE9DFFF),
    onPrimaryContainer = Color(0xFF27144F),
    background = Color(0xFFFFF8FC),
    surface = Color(0xFFFFF8FC),
    surfaceVariant = Color(0xFFF1EAF3)
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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    TesterApp()
                }
            }
        }
    }
}

enum class MainTab {
    SEARCH,
    TRANSLATE,
    MISSION,
    FAVORITES,
    HISTORY,
    CORRECTIONS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TesterApp() {
    var uiLanguage by remember {mutableStateOf(UiLanguage.FRENCH) }
    var languageMenuExpanded by remember {mutableStateOf(false)}
    var accessMenuExpanded by remember {
        mutableStateOf(false)
    }
    val appStrings = stringsFor(uiLanguage)
    var accessLevel by remember {
        mutableStateOf(AccessLevel.TESTER)
    }


    val context = LocalContext.current
    val database = remember { DictionaryDatabase(context) }
    val favoritesStore = remember { FavoritesStore(context) }
    val historyStore = remember { HistoryStore(context) }
    val correctionStore = remember { CorrectionStore(context) }
    val translationTrialStore = remember {
        TranslationTrialStore(context)
    }
    val assignmentStore = remember { AssignmentStore(context) }
    var testerName by remember { mutableStateOf(correctionStore.testerName()) }
    var testerNumber by remember {
        mutableStateOf(assignmentStore.testerNumber())
    }

    var assignedCategory by remember {
        mutableStateOf(assignmentStore.assignedCategory())
    }
    val validationStore = remember { ValidationStore(context) }
    val reviewStore = remember { ReviewStore(context) }
    val audioStore = remember { AudioStore(context) }

    var validatedCount by remember { mutableStateOf(validationStore.count()) }
    var activeTab by remember { mutableStateOf(MainTab.SEARCH) }

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
    var correctionEntry by remember { mutableStateOf<DictionaryEntry?>(null) }
    var query by remember { mutableStateOf("") }
    var selectedLanguage by remember { mutableStateOf(AppLanguage.FRENCH)    }
    var status by remember { mutableStateOf("Commence à écrire pour rechercher") }

    val searchResults = remember { mutableStateListOf<DictionaryEntry>() }
    val favoriteResults = remember { mutableStateListOf<DictionaryEntry>() }
    val historyResults = remember { mutableStateListOf<DictionaryEntry>() }
    val total = remember { database.countEntries() }
    val allEntries: List<DictionaryEntry> = remember { database.allEntries() }
    var remainingTranslationTrials by remember {
        mutableStateOf(
            translationTrialStore.remainingTrials()
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

    fun runSearch(text: String) {
        searchResults.clear()

        val cleaned = text.trim()

        if (cleaned.isEmpty()) {
            status = "Commence à écrire pour rechercher"
            return
        }

        val found = database.search(
            cleaned,
            selectedLanguage.code
        ).map { entry ->
            applyCorrection(entry)
        }

        searchResults.addAll(found)

        status = when (found.size) {
            0 -> "Aucun résultat trouvé"
            1 -> "1 résultat"
            else -> "${found.size} résultats"
        }
    }

    fun refreshFavorites() {
        favoriteResults.clear()
        favoriteResults.addAll(
            database.findByIds(favoritesStore.favoriteIds())
                .map { applyCorrection(it) }
        )
    }

    fun refreshHistory() {
        historyResults.clear()
        historyResults.addAll(
            database.findByOrderedIds(historyStore.ids())
                .map { applyCorrection(it) }
        )
    }

    fun openEntry(entry: DictionaryEntry) {
        selectedEntry = applyCorrection(entry)
        historyStore.add(entry.id)
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
            Toast.makeText(context, "Aucun autre mot à vérifier", Toast.LENGTH_SHORT).show()
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

    if (testerName.isBlank()) {
        TesterNameSetupScreen(
            initialName = "",
            strings = appStrings,
            onSave = { name ->

                correctionStore.setTesterName(name)
                testerName = name

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

    Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = when {
                                    correctionEntry != null -> appStrings.proposeCorrection
                                    selectedEntry != null -> appStrings.wordDetails
                                    else -> appStrings.appTitle
                                },
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = "$total entrées — version 3.7.0-beta1",
                                style = MaterialTheme.typography.labelMedium
                            )

                            Text(
                                text = when (accessLevel) {
                                    AccessLevel.GUEST -> "👤 ${appStrings.guest}"
                                    AccessLevel.FREE_ACCOUNT -> "🔐 ${appStrings.freeAccount}"
                                    AccessLevel.PREMIUM -> "👑 ${appStrings.premium}"
                                    AccessLevel.TESTER -> "🧪 ${appStrings.tester}"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    },

                    actions = {

                    // -------------------------
                    // LANGUE DE L'APPLICATION
                    // -------------------------
                    Box {
                        IconButton(
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
                                text = { Text("🇸🇷 Saamaka") },
                                onClick = {
                                    uiLanguage = UiLanguage.SAAMAKA
                                    languageMenuExpanded = false
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("🇫🇷 Français") },
                                onClick = {
                                    uiLanguage = UiLanguage.FRENCH
                                    languageMenuExpanded = false
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("🇬🇧 English") },
                                onClick = {
                                    uiLanguage = UiLanguage.ENGLISH
                                    languageMenuExpanded = false
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("🇳🇱 Nederlands") },
                                onClick = {
                                    uiLanguage = UiLanguage.DUTCH
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
                                text = { Text("👤 Invité") },
                                onClick = {
                                    accessLevel = AccessLevel.GUEST
                                    accessMenuExpanded = false
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("🔐 Compte gratuit") },
                                onClick = {
                                    accessLevel = AccessLevel.FREE_ACCOUNT
                                    accessMenuExpanded = false
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("👑 Premium") },
                                onClick = {
                                    accessLevel = AccessLevel.PREMIUM
                                    accessMenuExpanded = false
                                }
                            )

                            DropdownMenuItem(
                                text = { Text("🧪 Testeur") },
                                onClick = {
                                    accessLevel = AccessLevel.TESTER
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
                NavigationBar {

                    // Recherche : tout le monde
                    NavigationBarItem(
                        selected = activeTab == MainTab.SEARCH,
                        onClick = {
                            activeTab = MainTab.SEARCH
                        },
                        icon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null
                            )
                        },
                        label = {
                            Text(appStrings.search)
                        }
                    )

                    // Traduire : tout le monde
                    NavigationBarItem(
                        selected = activeTab == MainTab.TRANSLATE,
                        onClick = {
                            activeTab = MainTab.TRANSLATE
                        },
                        icon = {
                            Text("✨")
                        },
                        label = {
                            Text(appStrings.translate)
                        }
                    )

                    // Mission : TESTEUR seulement
                    if (accessLevel == AccessLevel.TESTER) {
                        NavigationBarItem(
                            selected = activeTab == MainTab.MISSION,
                            onClick = {
                                activeTab = MainTab.MISSION
                            },
                            icon = {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null
                                )
                            },
                            label = {
                                Text(appStrings.mission)
                            }
                        )
                    }

                    // Favoris : tout le monde
                    NavigationBarItem(
                        selected = activeTab == MainTab.FAVORITES,
                        onClick = {
                            activeTab = MainTab.FAVORITES
                            refreshFavorites()
                        },
                        icon = {
                            Icon(
                                Icons.Default.Favorite,
                                contentDescription = null
                            )
                        },
                        label = {
                            Text(appStrings.favorites)
                        }
                    )

                    // Historique : tout le monde
                    NavigationBarItem(
                        selected = activeTab == MainTab.HISTORY,
                        onClick = {
                            activeTab = MainTab.HISTORY
                            refreshHistory()
                        },
                        icon = {
                            Icon(
                                Icons.Default.History,
                                contentDescription = null
                            )
                        },
                        label = {
                            Text(appStrings.history)
                        }
                    )

                    // Corrections : TESTEUR seulement
                    if (accessLevel == AccessLevel.TESTER) {
                        NavigationBarItem(
                            selected = activeTab == MainTab.CORRECTIONS,
                            onClick = {
                                activeTab = MainTab.CORRECTIONS
                            },
                            icon = {
                                Icon(
                                    Icons.Default.Settings,
                                    contentDescription = null
                                )
                            },
                            label = {
                                Text(appStrings.corrections)
                            }
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
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
                        selectedLanguage = selectedLanguage,
                        audioStore = audioStore,
                        testerName = testerName,
                        initiallyFavorite = favoritesStore.isFavorite(entry.id),
                        isValidated = validationStore.isValidated(entry.id),
                        onValidate = {
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
                                "Mot validé par $reviewerName",
                                Toast.LENGTH_SHORT
                            ).show()

                            openNextUnvalidated()
                        },
                        onNext = { openNextUnvalidated() },
                        onFavoriteChange = { favoritesStore.setFavorite(entry.id, it) },
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
                        onCorrection = { correctionEntry = entry },
                        strings = appStrings,
                        onBack = { selectedEntry = null }
                    )
                }

                else -> when (activeTab) {
                    MainTab.TRANSLATE -> {
                        TranslateScreen(
                            strings = appStrings,
                            accessLevel = accessLevel,
                            remainingTrials = remainingTranslationTrials,
                            onUseTrial = {
                                if (translationTrialStore.useTrial()) {
                                    remainingTranslationTrials =
                                        translationTrialStore.remainingTrials()
                                }
                            },

                            onTranslate = { text, frenchToSaamaka ->
                                database.translatePhrase(
                                    text = text,
                                    frenchToSaamaka = frenchToSaamaka
                                )
                            }
                        )
                    }

                    MainTab.SEARCH -> SearchScreen(
                        total = total,
                        officiallyValidated = 76,
                        toReview = 990,
                        waiting = (total - 76 - 990).coerceAtLeast(0),
                        query = query,
                        status = status,
                        entries = searchResults.map { applyCorrection(it) },
                        isValidated = validationStore::isValidated,
                        selectedLanguage = selectedLanguage,
                        onLanguageChange = { selectedLanguage = it },

                        onQueryChange = {
                            query = it
                            runSearch(it)
                        },
                        onClear = {
                            query = ""
                            searchResults.clear()
                            status = "Commence à écrire pour rechercher"
                        },
                        strings = appStrings,
                        onOpen = ::openEntry)

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

                            val missionEntries = remember(selectedMissionCategory) {
                                if (selectedMissionCategory.isBlank()) {
                                    emptyList()
                                } else {
                                    database.missionByCategory(
                                        category = selectedMissionCategory,
                                        limit = 50
                                    )
                                }
                            }

                            LazyColumn {

                                item {

                                    Text(
                                        text = "Mission de $testerName",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Spacer(
                                        modifier = Modifier.height(8.dp)
                                    )

                                    Box {

                                        Button(
                                            onClick = {
                                                expandedCategory = true
                                            }
                                        ) {
                                            Text(
                                                if (selectedMissionCategory.isBlank()) {
                                                    "Choisir une catégorie"
                                                } else {
                                                    "Catégorie : $selectedMissionCategory"
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

                                    Spacer(
                                        modifier = Modifier.height(8.dp)
                                    )

                                    val doubtfulCount = missionEntries.count {
                                        it.valide.equals(
                                            "D",
                                            ignoreCase = true
                                        )
                                    }

                                    val newCount = missionEntries.count {
                                        it.saamaka.isBlank()
                                    }

                                    Text(
                                        text = "${missionEntries.size} mots dans cette mission"
                                    )

                                    Text(
                                        text = "⚠️ $doubtfulCount douteux • ⬜ $newCount nouveaux"
                                    )

                                    Spacer(
                                        modifier = Modifier.height(12.dp)
                                    )
                                }

                                items(missionEntries) { entry ->

                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp)
                                            .clickable {
                                                openEntry(entry)
                                            }
                                    ) {

                                        Column(
                                            modifier = Modifier.padding(12.dp)
                                        ) {

                                            Text(
                                                text = entry.french,
                                                fontWeight = FontWeight.Bold
                                            )

                                            Text(
                                                text = if (entry.saamaka.isBlank()) {
                                                    "Saamaka : À compléter"
                                                } else {
                                                    "Saamaka : ${entry.saamaka}"
                                                }
                                            )

                                            Text(
                                                text = when {

                                                    entry.valide.equals(
                                                        "D",
                                                        ignoreCase = true
                                                    ) -> "⚠️ Douteux"

                                                    entry.saamaka.isBlank() ->
                                                        "❓ Traduction manquante"

                                                    entry.valide.isBlank() ->
                                                        "⬜ Nouveau"

                                                    else ->
                                                        "✅ Déjà validé"
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    MainTab.FAVORITES -> SavedScreen(
                        title = if (favoriteResults.isEmpty()) {
                            "Aucun favori"
                        } else {
                            "${favoriteResults.size} favori(s)"
                        },
                        entries = favoriteResults,
                        selectedLanguage = selectedLanguage,
                        onOpen = ::openEntry
                    )

                    MainTab.HISTORY -> HistoryScreen(
                        entries = historyResults,
                        selectedLanguage = selectedLanguage,
                        onClear = {
                            historyStore.clear()
                            refreshHistory()
                        },
                        onOpen = ::openEntry
                    )

                    MainTab.CORRECTIONS -> CorrectionsScreen(
                        strings = appStrings,
                        testerName = correctionStore.testerName(),
                        correctionCount = reviewStore.all().size,
                        validatedCount = validatedCount,
                        total = total,

                        validatedReviewCount = reviewStore.validatedCount(),
                        correctedReviewCount = reviewStore.correctedCount(),

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
                                appendLine(audioStore.exportAudioSummary())
                            }

                            val zipFile = audioStore.createTesterExportZip(
                                testerName = testerName,
                                exportText = exportText
                            )

                            shareFile(
                                context = context,
                                file = zipFile,
                                subject = "Travail testeur Saamaka Dico"
                            )
                        },
                        onClearCorrections = {
                            correctionStore.clear()
                            Toast.makeText(
                                context,
                                "Corrections locales effacées",
                                Toast.LENGTH_SHORT
                            ).show()
                        },
                        onClearValidations = {
                            validationStore.clear()
                            validatedCount = 0
                            Toast.makeText(
                                context,
                                "Validations effacées",
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
            "Bienvenue dans Saamaka Dico",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(12.dp))

        Text(
            "Avant de commencer, indiquez votre nom. " +
                    "Il sera associé à vos validations, corrections et enregistrements audio."
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
            enabled = name.trim().isNotEmpty(),
            onClick = {
                onSave(name.trim())
            }
        ) {
            Text(strings.continueText)
        }
    }
}
@Composable
private fun SavedScreen(
    title: String,
    selectedLanguage: AppLanguage,
    entries: List<DictionaryEntry>,
    onOpen: (DictionaryEntry) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(Modifier.height(16.dp))
        Text(title, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        EntryList(
            entries = entries,
            selectedLanguage = selectedLanguage,
            onOpen = onOpen
        )
    }
}

@Composable
private fun HistoryScreen(
    entries: List<DictionaryEntry>,
    selectedLanguage: AppLanguage,
    onClear: () -> Unit,
    onOpen: (DictionaryEntry) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                if (entries.isEmpty()) "Historique vide" else "${entries.size} mot(s) récent(s)",
                fontWeight = FontWeight.Bold
            )
            TextButton(onClick = onClear, enabled = entries.isNotEmpty()) {
                Text("Tout effacer")
            }
        }
        EntryList(
            entries = entries,
            selectedLanguage = selectedLanguage,
            onOpen = onOpen
        )
    }
}
@Composable
private fun CorrectionsScreen(
    testerName: String,
    strings: AppStrings,
    correctionCount: Int,
    validatedCount: Int,
    total: Int,
    onTesterNameChange: (String) -> Unit,
    onExportCorrections: () -> Unit,
    onClearCorrections: () -> Unit,
    onClearValidations: () -> Unit,
    validatedReviewCount: Int,
    correctedReviewCount: Int
) {
    var name by remember(testerName) {
        mutableStateOf(testerName)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Spacer(Modifier.height(16.dp))

            Text(
                "Espace corrections",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(8.dp))

            Text(
                "Chaque correcteur peut proposer des modifications sans recevoir de lot attribué."
            )

            Spacer(Modifier.height(14.dp))

            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    onTesterNameChange(it)
                },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text("Nom du correcteur")
                },
                singleLine = true
            )

            Spacer(Modifier.height(14.dp))

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
                        "Résumé global",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )

                    Spacer(Modifier.height(6.dp))

                    Text("Mots dans le dictionnaire : $total")
                    Text("Validations O : $validatedReviewCount")
                    Text("Corrections : $correctedReviewCount")
                    Text("Actions enregistrées : $correctionCount")
                    Text("Validations locales : $validatedCount")
                }
            }

            Spacer(Modifier.height(14.dp))

            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = correctionCount > 0,
                onClick = onExportCorrections
            ) {
                Icon(
                    Icons.Default.Share,
                    contentDescription = null
                )

                Spacer(Modifier.width(8.dp))

                Text("Exporter les validations et corrections")
            }

            Spacer(Modifier.height(8.dp))

            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onClearCorrections
            ) {
                Text("Effacer les corrections locales")
            }

            Spacer(Modifier.height(8.dp))

            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onClearValidations
            ) {
                Text("Effacer les validations locales")
            }

            Spacer(Modifier.height(20.dp))

            Text(
                "Consignes",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Text(strings.instructionSearch)
            Text(strings.instructionValidate)
            Text(strings.instructionCorrect)
            Text(strings.instructionExport)
        }
    }
}


@Composable
private fun EntryList(
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
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {

                        val sourceText = when (selectedLanguage) {
                            AppLanguage.FRENCH -> entry.french
                            AppLanguage.ENGLISH -> entry.english
                            AppLanguage.DUTCH -> entry.dutch
                        }

                        Text(
                            text = sourceText,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(
                            modifier = Modifier.height(4.dp)
                        )

                        Text(
                            text = entry.saamaka
                        )
                    }

                    if (isValidated(entry.id)) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Validé",
                            tint = MaterialTheme.colorScheme.primary
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
    selectedLanguage: AppLanguage,
    audioStore: AudioStore,
    testerName: String,
    initiallyFavorite: Boolean,
    isValidated: Boolean,
    onValidate: () -> Unit,
    onNext: () -> Unit,
    onFavoriteChange: (Boolean) -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onCorrection: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Toast.makeText(
                context,
                "Permission microphone refusée",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    var favorite by remember(entry.id, initiallyFavorite) {
        mutableStateOf(initiallyFavorite)
    }

    var isRecording by remember(entry.id) {
        mutableStateOf(false)
    }

    var hasAudio by remember(entry.id, testerName) {
        mutableStateOf(
            audioStore.hasAudio(
                entry.id,
                testerName
            )
        )
    }

    val hasOfficialAudio = remember(entry.id) {
        audioStore.hasOfficialAudio(entry.id)
    }
    val sourceLabel = when (selectedLanguage) {
        AppLanguage.FRENCH -> "Français"
        AppLanguage.ENGLISH -> "English"
        AppLanguage.DUTCH -> "Nederlands"
    }

    val sourceText = when (selectedLanguage) {
        AppLanguage.FRENCH -> entry.french
        AppLanguage.ENGLISH -> entry.english
        AppLanguage.DUTCH -> entry.dutch
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Spacer(Modifier.height(8.dp))

            TextButton(
                onClick = onBack
            ) {
                Text(strings.back)
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(22.dp)
                ) {
                    Text(
                        sourceLabel,
                        style = MaterialTheme.typography.labelLarge
                    )

                    Text(
                        sourceText,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(16.dp))

                    Text(
                        "Saamaka",
                        style = MaterialTheme.typography.labelLarge
                    )

                    Text(
                        entry.saamaka,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(16.dp))

                    Text(
                        strings.pronunciationSaamaka,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = if (testerName.isBlank()) {
                            "${strings.speaker} : ${strings.notSpecified}"
                        } else {
                            "${strings.speaker} : $testerName"
                        },
                        style = MaterialTheme.typography.bodySmall
                    )

                    Spacer(Modifier.height(8.dp))

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
                                Text(strings.recordPronunciation)
                            }
                        } else {
                            Button(
                                modifier = Modifier.fillMaxWidth(),
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
                                onClick = {
                                    audioStore.playAudio(
                                        entry.id,
                                        testerName
                                    )
                                }
                            ) {
                                Text(strings.listen)
                            }

                            Spacer(Modifier.height(8.dp))
                        }

                        OutlinedButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                audioStore.stopPlayback()

                                audioStore.deleteAudio(
                                    entry.id,
                                    testerName
                                )

                                hasAudio = false
                            }
                        ) {
                            Text(strings.deleteRedo)
                        }
                    }


                    Spacer(Modifier.height(20.dp))

                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onNext
                    ) {
                        Icon(
                            Icons.Default.SkipNext,
                            contentDescription = null
                        )

                        Spacer(Modifier.width(8.dp))

                        Text(strings.nextWord)
                    }

                    Spacer(Modifier.height(8.dp))

                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            favorite = !favorite
                            onFavoriteChange(favorite)
                        }
                    ) {
                        Icon(
                            if (favorite) {
                                Icons.Default.Favorite
                            } else {
                                Icons.Default.FavoriteBorder
                            },
                            contentDescription = null
                        )

                        Spacer(Modifier.width(8.dp))

                        Text(
                            if (favorite) {
                                strings.removeFavorite
                            } else {
                                strings.addFavorite
                            }
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            modifier = Modifier.weight(1f),
                            onClick = onCopy
                        ) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = null
                            )

                            Spacer(Modifier.width(6.dp))

                            Text(strings.copy)
                        }

                        OutlinedButton(
                            modifier = Modifier.weight(1f),
                            onClick = onShare
                        ) {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = null
                            )

                            Spacer(Modifier.width(6.dp))

                            Text(strings.share)
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    if (accessLevel == AccessLevel.TESTER) {
                    Text(
                        strings.linguisticValidation,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        strings.validationExplanation,
                        style = MaterialTheme.typography.bodySmall
                    )

                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            modifier = Modifier.weight(1f),
                            enabled = !isValidated,
                            onClick = onValidate
                        ) {
                            Text(
                                if (isValidated) {
                                    strings.validatedO
                                } else {
                                    strings.validateO
                                }
                            )
                        }

                        OutlinedButton(
                            modifier = Modifier.weight(1f),
                            onClick = onCorrection
                        ) {
                            Text(strings.correct)
                        }
                    }

                        // Fin des outils réservés aux testeurs
                    }
                }
            }
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
    var frenchProposed by remember { mutableStateOf(entry.french) }
    var saamakaProposed by remember { mutableStateOf(entry.saamaka) }
    var comment by remember { mutableStateOf("") }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = testerName,
                onValueChange = { testerName = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(strings.correctorName) },
                singleLine = true
            )

            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = frenchProposed,
                onValueChange = { frenchProposed = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(strings.proposedFrench) },
            )

            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = saamakaProposed,
                onValueChange = { saamakaProposed = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(strings.proposedSaamaka) },
            )

            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = comment,
                onValueChange = { comment = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(strings.commentExplanation) },
                minLines = 3
            )

            Spacer(Modifier.height(14.dp))

            Button(
                modifier = Modifier.fillMaxWidth(),
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
                onClick = onCancel
            ) {
                Text(strings.cancel)
            }

            Spacer(Modifier.height(24.dp))
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

private fun shareFile(
    context: Context,
    file: File,
    subject: String
) {
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/zip"
        putExtra(Intent.EXTRA_SUBJECT, subject)
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    context.startActivity(
        Intent.createChooser(
            intent,
            "Partager le travail du testeur"
        )
    )
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