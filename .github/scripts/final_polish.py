from pathlib import Path

root = Path('app/src/main/java/com/saamaka/dico/testeurs')
main = root / 'MainActivity.kt'
library = root / 'LibraryLearningEnhancements.kt'
fav = root / 'repository/FavoritesStore.kt'

(root / 'LearningAccessPolicy.kt').write_text('''package com.saamaka.dico.testeurs

internal fun learningActivityForSection(section: String): LearningActivity? = when (section) {
    "QUIZ" -> LearningActivity.QUIZ
    "WORDS", "FAVORITES", "WORD_OF_DAY", "AUDIO" -> LearningActivity.REVIEW
    "PHRASES" -> LearningActivity.PHRASES
    "GAMES" -> LearningActivity.GAMES
    else -> null
}

internal fun hasLearningSectionAccess(
    accessLevel: AccessLevel,
    section: String,
    remainingTrials: Map<LearningActivity, Int>
): Boolean {
    if (accessLevel == AccessLevel.PREMIUM || accessLevel == AccessLevel.TESTER) return true
    val activity = learningActivityForSection(section) ?: return true
    return (remainingTrials[activity] ?: 0) > 0
}
''', encoding='utf-8')

(root / 'LearningContentPolicy.kt').write_text('''package com.saamaka.dico.testeurs

import com.saamaka.dico.testeurs.model.DictionaryEntry

internal fun trustedLearningEntries(
    entries: List<DictionaryEntry>,
    correctedEntryIds: Set<Int>,
    deletionProposalEntryIds: Set<Int>
): List<DictionaryEntry> = entries.filter { entry ->
    entry.valide.trim().equals("O", ignoreCase = true) &&
        entry.id !in correctedEntryIds &&
        entry.id !in deletionProposalEntryIds &&
        entry.saamaka.isNotBlank() &&
        entry.french.isNotBlank() &&
        entry.categorie.isNotBlank()
}
''', encoding='utf-8')

f = fav.read_text(encoding='utf-8')
if 'fun clear()' not in f:
    f = f.replace('\n    fun favoriteIds(): Set<Int> =', '\n    fun clear() {\n        val editor = preferences.edit()\n        preferences.all.keys.filter { it.startsWith("favorite_") }.forEach(editor::remove)\n        editor.apply()\n    }\n\n    fun favoriteIds(): Set<Int> =')
fav.write_text(f, encoding='utf-8')

l = library.read_text(encoding='utf-8')
if 'import androidx.compose.material3.AlertDialog' not in l:
    l = l.replace('import androidx.compose.material3.Button\n', 'import androidx.compose.material3.AlertDialog\nimport androidx.compose.material3.Button\nimport androidx.compose.material3.TextButton\n')
l = l.replace('    onRemove: (DictionaryEntry) -> Unit,\n    onSearch: () -> Unit\n) {\n    var query', '    onRemove: (DictionaryEntry) -> Unit,\n    onSearch: () -> Unit,\n    onClearAll: (() -> Unit)? = null\n) {\n    var showClearConfirmation by remember { mutableStateOf(false) }\n    var query', 1)
l = l.replace('            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)\n            Spacer(Modifier.height(10.dp))', '            Row(verticalAlignment = Alignment.CenterVertically) {\n                Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)\n                if (entries.isNotEmpty() && onClearAll != null) {\n                    OutlinedButton(onClick = { showClearConfirmation = true }) { Text("Tout effacer") }\n                }\n            }\n            Spacer(Modifier.height(10.dp))', 1)
for a,b in {
    'Color(0xFFFFFBF3)': 'MaterialTheme.colorScheme.surface',
    'Color(0xFFF4EFE5)': 'MaterialTheme.colorScheme.surfaceVariant',
    'Color(0xFFE0D8C9)': 'MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)',
    'Color(0xFF16372A)': 'MaterialTheme.colorScheme.onSurface',
    'Color(0xFF68736C)': 'MaterialTheme.colorScheme.onSurfaceVariant',
    'Color(0xFF0B5D3B)': 'MaterialTheme.colorScheme.primary',
    'Color(0xFFDCEEE2)': 'MaterialTheme.colorScheme.primaryContainer',
    'Color(0xFF8B2F2F)': 'MaterialTheme.colorScheme.error',
    'Color(0xFFF8DDDD)': 'MaterialTheme.colorScheme.errorContainer',
}.items():
    l = l.replace(a,b)
