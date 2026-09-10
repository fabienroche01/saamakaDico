package com.saamaka.dico.testeurs

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.saamaka.dico.testeurs.model.DictionaryEntry

@Composable
internal fun AudioAdminScreen(
    entries: List<DictionaryEntry>,
    locallyValidatedIds: Set<Int>,
    audioStore: AudioStore,
    testerName: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var audioRevision by remember { mutableIntStateOf(0) }
    var selectedEntryId by remember { mutableStateOf<Int?>(null) }
    var isRecording by remember { mutableStateOf(false) }

    val candidates = remember(entries, locallyValidatedIds, testerName, audioRevision) {
        entries
            .asSequence()
            .filter { entry ->
                entry.saamaka.isNotBlank() &&
                    entry.french.isNotBlank() &&
                    (
                        entry.valide.trim().equals("O", ignoreCase = true) ||
                            entry.id in locallyValidatedIds
                        ) &&
                    !audioStore.hasOfficialAudio(entry.id) &&
                    !audioStore.hasAudio(entry.id, testerName)
            }
            .sortedBy { it.id }
            .toList()
    }

    LaunchedEffect(candidates) {
        if (selectedEntryId == null || candidates.none { it.id == selectedEntryId }) {
            selectedEntryId = candidates.firstOrNull()?.id
        }
    }

    val selectedEntry = candidates.firstOrNull { it.id == selectedEntryId }
    val selectedIndex = candidates.indexOfFirst { it.id == selectedEntryId }.coerceAtLeast(0)

    val microphonePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    fun startRecording(entry: DictionaryEntry) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!granted) {
            microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }

        audioStore.startRecording(entry.id, testerName)
        isRecording = true
    }

    fun stopAndAdvance() {
        if (!isRecording) return
        val saved = audioStore.stopRecording()
        isRecording = false
        if (saved) {
            audioRevision++
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Admin audio",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF16372A)
                    )
                    Text(
                        text = "Mots validés sans audio",
                        fontSize = 13.sp,
                        color = Color(0xFF68736C)
                    )
                }
                OutlinedButton(
                    onClick = {
                        if (isRecording) audioStore.stopRecording()
                        isRecording = false
                        onBack()
                    }
                ) {
                    Text("Retour")
                }
            }
        }

        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFFDCEEE2),
                border = BorderStroke(1.dp, Color(0xFFBFD8C6))
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text(
                        text = "${candidates.size} audio(s) restant(s)",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0B5D3B)
                    )
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = {
                            if (entries.isEmpty()) 0f
                            else 1f - (candidates.size.toFloat() / entries.size.toFloat()).coerceIn(0f, 1f)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        if (selectedEntry == null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEFC4))
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Text(
                            text = "Tous les mots validés ont un audio.",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF16372A)
                        )
                    }
                }
            }
        } else {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBF3)),
                    border = BorderStroke(1.dp, Color(0xFFE0D8C9))
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Text(
                            text = "${selectedIndex + 1} / ${candidates.size}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF68736C)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = selectedEntry.saamaka,
                            fontSize = 34.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0B5D3B)
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = selectedEntry.french,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF2E332F)
                        )
                        Spacer(Modifier.height(18.dp))

                        if (isRecording) {
                            Button(
                                onClick = ::stopAndAdvance,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Arrêter et suivant")
                            }
                        } else {
                            Button(
                                onClick = { startRecording(selectedEntry) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Enregistrer")
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "File d'attente",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF16372A)
                )
            }

            items(candidates, key = { it.id }) { entry ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !isRecording) { selectedEntryId = entry.id },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (entry.id == selectedEntryId) {
                            Color(0xFFDCEEE2)
                        } else {
                            Color(0xFFFFFBF3)
                        }
                    ),
                    border = BorderStroke(1.dp, Color(0xFFE0D8C9))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = entry.saamaka,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0B5D3B)
                            )
                            Text(
                                text = entry.french,
                                fontSize = 12.sp,
                                color = Color(0xFF68736C)
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "#${entry.id}",
                            fontSize = 11.sp,
                            color = Color(0xFF68736C)
                        )
                    }
                }
            }
        }
    }
}
