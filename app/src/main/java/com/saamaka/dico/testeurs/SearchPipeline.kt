package com.saamaka.dico.testeurs

import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.mapLatest

import com.saamaka.dico.testeurs.model.DictionaryEntry

internal data class SearchRequest(
    val text: String,
    val languageCode: String?,
    val sourceLanguageCode: String = languageCode ?: "fr",
    val accessLevel: AccessLevel = AccessLevel.GUEST
)
internal data class SearchOutcome(
    val text: String,
    val results: List<DictionaryEntry>,
    val exactMatch: LocalExactMatch?,
    val phraseTranslation: PhraseTranslationPipelineResult? = null
)

internal fun shouldRouteHomePhraseThroughGrammar(request: SearchRequest): Boolean =
    request.sourceLanguageCode == AppLanguage.FRENCH.code &&
        normalizedInputWordCount(request.text) >= 3

internal fun shouldPreviewShortHomePhrase(request: SearchRequest): Boolean =
    request.sourceLanguageCode == AppLanguage.FRENCH.code &&
        isRecognizedShortVerbStructure(request.text)

internal fun retainChargedHomePhrase(currentPhrase: String?, newQuery: String): String? =
    currentPhrase.takeIf { normalizedInputWordCount(newQuery) >= 3 }

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
internal fun <T> Flow<SearchRequest>.debouncedSearch(
    search: suspend (SearchRequest) -> T
): Flow<T> = debounce(250)
    .distinctUntilChanged()
    .mapLatest(search)

internal fun requireBackgroundSearch(isMainThread: Boolean) {
    check(!isMainThread) { "SQLite search must not run on the main thread" }
}