marker = '    LazyColumn(\n        modifier = Modifier.fillMaxSize(),'
dialog = '''    if (showClearConfirmation && onClearAll != null) {
        AlertDialog(
            onDismissRequest = { showClearConfirmation = false },
            title = { Text("Effacer tous les favoris ?") },
            text = { Text("Cette action retire tous les mots de vos favoris.") },
            confirmButton = { Button(onClick = { showClearConfirmation = false; onClearAll() }) { Text("Tout effacer") } },
            dismissButton = { TextButton(onClick = { showClearConfirmation = false }) { Text("Annuler") } }
        )
    }

'''
if 'Effacer tous les favoris ?' not in l:
    l = l.replace(marker, dialog + marker, 1)
l = l.replace('    audioStore: AudioStore,\n    onConsumeTrial: () -> Boolean\n) {', '    audioStore: AudioStore,\n    onConsumeTrial: () -> Boolean,\n    onOpenEntry: (DictionaryEntry) -> Unit = {}\n) {', 1)
old = '''                if (selected != null) {
                    Button(
                        onClick = {
                            current = audioEntries.filter { it.id != entry.id }.randomOrNull() ?: entry
                            selected = null
                        },'''
new = '''                if (selected != null) {
                    OutlinedButton(onClick = { onOpenEntry(entry) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                        Text("Voir la fiche")
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            current = audioEntries.filter { it.id != entry.id }.randomOrNull() ?: entry
                            selected = null
                        },'''
l = l.replace(old, new, 1)
library.write_text(l, encoding='utf-8')

m = main.read_text(encoding='utf-8')
old_launch = '''    fun launchLearningActivity(section: String) {
        val activity = when (section) {
            "QUIZ" -> LearningActivity.QUIZ
            "WORDS", "FAVORITES", "WORD_OF_DAY", "AUDIO" -> LearningActivity.REVIEW
            "PHRASES" -> LearningActivity.PHRASES
            "GAMES" -> LearningActivity.GAMES
            else -> null
        }
        val limitedAccount =
            accessLevel == AccessLevel.GUEST || accessLevel == AccessLevel.FREE_ACCOUNT
        if (
            limitedAccount &&
            activity != null &&
            learningTrialStore.remainingTrials(accessLevel, activity) <= 0
        ) {
            Toast.makeText(
                context,
                appStrings.ui(UiCopyKey.LEARNING_TRIALS_EXHAUSTED),
                Toast.LENGTH_LONG
            ).show()
            activeTab = MainTab.PREMIUM
            return
        }
        if (learnSection == section) return
        if (
            section == "GAMES" &&
            !consumeLearningTrialOrOpenPremium(LearningActivity.GAMES)
        ) return
        learnSection = section
    }'''
new_launch = '''    fun launchLearningActivity(section: String) {
        val activity = learningActivityForSection(section)
        val limitedAccount = accessLevel == AccessLevel.GUEST || accessLevel == AccessLevel.FREE_ACCOUNT
        if (limitedAccount && activity != null && learningTrialStore.remainingTrials(accessLevel, activity) <= 0) {
            learnSection = section
            return
        }
        if (learnSection == section) return
        if (section == "GAMES" && !consumeLearningTrialOrOpenPremium(LearningActivity.GAMES)) return
        learnSection = section
    }'''
if old_launch not in m:
    raise SystemExit('launch block not found')
m = m.replace(old_launch, new_launch, 1)

