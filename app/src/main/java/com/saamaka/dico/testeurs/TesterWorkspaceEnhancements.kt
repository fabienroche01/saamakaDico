package com.saamaka.dico.testeurs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.saamaka.dico.testeurs.model.DictionaryEntry
import java.text.Normalizer
import java.util.Locale

enum class TesterListFilter {
    ALL,
    TO_REVIEW,
    VALIDATED,
    CORRECTED,
    DELETION_PROPOSED,
    UNTOUCHED
}

fun testerListFilterMatches(
    filter: TesterListFilter,
    databaseValidated: Boolean,
    locallyValidated: Boolean,
    corrected: Boolean,
    deletionProposed: Boolean,
    needsReview: Boolean
): Boolean {
    val validated = databaseValidated || locallyValidated
    val treated = validated || corrected || deletionProposed
    return when (filter) {
        TesterListFilter.ALL -> true
        TesterListFilter.TO_REVIEW -> needsReview && !treated
        TesterListFilter.VALIDATED -> validated
        TesterListFilter.CORRECTED -> corrected
        TesterListFilter.DELETION_PROPOSED -> deletionProposed
        TesterListFilter.UNTOUCHED -> !treated
    }
}

fun testerTreatedCount(
    entries: List<DictionaryEntry>,
    localValidationIds: Set<Int>,
    correctedIds: Set<Int>,
    deletionIds: Set<Int>
): Int = entries.count { entry ->
    entry.valide.trim().equals("O", ignoreCase = true) ||
        entry.id in localValidationIds ||
        entry.id in correctedIds ||
        entry.id in deletionIds
}

@Composable
fun TesterFilterBar(
    strings: AppStrings,
    selected: TesterListFilter,
    counts: Map<TesterListFilter, Int>,
    onSelected: (TesterListFilter) -> Unit
) {
    val labels = testerFilterLabels(strings)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TesterListFilter.entries.forEach { filter ->
            val active = selected == filter
            Surface(
                modifier = Modifier.clickable { onSelected(filter) },
                shape = RoundedCornerShape(16.dp),
                color = if (active) Color(0xFF0B5D3B) else Color(0xFFF4EFE5),
                border = BorderStroke(
                    if (active) 2.dp else 1.dp,
                    if (active) Color(0xFFF0C96A) else Color(0xFFE0D8C9)
                )
            ) {
                Column(Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
                    Text(
                        text = (counts[filter] ?: 0).toString(),
                        fontWeight = FontWeight.Bold,
                        color = if (active) Color.White else Color(0xFF16372A)
                    )
                    Text(
                        text = labels.getValue(filter),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (active) Color.White else Color(0xFF4C554F)
                    )
                }
            }
        }
    }
}

private fun testerFilterLabels(strings: AppStrings): Map<TesterListFilter, String> {
    val search = strings.search.lowercase(Locale.ROOT)
    val french = search.contains("recher")
    val dutch = search.contains("zoek")
    val saamaka = !french && !dutch && search !in setOf("search")
    fun t(fr: String, en: String, nl: String, srm: String) = when {
        french -> fr
        dutch -> nl
        saamaka -> srm
        else -> en
    }
    return mapOf(
        TesterListFilter.ALL to t("Tous", "All", "Alles", "Ala"),
        TesterListFilter.TO_REVIEW to t("À vérifier", "To review", "Te controleren", "Fu luku"),
        TesterListFilter.VALIDATED to t("Validés", "Validated", "Goedgekeurd", "Valide"),
        TesterListFilter.CORRECTED to t("Corrigés", "Corrected", "Gecorrigeerd", "Koregi"),
        TesterListFilter.DELETION_PROPOSED to t("Suppressions", "Deletions", "Verwijderen", "Puusu"),
        TesterListFilter.UNTOUCHED to t("Non traités", "Untouched", "Onbehandeld", "No du eti")
    )
}

private fun normalizedCategory(value: String): String = Normalizer
    .normalize(value.trim(), Normalizer.Form.NFD)
    .replace("\\p{M}+".toRegex(), "")
    .replace("\\s+".toRegex(), " ")
    .lowercase(Locale.ROOT)

data class CategoryOverview(
    val displayName: String,
    val count: Int,
    val variants: List<String>
)

fun buildCategoryOverviews(entries: List<DictionaryEntry>): List<CategoryOverview> = entries
    .asSequence()
    .map { it.categorie.trim() }
    .filter { it.isNotBlank() }
    .groupBy(::normalizedCategory)
    .values
    .map { variants ->
        val counts = variants.groupingBy { it }.eachCount()
        val display = counts.maxWithOrNull(
            compareBy<Map.Entry<String, Int>> { it.value }
                .thenByDescending { it.key.length }
        )?.key.orEmpty()
        CategoryOverview(
            displayName = display,
            count = variants.size,
            variants = counts.keys.sortedBy { it.lowercase(Locale.ROOT) }
        )
    }
    .sortedBy { it.displayName.lowercase(Locale.ROOT) }
    .toList()

