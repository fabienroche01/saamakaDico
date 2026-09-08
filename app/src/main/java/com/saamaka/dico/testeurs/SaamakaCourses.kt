package com.saamaka.dico.testeurs

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.saamaka.dico.testeurs.model.DictionaryEntry

internal data class SaamakaCourseLesson(
    val id: String,
    val title: UiCopyKey,
    val categories: Set<String> = emptySet(),
    val frenchTerms: Set<String> = emptySet(),
    val phraseOnly: Boolean = false
)

internal data class SaamakaCourseLevel(val title: UiCopyKey, val lessons: List<SaamakaCourseLesson>)

internal val saamakaCourseLevels = listOf(
    SaamakaCourseLevel(
        UiCopyKey.LEVEL_1_BASES,
        listOf(
            SaamakaCourseLesson("first_words", UiCopyKey.FIRST_WORDS),
            SaamakaCourseLesson("greetings", UiCopyKey.GREETINGS, setOf("Expression conversationnelle")),
            SaamakaCourseLesson(
                "pronouns", UiCopyKey.PRONOUNS,
                frenchTerms = setOf("je", "tu", "il", "elle", "nous", "vous", "ils", "elles")
            ),
            SaamakaCourseLesson(
                "numbers", UiCopyKey.NUMBERS,
                frenchTerms = setOf("zéro", "un", "deux", "trois", "quatre", "cinq", "six", "sept", "huit", "neuf", "dix")
            )
        )
    ),
    SaamakaCourseLevel(
        UiCopyKey.LEVEL_2_DAILY_LIFE,
        listOf(
            SaamakaCourseLesson("family", UiCopyKey.FAMILY, setOf("Famille")),
            SaamakaCourseLesson("house", UiCopyKey.HOUSE, setOf("Habitat")),
            SaamakaCourseLesson("food", UiCopyKey.FOOD, setOf("Alimentation")),
            SaamakaCourseLesson("body_health", UiCopyKey.BODY_HEALTH, setOf("Corps", "Santé"))
        )
    ),
    SaamakaCourseLevel(
        UiCopyKey.LEVEL_3_SPEAK,
        listOf(
            SaamakaCourseLesson("essential_verbs", UiCopyKey.ESSENTIAL_VERBS, setOf("Action")),
            SaamakaCourseLesson("build_sentence", UiCopyKey.BUILD_SENTENCE),
            SaamakaCourseLesson("questions_answers", UiCopyKey.QUESTIONS_ANSWERS),
            SaamakaCourseLesson("common_expressions", UiCopyKey.COMMON_EXPRESSIONS, setOf("Expression conversationnelle"))
        )
    ),
    SaamakaCourseLevel(
        UiCopyKey.LEVEL_4_FURTHER,
        listOf(
            SaamakaCourseLesson("complete_sentences", UiCopyKey.COMPLETE_SENTENCES, setOf("Expression"), phraseOnly = true),
            SaamakaCourseLesson("comprehension", UiCopyKey.COMPREHENSION),
            SaamakaCourseLesson("course_review", UiCopyKey.COURSE_REVIEW),
            SaamakaCourseLesson("culture_language", UiCopyKey.CULTURE_LANGUAGE, setOf("Culture", "Langue"))
        )
    )
)

internal fun courseEntries(lesson: SaamakaCourseLesson, entries: List<DictionaryEntry>): List<DictionaryEntry> =
    entries.asSequence()
        .filter { it.valide.trim().equals("O", ignoreCase = true) }
        .filter { it.saamaka.isNotBlank() && it.french.isNotBlank() }
        .filter { entry ->
            val categoryMatch = lesson.categories.any { it.equals(entry.categorie.trim(), ignoreCase = true) }
            val termMatch = lesson.frenchTerms.any { it.equals(entry.french.trim(), ignoreCase = true) }
            categoryMatch || termMatch
        }
        .filter { !lesson.phraseOnly || it.french.trim().split(Regex("\\s+")).size > 1 }
        .sortedBy { it.french.lowercase() }
        .take(8)
        .toList()

internal fun completedLessonsAfter(existing: Set<String>, lessonId: String): Set<String> =
    existing + lessonId

internal class SaamakaCourseProgressStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    fun completedLessonIds(): Set<String> = preferences.getStringSet(COMPLETED, emptySet()).orEmpty().toSet()

    fun markCompleted(lessonId: String) {
        preferences.edit().putStringSet(COMPLETED, completedLessonsAfter(completedLessonIds(), lessonId)).apply()
    }

    fun lastLessonId(): String? = preferences.getString(LAST_LESSON, null)

    fun setLastLesson(lessonId: String) {
        preferences.edit().putString(LAST_LESSON, lessonId).apply()
    }

    private companion object {
        const val PREFERENCES = "saamaka_course_progress"
        const val COMPLETED = "completed_lessons"
        const val LAST_LESSON = "last_lesson"
    }
}

