package com.saamaka.dico.testeurs

enum class ReviewActionType {
    VALIDATED,
    CORRECTED,
    REJECTED
}

data class ReviewAction(
    val id: Long,
    val entryId: Int,

    val sourceLanguage: String,
    val targetLanguage: String,

    val frenchCurrent: String,
    val saamakaCurrent: String,

    val action: ReviewActionType,

    val frenchProposed: String?,
    val saamakaProposed: String?,

    val comment: String,

    val reviewer: String,

    val createdAt: Long,
    val categoryCurrent: String = "",
    val categoryProposed: String? = null
)
