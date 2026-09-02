package com.saamaka.dico.testeurs.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saamaka.dico.testeurs.model.DictionaryEntry
import com.saamaka.dico.testeurs.AppStrings
import com.saamaka.dico.testeurs.UiCopyKey
import com.saamaka.dico.testeurs.ui

private val HomeGreen = Color(0xFF0B5D3B)
private val HomeDarkGreen = Color(0xFF16372A)
private val HomeGold = Color(0xFFF0C96A)
private val HomeCream = Color(0xFFFFFBF3)
private val HomeSoftGreen = Color(0xFFDCEEE2)
private val HomeSoftGold = Color(0xFFFFEFC4)

@Composable
fun HomeScreen(
    strings: AppStrings,
    total: Int,
    query: String,
    onQueryChange: (String) -> Unit,
    wordOfDay: DictionaryEntry?,
    isWordOfDayFavorite: Boolean,
    hasWordOfDayAudio: Boolean,
    onPlayWordOfDay: () -> Unit,
    onToggleWordOfDayFavorite: () -> Unit,
    onOpenWordOfDay: () -> Unit,
    learningProgress: Float,
    onLearnClick: () -> Unit,
    categories: List<String>,
    onTranslateClick: () -> Unit,
    onCategoriesClick: () -> Unit,
    onFavoritesClick: () -> Unit,
    onHistoryClick: () -> Unit,
    isTester: Boolean,
    toVerifyToday: Int,
    onMissionClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 24.dp)
    ) {
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Saamaka Dico", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = HomeDarkGreen)
                Text(strings.ui(UiCopyKey.APP_TAGLINE), fontSize = 13.sp, color = Color(0xFF68736C))
            }
            Surface(shape = RoundedCornerShape(50), color = HomeSoftGreen) {
                Text(strings.ui(UiCopyKey.ENTRIES, total), Modifier.padding(horizontal = 10.dp, vertical = 6.dp), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = HomeGreen)
            }
        }

        Spacer(Modifier.height(18.dp))
        HomeQuickSearchField(query, onQueryChange, strings)

        Spacer(Modifier.height(18.dp))
        SectionTitle(strings.ui(UiCopyKey.WORD_OF_DAY))
        Spacer(Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = HomeGreen),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Column(Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(wordOfDay?.saamaka.orEmpty().ifBlank { "—" }, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(wordOfDay?.french.orEmpty(), fontSize = 14.sp, color = Color.White.copy(alpha = 0.82f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    IconButton(onClick = onPlayWordOfDay, enabled = hasWordOfDayAudio) {
                        Icon(Icons.Default.PlayArrow, strings.ui(UiCopyKey.LISTEN), tint = if (hasWordOfDayAudio) HomeGold else Color.White.copy(alpha = 0.35f))
                    }
                    IconButton(onClick = onToggleWordOfDayFavorite, enabled = wordOfDay != null) {
                        Icon(if (isWordOfDayFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, strings.ui(UiCopyKey.FAVORITE), tint = HomeGold)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onOpenWordOfDay,
                    enabled = wordOfDay != null,
                    colors = ButtonDefaults.buttonColors(containerColor = HomeGold, contentColor = HomeDarkGreen)
                ) { Text(strings.ui(UiCopyKey.OPEN_ENTRY), fontWeight = FontWeight.Bold) }
            }
        }

        Spacer(Modifier.height(18.dp))
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF4EFE5))) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.School, null, tint = HomeGreen)
                    Spacer(Modifier.width(9.dp))
                    Text(strings.ui(UiCopyKey.CONTINUE_LEARNING), fontWeight = FontWeight.Bold, color = HomeDarkGreen)
                }
                Spacer(Modifier.height(12.dp))
                LinearProgressIndicator(progress = { learningProgress.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth(), color = HomeGreen, trackColor = HomeSoftGreen)
                Spacer(Modifier.height(12.dp))
                Button(onClick = onLearnClick, modifier = Modifier.fillMaxWidth()) { Text(strings.ui(UiCopyKey.CONTINUE_ACTION)) }
            }
        }

        Spacer(Modifier.height(20.dp))
        SectionTitle(strings.ui(UiCopyKey.QUICK_ACCESS))
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            QuickAccess(strings.translate, Icons.Default.Translate, onTranslateClick, Modifier.weight(1f))
            QuickAccess(strings.ui(UiCopyKey.CATEGORIES), Icons.AutoMirrored.Filled.MenuBook, onCategoriesClick, Modifier.weight(1f))
            QuickAccess(strings.favorites, Icons.Default.Favorite, onFavoritesClick, Modifier.weight(1f))
            QuickAccess(strings.history, Icons.Default.History, onHistoryClick, Modifier.weight(1f))
        }

        Spacer(Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionTitle(strings.ui(UiCopyKey.DISCOVER), Modifier.weight(1f))
            Text(strings.ui(UiCopyKey.SEE_ALL_CATEGORIES), Modifier.clickable(onClick = onCategoriesClick), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = HomeGreen)
        }
        Spacer(Modifier.height(8.dp))
        categories.take(3).forEach { category ->
            Surface(
                modifier = Modifier.fillMaxWidth().padding(bottom = 7.dp).clickable(onClick = onCategoriesClick),
                shape = RoundedCornerShape(16.dp),
                color = HomeSoftGold
            ) {
                Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(category, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold, color = HomeDarkGreen)
                    Icon(Icons.Default.ChevronRight, null, tint = HomeGreen)
                }
            }
        }

        if (isTester) {
            Spacer(Modifier.height(12.dp))
            Surface(modifier = Modifier.fillMaxWidth().clickable(onClick = onMissionClick), shape = RoundedCornerShape(18.dp), color = HomeSoftGreen) {
                Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(strings.ui(UiCopyKey.VERIFY_TODAY), fontWeight = FontWeight.Bold, color = HomeDarkGreen)
                        Text(strings.ui(UiCopyKey.AVAILABLE_WORDS, toVerifyToday), fontSize = 12.sp, color = Color(0xFF52645B))
                    }
                    Text(strings.mission, fontWeight = FontWeight.Bold, color = HomeGreen)
                    Icon(Icons.Default.ChevronRight, null, tint = HomeGreen)
                }
            }
        }
    }
}

@Composable
fun HomeQuickSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    strings: AppStrings
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(QUICK_SEARCH_TEST_TAG),
        singleLine = true,
        shape = RoundedCornerShape(22.dp),
        placeholder = { Text(strings.ui(UiCopyKey.QUICK_SEARCH_HINT)) },
        leadingIcon = { Icon(Icons.Default.Search, strings.ui(UiCopyKey.SEARCH_ACTION), tint = HomeGreen) },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = HomeGreen,
            unfocusedBorderColor = Color(0xFFD6D0C5),
            focusedContainerColor = HomeCream,
            unfocusedContainerColor = HomeCream
        )
    )
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier = modifier, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = HomeDarkGreen)
}

@Composable
private fun QuickAccess(label: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.clickable(onClick = onClick), shape = RoundedCornerShape(16.dp), color = Color(0xFFF4EFE5)) {
        Column(Modifier.padding(horizontal = 4.dp, vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, modifier = Modifier.size(23.dp), tint = HomeGreen)
            Spacer(Modifier.height(6.dp))
            Text(label, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
    }
}
