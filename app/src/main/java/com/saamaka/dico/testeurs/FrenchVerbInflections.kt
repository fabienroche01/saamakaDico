package com.saamaka.dico.testeurs

internal enum class FrenchVerbTense { PRESENT, PAST, FUTURE, OTHER }

/**
 * Safe, explicit French inflections used before an exact lemma lookup in the dictionary.
 * Recognition is French-only: Saamaka translation remains driven by attested dictionary data.
 */
internal object FrenchVerbInflections {
    private data class VerbForms(
        val lemma: String,
        val present: List<String>,
        val past: List<String>,
        val future: List<String>,
        val other: List<String> = emptyList()
    )

    private val verbs = listOf(
        verb("aller",
            present = "vais vas va allons allez vont",
            past = "allais allait allions alliez allaient",
            future = "irai iras ira irons irez iront"),
        verb("être",
            present = "suis es est sommes êtes sont",
            past = "étais était étions étiez étaient",
            future = "serai seras sera serons serez seront"),
        verb("avoir",
            present = "ai as a avons avez ont",
            past = "avais avait avions aviez avaient",
            future = "aurai auras aura aurons aurez auront"),
        verb("faire",
            present = "fais fait faisons faites font",
            past = "faisais faisait faisions faisiez faisaient",
            future = "ferai feras fera ferons ferez feront"),
        verb("vouloir",
            present = "veux veut voulons voulez veulent",
            past = "voulais voulait voulions vouliez voulaient",
            future = "voudrai voudras voudra voudrons voudrez voudront",
            other = "voudrais voudrait voudrions voudriez voudraient"),
        verb("pouvoir",
            present = "peux peut pouvons pouvez peuvent",
            past = "pouvais pouvait pouvions pouviez pouvaient",
            future = "pourrai pourras pourra pourrons pourrez pourront"),
        verb("devoir",
            present = "dois doit devons devez doivent",
            past = "devais devait devions deviez devaient",
            future = "devrai devras devra devrons devrez devront"),
        verb("venir",
            present = "viens vient venons venez viennent",
            past = "venais venait venions veniez venaient",
            future = "viendrai viendras viendra viendrons viendrez viendront"),
        verb("prendre",
            present = "prends prend prenons prenez prennent",
            past = "prenais prenait prenions preniez prenaient",
            future = "prendrai prendras prendra prendrons prendrez prendront"),
        verb("dormir",
            present = "dors dort dormons dormez dorment",
            past = "dormais dormait dormions dormiez dormaient",
            future = "dormirai dormiras dormira dormirons dormirez dormiront"),
        verb("manger",
            present = "mange manges mangeons mangez mangent",
            past = "mangeais mangeait mangions mangiez mangeaient",
            future = "mangerai mangeras mangera mangerons mangerez mangeront"),
        verb("aimer",
            present = "aime aimes aimons aimez aiment",
            past = "aimais aimait aimions aimiez aimaient",
            future = "aimerai aimeras aimera aimerons aimerez aimeront"),
        verb("parler",
            present = "parle parles parlons parlez parlent",
            past = "parlais parlait parlions parliez parlaient",
            future = "parlerai parleras parlera parlerons parlerez parleront"),
        verb("dire",
            present = "dis dit disons dites disent",
            past = "disais disait disions disiez disaient",
            future = "dirai diras dira dirons direz diront"),
        verb("voir",
            present = "vois voit voyons voyez voient",
            past = "voyais voyait voyions voyiez voyaient",
            future = "verrai verras verra verrons verrez verront"),
        verb("savoir",
            present = "sais sait savons savez savent",
            past = "savais savait savions saviez savaient",
            future = "saurai sauras saura saurons saurez sauront"),
        verb("donner",
            present = "donne donnes donnons donnez donnent",
            past = "donnais donnait donnions donniez donnaient",
            future = "donnerai donneras donnera donnerons donnerez donneront"),
        verb("mettre",
            present = "mets met mettons mettez mettent",
            past = "mettais mettait mettions mettiez mettaient",
            future = "mettrai mettras mettra mettrons mettrez mettront"),
        verb("partir",
            present = "pars part partons partez partent",
            past = "partais partait partions partiez partaient",
            future = "partirai partiras partira partirons partirez partiront"),
        verb("sortir",
            present = "sors sort sortons sortez sortent",
            past = "sortais sortait sortions sortiez sortaient",
            future = "sortirai sortiras sortira sortirons sortirez sortiront"),
        verb("lire",
            present = "lis lit lisons lisez lisent",
            past = "lisais lisait lisions lisiez lisaient",
            future = "lirai liras lira lirons lirez liront"),
        verb("écrire",
            present = "écris écrit écrivons écrivez écrivent",
            past = "écrivais écrivait écrivions écriviez écrivaient",
            future = "écrirai écriras écrira écrirons écrirez écriront"),
        verb("boire",
            present = "bois boit buvons buvez boivent",
            past = "buvais buvait buvions buviez buvaient",
            future = "boirai boiras boira boirons boirez boiront"),
        regularEr("travailler", "travaill"),
        regularEr("marcher", "march"),
        regularEr("regarder", "regard"),
        regularEr("écouter", "écout"),
        verb("comprendre",
            present = "comprends comprend comprenons comprenez comprennent",
            past = "comprenais comprenait comprenions compreniez comprenaient",
            future = "comprendrai comprendras comprendra comprendrons comprendrez comprendront"),
        verb("apprendre",
            present = "apprends apprend apprenons apprenez apprennent",
            past = "apprenais apprenait apprenions appreniez apprenaient",
            future = "apprendrai apprendras apprendra apprendrons apprendrez apprendront"),
        regularEr("chercher", "cherch"),
        regularEr("trouver", "trouv"),
        regularEr("demander", "demand"),
        verb("répondre",
            present = "réponds répond répondons répondez répondent",
            past = "répondais répondait répondions répondiez répondaient",
            future = "répondrai répondras répondra répondrons répondrez répondront"),
        verb("vivre",
            present = "vis vit vivons vivez vivent",
            past = "vivais vivait vivions viviez vivaient",
            future = "vivrai vivras vivra vivrons vivrez vivront"),
        regularEr("rester", "rest"),
        regularEr("arriver", "arriv"),

        // Additional high-frequency verbs.
        verb("finir",
            present = "finis finit finissons finissez finissent",
            past = "finissais finissait finissions finissiez finissaient",
            future = "finirai finiras finira finirons finirez finiront"),
        regularEr("commencer", "commenc", nousPresent = "commençons", nousPast = "commencions"),
        regularEr("porter", "port"),
        verb("acheter",
            present = "achète achètes achetons achetez achètent",
            past = "achetais achetait achetions achetiez achetaient",
            future = "achèterai achèteras achètera achèterons achèterez achèteront"),
        regularEr("payer", "pay", presentOverride = "paie paies paie payons payez paient", futureOverride = "paierai paieras paiera paierons paierez paieront"),
        regularEr("jouer", "jou"),
        verb("ouvrir",
            present = "ouvre ouvres ouvrons ouvrez ouvrent",
            past = "ouvrais ouvrait ouvrions ouvriez ouvraient",
            future = "ouvrirai ouvriras ouvrira ouvrirons ouvrirez ouvriront"),
        regularEr("fermer", "ferm"),
        verb("attendre",
            present = "attends attend attendons attendez attendent",
            past = "attendais attendait attendions attendiez attendaient",
            future = "attendrai attendras attendra attendrons attendrez attendront"),
        verb("entendre",
            present = "entends entend entendons entendez entendent",
            past = "entendais entendait entendions entendiez entendaient",
            future = "entendrai entendras entendra entendrons entendrez entendront"),
        verb("sentir",
            present = "sens sent sentons sentez sentent",
            past = "sentais sentait sentions sentiez sentaient",
            future = "sentirai sentiras sentira sentirons sentirez sentiront"),
        verb("connaître",
            present = "connais connaît connaissons connaissez connaissent",
            past = "connaissais connaissait connaissions connaissiez connaissaient",
            future = "connaîtrai connaîtras connaîtra connaîtrons connaîtrez connaîtront"),
        verb("croire",
            present = "crois croit croyons croyez croient",
            past = "croyais croyait croyions croyiez croyaient",
            future = "croirai croiras croira croirons croirez croiront"),
        regularEr("penser", "pens"),
        regularEr("entrer", "entr"),
        regularEr("monter", "mont"),
        verb("descendre",
            present = "descends descend descendons descendez descendent",
            past = "descendais descendait descendions descendiez descendaient",
            future = "descendrai descendras descendra descendrons descendrez descendront"),
        regularEr("appeler", "appel", presentOverride = "appelle appelles appelle appelons appelez appellent", futureOverride = "appellerai appelleras appellera appellerons appellerez appelleront"),
        regularEr("utiliser", "utilis"),
        regularEr("aider", "aid"),
        regularEr("laisser", "laiss"),
        regularEr("passer", "pass"),
        verb("tenir",
            present = "tiens tient tenons tenez tiennent",
            past = "tenais tenait tenions teniez tenaient",
            future = "tiendrai tiendras tiendra tiendrons tiendrez tiendront"),
        verb("suivre",
            present = "suis suit suivons suivez suivent",
            past = "suivais suivait suivions suiviez suivaient",
            future = "suivrai suivras suivra suivrons suivrez suivront"),
        verb("recevoir",
            present = "reçois reçoit recevons recevez reçoivent",
            past = "recevais recevait recevions receviez recevaient",
            future = "recevrai recevras recevra recevrons recevrez recevront"),
        regularEr("envoyer", "envoy", presentOverride = "envoie envoies envoie envoyons envoyez envoient", futureOverride = "enverrai enverras enverra enverrons enverrez enverront"),
        verb("perdre",
            present = "perds perd perdons perdez perdent",
            past = "perdais perdait perdions perdiez perdaient",
            future = "perdrai perdras perdra perdrons perdrez perdront"),
        regularEr("gagner", "gagn"),
        regularEr("oublier", "oubli"),
        verb("choisir",
            present = "choisis choisit choisissons choisissez choisissent",
            past = "choisissais choisissait choisissions choisissiez choisissaient",
            future = "choisirai choisiras choisira choisirons choisirez choisiront"),
        verb("courir",
            present = "cours court courons courez courent",
            past = "courais courait courions couriez couraient",
            future = "courrai courras courra courrons courrez courront"),
        verb("mourir",
            present = "meurs meurt mourons mourez meurent",
            past = "mourais mourait mourions mouriez mouraient",
            future = "mourrai mourras mourra mourrons mourrez mourront"),
        regularEr("tomber", "tomb"),
        regularEr("retourner", "retourn"),
        regularEr("rentrer", "rentr")
    )

