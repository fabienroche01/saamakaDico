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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AppLanguage.entries.forEach { language ->

                FilterChip(
                    selected = selectedLanguage == language,
                    onClick = {
                        onLanguageChange(language)
                    },
                    label = {
                        Text(language.label)
                    }
                )
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
            placeholder = {
                Text(strings.searchPlaceholder)
            },
            leadingIcon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = "Rechercher"
                )
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = onClear) {
                        Icon(
                            Icons.Default.Clear,
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
                Text(
                    strings.startSearching,
                    fontWeight = FontWeight.SemiBold
                )
            }

            entries.isEmpty() -> {
                Text(
                    status.ifBlank {
                        strings.noResult
                    },
                    fontWeight = FontWeight.SemiBold
                )
            }

            else -> {

                Text(
                    "${entries.size} ${strings.results}",
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    items(
                        entries,
                        key = { it.id }
                    ) { entry ->

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onOpen(entry)
                                },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor =
                                    MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {

                            Column(
                                Modifier.padding(14.dp)
                            ) {
                                val sourceText = when (selectedLanguage) {
                                    AppLanguage.FRENCH -> entry.french
                                    AppLanguage.ENGLISH -> entry.english
                                    AppLanguage.DUTCH -> entry.dutch
                                }

                                Text(
                                    sourceText,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleMedium
                                )

                                Spacer(
                                    Modifier.height(4.dp)
                                )

                                Text(
                                    entry.saamaka,
                                    style = MaterialTheme.typography.bodyLarge
                                )

                                if (isValidated(entry.id)) {

                                    Spacer(
                                        Modifier.height(6.dp)
                                    )

                                    Text(
                                        "✅ ${strings.verified}",
                                        style =
                                            MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}