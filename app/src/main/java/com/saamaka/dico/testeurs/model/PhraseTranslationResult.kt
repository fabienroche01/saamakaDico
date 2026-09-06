package com.saamaka.dico.testeurs.model

data class RecognizedPhraseSegment(
    val source: String,
    val translation: String,
    val detail: String? = null,
    val matchedSource: String? = null,
    val alternatives: List<String> = emptyList()
)

enum class TranslationReliability(val label: String) {
    HIGH("Élevée"),
    MEDIUM("Moyenne"),
    LOW("Faible")
}

enum class PhraseTranslationKind {
    PROPOSAL,
    VALIDATED_RULE,
    GRAMMATICAL,
    WORD_BY_WORD,
    PARTIAL
}

data class PhraseTranslationResult(
    val translation: String,
    val recognizedSegments: List<RecognizedPhraseSegment>,
    val untranslatedSegments: List<String>,
    val isComplete: Boolean,
    val reliability: TranslationReliability,
    val kind: PhraseTranslationKind = PhraseTranslationKind.PROPOSAL,
    val unresolvedHints: Map<String, List<String>> = emptyMap()
) {
    init {
        require(translation.isNotBlank()) { "A valid request must expose a visible proposal" }
        require(isComplete == untranslatedSegments.isEmpty())
    }

    fun shareableText(): String = if (reliability == TranslationReliability.HIGH) {
        translation
    } else {
        buildString {
            appendLine("Proposition approximative — à vérifier")
            appendLine(translation)
            recognizedSegments.mapNotNull { it.detail }.forEach(::appendLine)
            if (untranslatedSegments.isNotEmpty()) {
                append("Éléments à vérifier : ")
                append(untranslatedSegments.joinToString(", "))
            }
        }
    }
}
