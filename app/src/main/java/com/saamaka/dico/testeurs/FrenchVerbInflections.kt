package com.saamaka.dico.testeurs

/**
 * Safe, explicit French inflections used before an exact lemma lookup in the dictionary.
 * Adding a verb only requires adding its attested forms here; translation remains DB-driven.
 */
internal object FrenchVerbInflections {
    private val lemmaByForm = buildMap {
        add("aller", "vais", "vas", "va", "allons", "allez", "vont", "allais", "allait", "allaient", "irai", "iras", "ira", "irons", "irez", "iront")
        add("être", "suis", "es", "est", "sommes", "êtes", "sont", "étais", "était", "étions", "étiez", "étaient", "serai", "seras", "sera", "serons", "serez", "seront")
        add("avoir", "ai", "as", "avons", "avez", "ont", "avais", "avait", "avions", "aviez", "avaient", "aurai", "auras", "aura", "aurons", "aurez", "auront")
        add("faire", "fais", "fait", "faisons", "faites", "font", "faisais", "faisait", "faisaient", "ferai", "feras", "fera", "ferons", "ferez", "feront")
        add("vouloir", "veux", "veut", "voulons", "voulez", "veulent", "voulais", "voulait", "voulaient", "voudrais", "voudrait")
        add("pouvoir", "peux", "peut", "pouvons", "pouvez", "peuvent", "pouvais", "pouvait", "pouvaient", "pourrai", "pourras", "pourra", "pourront")
        add("devoir", "dois", "doit", "devons", "devez", "doivent", "devais", "devait", "devaient", "devrai", "devras", "devra", "devrons", "devrez", "devront")
        add("venir", "viens", "vient", "venons", "venez", "viennent", "venais", "venait", "venaient", "viendrai", "viendras", "viendra", "viendront")
        add("prendre", "prends", "prend", "prenons", "prenez", "prennent", "prenais", "prenait", "prenaient", "prendrai", "prendras", "prendra", "prendront")
        add("dormir", "dors", "dort", "dormons", "dormez", "dorment", "dormais", "dormait", "dormaient", "dormirai", "dormiras", "dormira", "dormiront")
        add("manger", "mange", "manges", "mangeons", "mangez", "mangent", "mangeais", "mangeait", "mangeaient", "mangerai", "mangeras", "mangera", "mangeront")
        add("aimer", "aime", "aimes", "aimons", "aimez", "aiment", "aimais", "aimait", "aimions", "aimiez", "aimaient", "aimerai", "aimeras", "aimera", "aimerons", "aimerez", "aimeront")
    }

    fun lemma(form: String): String? = lemmaByForm[normalizeAttestedPhraseKey(form)]

    fun canComposeFromAttestedTranslation(lemma: String): Boolean =
        normalizeAttestedPhraseKey(lemma) in setOf("devoir", "aimer", "manger", "dormir")

    private fun MutableMap<String, String>.add(lemma: String, vararg forms: String) {
        forms.forEach { form -> put(normalizeAttestedPhraseKey(form), lemma) }
    }
}
