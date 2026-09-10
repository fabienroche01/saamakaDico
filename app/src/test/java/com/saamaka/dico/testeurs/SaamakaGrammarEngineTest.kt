package com.saamaka.dico.testeurs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SaamakaGrammarEngineTest {
    private val resolver = FrenchFallbackResolver(
        listOf(
            candidate("malade", "siki"),
            candidate("seul", "wanwan"),
            candidate("vouloir", "kɛ́"),
            candidate("devoir", "da"),
            candidate("aimer", "lobi"),
            candidate("manger", "njã", "O"),
            candidate("dormir", "duumí"),
            candidate("voir", "lúku"),
            candidate("aider", "heepi"),
            candidate("marcher", "wáka"),
            candidate("écrire", "sikífi"),
            candidate("aller", "gó"),
            candidate("venir", "kumísu"),
            candidate("travailler", "wóoko"),
            candidate("boire", "bebe"),
            candidate("parler", "táki"),
            candidate("savoir", "sá"),
            candidate("dire", "táki"),
            candidate("croire", "bili"),
            candidate("penser", "fíki"),
            candidate("donner", "da"),
            candidate("regarder", "lúku"),
            candidate("acheter", "bái"),
            candidate("appeler", "káai"),
            candidate("pleurer", "bëë"),
            candidate("laver", "wási"),
            candidate("père", "tatá"),
            candidate("mère", "mamá"),
            candidate("homme", "kɔni"),
            candidate("maison", "wósu"),
            candidate("village", "kónde"),
            candidate("livre", "búku"),
            candidate("téléphone", "fúnu"),
            candidate("chien", "dágu"),
            candidate("voiture", "otó"),
            candidate("bicyclette", "baisígi"),
            candidate("blanc", "wéti"),
            candidate("grand", "gãã"),
            candidate("nouveau", "njunjún"),
            candidate("vert", "guúun")
        )
    )
    private val engine = SaamakaGrammarEngine(resolver::resolve)

    @Test
    fun statePredicatesUseZeroCopulaAndTamInStrictOrder() {
        assertEquals("mi siki", engine.translate("je suis malade")?.translation)
        assertEquals("mi bi siki", engine.translate("j'étais malade")?.translation)
        assertEquals("mi o siki", engine.translate("je serai malade")?.translation)
        assertEquals("mi á siki", engine.translate("je ne suis pas malade")?.translation)
        assertEquals("mi á bi siki", engine.translate("je n'étais pas malade")?.translation)
    }

    @Test
    fun locativePredicatesRequireVerbalCopulaDe() {
        assertEquals("a dɛ a wósu", engine.translate("il est à la maison")?.translation)
        assertEquals("a bi dɛ a wósu", engine.translate("il était à la maison")?.translation)
        assertEquals("a o dɛ a wósu", engine.translate("il sera à la maison")?.translation)
        assertEquals("a á dɛ a wósu", engine.translate("il n'est pas à la maison")?.translation)
    }

    @Test
    fun nominalEquationUsesDaOnlyInUnmarkedPresent() {
        assertEquals("mi da wán kɔni", engine.translate("je suis un homme")?.translation)
        assertEquals("a da mi tatá", engine.translate("il est mon père")?.translation)
        assertEquals("a bi dɛ mi tatá", engine.translate("il était mon père")?.translation)
        assertEquals("a o dɛ mi tatá", engine.translate("il sera mon père")?.translation)
        assertEquals("a á dɛ mi tatá", engine.translate("il n'est pas mon père")?.translation)
    }

    @Test
    fun presenterUsesDaButSwitchesToDeWithTamOrNegation() {
        assertEquals("da mi tatá", engine.translate("c'est mon père")?.translation)
        assertEquals("a bi dɛ mi tatá", engine.translate("c'était mon père")?.translation)
        assertEquals("á dɛ mi tatá", engine.translate("ce n'est pas mon père")?.translation)
    }

    @Test
    fun tamSeparatesStateDynamicFuturePotentialAndProgressive() {
        assertEquals("mi lobi", engine.translate("j'aime")?.translation)
        assertEquals("mi bi lobi", engine.translate("j'aimais")?.translation)
        assertEquals("mi ta njã", engine.translate("je mange")?.translation)
        assertEquals("mi bi ta njã", engine.translate("je mangeais")?.translation)
        assertEquals("mi o njã", engine.translate("je mangerai")?.translation)
        assertEquals("mi sa njã", engine.translate("je peux manger")?.translation)
        assertEquals("mi á sa njã", engine.translate("je ne peux pas manger")?.translation)
        assertEquals("mi ta sikífi", engine.translate("je suis en train d'écrire")?.translation)
    }

    @Test
    fun negationPrecedesAllOtherTamMarkers() {
        assertEquals("mi á lúku ɛn", engine.translate("je ne la vois pas")?.translation)
        assertEquals("mi á ta njã", engine.translate("je ne mange pas")?.translation)
        assertEquals("mi á bi ta njã", engine.translate("je ne mangeais pas")?.translation)
        assertEquals("mi á o njã", engine.translate("je ne mangerai pas")?.translation)
    }

    @Test
    fun weakObjectPronounsRespectSyntacticPersonAndNoGender() {
        assertEquals("a lúku mi", engine.translate("elle me voit")?.translation)
        assertEquals("mi lúku ɛn", engine.translate("je la vois")?.translation)
        assertEquals("mi lúku ɛn", engine.translate("je le vois")?.translation)
        assertEquals("mi lúku i", engine.translate("je te vois")?.translation)
        assertEquals("mi lúku u", engine.translate("je nous vois")?.translation)
        assertEquals("mi lúku unu", engine.translate("je vous vois")?.translation)
        assertEquals("mi lúku de", engine.translate("je les vois")?.translation)
    }

    @Test
    fun possessivesDependOnPossessorAndCanMarkPluralPossessedNoun() {
        assertEquals("mi lúku i mamá", engine.translate("je vois ta mère")?.translation)
        assertEquals("mi lúku ɛn mamá", engine.translate("je vois sa mère")?.translation)
        assertEquals("mi da i tatá", engine.translate("je suis ton père")?.translation)
        assertEquals("mi da di u tatá", engine.translate("je suis notre père")?.translation)
        assertEquals("mi ta lúku mi dée búku", engine.translate("je regarde mes livres")?.translation)
    }

    @Test
    fun nominalGroupHandlesArticlesPluralQuantitiesAdjectivesAndDemonstratives() {
        assertEquals("mi ta lúku dí fúnu", engine.translate("je regarde le téléphone")?.translation)
        assertEquals("mi ta lúku wán gãã kónde", engine.translate("je regarde un grand village")?.translation)
        assertEquals("mi ta lúku dée tuu wéti dágu akí", engine.translate("je regarde ces deux chiens blancs-ci")?.translation)
        assertEquals("mi ta lúku búku", engine.translate("je regarde des livres")?.translation)
    }

    @Test
    fun prepositionsKeepObjectSeriesAndPurposeCanUseStrongSeries() {
        assertEquals("mi ta wóoko ku mi tatá", engine.translate("je travaille avec mon père")?.translation)
        assertEquals("mi ta wóoko ku de", engine.translate("je travaille avec eux")?.translation)
        assertEquals("mi ta njã fu mií", engine.translate("je mange pour moi")?.translation)
        assertEquals("mi ta njã fu hɛ̃́", engine.translate("je mange pour elle")?.translation)
    }

    @Test
    fun reflexiveAndReciprocalConstructionsUseAttestedStructures() {
        assertEquals("mi ta wási mi sinkii", engine.translate("je me lave")?.translation)
        assertEquals("a ta lúku ɛn seéi", engine.translate("il se regarde")?.translation)
        assertEquals("de ta lúku di ún ku di ún", engine.translate("ils se voient")?.translation)
    }

    @Test
    fun doubleObjectPlacesBeneficiaryBeforeThing() {
        assertEquals("mi ta da mi mamá dí búku", engine.translate("je donne le livre à ma mère")?.translation)
    }

    @Test
    fun peripheralComplementsEndWithLocationThenTime() {
        assertEquals("mi o gó a dí kónde amánu", engine.translate("je vais au village demain")?.translation)
        assertEquals("mi o gó a dí kónde amánu", engine.translate("demain je vais au village")?.translation)
        assertEquals("mi ta wóoko ku mi tatá", engine.translate("je travaille avec mon père")?.translation)
    }

    @Test
    fun yesNoAndOpenQuestionsNeverInvertSaamakaSubjectVerbOrder() {
        assertEquals("i ta njã ?", engine.translate("tu manges ?")?.translation)
        assertEquals("i ta njã ?", engine.translate("est-ce que tu manges ?")?.translation)
        assertEquals("Andí i ta sikífi ?", engine.translate("qu'écris-tu ?")?.translation)
        assertEquals("Ún kamía i ta gó ?", engine.translate("où vas-tu ?")?.translation)
        assertEquals("Fa andí i ta bëë ?", engine.translate("pourquoi pleures-tu ?")?.translation)
        assertEquals("Unfá i dɛ ?", engine.translate("comment vas-tu ?")?.translation)
        assertEquals("Unte a o gó ?", engine.translate("quand ira-t-il ?")?.translation)
        assertEquals("Ambé dɛ aálá ?", engine.translate("qui est là ?")?.translation)
        assertEquals("Andí da dí sã akí ?", engine.translate("qu'est-ce que c'est ?")?.translation)
    }

    @Test
    fun factualAndVolitiveComplementsUseDifferentComplementizers() {
        assertEquals("mi sá táa a ta kumísu", engine.translate("je sais qu'il vient")?.translation)
        assertEquals("mi kɛ́ fu i kumísu", engine.translate("je veux que tu viennes")?.translation)
    }

    @Test
    fun temporalCausalAndConditionalSubordinatesKeepTheirOwnClauseOrder() {
        assertEquals("Te mi ta njã, mi á ta táki", engine.translate("quand je mange, je ne parle pas")?.translation)
        assertEquals("mi á ta wáka bika a siki", engine.translate("je ne marche pas parce qu'il est malade")?.translation)
        assertEquals("Ee i ta kumísu, mi o siki", engine.translate("si tu viens, je serai malade")?.translation)
    }

    @Test
    fun relativeConnectorIsInvariantForSubjectAndObject() {
        assertEquals("dí kɔni di ta wóoko akí", engine.translate("l'homme qui travaille ici")?.translation)
        assertEquals("dí búku di mi ta bái", engine.translate("le livre que j'achète")?.translation)
    }

    @Test
    fun unknownOrUnsafeLexemeStillDoesNotInventSaamaka() {
        assertNull(engine.translate("je veux téléporter"))
        assertNull(engine.translate("on veut dormir"))
    }

    private fun candidate(french: String, saamaka: String, validation: String = "O") =
        FrenchTranslationCandidate(french, saamaka, validation)
}
