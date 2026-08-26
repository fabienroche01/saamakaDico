package com.saamaka.dico.testeurs

import com.saamaka.dico.testeurs.model.DictionaryEntry

internal class LocalPhraseIndex(entries: List<DictionaryEntry>) {
    private val usableEntries = entries.filter {
        it.french.isNotBlank() && it.saamaka.isNotBlank()
    }
    private val frenchIndex = usableEntries.groupBy {
        normalizeAttestedPhraseKey(it.french)
    }
    private val saamakaIndex = usableEntries.groupBy {
        normalizeAttestedPhraseKey(it.saamaka)
    }

    val frenchFallbackResolver = FrenchFallbackResolver(
        usableEntries.map {
            FrenchTranslationCandidate(it.french, it.saamaka, it.valide)
        }
    )

    fun exactAlternatives(
        text: String,
        frenchToSaamaka: Boolean
    ): List<String> {
        val candidates = if (frenchToSaamaka) {
            frenchIndex[normalizeAttestedPhraseKey(text)]
        } else {
            saamakaIndex[normalizeAttestedPhraseKey(text)]
        }.orEmpty()

        return candidates
            .sortedWith(
                compareByDescending<DictionaryEntry> {
                    it.valide.trim().equals("O", ignoreCase = true)
                }.thenBy {
                    it.valide.trim().equals("D", ignoreCase = true)
                }
            )
            .flatMap {
                val value = if (frenchToSaamaka) it.saamaka else it.french
                value.split(Regex("\\s*[,;/]\\s*"))
            }
            .map(String::trim)
            .filter(String::isNotBlank)
            .distinctBy(::normalizeAttestedPhraseKey)
    }

    fun exactTranslation(text: String, frenchToSaamaka: Boolean): String? =
        exactAlternatives(text, frenchToSaamaka).firstOrNull()
}
