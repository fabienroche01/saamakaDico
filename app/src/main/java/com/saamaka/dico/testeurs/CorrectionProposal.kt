package com.saamaka.dico.testeurs

data class CorrectionProposal(
    val id: Long,
    val entryId: Int,
    val frenchCurrent: String,
    val saamakaCurrent: String,
    val frenchProposed: String,
    val saamakaProposed: String,
    val comment: String,
    val testerName: String,
    val createdAt: Long
)
