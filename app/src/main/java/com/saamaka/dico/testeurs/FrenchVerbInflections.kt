package com.saamaka.dico.testeurs

internal enum class FrenchVerbTense { PRESENT, PAST, FUTURE, OTHER }

/**
 * Safe, explicit French inflections used before an exact lemma lookup in the dictionary.
 * Adding a verb only requires adding its attested forms here; translation remains DB-driven.
 */
internal object FrenchVerbInflections {
    private val lemmaByForm = buildMap {
        add("aller", "vais", "vas", "va", "allons", "allez", "vont", "allais", "allait", "allions", "alliez", "allaient", "irai", "iras", "ira", "irons", "irez", "iront")
        add("être", "suis", "es", "est", "sommes", "êtes", "sont", "étais", "était", "étions", "étiez", "étaient", "serai", "seras", "sera", "serons", "serez", "seront")
        add("avoir", "ai", "as", "a", "avons", "avez", "ont", "avais", "avait", "avions", "aviez", "avaient", "aurai", "auras", "aura", "aurons", "aurez", "auront")
        add("faire", "fais", "fait", "faisons", "faites", "font", "faisais", "faisait", "faisions", "faisiez", "faisaient", "ferai", "feras", "fera", "ferons", "ferez", "feront")
        add("vouloir", "veux", "veut", "voulons", "voulez", "veulent", "voulais", "voulait", "voulions", "vouliez", "voulaient", "voudrai", "voudras", "voudra", "voudrons", "voudrez", "voudront", "voudrais", "voudrait")
        add("pouvoir", "peux", "peut", "pouvons", "pouvez", "peuvent", "pouvais", "pouvait", "pouvions", "pouviez", "pouvaient", "pourrai", "pourras", "pourra", "pourrons", "pourrez", "pourront")
        add("devoir", "dois", "doit", "devons", "devez", "doivent", "devais", "devait", "devions", "deviez", "devaient", "devrai", "devras", "devra", "devrons", "devrez", "devront")
        add("venir", "viens", "vient", "venons", "venez", "viennent", "venais", "venait", "venions", "veniez", "venaient", "viendrai", "viendras", "viendra", "viendrons", "viendrez", "viendront")
        add("prendre", "prends", "prend", "prenons", "prenez", "prennent", "prenais", "prenait", "prenions", "preniez", "prenaient", "prendrai", "prendras", "prendra", "prendrons", "prendrez", "prendront")
        add("dormir", "dors", "dort", "dormons", "dormez", "dorment", "dormais", "dormait", "dormions", "dormiez", "dormaient", "dormirai", "dormiras", "dormira", "dormirons", "dormirez", "dormiront")
        add("manger", "mange", "manges", "mangeons", "mangez", "mangent", "mangeais", "mangeait", "mangions", "mangiez", "mangeaient", "mangerai", "mangeras", "mangera", "mangerons", "mangerez", "mangeront")
        add("aimer", "aime", "aimes", "aimons", "aimez", "aiment", "aimais", "aimait", "aimions", "aimiez", "aimaient", "aimerai", "aimeras", "aimera", "aimerons", "aimerez", "aimeront")

        add("parler", "parle", "parles", "parlons", "parlez", "parlent", "parlais", "parlait", "parlions", "parliez", "parlaient", "parlerai", "parleras", "parlera", "parlerons", "parlerez", "parleront")
        add("dire", "dis", "dit", "disons", "dites", "disent", "disais", "disait", "disions", "disiez", "disaient", "dirai", "diras", "dira", "dirons", "direz", "diront")
        add("voir", "vois", "voit", "voyons", "voyez", "voient", "voyais", "voyait", "voyions", "voyiez", "voyaient", "verrai", "verras", "verra", "verrons", "verrez", "verront")
        add("savoir", "sais", "sait", "savons", "savez", "savent", "savais", "savait", "savions", "saviez", "savaient", "saurai", "sauras", "saura", "saurons", "saurez", "sauront")
        add("donner", "donne", "donnes", "donnons", "donnez", "donnent", "donnais", "donnait", "donnions", "donniez", "donnaient", "donnerai", "donneras", "donnera", "donnerons", "donnerez", "donneront")
        add("mettre", "mets", "met", "mettons", "mettez", "mettent", "mettais", "mettait", "mettions", "mettiez", "mettaient", "mettrai", "mettras", "mettra", "mettrons", "mettrez", "mettront")
        add("partir", "pars", "part", "partons", "partez", "partent", "partais", "partait", "partions", "partiez", "partaient", "partirai", "partiras", "partira", "partirons", "partirez", "partiront")
        add("sortir", "sors", "sort", "sortons", "sortez", "sortent", "sortais", "sortait", "sortions", "sortiez", "sortaient", "sortirai", "sortiras", "sortira", "sortirons", "sortirez", "sortiront")
        add("lire", "lis", "lit", "lisons", "lisez", "lisent", "lisais", "lisait", "lisions", "lisiez", "lisaient", "lirai", "liras", "lira", "lirons", "lirez", "liront")
        add("écrire", "écris", "écrit", "écrivons", "écrivez", "écrivent", "écrivais", "écrivait", "écrivions", "écriviez", "écrivaient", "écrirai", "écriras", "écrira", "écrirons", "écrirez", "écriront")
        add("boire", "bois", "boit", "buvons", "buvez", "boivent", "buvais", "buvait", "buvions", "buviez", "buvaient", "boirai", "boiras", "boira", "boirons", "boirez", "boiront")
        add("travailler", "travaille", "travailles", "travaillons", "travaillez", "travaillent", "travaillais", "travaillait", "travaillions", "travailliez", "travaillaient", "travaillerai", "travailleras", "travaillera", "travaillerons", "travaillerez", "travailleront")
        add("marcher", "marche", "marches", "marchons", "marchez", "marchent", "marchais", "marchait", "marchions", "marchiez", "marchaient", "marcherai", "marcheras", "marchera", "marcherons", "marcherez", "marcheront")
        add("regarder", "regarde", "regardes", "regardons", "regardez", "regardent", "regardais", "regardait", "regardions", "regardiez", "regardaient", "regarderai", "regarderas", "regardera", "regarderons", "regarderez", "regarderont")
        add("écouter", "écoute", "écoutes", "écoutons", "écoutez", "écoutent", "écoutais", "écoutait", "écoutions", "écoutiez", "écoutaient", "écouterai", "écouteras", "écoutera", "écouterons", "écouterez", "écouteront")
        add("comprendre", "comprends", "comprend", "comprenons", "comprenez", "comprennent", "comprenais", "comprenait", "comprenions", "compreniez", "comprenaient", "comprendrai", "comprendras", "comprendra", "comprendrons", "comprendrez", "comprendront")
        add("apprendre", "apprends", "apprend", "apprenons", "apprenez", "apprennent", "apprenais", "apprenait", "apprenions", "appreniez", "apprenaient", "apprendrai", "apprendras", "apprendra", "apprendrons", "apprendrez", "apprendront")
        add("chercher", "cherche", "cherches", "cherchons", "cherchez", "cherchent", "cherchais", "cherchait", "cherchions", "cherchiez", "cherchaient", "chercherai", "chercheras", "cherchera", "chercherons", "chercherez", "chercheront")
        add("trouver", "trouve", "trouves", "trouvons", "trouvez", "trouvent", "trouvais", "trouvait", "trouvions", "trouviez", "trouvaient", "trouverai", "trouveras", "trouvera", "trouverons", "trouverez", "trouveront")
        add("demander", "demande", "demandes", "demandons", "demandez", "demandent", "demandais", "demandait", "demandions", "demandiez", "demandaient", "demanderai", "demanderas", "demandera", "demanderons", "demanderez", "demanderont")
        add("répondre", "réponds", "répond", "répondons", "répondez", "répondent", "répondais", "répondait", "répondions", "répondiez", "répondaient", "répondrai", "répondras", "répondra", "répondrons", "répondrez", "répondront")
        add("vivre", "vis", "vit", "vivons", "vivez", "vivent", "vivais", "vivait", "vivions", "viviez", "vivaient", "vivrai", "vivras", "vivra", "vivrons", "vivrez", "vivront")
        add("rester", "reste", "restes", "restons", "restez", "restent", "restais", "restait", "restions", "restiez", "restaient", "resterai", "resteras", "restera", "resterons", "resterez", "resteront")
        add("arriver", "arrive", "arrives", "arrivons", "arrivez", "arrivent", "arrivais", "arrivait", "arrivions", "arriviez", "arrivaient", "arriverai", "arriveras", "arrivera", "arriverons", "arriverez", "arriveront")
    }

