package com.saamaka.dico.testeurs

internal object TesterAccess {
    const val REQUIRE_TESTER_ACCESS_CODE = false
    private const val ACCESS_CODE = "3333"

    fun isValid(code: String): Boolean = code.trim() == ACCESS_CODE
}
