package com.saamaka.dico.testeurs

data class DeletionProposal(
    val entryId: Int,
    val saamaka: String,
    val french: String,
    val english: String,
    val dutch: String,
    val reason: String,
    val comment: String,
    val testerName: String,
    val createdAt: Long
)
