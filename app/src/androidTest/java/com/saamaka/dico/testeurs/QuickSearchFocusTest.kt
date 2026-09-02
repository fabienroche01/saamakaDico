package com.saamaka.dico.testeurs

import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.saamaka.dico.testeurs.ui.QUICK_SEARCH_TEST_TAG
import com.saamaka.dico.testeurs.ui.SEARCH_RESULT_TEST_TAG
import org.junit.Rule
import org.junit.Test

class QuickSearchFocusTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun quickSearchKeepsFocusWhileResultsArePublished() {
        completeTesterSetupWhenNeeded()

        composeRule.onNodeWithTag(QUICK_SEARCH_TEST_TAG)
            .performClick()
            .performTextInput("j")

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(SEARCH_RESULT_TEST_TAG)
                .fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithTag(QUICK_SEARCH_TEST_TAG).assertIsFocused()
        composeRule.onNodeWithTag(QUICK_SEARCH_TEST_TAG)
            .performTextInput("e m’appelle")
        composeRule.onNodeWithTag(QUICK_SEARCH_TEST_TAG)
            .assertTextEquals("je m’appelle")
            .assertIsFocused()
    }

    private fun completeTesterSetupWhenNeeded() {
        if (composeRule.onAllNodesWithTag(QUICK_SEARCH_TEST_TAG)
                .fetchSemanticsNodes().isNotEmpty()
        ) {
            return
        }

        composeRule.onNodeWithText("Nom du testeur / locuteur")
            .performTextInput("Test focus")
        composeRule.onNodeWithText("Continuer").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasTestTag(QUICK_SEARCH_TEST_TAG))
                .fetchSemanticsNodes().isNotEmpty()
        }
    }
}