@Composable
internal fun SaamakaCoursesScreen(
    strings: AppStrings,
    entries: List<DictionaryEntry>,
    progressStore: SaamakaCourseProgressStore,
    selectedLessonId: String?,
    onSelectedLessonChange: (String?) -> Unit,
    onBackToLearn: () -> Unit,
    onOpenEntry: (DictionaryEntry) -> Unit,
    hasAudio: (DictionaryEntry) -> Boolean,
    onPlayAudio: (DictionaryEntry) -> Unit
) {
    var completed by remember { mutableStateOf(progressStore.completedLessonIds()) }
    val lessons = remember { saamakaCourseLevels.flatMap { it.lessons } }
    val available = remember(entries) { lessons.associateWith { courseEntries(it, entries) } }
    val selectedLesson = lessons.firstOrNull { it.id == selectedLessonId }

    if (selectedLesson != null) {
        val lessonEntries = available[selectedLesson].orEmpty()
        CourseLessonScreen(
            strings = strings,
            lesson = selectedLesson,
            entries = lessonEntries,
            completed = selectedLesson.id in completed,
            onBack = { onSelectedLessonChange(null) },
            onComplete = {
                progressStore.markCompleted(selectedLesson.id)
                completed = progressStore.completedLessonIds()
            },
            onOpenEntry = onOpenEntry,
            hasAudio = hasAudio,
            onPlayAudio = onPlayAudio
        )
        return
    }

    val progress = completed.size.toFloat() / lessons.size
    OutlinedButton(onClick = onBackToLearn) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.ui(UiCopyKey.BACK_DESCRIPTION))
        Text(strings.ui(UiCopyKey.LEARN))
    }
    Spacer(Modifier.height(10.dp))
    Text(strings.ui(UiCopyKey.SAAMAKA_COURSES), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
    Text(strings.ui(UiCopyKey.COURSES_SUBTITLE), color = Color(0xFF68736C))
    Spacer(Modifier.height(12.dp))
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0B5D3B)),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(Modifier.padding(18.dp)) {
            Text(strings.ui(UiCopyKey.COURSE_PROGRESS), color = Color.White, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress }, modifier = Modifier.fillMaxWidth(),
                color = Color(0xFFF0C96A), trackColor = Color.White.copy(alpha = 0.22f)
            )
            Spacer(Modifier.height(6.dp))
            Text(strings.ui(UiCopyKey.LESSONS_COMPLETED, completed.size), color = Color.White)
            Spacer(Modifier.height(10.dp))
            Button(onClick = {
                val target = progressStore.lastLessonId()?.let { id -> lessons.firstOrNull { it.id == id } }
                    ?: lessons.firstOrNull { it.id !in completed && available[it].orEmpty().isNotEmpty() }
                target?.let {
                    progressStore.setLastLesson(it.id)
                    onSelectedLessonChange(it.id)
                }
            }) { Text(strings.ui(UiCopyKey.CONTINUE_COURSE)) }
        }
    }

    saamakaCourseLevels.forEach { level ->
        Spacer(Modifier.height(16.dp))
        Text(strings.ui(level.title), fontWeight = FontWeight.ExtraBold, color = Color(0xFF16372A))
        Spacer(Modifier.height(6.dp))
        level.lessons.forEach { lesson ->
            val lessonEntries = available[lesson].orEmpty()
            val enabled = lessonEntries.isNotEmpty()
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .then(if (enabled) Modifier.clickable {
                        progressStore.setLastLesson(lesson.id)
                        onSelectedLessonChange(lesson.id)
                    } else Modifier),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBF3)),
                border = BorderStroke(1.dp, Color(0xFFE0D8C9)),
                shape = RoundedCornerShape(18.dp)
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(strings.ui(lesson.title), fontWeight = FontWeight.SemiBold)
                        Text(
                            if (enabled) strings.ui(UiCopyKey.WORD_COUNT, lessonEntries.size)
                            else strings.ui(UiCopyKey.COMING_SOON),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF68736C)
                        )
                    }
                    if (lesson.id in completed) Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF0B5D3B))
                    else if (enabled) Icon(Icons.Default.ChevronRight, null, tint = Color(0xFF0B5D3B))
                }
            }
        }
    }
}

@Composable
private fun CourseLessonScreen(
    strings: AppStrings,
    lesson: SaamakaCourseLesson,
    entries: List<DictionaryEntry>,
    completed: Boolean,
    onBack: () -> Unit,
    onComplete: () -> Unit,
    onOpenEntry: (DictionaryEntry) -> Unit,
    hasAudio: (DictionaryEntry) -> Boolean,
    onPlayAudio: (DictionaryEntry) -> Unit
) {
    var currentIndex by remember(lesson.id) { mutableStateOf(0) }
    val entry = entries.getOrNull(currentIndex)
    OutlinedButton(onClick = onBack) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.ui(UiCopyKey.BACK_DESCRIPTION))
        Text(strings.ui(UiCopyKey.SAAMAKA_COURSES))
    }
    Spacer(Modifier.height(10.dp))
    Text(strings.ui(lesson.title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
    if (entry != null) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp).clickable { onOpenEntry(entry) },
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBF3)),
            border = BorderStroke(1.dp, Color(0xFFE0D8C9)),
            shape = RoundedCornerShape(18.dp)
        ) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(entry.saamaka, fontWeight = FontWeight.Bold, color = Color(0xFF0B5D3B))
                    Text(entry.french, color = Color(0xFF68736C))
                }
                if (hasAudio(entry)) {
                    OutlinedButton(onClick = { onPlayAudio(entry) }) {
                        Icon(Icons.Default.PlayArrow, contentDescription = strings.ui(UiCopyKey.LISTEN))
                    }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { currentIndex-- },
                enabled = currentIndex > 0,
                modifier = Modifier.weight(1f)
            ) { Text(strings.ui(UiCopyKey.PREVIOUS)) }
            OutlinedButton(
                onClick = { currentIndex++ },
                enabled = currentIndex < entries.lastIndex,
                modifier = Modifier.weight(1f)
            ) { Text(strings.ui(UiCopyKey.NEXT)) }
        }
    }
    Spacer(Modifier.height(12.dp))
    Button(onClick = onComplete, enabled = !completed, modifier = Modifier.fillMaxWidth()) {
        Text(strings.ui(if (completed) UiCopyKey.LESSON_COMPLETED else UiCopyKey.MARK_LESSON_COMPLETE))
    }
}