    fun lemma(form: String): String? = lemmaByForm[normalizeAttestedPhraseKey(form)]

    fun tense(form: String): FrenchVerbTense? {
        val normalized = normalizeAttestedPhraseKey(form)
        if (normalized !in lemmaByForm) return null
        return when (normalized) {
            in presentForms -> FrenchVerbTense.PRESENT
            in pastForms -> FrenchVerbTense.PAST
            in futureForms -> FrenchVerbTense.FUTURE
            else -> FrenchVerbTense.OTHER
        }
    }

    fun canComposeFromAttestedTranslation(lemma: String): Boolean =
        normalizeAttestedPhraseKey(lemma) in setOf("devoir", "aimer", "manger", "dormir")

    private fun MutableMap<String, String>.add(lemma: String, vararg forms: String) {
        forms.forEach { form -> put(normalizeAttestedPhraseKey(form), lemma) }
    }

    private val presentForms = normalizedSet(
        "vais", "vas", "va", "allons", "allez", "vont",
        "suis", "es", "est", "sommes", "êtes", "sont",
        "ai", "as", "a", "avons", "avez", "ont",
        "fais", "fait", "faisons", "faites", "font",
        "veux", "veut", "voulons", "voulez", "veulent",
        "peux", "peut", "pouvons", "pouvez", "peuvent",
        "dois", "doit", "devons", "devez", "doivent",
        "viens", "vient", "venons", "venez", "viennent",
        "prends", "prend", "prenons", "prenez", "prennent",
        "dors", "dort", "dormons", "dormez", "dorment",
        "mange", "manges", "mangeons", "mangez", "mangent",
        "aime", "aimes", "aimons", "aimez", "aiment",
        "parle", "parles", "parlons", "parlez", "parlent",
        "dis", "dit", "disons", "dites", "disent",
        "vois", "voit", "voyons", "voyez", "voient",
        "sais", "sait", "savons", "savez", "savent",
        "donne", "donnes", "donnons", "donnez", "donnent",
        "mets", "met", "mettons", "mettez", "mettent",
        "pars", "part", "partons", "partez", "partent",
        "sors", "sort", "sortons", "sortez", "sortent",
        "lis", "lit", "lisons", "lisez", "lisent",
        "écris", "écrit", "écrivons", "écrivez", "écrivent",
        "bois", "boit", "buvons", "buvez", "boivent",
        "travaille", "travailles", "travaillons", "travaillez", "travaillent",
        "marche", "marches", "marchons", "marchez", "marchent",
        "regarde", "regardes", "regardons", "regardez", "regardent",
        "écoute", "écoutes", "écoutons", "écoutez", "écoutent",
        "comprends", "comprend", "comprenons", "comprenez", "comprennent",
        "apprends", "apprend", "apprenons", "apprenez", "apprennent",
        "cherche", "cherches", "cherchons", "cherchez", "cherchent",
        "trouve", "trouves", "trouvons", "trouvez", "trouvent",
        "demande", "demandes", "demandons", "demandez", "demandent",
        "réponds", "répond", "répondons", "répondez", "répondent",
        "vis", "vit", "vivons", "vivez", "vivent",
        "reste", "restes", "restons", "restez", "restent",
        "arrive", "arrives", "arrivons", "arrivez", "arrivent"
    )

