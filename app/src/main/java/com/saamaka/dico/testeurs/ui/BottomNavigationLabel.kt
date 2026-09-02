package com.saamaka.dico.testeurs.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp

@Composable
internal fun BottomNavigationLabel(text: String) {
    Text(
        text = text,
        modifier = Modifier.fillMaxWidth(),
        fontSize = 10.sp,
        lineHeight = 12.sp,
        fontWeight = FontWeight.Medium,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center
    )
}

@Preview(showBackground = true, widthDp = 360, heightDp = 360, fontScale = 1.3f)
@Composable
private fun BottomNavigationLabelsPreview() {
    val translatedLabels = listOf(
        listOf("Accueil", "Suku", "Lobi", "Apprendre", "Plus"),
        listOf("Accueil", "Recherche", "Favoris", "Apprendre", "Plus"),
        listOf("Accueil", "Search", "Favorites", "Apprendre", "Plus"),
        listOf("Accueil", "Zoeken", "Favorieten", "Apprendre", "Plus")
    )
    Column {
        translatedLabels.forEach { labels ->
            NavigationBar {
                labels.forEachIndexed { index, label ->
                    NavigationBarItem(
                        modifier = Modifier.weight(1f),
                        selected = index == 0,
                        onClick = {},
                        icon = { Icon(Icons.Default.Circle, contentDescription = null) },
                        label = { BottomNavigationLabel(label) }
                    )
                }
            }
        }
    }
}