old_trusted = '''                        val validatedLearningWordEntries = remember(
                            quizEntries,
                            deletionProposalEntryIds,
                            correctedEntryIds
                        ) {
                            quizEntries.filter { entry ->
                                entry.valide.trim().equals("O", ignoreCase = true) &&
                                        entry.id !in deletionProposalEntryIds &&
                                        entry.id !in correctedEntryIds
                            }
                        }'''
new_trusted = '''                        val validatedLearningWordEntries = remember(
                            quizEntries,
                            deletionProposalEntryIds,
                            correctedEntryIds
                        ) {
                            trustedLearningEntries(quizEntries, correctedEntryIds, deletionProposalEntryIds)
                        }'''
if old_trusted in m:
    m = m.replace(old_trusted, new_trusted, 1)

old_selected = '''                            val selectedLearningActivity = when (learnSection) {
                                "QUIZ" -> LearningActivity.QUIZ
                                "WORDS" -> LearningActivity.REVIEW
                                "PHRASES" -> LearningActivity.PHRASES
                                "GAMES" -> LearningActivity.GAMES
                                "FAVORITES", "WORD_OF_DAY", "AUDIO" -> LearningActivity.REVIEW
                                else -> null
                            }'''
m = m.replace(old_selected, '                            val selectedLearningActivity = learningActivityForSection(learnSection)', 1)

when_marker = '                            when (learnSection) {\n\n                                "QUIZ" -> {'
gated = '''                            val sectionLocked = !hasLearningSectionAccess(accessLevel, learnSection, remainingLearningTrials)
                            if (sectionLocked && learnSection.isNotBlank()) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(22.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                                ) {
                                    Column(Modifier.padding(18.dp)) {
                                        Text("Activité Premium", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                                        Spacer(Modifier.height(6.dp))
                                        Text(appStrings.ui(UiCopyKey.LEARNING_TRIALS_EXHAUSTED))
                                        Spacer(Modifier.height(14.dp))
                                        Button(onClick = { activeTab = MainTab.PREMIUM }, modifier = Modifier.fillMaxWidth()) {
                                            Text(appStrings.ui(UiCopyKey.PREMIUM_TITLE))
                                        }
                                    }
                                }
                            } else when (learnSection) {

                                "QUIZ" -> {'''
if when_marker not in m:
    raise SystemExit('when marker not found')
m = m.replace(when_marker, gated, 1)

surface = '''                                            Surface(
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(18.dp),
                                                color = Color(0xFFDCEEE2)
                                            ) {'''
words_new = '''                                            Surface(
                                                modifier = Modifier.fillMaxWidth().clickable(enabled = wordReviewEntry != null) {
                                                    wordReviewEntry?.let { openEntryInContext(it, quizEntries) }
                                                },
                                                shape = RoundedCornerShape(18.dp),
                                                color = Color(0xFFDCEEE2)
                                            ) {'''
idx = m.find('                                "WORDS" -> {')
pos = m.find(surface, idx)
if pos >= 0:
    m = m[:pos] + m[pos:].replace(surface, words_new, 1)

fav_text = '                                                Text(favoriteEntry.saamaka, fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0B5D3B))'
fav_new = '''                                                Column(modifier = Modifier.fillMaxWidth().clickable { openEntryInContext(favoriteEntry, favoriteLearningEntries) }) {
                                                    Text(favoriteEntry.saamaka, fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0B5D3B))
                                                    Text("Voir la fiche", fontSize = 12.sp, color = Color(0xFF68736C))
                                                }'''
m = m.replace(fav_text, fav_new, 1)

phrase_new = '''                                            Surface(
                                                modifier = Modifier.fillMaxWidth().clickable(enabled = phraseReviewEntry != null) {
                                                    phraseReviewEntry?.let { openEntryInContext(it, phraseEntries) }
                                                },
                                                shape = RoundedCornerShape(18.dp),
                                                color = Color(0xFFDCEEE2)
                                            ) {'''