    private val pastForms = normalizedSet(
        "allais", "allait", "allions", "alliez", "allaient",
        "étais", "était", "étions", "étiez", "étaient",
        "avais", "avait", "avions", "aviez", "avaient",
        "faisais", "faisait", "faisions", "faisiez", "faisaient",
        "voulais", "voulait", "voulions", "vouliez", "voulaient",
        "pouvais", "pouvait", "pouvions", "pouviez", "pouvaient",
        "devais", "devait", "devions", "deviez", "devaient",
        "venais", "venait", "venions", "veniez", "venaient",
        "prenais", "prenait", "prenions", "preniez", "prenaient",
        "dormais", "dormait", "dormions", "dormiez", "dormaient",
        "mangeais", "mangeait", "mangions", "mangiez", "mangeaient",
        "aimais", "aimait", "aimions", "aimiez", "aimaient",
        "parlais", "parlait", "parlions", "parliez", "parlaient",
        "disais", "disait", "disions", "disiez", "disaient",
        "voyais", "voyait", "voyions", "voyiez", "voyaient",
        "savais", "savait", "savions", "saviez", "savaient",
        "donnais", "donnait", "donnions", "donniez", "donnaient",
        "mettais", "mettait", "mettions", "mettiez", "mettaient",
        "partais", "partait", "partions", "partiez", "partaient",
        "sortais", "sortait", "sortions", "sortiez", "sortaient",
        "lisais", "lisait", "lisions", "lisiez", "lisaient",
        "écrivais", "écrivait", "écrivions", "écriviez", "écrivaient",
        "buvais", "buvait", "buvions", "buviez", "buvaient",
        "travaillais", "travaillait", "travaillions", "travailliez", "travaillaient",
        "marchais", "marchait", "marchions", "marchiez", "marchaient",
        "regardais", "regardait", "regardions", "regardiez", "regardaient",
        "écoutais", "écoutait", "écoutions", "écoutiez", "écoutaient",
        "comprenais", "comprenait", "comprenions", "compreniez", "comprenaient",
        "apprenais", "apprenait", "apprenions", "appreniez", "apprenaient",
        "cherchais", "cherchait", "cherchions", "cherchiez", "cherchaient",
        "trouvais", "trouvait", "trouvions", "trouviez", "trouvaient",
        "demandais", "demandait", "demandions", "demandiez", "demandaient",
        "répondais", "répondait", "répondions", "répondiez", "répondaient",
        "vivais", "vivait", "vivions", "viviez", "vivaient",
        "restais", "restait", "restions", "restiez", "restaient",
        "arrivais", "arrivait", "arrivions", "arriviez", "arrivaient"
    )

