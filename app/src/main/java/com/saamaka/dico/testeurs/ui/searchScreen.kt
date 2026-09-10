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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.saamaka.dico.testeurs.AppStrings
import com.saamaka.dico.testeurs.UiCopyKey
import com.saamaka.dico.testeurs.ui
import com.saamaka.dico.testeurs.provenanceLabel
import com.saamaka.dico.testeurs.reliabilityLabel
import com.saamaka.dico.testeurs.LocalExactMatch
import com.saamaka.dico.testeurs.homeSearchPresentation
import com.saamaka.dico.testeurs.matchingLanguageForEntry
import com.saamaka.dico.testeurs.searchTextForLanguage
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.testTag
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import com.saamaka.dico.testeurs.PhraseTranslationPipelineResult
import com.saamaka.dico.testeurs.normalizedInputWordCount
import com.saamaka.dico.testeurs.shouldAnalyzeAsPhrase

enum class SearchLanguageFilter(val label: String, val language: AppLanguage?) {
    ALL("Tous", null),
    SAAMAKA("Saamaka", AppLanguage.SAAMAKA),
    FRENCH("Français", AppLanguage.FRENCH),
    ENGLISH("English", AppLanguage.ENGLISH),
    DUTCH("Nederlands", AppLanguage.DUTCH)
}

@Composable
private fun HomePhraseResultBlocks(
    result: PhraseTranslationPipelineResult,
    strings: AppStrings
) {
    val grammatical = result.grammaticalTranslation
    val wordByWord = result.wordByWordTranslation
    val grammaticalFirst = grammatical?.isComplete == true

    @Composable
    fun GrammaticalCard(emphasized: Boolean) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    strings.ui(
                        if (grammatical?.kind == com.saamaka.dico.testeurs.model.PhraseTranslationKind.PARTIAL) {
                            UiCopyKey.GRAMMATICAL_PARTIAL_TRANSLATION
                        } else {
                            UiCopyKey.GRAMMATICAL_TRANSLATION
                        }
                    ),
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0B5D3B)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    grammatical?.translation
                        ?: strings.ui(UiCopyKey.GRAMMATICAL_TRANSLATION_UNAVAILABLE),
                    style = if (emphasized) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium,
                    fontWeight = if (emphasized) FontWeight.ExtraBold else FontWeight.Bold
                )
                grammatical?.unresolvedHints?.takeIf { it.isNotEmpty() }?.let { hints ->
                    Spacer(Modifier.height(6.dp))
                    Text(
                        strings.ui(
                            UiCopyKey.RELATED_DICTIONARY_EXPRESSIONS,
                            hints.values.flatten().distinct().joinToString(", ")
                        )
                    )
                }
            }
        }
    }

    @Composable
    fun WordByWordCard(emphasized: Boolean) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    strings.ui(UiCopyKey.WORD_BY_WORD_TRANSLATION),
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0B5D3B)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    wordByWord?.translation
                        ?: strings.ui(UiCopyKey.TRANSLATION_UNAVAILABLE),
                    style = if (emphasized) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleMedium,
                    fontWeight = if (emphasized) FontWeight.ExtraBold else FontWeight.SemiBold
                )
                wordByWord?.untranslatedSegments
                    ?.takeIf { it.isNotEmpty() }
                    ?.let {
                        Spacer(Modifier.height(6.dp))
                        Text(strings.ui(UiCopyKey.ITEMS_TO_REVIEW, it.joinToString(", ")))
                    }
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (grammaticalFirst) {
            GrammaticalCard(emphasized = true)
            WordByWordCard(emphasized = false)
        } else {
            WordByWordCard(emphasized = true)
            GrammaticalCard(emphasized = false)
        }
    }
}