    private val lemmaByForm: Map<String, String> = buildMap {
        verbs.forEach { forms ->
            put(normalizeAttestedPhraseKey(forms.lemma), forms.lemma)
            (forms.present + forms.past + forms.future + forms.other).forEach { form ->
                put(normalizeAttestedPhraseKey(form), forms.lemma)
            }
        }
    }

    private val tenseByForm: Map<String, FrenchVerbTense> = buildMap {
        verbs.forEach { forms ->
            forms.present.forEach { put(normalizeAttestedPhraseKey(it), FrenchVerbTense.PRESENT) }
            forms.past.forEach { put(normalizeAttestedPhraseKey(it), FrenchVerbTense.PAST) }
            forms.future.forEach { put(normalizeAttestedPhraseKey(it), FrenchVerbTense.FUTURE) }
            forms.other.forEach { put(normalizeAttestedPhraseKey(it), FrenchVerbTense.OTHER) }
        }
    }

    fun lemma(form: String): String? = lemmaByForm[normalizeAttestedPhraseKey(form)]

    fun tense(form: String): FrenchVerbTense? = tenseByForm[normalizeAttestedPhraseKey(form)]

    fun canComposeFromAttestedTranslation(lemma: String): Boolean =
        normalizeAttestedPhraseKey(lemma) in setOf("devoir", "aimer", "manger", "dormir")