    private val futureForms = normalizedSet(
        "irai", "iras", "ira", "irons", "irez", "iront",
        "serai", "seras", "sera", "serons", "serez", "seront",
        "aurai", "auras", "aura", "aurons", "aurez", "auront",
        "ferai", "feras", "fera", "ferons", "ferez", "feront",
        "voudrai", "voudras", "voudra", "voudrons", "voudrez", "voudront",
        "pourrai", "pourras", "pourra", "pourrons", "pourrez", "pourront",
        "devrai", "devras", "devra", "devrons", "devrez", "devront",
        "viendrai", "viendras", "viendra", "viendrons", "viendrez", "viendront",
        "prendrai", "prendras", "prendra", "prendrons", "prendrez", "prendront",
        "dormirai", "dormiras", "dormira", "dormirons", "dormirez", "dormiront",
        "mangerai", "mangeras", "mangera", "mangerons", "mangerez", "mangeront",
        "aimerai", "aimeras", "aimera", "aimerons", "aimerez", "aimeront",
        "parlerai", "parleras", "parlera", "parlerons", "parlerez", "parleront",
        "dirai", "diras", "dira", "dirons", "direz", "diront",
        "verrai", "verras", "verra", "verrons", "verrez", "verront",
        "saurai", "sauras", "saura", "saurons", "saurez", "sauront",
        "donnerai", "donneras", "donnera", "donnerons", "donnerez", "donneront",
        "mettrai", "mettras", "mettra", "mettrons", "mettrez", "mettront",
        "partirai", "partiras", "partira", "partirons", "partirez", "partiront",
        "sortirai", "sortiras", "sortira", "sortirons", "sortirez", "sortiront",
        "lirai", "liras", "lira", "lirons", "lirez", "liront",
        "écrirai", "écriras", "écrira", "écrirons", "écrirez", "écriront",
        "boirai", "boiras", "boira", "boirons", "boirez", "boiront",
        "travaillerai", "travailleras", "travaillera", "travaillerons", "travaillerez", "travailleront",
        "marcherai", "marcheras", "marchera", "marcherons", "marcherez", "marcheront",
        "regarderai", "regarderas", "regardera", "regarderons", "regarderez", "regarderont",
        "écouterai", "écouteras", "écoutera", "écouterons", "écouterez", "écouteront",
        "comprendrai", "comprendras", "comprendra", "comprendrons", "comprendrez", "comprendront",
        "apprendrai", "apprendras", "apprendra", "apprendrons", "apprendrez", "apprendront",
        "chercherai", "chercheras", "cherchera", "chercherons", "chercherez", "chercheront",
        "trouverai", "trouveras", "trouvera", "trouverons", "trouverez", "trouveront",
        "demanderai", "demanderas", "demandera", "demanderons", "demanderez", "demanderont",
        "répondrai", "répondras", "répondra", "répondrons", "répondrez", "répondront",
        "vivrai", "vivras", "vivra", "vivrons", "vivrez", "vivront",
        "resterai", "resteras", "restera", "resterons", "resterez", "resteront",
        "arriverai", "arriveras", "arrivera", "arriverons", "arriverez", "arriveront"
    )

    private fun normalizedSet(vararg forms: String): Set<String> =
        forms.mapTo(mutableSetOf(), ::normalizeAttestedPhraseKey)
}
