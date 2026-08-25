package com.saamaka.dico.testeurs.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.FilterChip
import com.saamaka.dico.testeurs.AppLanguage
import com.saamaka.dico.testeurs.model.DictionaryEntry
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.WbSunny
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Row


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
    onOpen: (DictionaryEntry) -> Unit,
    onTranslateClick: () -> Unit,
    onLearnClick: () -> Unit,
    onFavoritesClick: () -> Unit,
    onHistoryClick: () -> Unit,
    categoryCount: Int,
    wordOfDay: DictionaryEntry?,
    onWordOfDayClick: () -> Unit,
    onCategoriesClick: () -> Unit,
    showHomeContent: Boolean = true
) {
    val homeScrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (showHomeContent && query.isBlank()) {
                    Modifier.verticalScroll(homeScrollState)
                } else {
                    Modifier
                }
            )
    ) {

        Spacer(Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF0A5A39)
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 4.dp
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 18.dp,
                        vertical = 16.dp
                    )
            ) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {

                        Text(
                            text = "SAAMAKA TONGO",
                            fontSize = 23.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )

                        Spacer(Modifier.height(2.dp))

                        Text(
                            text = "DICTIONNAIRE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF0C96A),
                            letterSpacing = 1.4.sp
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color.White.copy(alpha = 0.12f)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Spa,
                            contentDescription = null,
                            tint = Color(0xFFF0C96A),
                            modifier = Modifier.padding(9.dp)
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))

                Text(
                    text = "Français ↔ Saamaka",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = "Apprendre • Comprendre • Préserver",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.88f)
                )

                Spacer(Modifier.height(2.dp))

                Text(
                    text = "Notre langue, notre patrimoine",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.72f)
                )

                Spacer(Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(50),
                        color = Color(0xFFF7F2E8)
                    ) {
                        Row(
                            modifier = Modifier.padding(
                                horizontal = 10.dp,
                                vertical = 7.dp
                            ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                contentDescription = null,
                                tint = Color(0xFF0B5D3B),
                                modifier = Modifier.size(16.dp)
                            )

                            Spacer(Modifier.width(5.dp))

                            Text(
                                text = "$total mots et expressions",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF0B5D3B),
                                maxLines = 1
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(0xFFF0C96A)
                    ) {
                        Text(
                            text = "V15",
                            modifier = Modifier.padding(
                                horizontal = 10.dp,
                                vertical = 7.dp
                            ),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF16372A)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(24.dp),

            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF0B5D3B),
                unfocusedBorderColor = Color(0xFFD6D0C5),
                focusedContainerColor = Color(0xFFFFFBF3),
                unfocusedContainerColor = Color(0xFFFFFBF3),
                cursorColor = Color(0xFF0B5D3B)
            ),

            placeholder = {
                Text(
                    text = "Rechercher un mot...",
                    fontSize = 13.sp,
                    color = Color(0xFF7A817C)
                )
            },

            leadingIcon = {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color(0xFFDCEEE2)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Rechercher",
                        tint = Color(0xFF0B5D3B),
                        modifier = Modifier.padding(8.dp)
                    )
                }
            },

            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(
                        onClick = onClear
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Effacer",
                            tint = Color(0xFF52645B)
                        )
                    }
                }
            }
        )

        if (showHomeContent && query.isBlank()) {
            Spacer(Modifier.height(16.dp))

            Text(
                text = "Accès rapide",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF16372A)
            )

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(112.dp)
                        .clickable {
                            onCategoriesClick()
                        },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF0B5D3B)
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 3.dp
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp)
                    ) {

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.MenuBook,
                                contentDescription = null,
                                tint = Color(0xFFF0C96A),
                                modifier = Modifier.size(24.dp)
                            )

                            Spacer(Modifier.weight(1f))

                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color.White.copy(alpha = 0.14f)
                            ) {
                                Text(
                                    text = "$categoryCount thèmes",
                                    modifier = Modifier.padding(
                                        horizontal = 7.dp,
                                        vertical = 3.dp
                                    ),
                                    fontSize = 9.sp,
                                    color = Color.White
                                )
                            }
                        }

                        Spacer(Modifier.height(7.dp))

                        Text(
                            text = "Catégories",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Text(
                            text = "Explorer par thème",
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.78f)
                        )

                        Spacer(Modifier.weight(1f))

                        Text(
                            text = "Explorer →",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFF0C96A)
                        )
                    }
                }

                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(112.dp)
                        .clickable {
                            onFavoritesClick()
                        },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF174C36)
                    ),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 3.dp
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp)
                    ) {

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                tint = Color(0xFFF0C96A),
                                modifier = Modifier.size(24.dp)
                            )

                            Spacer(Modifier.weight(1f))

                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color.White.copy(alpha = 0.14f)
                            ) {
                                Text(
                                    text = "Mes mots",
                                    modifier = Modifier.padding(
                                        horizontal = 7.dp,
                                        vertical = 3.dp
                                    ),
                                    fontSize = 9.sp,
                                    color = Color.White
                                )
                            }
                        }

                        Spacer(Modifier.height(7.dp))

                        Text(
                            text = "Favoris",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Text(
                            text = "Retrouver mes mots",
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.78f)
                        )

                        Spacer(Modifier.weight(1f))

                        Text(
                            text = "Voir mes mots →",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFF0C96A)
                        )
                    }
                }
            }

                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {

                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                onWordOfDayClick()
                            },
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFFEFC4)
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 2.dp
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        ) {

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {

                                Icon(
                                    imageVector = Icons.Default.WbSunny,
                                    contentDescription = null,
                                    tint = Color(0xFF9A7414),
                                    modifier = Modifier.size(24.dp)
                                )

                                Spacer(Modifier.weight(1f))

                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = Color.White.copy(alpha = 0.65f)
                                ) {
                                    Text(
                                        text = "Aujourd'hui",
                                        modifier = Modifier.padding(
                                            horizontal = 7.dp,
                                            vertical = 3.dp
                                        ),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF705A1D)
                                    )
                                }
                            }

                            Spacer(Modifier.height(7.dp))

                            Text(
                                text = "Mot du jour",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF3B3014)
                            )

                            Spacer(Modifier.height(4.dp))

                            Text(
                                text = wordOfDay?.saamaka.orEmpty(),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0B5D3B),
                                maxLines = 1
                            )

                            Spacer(Modifier.height(2.dp))

                            Text(
                                text = wordOfDay?.french.orEmpty(),
                                fontSize = 10.sp,
                                color = Color(0xFF7A6B46),
                                maxLines = 1
                            )
                        }
                    }

                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                onLearnClick()
                            },
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFF4EFE5)
                        ),
                        elevation = CardDefaults.cardElevation(
                            defaultElevation = 2.dp
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        ) {

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {

                                Icon(
                                    imageVector = Icons.Default.School,
                                    contentDescription = null,
                                    tint = Color(0xFF0B5D3B),
                                    modifier = Modifier.size(24.dp)
                                )

                                Spacer(Modifier.weight(1f))

                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = Color(0xFFDCEEE2)
                                ) {
                                    Text(
                                        text = "Progression",
                                        modifier = Modifier.padding(
                                            horizontal = 7.dp,
                                            vertical = 3.dp
                                        ),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF0B5D3B)
                                    )
                                }
                            }

                            Spacer(Modifier.height(7.dp))

                            Text(
                                text = "Apprendre",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF16372A)
                            )

                            Spacer(Modifier.height(4.dp))

                            Text(
                                text = "Quiz • Mots • Phrases • Jeux",
                                fontSize = 10.sp,
                                color = Color(0xFF68736C),
                                maxLines = 1
                            )

                            Spacer(Modifier.height(3.dp))

                            Text(
                                text = "Continuer →",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF0B5D3B)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = null,
                        tint = Color(0xFF0B5D3B),
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = strings.language,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E332F)
                    )
                }

                Spacer(Modifier.height(6.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {

                    AppLanguage.entries
                        .chunked(2)
                        .forEach { rowLanguages ->

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {

                                rowLanguages.forEach { language ->

                                    val selected =
                                        selectedLanguage == language

                                    FilterChip(
                                        selected = selected,
                                        onClick = {
                                            onLanguageChange(language)
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(32.dp),
                                        shape = RoundedCornerShape(16.dp),

                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = Color(0xFF0B5D3B),
                                            selectedLabelColor = Color.White,
                                            containerColor = Color(0xFFF4EFE5),
                                            labelColor = Color(0xFF3F4842)
                                        ),

                                        border = FilterChipDefaults.filterChipBorder(
                                            enabled = true,
                                            selected = selected,
                                            borderColor = Color(0xFFD2CCC0),
                                            selectedBorderColor = Color(0xFF0B5D3B)
                                        ),

                                        label = {
                                            Text(
                                                text = when (language) {
                                                    AppLanguage.FRENCH -> "FR · Français"
                                                    AppLanguage.SAAMAKA -> "SM · Saamaka"
                                                    AppLanguage.ENGLISH -> "EN · English"
                                                    AppLanguage.DUTCH -> "NL · Nederlands"
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                textAlign = TextAlign.Center,
                                                maxLines = 1,
                                                fontSize = 11.sp,
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

            } // ferme if (showHomeContent)

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


