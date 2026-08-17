package com.saamaka.dico.testeurs.model

data class DictionaryEntry(
    val id: Int,
    val french: String,
    val english: String = "",
    val dutch: String = "",
    val saamaka: String,
    val categorie: String = "",
    val valide: String = ""
)