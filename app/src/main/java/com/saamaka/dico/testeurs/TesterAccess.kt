package com.saamaka.dico.testeurs

internal object TesterAccess {
    const val FORCE_TESTER_MODE_FOR_BETA = false
    const val REQUIRE_TESTER_ACCESS_CODE = true
    private const val ACCESS_CODE = "3333"

    fun isValid(code: String): Boolean = code.trim() == ACCESS_CODE
}