@Composable
fun EnhancedCategoriesScreen(
    strings: AppStrings,
    entries: List<DictionaryEntry>,
    selectedCategory: String?,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSelectedCategoryChange: (String?) -> Unit,
    onBack: () -> Unit,
    onOpen: (DictionaryEntry) -> Unit
) {
    val overview = remember(entries) { buildCategoryOverviews(entries) }
    val filteredCategories = remember(overview, searchQuery) {
        val q = normalizedCategory(searchQuery)
        if (q.isBlank()) overview else overview.filter {
            normalizedCategory(it.displayName).contains(q) ||
                it.variants.any { variant -> normalizedCategory(variant).contains(q) }
        }
    }

    if (selectedCategory != null) {
        val normalizedSelected = normalizedCategory(selectedCategory)
        val categoryEntries = remember(entries, selectedCategory) {
            entries.filter { normalizedCategory(it.categorie) == normalizedSelected }
                .sortedWith(compareBy<DictionaryEntry> { it.french.lowercase(Locale.ROOT) }.thenBy { it.saamaka.lowercase(Locale.ROOT) })
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "← ${selectedCategory}",
                    modifier = Modifier.clickable { onSelectedCategoryChange(null) },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0B5D3B)
                )
                Text(
                    text = "${categoryEntries.size} ${categoryWordLabel(strings, categoryEntries.size)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF68736C)
                )
                Spacer(Modifier.height(8.dp))
            }
            items(categoryEntries, key = { it.id }) { entry ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onOpen(entry) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBF3)),
                    border = BorderStroke(1.dp, Color(0xFFE0D8C9))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(entry.french, fontWeight = FontWeight.Bold, color = Color(0xFF16372A))
                            Text(entry.saamaka, color = Color(0xFF4C554F))
                        }
                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF0B5D3B))
                    }
                }
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "←",
                    modifier = Modifier.clickable(onClick = onBack),
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color(0xFF0B5D3B)
                )
                Spacer(Modifier.width(10.dp))
                Text(strings.category, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                label = { Text(categorySearchLabel(strings)) }
            )
            Spacer(Modifier.height(6.dp))
            val inconsistent = overview.count { it.variants.size > 1 }
            if (inconsistent > 0) {
                Text(
                    text = categoryVariantLabel(strings, inconsistent),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF8A6712)
                )
            }
            Spacer(Modifier.height(8.dp))
        }
        items(filteredCategories, key = { normalizedCategory(it.displayName) }) { category ->
            Card(
                modifier = Modifier.fillMaxWidth().clickable {
                    onSelectedCategoryChange(category.displayName)
                },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBF3)),
                border = BorderStroke(1.dp, Color(0xFFE0D8C9))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(category.displayName, fontWeight = FontWeight.Bold, color = Color(0xFF16372A))
                        Text(
                            "${category.count} ${categoryWordLabel(strings, category.count)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF68736C)
                        )
                        if (category.variants.size > 1) {
                            Text(
                                category.variants.joinToString(" • "),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF8A6712)
                            )
                        }
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color(0xFF0B5D3B))
                }
            }
        }
    }
}

private fun categorySearchLabel(strings: AppStrings): String {
    val s = strings.search.lowercase(Locale.ROOT)
    return when {
        s.contains("recher") -> "Rechercher une catégorie"
        s.contains("zoek") -> "Categorie zoeken"
        s == "search" -> "Search categories"
        else -> "Suku wan kategorii"
    }
}

private fun categoryVariantLabel(strings: AppStrings, count: Int): String {
    val s = strings.search.lowercase(Locale.ROOT)
    return when {
        s.contains("recher") -> "$count catégorie(s) avec variantes d’écriture détectées"
        s.contains("zoek") -> "$count categorie(ën) met schrijfvarianten gevonden"
        s == "search" -> "$count categor${if (count == 1) "y" else "ies"} with naming variants detected"
        else -> "$count kategorii abi difrenti nen"
    }
}

private fun categoryWordLabel(strings: AppStrings, count: Int): String {
    val s = strings.search.lowercase(Locale.ROOT)
    return when {
        s.contains("recher") -> if (count == 1) "mot" else "mots"
        s.contains("zoek") -> if (count == 1) "woord" else "woorden"
        s == "search" -> if (count == 1) "word" else "words"
        else -> "wootu"
    }
}
