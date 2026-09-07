package com.saamaka.dico.testeurs

import com.saamaka.dico.testeurs.model.DictionaryEntry

internal class LocalPhraseIndex(entries: List<DictionaryEntry>) {
    private val usableEntries = entries.filter {
        it.french.isNotBlank() && it.saamaka.isNotBlank()
    }

    /**
     * Entries already loaded while preparing the local phrase index.
     * Reusing them avoids reopening SQLite and rebuilding the whole dictionary
     * when a typo fallback search is triggered.
     */
    val searchEntries: List<DictionaryEntry> = usableEntries

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

    fun exactEntry(text: String, frenchToSaamaka: Boolean): DictionaryEntry? {
        val candidates = if (frenchToSaamaka) {
            frenchIndex[normalizeAttestedPhraseKey(text)]
        } else {
            saamakaIndex[normalizeAttestedPhraseKey(text)]
        }.orEmpty()
        return candidates.sortedWith(
            compareByDescending<DictionaryEntry> {
                it.valide.trim().equals("O", ignoreCase = true)
            }.thenBy {
                it.valide.trim().equals("D", ignoreCase = true)
            }
        ).firstOrNull()
    }
}