    private fun verb(
        lemma: String,
        present: String,
        past: String,
        future: String,
        other: String = ""
    ): VerbForms = VerbForms(
        lemma = lemma,
        present = forms(present),
        past = forms(past),
        future = forms(future),
        other = forms(other)
    )

    private fun regularEr(
        lemma: String,
        stem: String,
        presentOverride: String? = null,
        futureOverride: String? = null,
        nousPresent: String? = null,
        nousPast: String? = null
    ): VerbForms {
        val present = presentOverride ?: listOf(
            "${stem}e", "${stem}es", "${stem}e",
            nousPresent ?: "${stem}ons",
            "${stem}ez", "${stem}ent"
        ).joinToString(" ")
        val past = listOf(
            "${stem}ais", "${stem}ait",
            nousPast ?: "${stem}ions",
            "${stem}iez", "${stem}aient"
        ).joinToString(" ")
        val futureStem = lemma
        val future = futureOverride ?: listOf(
            "${futureStem}ai", "${futureStem}as", "${futureStem}a",
            "${futureStem}ons", "${futureStem}ez", "${futureStem}ont"
        ).joinToString(" ")
        return verb(lemma, present, past, future)
    }

    private fun forms(value: String): List<String> =
        value.split(' ').map(String::trim).filter(String::isNotBlank)
}