const val QUICK_SEARCH_TEST_TAG = "quick_search_field"
const val SEARCH_RESULT_TEST_TAG = "search_result"

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
    onOpen: (DictionaryEntry, AppLanguage) -> Unit,
    onOpenExactDictionaryMatch: ((DictionaryEntry, AppLanguage) -> Unit)? = null,
    onTranslateClick: () -> Unit,
    onPhraseSubmit: () -> Unit = {},
    phraseResult: PhraseTranslationPipelineResult? = null,
    exactCompleteMatch: LocalExactMatch? = null,
    onLearnClick: () -> Unit,
    onFavoritesClick: () -> Unit,
    onHistoryClick: () -> Unit,
    categoryCount: Int,
    wordOfDay: DictionaryEntry?,
    onWordOfDayClick: () -> Unit,
    onCategoriesClick: () -> Unit,
    searchLanguageFilter: SearchLanguageFilter = SearchLanguageFilter.ALL,
    onSearchLanguageFilterChange: (SearchLanguageFilter) -> Unit = {},
    hasAudio: (DictionaryEntry) -> Boolean = { false },
    onPlayAudio: (DictionaryEntry) -> Unit = {},
    isFavorite: (DictionaryEntry) -> Boolean = { false },
    onToggleFavorite: (DictionaryEntry) -> Unit = {},
    showHomeContent: Boolean = true
) {
    val homeScrollState = rememberScrollState()
    val searchPresentation = homeSearchPresentation(
        text = query,
        localResultCount = entries.size,
        hasExactCompleteMatch = exactCompleteMatch != null,
        hasStructuredPhraseResult = phraseResult != null
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (showHomeContent && query.isBlank()) {
                    Modifier
                        .verticalScroll(homeScrollState)
                        .padding(bottom = 24.dp)
                } else {
                    Modifier
                }
            )
    ) {

        Spacer(Modifier.height(12.dp))

        if (showHomeContent) Card(
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
                            text = strings.saamaka.uppercase(),
                            fontSize = 23.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )

                        Spacer(Modifier.height(2.dp))

                        Text(
                            text = strings.dictionaryTitle.uppercase(),
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
                    text = "${strings.french} ↔ ${strings.saamaka}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = strings.ui(UiCopyKey.LEARN_UNDERSTAND_PRESERVE),
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.88f)
                )

                Spacer(Modifier.height(2.dp))

                Text(
                    text = strings.ui(UiCopyKey.HERITAGE_TAGLINE),
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
                                text = strings.ui(UiCopyKey.WORDS_AND_EXPRESSIONS, total),
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
                            text = "V16",
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
            modifier = Modifier
                .fillMaxWidth()
                .testTag(QUICK_SEARCH_TEST_TAG),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    if (shouldAnalyzeAsPhrase(query)) onPhraseSubmit()
                }
            ),
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
                    text = strings.ui(UiCopyKey.SEARCH_ALL_LANGUAGES_HINT),
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
                        contentDescription = strings.ui(UiCopyKey.SEARCH_ACTION),
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
                            contentDescription = strings.ui(UiCopyKey.CLEAR),
                            tint = Color(0xFF52645B)
                        )
                    }
                }
            }
        )

        if (!showHomeContent) {
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                SearchLanguageFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = searchLanguageFilter == filter,
                        onClick = { onSearchLanguageFilterChange(filter) },
                        label = {
                            Text(
                                if (filter == SearchLanguageFilter.ALL) strings.ui(UiCopyKey.ALL)
                                else filter.label
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF0B5D3B),
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFFF4EFE5)
                        )
                    )
                }
            }
        }

        if (showHomeContent && query.isBlank()) {
            Spacer(Modifier.height(16.dp))

            Text(
                text = strings.ui(UiCopyKey.QUICK_ACCESS_TITLE),
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
                                    text = strings.ui(UiCopyKey.CATEGORIES_COUNT, categoryCount),
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
                            text = strings.ui(UiCopyKey.CATEGORIES),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Text(
                            text = strings.ui(UiCopyKey.EXPLORE_BY_THEME),
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.78f)
                        )

                        Spacer(Modifier.weight(1f))

                        Text(
                            text = strings.ui(UiCopyKey.EXPLORE_ACTION),
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
                                    text = strings.ui(UiCopyKey.MY_WORDS),
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
                            text = strings.favorites,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Text(
                            text = strings.ui(UiCopyKey.FIND_MY_WORDS),
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.78f)
                        )

                        Spacer(Modifier.weight(1f))

                        Text(
                            text = strings.ui(UiCopyKey.SEE_MY_WORDS),
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
                                        text = strings.ui(UiCopyKey.TODAY),
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
                                text = strings.ui(UiCopyKey.WORD_OF_DAY),
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
                                        text = strings.ui(UiCopyKey.PROGRESSION),
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
                                text = strings.ui(UiCopyKey.LEARN),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF16372A)
                            )

                            Spacer(Modifier.height(4.dp))

                            Text(
                                text = strings.ui(UiCopyKey.LEARNING_ACTIVITIES),
                                fontSize = 10.sp,
                                color = Color(0xFF68736C),
                                maxLines = 1
                            )

                            Spacer(Modifier.height(3.dp))

                            Text(
                                text = "${strings.ui(UiCopyKey.CONTINUE_ACTION)} →",
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

                    SearchLanguageFilter.entries
                        .chunked(2)
                        .forEach { rowFilters ->

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {

                                rowFilters.forEach { filter ->

                                    val selected =
                                        searchLanguageFilter == filter

                                    FilterChip(
                                        selected = selected,
                                        onClick = {
                                            onSearchLanguageFilterChange(filter)
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 32.dp),
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
                                                text = when (filter) {
                                                    SearchLanguageFilter.ALL -> strings.ui(UiCopyKey.ALL)
                                                    SearchLanguageFilter.FRENCH -> "FR · Français"
                                                    SearchLanguageFilter.SAAMAKA -> "SM · Saamaka"
                                                    SearchLanguageFilter.ENGLISH -> "EN · English"
                                                    SearchLanguageFilter.DUTCH -> "NL · Nederlands"
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

                                if (rowFilters.size == 1) {
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

                phraseResult != null -> {
                    HomePhraseResultBlocks(phraseResult, strings)
                    if (entries.isNotEmpty()) {
                        Spacer(Modifier.height(10.dp))
                        Text(strings.ui(UiCopyKey.DICTIONARY_RESULTS), fontWeight = FontWeight.Bold)
                        entries.take(5).forEach { entry ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 6.dp)
                                    .clickable {
                                        onOpen(
                                            entry,
                                            matchingLanguageForEntry(
                                                entry,
                                                query,
                                                searchLanguageFilter.language,
                                                selectedLanguage
                                            )
                                        )
                                    }
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    Text(entry.french, fontWeight = FontWeight.Bold)
                                    Text(entry.saamaka.ifBlank { strings.ui(UiCopyKey.TRANSLATION_UNAVAILABLE) })
                                }
                            }
                        }
                    }
                }

                exactCompleteMatch != null -> {
                    val dictionaryEntry = exactCompleteMatch.entry
                    val exactMatchModifier = if (
                        dictionaryEntry != null && onOpenExactDictionaryMatch != null
                    ) {
                        Modifier.clickable {
                            onOpenExactDictionaryMatch(
                                dictionaryEntry,
                                matchingLanguageForEntry(
                                    entry = dictionaryEntry,
                                    query = query,
                                    filteredLanguage = searchLanguageFilter.language,
                                    preferredLanguage = selectedLanguage
                                )
                            )
                        }
                    } else {
                        Modifier
                    }
                    Card(modifier = Modifier.fillMaxWidth().then(exactMatchModifier)) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                strings.provenanceLabel(exactCompleteMatch.provenance),
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0B5D3B)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                exactCompleteMatch.translation,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                strings.ui(
                                    UiCopyKey.RELIABILITY,
                                    strings.reliabilityLabel(exactCompleteMatch.reliability)
                                )
                            )
                        }
                    }
                }

                searchPresentation.showPhraseCta -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(strings.ui(UiCopyKey.TRANSLATE_THIS_PHRASE), fontWeight = FontWeight.Bold)
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = Color(0xFFFFEFC4)
                                ) {
                                    Text(
                                        "Premium",
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF6D5312)
                                    )
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(strings.ui(UiCopyKey.NO_COMPLETE_EXPRESSION))
                            Spacer(Modifier.height(12.dp))
                            Button(
                                modifier = Modifier.fillMaxWidth(),
                                enabled = normalizedInputWordCount(query) >= 3,
                                onClick = onTranslateClick
                            ) {
                                Text(strings.ui(UiCopyKey.SEARCH_OR_TRANSLATE))
                            }
                        }
                    }
                }

                searchPresentation.showNoResult || entries.isEmpty() -> {

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

                            val resultLanguage = matchingLanguageForEntry(
                                entry = entry,
                                query = query,
                                filteredLanguage = searchLanguageFilter.language,
                                preferredLanguage = selectedLanguage
                            )
                            val audioAvailable = hasAudio(entry)
                            val favorite = isFavorite(entry)
                            val validated = isValidated(entry.id)
                            val requestedTranslation = when (searchLanguageFilter) {
                                SearchLanguageFilter.ALL -> if (resultLanguage == AppLanguage.SAAMAKA) {
                                    entry.french
                                } else {
                                    searchTextForLanguage(entry, resultLanguage.code)
                                }
                                SearchLanguageFilter.SAAMAKA -> entry.french
                                SearchLanguageFilter.FRENCH -> entry.french
                                SearchLanguageFilter.ENGLISH -> entry.english
                                SearchLanguageFilter.DUTCH -> entry.dutch
                            }
                            val selectedTranslation = requestedTranslation.ifBlank {
                                strings.ui(UiCopyKey.TRANSLATION_UNAVAILABLE)
                            }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag(SEARCH_RESULT_TEST_TAG)
                                    .clickable {
                                        onOpen(
                                            entry,
                                            resultLanguage
                                        )
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
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {

                                    Column(
                                        modifier = Modifier.weight(1f)
                                    ) {

                                        Text(
                                            text = entry.saamaka.ifBlank { strings.ui(UiCopyKey.TO_COMPLETE) },
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF16372A),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        Spacer(Modifier.height(3.dp))

                                        Text(
                                            text = selectedTranslation,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                                                alpha = if (selectedTranslation == strings.ui(UiCopyKey.TRANSLATION_UNAVAILABLE)) 0.65f else 1f
                                            ),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        if (validated) {

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

                                    IconButton(
                                        onClick = { onPlayAudio(entry) },
                                        enabled = audioAvailable
                                    ) {
                                        Icon(
                                            Icons.Default.VolumeUp,
                                            strings.ui(UiCopyKey.LISTEN),
                                            tint = if (audioAvailable) Color(0xFF0B5D3B) else Color(0xFFB7B8B3)
                                        )
                                    }
                                    IconButton(onClick = { onToggleFavorite(entry) }) {
                                        Icon(
                                            if (favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                            strings.ui(UiCopyKey.FAVORITE),
                                            tint = if (favorite) Color(0xFFC99A2E) else Color(0xFF68736C)
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

