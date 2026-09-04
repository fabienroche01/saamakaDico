package com.saamaka.dico.testeurs

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchPipelineTest {
    @Test
    fun latestTypedQueryWinsAndObsoleteSearchIsCancelled() = runTest {
        val input = MutableSharedFlow<SearchRequest>(extraBufferCapacity = 4)
        val results = mutableListOf<String>()
        val job = launch {
            input.debouncedSearch { request ->
                delay(if (request.text == "mai") 1_000 else 10)
                request.text
            }.collect { result -> results.add(result) }
        }
        runCurrent()
        input.emit(SearchRequest("m", null))
        advanceTimeBy(100)
        input.emit(SearchRequest("ma", null))
        advanceTimeBy(100)
        input.emit(SearchRequest("mai", null))
        advanceTimeBy(300)
        input.emit(SearchRequest("maison", null))
        advanceTimeBy(300)
        runCurrent()
        assertEquals(listOf("maison"), results)
        job.cancel()
    }

    @Test
    fun identicalQueriesAreNotRepeated() = runTest {
        val input = MutableSharedFlow<SearchRequest>(extraBufferCapacity = 2)
        var calls = 0
        val job = launch {
            input.debouncedSearch { ++calls }
                .collect { _ -> }
        }
        runCurrent()
        input.emit(SearchRequest("eau", "fr"))
        advanceTimeBy(300)
        input.emit(SearchRequest("eau", "fr"))
        advanceTimeBy(300)
        assertEquals(1, calls)
        job.cancel()
    }

    @Test
    fun sqliteSearchRejectsMainThread() {
        assertThrows(IllegalStateException::class.java) { requireBackgroundSearch(true) }
        requireBackgroundSearch(false)
    }

    @Test
    fun frenchHomePhrasesAreAlwaysRoutedThroughGrammar() {
        listOf("je veux", "je veux manger", "je veux dormir", "tu veux dormir").forEach { text ->
            assertEquals(
                true,
                shouldRouteHomePhraseThroughGrammar(
                    SearchRequest(text, "fr", "fr", AccessLevel.FREE_ACCOUNT)
                )
            )
        }
        assertEquals(
            false,
            shouldRouteHomePhraseThroughGrammar(
                SearchRequest("je veux dormir", "en", "en", AccessLevel.FREE_ACCOUNT)
            )
        )
    }

    @Test
    fun homePhraseUsesExistingTranslationAccessPolicy() {
        assertEquals(false, shouldRouteHomePhraseThroughGrammar(SearchRequest("mot", "fr", "fr", AccessLevel.FREE_ACCOUNT)))
        assertEquals(true, shouldRouteHomePhraseThroughGrammar(SearchRequest("je dors", "fr", "fr", AccessLevel.GUEST)))
    }

    @Test
    fun clearingAndRetypingDoesNotResetThePersistentTranslationQuota() {
        val chargedPhrase = normalizeAttestedPhraseKey("je veux dormir")

        assertEquals(chargedPhrase, retainChargedHomePhrase(chargedPhrase, "je veux dormir"))
        assertEquals(null, retainChargedHomePhrase(chargedPhrase, ""))
        assertEquals(null, retainChargedHomePhrase(chargedPhrase, "mot"))
        // Only the in-flight duplicate marker is cleared. TranslationTrialStore is never reset here,
        // so retyping the phrase requires another useTrial() on the same persistent store.
    }
}
