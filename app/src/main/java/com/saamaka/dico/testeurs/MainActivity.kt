package com.saamaka.dico.testeurs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
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
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.ui.Alignment
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll


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
    HOME,
    SEARCH,
    FAVORITES,
    LEARN,
    MORE,

    TRANSLATE,
    CATEGORIES,
    MISSION,
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
    var correctionEntry by remember { mutableStateOf<DictionaryEntry?>(null) }
    var query by remember { mutableStateOf("") }
    var selectedLanguage by remember { mutableStateOf(AppLanguage.FRENCH)    }
    var status by remember { mutableStateOf("Commence à écrire pour rechercher") }

    val searchResults = remember { mutableStateListOf<DictionaryEntry>() }
    val favoriteResults = remember { mutableStateListOf<DictionaryEntry>() }
    val historyResults = remember { mutableStateListOf<DictionaryEntry>() }
    val total = remember { database.countEntries() }
    val allEntries: List<DictionaryEntry> = remember { database.allEntries() }
    var quizScore by remember { mutableStateOf(0) }
    var quizQuestionNumber by remember { mutableStateOf(1) }
    var learnSection by remember {
        mutableStateOf("QUIZ")
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
                testerName = correctionStore.testerName()

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
                                    else -> "Dictionnaire Saamaka"
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

                                Spacer(Modifier.height(1.dp))

                                Text(
                                    text = "$total entrées • version $versionName",
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
                        selected = activeTab == MainTab.HOME,
                        onClick = {
                            activeTab = MainTab.HOME
                        },
                        colors = navigationItemColors,
                        icon = {
                            Icon(
                                Icons.Default.Home,
                                contentDescription = null
                            )
                        },
                        label = {
                            Text("Accueil")
                        }
                    )

                    // Recherche
                    NavigationBarItem(
                        selected = activeTab == MainTab.SEARCH,
                        onClick = {
                            activeTab = MainTab.SEARCH
                        },
                        colors = navigationItemColors,
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

                    // Favoris
                    NavigationBarItem(
                        selected = activeTab == MainTab.FAVORITES,
                        onClick = {
                            activeTab = MainTab.FAVORITES
                            refreshFavorites()
                        },
                        colors = navigationItemColors,
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

                    // Apprendre
                    NavigationBarItem(
                        selected = activeTab == MainTab.LEARN,
                        onClick = {
                            activeTab = MainTab.LEARN
                        },
                        colors = navigationItemColors,
                        icon = {
                            Icon(
                                Icons.Default.School,
                                contentDescription = null
                            )
                        },
                        label = {
                            Text("Apprendre")
                        }
                    )

                    // Plus
                    NavigationBarItem(
                        selected = activeTab == MainTab.MORE ||
                            activeTab == MainTab.TRANSLATE,
                        onClick = {
                            activeTab = MainTab.MORE
                        },
                        colors = navigationItemColors,
                        icon = {
                            Icon(
                                Icons.Default.MoreHoriz,
                                contentDescription = null
                            )
                        },
                        label = {
                            Text("Plus")
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

                    MainTab.CATEGORIES -> {
                        CategoriesScreen(
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
                            entries = searchResults.map { applyCorrection(it) },
                            isValidated = validationStore::isValidated,
                            selectedLanguage = selectedLanguage,
                            onLanguageChange = { selectedLanguage = it },

                            onLearnClick = {
                                activeTab = MainTab.LEARN
                            },

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
                            onOpen = ::openEntry,
                            onTranslateClick = {
                                activeTab = MainTab.TRANSLATE
                            },
                            onFavoritesClick = {
                                activeTab = MainTab.FAVORITES
                            },
                            onCategoriesClick = {
                                activeTab = MainTab.CATEGORIES
                            },
                            onHistoryClick = {
                                activeTab = MainTab.HISTORY
                            },
                            onWordOfDayClick = {
                                wordOfDay?.let(::openEntry)
                            }
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
                            onOpen = ::openEntry,

                            onTranslateClick = {
                                activeTab = MainTab.TRANSLATE
                            },

                            onFavoritesClick = {
                                activeTab = MainTab.FAVORITES
                            },

                            onCategoriesClick = {
                                activeTab = MainTab.CATEGORIES
                            },

                            onHistoryClick = {
                                activeTab = MainTab.HISTORY
                            },

                            onLearnClick = {
                                activeTab = MainTab.LEARN
                            },

                            onWordOfDayClick = { },
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

                            LazyColumn {

                                item {

                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(20.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(18.dp)
                                        ) {

                                            Text(
                                                text = "Mission de $testerName",
                                                style = MaterialTheme.typography.headlineSmall,
                                                fontWeight = FontWeight.Bold
                                            )

                                            Spacer(
                                                modifier = Modifier.height(4.dp)
                                            )

                                            Text(
                                                text = "Valide, corrige et complète les mots de ta mission.",
                                                style = MaterialTheme.typography.bodyMedium
                                            )

                                            Spacer(
                                                modifier = Modifier.height(14.dp)
                                            )

                                            Box(
                                                modifier = Modifier.fillMaxWidth()
                                            ) {

                                                Button(
                                                    modifier = Modifier.fillMaxWidth(),
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

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {

                                        Card(
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(16.dp)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(12.dp)
                                            ) {
                                                Text(
                                                    text = "${missionEntries.size}",
                                                    style = MaterialTheme.typography.titleLarge,
                                                    fontWeight = FontWeight.Bold
                                                )

                                                Text(
                                                    text = "Mots",
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }
                                        }

                                        Card(
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(16.dp)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(12.dp)
                                            ) {
                                                Text(
                                                    text = "$doubtfulCount",
                                                    style = MaterialTheme.typography.titleLarge,
                                                    fontWeight = FontWeight.Bold
                                                )

                                                Text(
                                                    text = "Douteux",
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }
                                        }

                                        Card(
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(16.dp)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(12.dp)
                                            ) {
                                                Text(
                                                    text = "$newCount",
                                                    style = MaterialTheme.typography.titleLarge,
                                                    fontWeight = FontWeight.Bold
                                                )

                                                Text(
                                                    text = "À compléter",
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }
                                        }
                                    }

                                    Spacer(
                                        modifier = Modifier.height(14.dp)
                                    )
                                }

                                items(missionEntries) { entry ->

                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 5.dp)
                                            .clickable {
                                                openEntry(entry)
                                            },
                                        shape = RoundedCornerShape(16.dp)
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
                                                    fontWeight = FontWeight.Bold
                                                )

                                                Spacer(
                                                    modifier = Modifier.height(4.dp)
                                                )

                                                Text(
                                                    text = if (entry.saamaka.isBlank()) {
                                                        "Saamaka : à compléter"
                                                    } else {
                                                        "Saamaka : ${entry.saamaka}"
                                                    },
                                                    style = MaterialTheme.typography.bodyMedium
                                                )

                                                Spacer(
                                                    modifier = Modifier.height(6.dp)
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
                                                    },
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }

                                            Icon(
                                                imageVector = Icons.Default.ChevronRight,
                                                contentDescription = null
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
                                .padding(top = 12.dp)
                        ) {

                            Text(
                                text = "Apprendre",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF16372A)
                            )

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
                                    modifier = Modifier.padding(16.dp)
                                ) {

                                    Text(
                                        text = "Ma progression",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )

                                    Spacer(Modifier.height(4.dp))

                                    Text(
                                        text = "Ton parcours d'apprentissage",
                                        fontSize = 12.sp,
                                        color = Color.White.copy(alpha = 0.78f)
                                    )

                                    Spacer(Modifier.height(12.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        LearnProgressStat(
                                            modifier = Modifier.weight(1f),
                                            value = knownWordIds.size,
                                            label = "Mots connus"
                                        )
                                        LearnProgressStat(
                                            modifier = Modifier.weight(1f),
                                            value = reviewWordIds.size,
                                            label = "À revoir"
                                        )
                                    }

                                    Spacer(Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        LearnProgressStat(
                                            modifier = Modifier.weight(1f),
                                            value = phraseKnownIds.size,
                                            label = "Phrases • ${phraseReviewIds.size} à revoir"
                                        )
                                        LearnProgressStat(
                                            modifier = Modifier.weight(1f),
                                            value = matchingGamesPlayed,
                                            label = "$matchingPerfectGames parfaites"
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {

                                FilterChip(
                                    selected = learnSection == "QUIZ",
                                    onClick = {
                                        learnSection = "QUIZ"
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = learnSectionChipColors(),
                                    border = learnSectionChipBorder(
                                        selected = learnSection == "QUIZ"
                                    ),
                                    label = {
                                        Text(
                                            text = "Quiz",
                                            modifier = Modifier.fillMaxWidth(),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                )

                                FilterChip(
                                    selected = learnSection == "WORDS",
                                    onClick = {
                                        learnSection = "WORDS"
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = learnSectionChipColors(),
                                    border = learnSectionChipBorder(
                                        selected = learnSection == "WORDS"
                                    ),
                                    label = {
                                        Text(
                                            text = "Mots",
                                            modifier = Modifier.fillMaxWidth(),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                )

                                FilterChip(
                                    selected = learnSection == "PHRASES",
                                    onClick = {
                                        learnSection = "PHRASES"
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = learnSectionChipColors(),
                                    border = learnSectionChipBorder(
                                        selected = learnSection == "PHRASES"
                                    ),
                                    label = {
                                        Text(
                                            text = "Phrases",
                                            modifier = Modifier.fillMaxWidth(),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                )

                                FilterChip(
                                    selected = learnSection == "GAMES",
                                    onClick = {
                                        learnSection = "GAMES"
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = learnSectionChipColors(),
                                    border = learnSectionChipBorder(
                                        selected = learnSection == "GAMES"
                                    ),
                                    label = {
                                        Text(
                                            text = "Jeux",
                                            modifier = Modifier.fillMaxWidth(),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                )
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
                                                text = "Quiz du jour",
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF16372A)
                                            )

                                            Spacer(Modifier.height(6.dp))

                                            Text(
                                                text = "Question $quizQuestionNumber • Score : $quizScore",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFF0B5D3B)
                                            )

                                            Text(
                                                text = "Teste tes connaissances en Saamaka",
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
                                                        text = "Que signifie ce mot ?",
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

                                                            selectedQuizAnswer = answer

                                                            if (
                                                                answer ==
                                                                quizEntry?.french?.trim()
                                                            ) {
                                                                quizScore++
                                                            }
                                                        }
                                                    },
                                                    modifier = Modifier.fillMaxWidth(),
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
                                                        text = "✅ Bonne réponse",
                                                        fontSize = 13.sp,
                                                        fontWeight =
                                                            FontWeight.SemiBold,
                                                        color =
                                                            Color(0xFF0B5D3B)
                                                    )

                                                } else {

                                                    Text(
                                                        text = "❌ Mauvaise réponse",
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
                                                            "✅ Réponse correcte : $correctAnswer",
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
                                                        text = "Suivant",
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
                                                text = "Révision des mots",
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF16372A)
                                            )

                                            Spacer(Modifier.height(6.dp))

                                            Text(
                                                text = "Révise ton vocabulaire Saamaka",
                                                fontSize = 13.sp,
                                                color = Color(0xFF68736C)
                                            )

                                            Spacer(Modifier.height(8.dp))

                                            Text(
                                                text = "✅ Je connais : $knownWordsCount   •   🔁 À revoir : $reviewWordsCount",
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
                                                        Modifier.padding(18.dp)
                                                ) {

                                                    Text(
                                                        text =
                                                            wordReviewEntry
                                                                ?.saamaka
                                                                ?: "—",
                                                        fontSize = 28.sp,
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
                                                    Text("À revoir")
                                                }

                                                Button(
                                                    onClick = {
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
                                                        text = "Je connais",
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
                                                text = "Phrases",
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF16372A)
                                            )

                                            Spacer(Modifier.height(6.dp))

                                            Text(
                                                text = "Découvre des expressions courtes en Saamaka",
                                                fontSize = 13.sp,
                                                color = Color(0xFF68736C)
                                            )

                                            Spacer(Modifier.height(8.dp))

                                            Text(
                                                text = "✅ Je connais : ${phraseKnownIds.size}   •   🔁 À revoir : ${phraseReviewIds.size}",
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
                                                    Text("À revoir")
                                                }

                                                Button(
                                                    onClick = {
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
                                                        text = "Je connais",
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
                                                text = "🧩 Associer les mots",
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
                                                        text = "Mes statistiques",
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF16372A)
                                                    )

                                                    Spacer(Modifier.height(6.dp))

                                                    Text(
                                                        text = "🎮 Parties : $matchingGamesPlayed",
                                                        fontSize = 12.sp,
                                                        color = Color(0xFF2E332F)
                                                    )

                                                    Text(
                                                        text = "✅ Associations réussies : $matchingTotalCorrect",
                                                        fontSize = 12.sp,
                                                        color = Color(0xFF2E332F)
                                                    )

                                                    Text(
                                                        text = "❌ Erreurs : $matchingTotalErrors",
                                                        fontSize = 12.sp,
                                                        color = Color(0xFF2E332F)
                                                    )

                                                    Text(
                                                        text = "🏆 Parties parfaites : $matchingPerfectGames",
                                                        fontSize = 12.sp,
                                                        color = Color(0xFF2E332F)
                                                    )
                                                }
                                            }

                                            Spacer(Modifier.height(4.dp))

                                            Text(
                                                text = "Choisis un mot Saamaka puis sa traduction",
                                                fontSize = 12.sp,
                                                color = Color(0xFF68736C)
                                            )

                                            Spacer(Modifier.height(8.dp))

                                            Text(
                                                text = "Score : ${matchedEntryIds.size}/${matchingEntries.size} • Erreurs : $matchingErrors",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFF0B5D3B)
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
                                                        text = "Saamaka",
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
                                                        text = "Français",
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
                                                                            "ℹ️ Choisis d'abord un mot Saamaka."

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
                                                        text = "🎉 Partie terminée !",
                                                        fontSize = 15.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF0B5D3B)
                                                    )

                                                    Spacer(Modifier.height(4.dp))

                                                    Text(
                                                        text = "✅ ${matchedEntryIds.size}/${matchingEntries.size} associations réussies",
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = Color(0xFF0B5D3B)
                                                    )

                                                    Spacer(Modifier.height(2.dp))

                                                    Text(
                                                        text = "❌ $matchingErrors erreur(s)",
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
                                                                "Excellent ! Aucune erreur 👏"

                                                            matchingErrors <= 2 ->
                                                                "Très bien ! Continue comme ça 👍"

                                                            else ->
                                                                "Bien joué ! Encore un peu d'entraînement 💪"
                                                        },
                                                        fontSize = 12.sp,
                                                        color = Color(0xFF68736C)
                                                    )
                                                    }
                                                }

                                                Spacer(Modifier.height(10.dp))

                                                Button(
                                                    onClick = {

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

                                                        matchedEntryIds.clear()
                                                        selectedSaamakaMatch = null
                                                        selectedFrenchMatch = null
                                                        matchingFeedback = null
                                                        matchingErrors = 0
                                                        matchingGameKey++
                                                    },
                                                    modifier = Modifier.fillMaxWidth(),
                                                    shape = RoundedCornerShape(14.dp),
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = Color(0xFF0B5D3B)
                                                    )
                                                ) {
                                                    Text(
                                                        text = "Nouvelle partie",
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
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {

                            Text(
                                text = "Plus",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF16372A)
                            )

                            Spacer(Modifier.height(4.dp))

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        activeTab = MainTab.TRANSLATE
                                    },
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF0B5D3B)
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = Color.White.copy(alpha = 0.13f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Translate,
                                            contentDescription = null,
                                            tint = Color(0xFFF0C96A),
                                            modifier = Modifier
                                                .padding(9.dp)
                                                .size(22.dp)
                                        )
                                    }

                                    Spacer(Modifier.width(12.dp))

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Traduire",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = Color.White
                                        )

                                        Spacer(Modifier.height(2.dp))

                                        Text(
                                            text = "Français ↔ Saamaka",
                                            fontSize = 12.sp,
                                            color = Color.White.copy(alpha = 0.82f)
                                        )
                                    }

                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = Color(0xFFF0C96A)
                                    )
                                }
                            }

                            if (accessLevel == AccessLevel.TESTER) {

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            activeTab = MainTab.MISSION
                                        },
                                    shape = RoundedCornerShape(18.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color(0xFFDCEEE2)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color(0xFF0B5D3B)
                                        )

                                        Spacer(Modifier.width(12.dp))

                                        Column(
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(
                                                text = "Mission testeur",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )

                                            Text(
                                                text = "Valider et corriger les mots",
                                                fontSize = 12.sp,
                                                color = Color(0xFF68736C)
                                            )
                                        }

                                        Icon(
                                            imageVector = Icons.Default.ChevronRight,
                                            contentDescription = null
                                        )
                                    }
                                }
                            }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        activeTab = MainTab.HISTORY
                                        refreshHistory()
                                    },
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFFF4EFE5)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = null,
                                        tint = Color(0xFF0B5D3B)
                                    )

                                    Spacer(Modifier.width(12.dp))

                                    Column(
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = "Historique",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )

                                        Text(
                                            text = "Retrouver les mots consultés",
                                            fontSize = 12.sp,
                                            color = Color(0xFF68736C)
                                        )
                                    }

                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null
                                    )
                                }
                            }

                            if (accessLevel == AccessLevel.TESTER) {

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            activeTab = MainTab.CORRECTIONS
                                        },
                                    shape = RoundedCornerShape(18.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color(0xFFF4EFE5)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Settings,
                                            contentDescription = null,
                                            tint = Color(0xFF0B5D3B)
                                        )

                                        Spacer(Modifier.width(12.dp))

                                        Column(
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(
                                                text = "Corrections",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )

                                            Text(
                                                text = "Exports et travail testeur",
                                                fontSize = 12.sp,
                                                color = Color(0xFF68736C)
                                            )
                                        }

                                        Icon(
                                            imageVector = Icons.Default.ChevronRight,
                                            contentDescription = null
                                        )
                                    }
                                }
                            }
                        }
                    }
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

                            val exportHistoryStore = ExportHistoryStore(context)

                            exportHistoryStore.markCreated(
                                testerName = testerName,
                                file = zipFile
                            )

                            shareFile(
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
            shape = RoundedCornerShape(14.dp),
            enabled = name.trim().isNotEmpty(),
            onClick = {
                onSave(name.trim())
            }
        ){
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
    Column(
        modifier = Modifier.fillMaxSize()
    ) {

        Spacer(Modifier.height(14.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Mes favoris",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                Spacer(Modifier.height(3.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Spacer(Modifier.height(12.dp))

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
    Column(
        modifier = Modifier.fillMaxSize()
    ) {

        Spacer(Modifier.height(14.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.primaryContainer
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
                        text = "Historique",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Spacer(Modifier.height(3.dp))

                    Text(
                        text = if (entries.isEmpty()) {
                            "Aucun mot consulté récemment"
                        } else {
                            "${entries.size} mot(s) récent(s)"
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                TextButton(
                    onClick = onClear,
                    enabled = entries.isNotEmpty()
                ) {
                    Text(
                        text = "Tout effacer",
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Spacer(Modifier.height(12.dp))

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

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {

        item {

            Spacer(Modifier.height(14.dp))

            Text(
                text = "Espace corrections",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = "Valide, corrige et exporte ton travail de testeur.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(14.dp))

            // Identité V13 verrouillée
            OutlinedTextField(
                value = testerName,
                onValueChange = { },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text("Testeur")
                },
                supportingText = {
                    Text("Identité enregistrée et protégée")
                },
                singleLine = true,
                readOnly = true,
                shape = RoundedCornerShape(14.dp)
            )

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
                        text = "Résumé du travail",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {

                        StatCard(
                            modifier = Modifier.weight(1f),
                            value = "$total",
                            label = "Mots"
                        )

                        StatCard(
                            modifier = Modifier.weight(1f),
                            value = "$validatedReviewCount",
                            label = "Validations"
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
                            label = "Corrections"
                        )

                        StatCard(
                            modifier = Modifier.weight(1f),
                            value = "$correctionCount",
                            label = "Actions"
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = "$validatedCount validation(s) locale(s)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            Button(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                enabled = correctionCount > 0,
                onClick = onExportCorrections
            ) {
                Icon(
                    Icons.Default.Share,
                    contentDescription = null
                )

                Spacer(Modifier.width(8.dp))

                Text("Exporter mon travail")
            }

            Spacer(Modifier.height(8.dp))

            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                onClick = onClearCorrections
            ) {
                Text("Effacer les corrections locales")
            }

            Spacer(Modifier.height(8.dp))

            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                onClick = onClearValidations
            ) {
                Text("Effacer les validations locales")
            }

            Spacer(Modifier.height(18.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {

                Column(
                    modifier = Modifier.padding(16.dp)
                ) {

                    Text(
                        text = "Consignes",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
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
        color = MaterialTheme.colorScheme.surface.copy(
            alpha = 0.75f
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
                            AppLanguage.SAAMAKA -> entry.saamaka
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

    var favorite by remember(
        entry.id,
        initiallyFavorite
    ) {
        mutableStateOf(initiallyFavorite)
    }

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
                    "Permission microphone refusée",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    val hasOfficialAudio = remember(entry.id) {
        audioStore.hasOfficialAudio(entry.id)
    }
    val sourceLabel = when (selectedLanguage) {
        AppLanguage.FRENCH -> "Français"
        AppLanguage.ENGLISH -> "English"
        AppLanguage.DUTCH -> "Nederlands"
        AppLanguage.SAAMAKA -> "Saamaka"
    }

    val sourceText = when (selectedLanguage) {
        AppLanguage.FRENCH -> entry.french
        AppLanguage.SAAMAKA -> entry.saamaka
        AppLanguage.ENGLISH -> entry.english
        AppLanguage.DUTCH -> entry.dutch
    }
    val translationLabel =
        if (selectedLanguage == AppLanguage.SAAMAKA) {
            "Français"
        } else {
            "Saamaka"
        }

    val translationText =
        if (selectedLanguage == AppLanguage.SAAMAKA) {
            entry.french
        } else {
            entry.saamaka
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
                                    text = sourceLabel,
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

                            Text(
                                text = sourceText,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

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
                                    text = translationLabel,
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
                                text = translationText,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }



                    Spacer(Modifier.height(20.dp))

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
                                        text = "Locuteur : $testerName",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))
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

                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
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

                    if (accessLevel == AccessLevel.TESTER) {

                        Spacer(Modifier.height(14.dp))

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(
                                alpha = 0.45f
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {

                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {

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
                                    text = strings.validationExplanation,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(Modifier.height(14.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {

                                    Button(
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(16.dp),
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
                                        shape = RoundedCornerShape(16.dp),
                                        onClick = onCorrection
                                    ) {
                                        Text(strings.correct)
                                    }
                                }
                            }
                        }

                        // Fin des outils réservés aux testeurs
                    }
                    Spacer(Modifier.height(18.dp))
                    }
                }
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
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = value.toString(),
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFFF0C96A)
            )
            Text(
                text = label,
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.82f),
                maxLines = 1
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
private fun CategoriesScreen(
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
                            Text("Catégories", fontWeight = FontWeight.SemiBold)
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
                            text = "${categoryEntries.size} mot${if (categoryEntries.size > 1) "s" else ""} à découvrir",
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
                        Text("Retour", fontWeight = FontWeight.SemiBold)
                    }

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = "Catégories",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = "Explore le vocabulaire par thème",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.84f)
                    )

                    Spacer(Modifier.height(12.dp))

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color.White.copy(alpha = 0.13f)
                    ) {
                        Text(
                            text = "${categories.size} thèmes • ${allEntries.size} mots et expressions",
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
                            text = "$count mot${if (count > 1) "s" else ""}",
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
    var frenchProposed by remember { mutableStateOf(entry.french) }
    var saamakaProposed by remember { mutableStateOf(entry.saamaka) }
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
                        text = "Proposer une correction",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        text = "Modifie uniquement les éléments qui doivent être corrigés.",
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
                    Text("Identité du testeur enregistrée")
                },
                readOnly = true,
                singleLine = true,
                shape = RoundedCornerShape(14.dp)
            )

            Spacer(Modifier.height(10.dp))

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
                    Text("Explique brièvement la correction si nécessaire")
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
    context: Context,
    file: File,
    subject: String,
    testerName: String,
    exportHistoryStore: ExportHistoryStore
) {

    if (!file.exists()) {
        Toast.makeText(
            context,
            "Export introuvable. Veuillez recréer l'envoi.",
            Toast.LENGTH_LONG
        ).show()
        return
    }

    if (!file.isFile || file.length() <= 0L) {
        Toast.makeText(
            context,
            "Export invalide ou vide. Envoi annulé.",
            Toast.LENGTH_LONG
        ).show()
        return
    }

    if (!file.name.endsWith(".zip", ignoreCase = true)) {
        Toast.makeText(
            context,
            "Le fichier d'export n'est pas un ZIP valide.",
            Toast.LENGTH_LONG
        ).show()
        return
    }

    if (!isValidTesterExportZip(file)) {
        Toast.makeText(
            context,
            "L'export est incomplet ou corrompu. Envoi annulé.",
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
            "Impossible de préparer le fichier pour le partage.",
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
            "Ce contenu a déjà été ouvert pour partage. Nouvelle tentative autorisée.",
            Toast.LENGTH_LONG
        ).show()
    }

    // Ouverture du partage Android
    try {
        context.startActivity(
            Intent.createChooser(
                intent,
                "Partager le travail du testeur"
            )
        )
    } catch (e: Exception) {
        Toast.makeText(
            context,
            "Impossible d'ouvrir le partage.",
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
            "Partage ouvert, mais l'historique local n'a pas pu être enregistré.",
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
