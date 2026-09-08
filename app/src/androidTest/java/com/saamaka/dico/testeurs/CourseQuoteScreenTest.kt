package com.saamaka.dico.testeurs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class CourseQuoteScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun courseQuoteRendersInsideScrollableLearnScreen() {
        composeRule.setContent {
            MaterialTheme {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    CourseQuoteScreen()
                }
            }
        }

        composeRule.onNodeWithText("Demande de devis").assertIsDisplayed()
    }
}
