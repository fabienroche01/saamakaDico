package com.saamaka.dico.testeurs.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.FilterChip
import com.saamaka.dico.testeurs.AppLanguage
import com.saamaka.dico.testeurs.model.DictionaryEntry
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.saamaka.dico.testeurs.AppStrings
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment

@Composable
fun SearchScreen(
    total: Int,
    officiallyValidated: Int,
    toReview: Int,
    waiting: Int,
    query: String,
    status: String,
    selectedLanguage: AppLanguage,
    strings: AppStrings,
    onLanguageChange: (AppLanguage) -> Unit,
    entries: List<DictionaryEntry>,
    isValidated: (Int) -> Boolean,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    onOpen: (DictionaryEntry) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {

        Spacer(Modifier.height(14.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(Modifier.padding(18.dp)) {
                Text(
                    strings.dictionaryTitle,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )

                Spacer(Modifier.height(8.dp))

                Text(
                    "$total ${strings.wordsAvailable}",
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(Modifier.height(12.dp))

                Text("✅ $officiallyValidated ${strings.officiallyValidated}")
                Text("🟡 $toReview ${strings.toReview}")
                Text("⚪ $waiting ${strings.waiting}")
            }
        }

        // -------------------------------------------------
        // CHOIX DE LA LANGUE
        // -------------------------------------------------

        Spacer(Modifier.height(12.dp))

        Text(
            strings.language,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(8.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            AppLanguage.entries
                .chunked(2)
                .forEach { rowLanguages ->

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {

                        rowLanguages.forEach { language ->

                            val selected =
                                selectedLanguage == language

                            FilterChip(
                                selected = selected,
                                onClick = {
                                    onLanguageChange(language)
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(14.dp),

                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor =
                                        MaterialTheme.colorScheme.primary,
                                    selectedLabelColor =
                                        MaterialTheme.colorScheme.onPrimary,
                                    containerColor =
                                        MaterialTheme.colorScheme.surfaceVariant,
                                    labelColor =
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                ),

                                border =
                                    FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = selected,
                                        borderColor =
                                            MaterialTheme.colorScheme.outline.copy(
                                                alpha = 0.35f
                                            ),
                                        selectedBorderColor =
                                            MaterialTheme.colorScheme.primary
                                    ),

                                label = {
                                    Text(
                                        text = when (language) {
                                            AppLanguage.FRENCH -> "🇫🇷 Français"
                                            AppLanguage.SAAMAKA -> "🇸🇷 Saamaka"
                                            AppLanguage.ENGLISH -> "🇬🇧 English"
                                            AppLanguage.DUTCH -> "🇳🇱 Nederlands"
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        fontWeight =
                                            if (selected) {
                                                FontWeight.Bold
                                            } else {
                                                FontWeight.Medium
                                            }
                                    )
                                }
                            )
                        }

                        if (rowLanguages.size == 1) {
                            Spacer(
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
        }

        // -------------------------------------------------
        // RECHERCHE
        // -------------------------------------------------

        Spacer(Modifier.height(14.dp))

        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(18.dp),

            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(
                    alpha = 0.55f
                ),
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            ),

            placeholder = {
                Text(
                    text = strings.searchPlaceholder,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },

            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Rechercher",
                    tint = MaterialTheme.colorScheme.primary
                )
            },

            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(
                        onClick = onClear
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Effacer"
                        )
                    }
                }
            }
        )

        Spacer(Modifier.height(10.dp))

        // -------------------------------------------------
        // RÉSULTATS
        // -------------------------------------------------

        when {

            query.isBlank() -> {

                Spacer(Modifier.height(8.dp))

                Text(
                    text = strings.startSearching,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            entries.isEmpty() -> {

                Spacer(Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text(
                        text = status.ifBlank {
                            strings.noResult
                        },
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            else -> {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Text(
                        text = "${entries.size} ${strings.results}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = selectedLanguage.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {

                    items(
                        entries,
                        key = { it.id }
                    ) { entry ->

                        val sourceText = when (selectedLanguage) {
                            AppLanguage.FRENCH -> entry.french
                            AppLanguage.ENGLISH -> entry.english
                            AppLanguage.DUTCH -> entry.dutch
                            AppLanguage.SAAMAKA -> entry.saamaka
                        }

                        val translationText = when (selectedLanguage) {
                            AppLanguage.FRENCH -> entry.saamaka
                            AppLanguage.SAAMAKA -> entry.french
                            AppLanguage.ENGLISH -> entry.saamaka
                            AppLanguage.DUTCH -> entry.saamaka
                        }
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onOpen(entry)
                                },
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor =
                                    MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(15.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {

                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {

                                    Text(
                                        text = sourceText,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )

                                    Spacer(Modifier.height(4.dp))

                                    Text(
                                        text = translationText.ifBlank {
                                            if (selectedLanguage == AppLanguage.SAAMAKA) {
                                                "Français : à compléter"
                                            } else {
                                                "Saamaka : à compléter"
                                            }
                                        },
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    if (isValidated(entry.id)) {

                                        Spacer(Modifier.height(7.dp))

                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color =
                                                MaterialTheme.colorScheme.primaryContainer
                                        ) {
                                            Text(
                                                text = "✓ ${strings.verified}",
                                                modifier = Modifier.padding(
                                                    horizontal = 9.dp,
                                                    vertical = 4.dp
                                                ),
                                                style =
                                                    MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color =
                                                    MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }
                                }

                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}