idx = m.find('                                "PHRASES" -> {')
pos = m.find(surface, idx)
if pos >= 0:
    m = m[:pos] + m[pos:].replace(surface, phrase_new, 1)

m = m.replace('''                                        audioStore = audioStore,
                                        onConsumeTrial = { consumeLearningTrialOrOpenPremium(LearningActivity.REVIEW) }
                                    )''', '''                                        audioStore = audioStore,
                                        onConsumeTrial = { consumeLearningTrialOrOpenPremium(LearningActivity.REVIEW) },
                                        onOpenEntry = { entry -> openEntryInContext(entry, quizEntries) }
                                    )''', 1)

fav_call = '''                        onSearch = {
                            activeTab = MainTab.SEARCH
                        }
                    )

                    MainTab.HISTORY ->'''
fav_call_new = '''                        onSearch = {
                            activeTab = MainTab.SEARCH
                        },
                        onClearAll = {
                            val previousFavoriteIds = favoritesStore.favoriteIds()
                            favoritesStore.clear()
                            refreshFavorites()
                            coroutineScope.launch {
                                val result = snackbarHostState.showSnackbar(
                                    message = "Favoris effacés",
                                    actionLabel = appStrings.cancel,
                                    withDismissAction = true,
                                    duration = SnackbarDuration.Long
                                )
                                if (result == SnackbarResult.ActionPerformed) {
                                    previousFavoriteIds.forEach { favoritesStore.setFavorite(it, true) }
                                    refreshFavorites()
                                }
                            }
                        }
                    )

                    MainTab.HISTORY ->'''
if fav_call not in m:
    raise SystemExit('favorites call not found')
m = m.replace(fav_call, fav_call_new, 1)
main.write_text(m, encoding='utf-8')

testroot = Path('app/src/test/java/com/saamaka/dico/testeurs')
(testroot / 'LearningAccessPolicyTest.kt').write_text('''package com.saamaka.dico.testeurs

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LearningAccessPolicyTest {
    @Test fun premiumAndTesterAlwaysHaveAccess() {
        assertTrue(hasLearningSectionAccess(AccessLevel.PREMIUM, "WORDS", emptyMap()))
        assertTrue(hasLearningSectionAccess(AccessLevel.TESTER, "AUDIO", emptyMap()))
    }
    @Test fun exhaustedReviewQuotaLocksAllReviewSections() {
        val remaining = mapOf(LearningActivity.REVIEW to 0)
        assertFalse(hasLearningSectionAccess(AccessLevel.GUEST, "WORDS", remaining))
        assertFalse(hasLearningSectionAccess(AccessLevel.GUEST, "FAVORITES", remaining))
        assertFalse(hasLearningSectionAccess(AccessLevel.GUEST, "WORD_OF_DAY", remaining))
        assertFalse(hasLearningSectionAccess(AccessLevel.GUEST, "AUDIO", remaining))
    }
    @Test fun remainingTrialKeepsSectionOpen() {
        assertTrue(hasLearningSectionAccess(AccessLevel.FREE_ACCOUNT, "WORDS", mapOf(LearningActivity.REVIEW to 1)))
    }
}
''', encoding='utf-8')

(testroot / 'LearningContentPolicyTest.kt').write_text('''package com.saamaka.dico.testeurs

import com.saamaka.dico.testeurs.model.DictionaryEntry
import org.junit.Assert.assertEquals
import org.junit.Test

class LearningContentPolicyTest {
    private fun entry(id: Int, valide: String = "O", category: String = "Base") = DictionaryEntry(id, "w$id", "mot$id", "", "", category, valide)
    @Test fun trustedSelectionRejectsDoubtfulLocalChangesDeletionAndEmptyCategory() {
        val result = trustedLearningEntries(
            listOf(entry(1), entry(2, "D"), entry(3), entry(4), entry(5, category = "")),
            correctedEntryIds = setOf(3), deletionProposalEntryIds = setOf(4)
        )
        assertEquals(listOf(1), result.map { it.id })
    }
}
''', encoding='utf-8')
