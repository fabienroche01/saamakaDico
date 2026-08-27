package com.saamaka.dico.testeurs

data class NewEntryProposal(
    val localId: String,
    val saamaka: String,
    val french: String,
    val english: String,
    val dutch: String,
    val category: String,
    val comment: String,
    val testerName: String,
    val createdAt: Long,
    val audioFileName: String?
)
