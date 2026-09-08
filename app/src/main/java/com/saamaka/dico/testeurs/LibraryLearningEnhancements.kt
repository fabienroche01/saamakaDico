package com.saamaka.dico.testeurs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.sp
import com.saamaka.dico.testeurs.model.DictionaryEntry

private fun libraryMatches(entry: DictionaryEntry, query: String): Boolean {
    val needle = query.trim().lowercase()
    if (needle.isBlank()) return true
    return entry.saamaka.lowercase().contains(needle) ||
        entry.french.lowercase().contains(needle) ||
        entry.english.lowercase().contains(needle) ||
        entry.dutch.lowercase().contains(needle) ||
        entry.categorie.lowercase().contains(needle)
}

@Composable
fun EnhancedSavedScreen(
    strings: AppStrings,
    title: String,
    entries: List<DictionaryEntry>,
    onOpen: (DictionaryEntry) -> Unit,
    onRemove: (DictionaryEntry) -> Unit,
    onSearch: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = entries.filter { libraryMatches(it, query) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Spacer(Modifier.height(12.dp))
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                label = { Text(strings.search) }
            )
            Spacer(Modifier.height(4.dp))
            Text("${filtered.size} / ${entries.size}", fontSize = 12.sp, color = Color(0xFF68736C))
        }

        if (filtered.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFFF4EFE5)
                ) {
                    Column(Modifier.padding(18.dp)) {
                        Text(if (query.isBlank()) title else "Aucun résultat", fontWeight = FontWeight.Bold)
                        if (query.isBlank()) {
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(onClick = onSearch) { Text(strings.search) }
                        }
                    }
                }
            }
        } else {
            items(filtered, key = { it.id }) { entry ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpen(entry) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBF3)),
                    border = BorderStroke(1.dp, Color(0xFFE0D8C9))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(entry.saamaka, fontWeight = FontWeight.Bold, color = Color(0xFF16372A))
                            Text(entry.french, fontSize = 13.sp, color = Color(0xFF68736C))
                        }
                        IconButton(onClick = { onRemove(entry) }) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFF8B2F2F))
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
fun EnhancedHistoryScreen(
    strings: AppStrings,
    entries: List<DictionaryEntry>,
    onClear: () -> Unit,
    onOpen: (DictionaryEntry) -> Unit,
    onRemove: (DictionaryEntry) -> Unit,
    onSearch: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val filtered = entries.filter { libraryMatches(it, query) }
    val latest = entries.firstOrNull()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(strings.history, modifier = Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                if (entries.isNotEmpty()) {
                    OutlinedButton(onClick = onClear) { Text("Effacer") }
                }
            }
            latest?.let { entry ->
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { onOpen(entry) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0B5D3B)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.History, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("${entry.saamaka} · ${entry.french}")
                }
            }
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                label = { Text(strings.search) }
            )
            Spacer(Modifier.height(4.dp))
            Text("${filtered.size} / ${entries.size}", fontSize = 12.sp, color = Color(0xFF68736C))
        }

        if (filtered.isEmpty()) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFFF4EFE5)
                ) {
                    Column(Modifier.padding(18.dp)) {
                        Text(if (query.isBlank()) strings.history else "Aucun résultat", fontWeight = FontWeight.Bold)
                        if (query.isBlank()) {
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(onClick = onSearch) { Text(strings.search) }
                        }
                    }
                }
            }
        } else {
            items(filtered, key = { it.id }) { entry ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpen(entry) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBF3)),
                    border = BorderStroke(1.dp, Color(0xFFE0D8C9))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(entry.saamaka, fontWeight = FontWeight.Bold, color = Color(0xFF16372A))
                            Text(entry.french, fontSize = 13.sp, color = Color(0xFF68736C))
                        }
                        IconButton(onClick = { onRemove(entry) }) {
                            Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFF8B2F2F))
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
fun AudioLearningSection(
    strings: AppStrings,
    entries: List<DictionaryEntry>,
    audioStore: AudioStore,
    onConsumeTrial: () -> Boolean
) {
    val audioEntries = remember(entries) {
        entries.shuffled().take(300).filter { audioStore.hasOfficialAudio(it.id) }.take(40)
    }
    var current by remember(audioEntries) { mutableStateOf(audioEntries.firstOrNull()) }
    var selected by remember(current?.id) { mutableStateOf<String?>(null) }
    var score by remember { mutableStateOf(0) }
    var answered by remember { mutableStateOf(0) }
    val answers = remember(current?.id, audioEntries) {
        val entry = current
        if (entry == null) emptyList() else {
            (audioEntries.asSequence()
                .filter { it.id != entry.id }
                .map { it.french.trim() }
                .filter { it.isNotBlank() && it != entry.french.trim() }
                .distinct()
                .shuffled()
                .take(3)
                .toList() + entry.french.trim()).shuffled()
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBF3)),
        border = BorderStroke(1.dp, Color(0xFFE0D8C9))
    ) {
        Column(Modifier.padding(18.dp)) {
            Text("Quiz audio", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF16372A))
            Text("$score / $answered", fontSize = 13.sp, color = Color(0xFF68736C))
            Spacer(Modifier.height(14.dp))

            val entry = current
            if (entry == null) {
                Text("Aucun audio officiel disponible dans cette sélection.")
            } else {
                Button(
                    onClick = { audioStore.playOfficialAudio(entry.id) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0B5D3B))
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(strings.listen.replace("▶", "").trim())
                }
                Spacer(Modifier.height(14.dp))
                answers.forEach { answer ->
                    OutlinedButton(
                        onClick = {
                            if (selected == null && onConsumeTrial()) {
                                selected = answer
                                answered++
                                if (answer == entry.french.trim()) score++
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = when {
                                selected == null -> Color.Transparent
                                answer == entry.french.trim() -> Color(0xFFDCEEE2)
                                selected == answer -> Color(0xFFF8DDDD)
                                else -> Color.Transparent
                            }
                        )
                    ) {
                        Text(answer, modifier = Modifier.fillMaxWidth())
                    }
                    Spacer(Modifier.height(6.dp))
                }
                if (selected != null) {
                    Button(
                        onClick = {
                            current = audioEntries.filter { it.id != entry.id }.randomOrNull() ?: entry
                            selected = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(strings.ui(UiCopyKey.NEXT))
                    }
                }
            }
        }
    }
}
