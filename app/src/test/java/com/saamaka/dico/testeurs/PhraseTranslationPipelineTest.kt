package com.saamaka.dico.testeurs

import com.saamaka.dico.testeurs.model.PhraseTranslationKind
import com.saamaka.dico.testeurs.model.PhraseTranslationResult
import com.saamaka.dico.testeurs.model.RecognizedPhraseSegment
import com.saamaka.dico.testeurs.model.TranslationReliability
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class PhraseTranslationPipelineTest {
    @Test
    fun recognizedTwoWordVerbShowsSeparateBlocksAndConsumesExactlyOnce() = runBlocking {
        var remaining = 3
        var calls = 0
        val vouloir = FrenchFallbackResolver(
            listOf(FrenchTranslationCandidate("vouloir", "kë", ""))
        )
        val grammar = SaamakaGrammarEngine(vouloir::resolve)
        val wordByWord = PhraseTranslationResult(
            translation = "mi • kë",
            recognizedSegments = listOf(
                RecognizedPhraseSegment("je", "mi"),
                RecognizedPhraseSegment("veux", "kë", matchedSource = "vouloir")
            ),
            untranslatedSegments = emptyList(),
            isComplete = true,
            reliability = TranslationReliability.MEDIUM,
            kind = PhraseTranslationKind.WORD_BY_WORD
        )
        val pipeline = PhraseTranslationPipeline(
            resolvePhrase = { text, _ -> grammar.translate(text) },
            resolveWordByWord = { _, _ -> wordByWord },
            remainingTrials = { remaining },
            consumeTrial = {
                calls++
                remaining--
                true
            }
        )

        val result = pipeline.translate("je veux", true, AccessLevel.GUEST)

        assertEquals("mi • kë", result.wordByWordTranslation?.translation)
        assertEquals(null, result.grammaticalTranslation)
        assertEquals(1, calls)
        assertEquals(2, result.remainingTrials)
    }

    @Test
    fun unrecognizedTwoWordGroupRemainsFreeDictionaryFallback() = runBlocking {
        var calls = 0
        val pipeline = PhraseTranslationPipeline(
            resolvePhrase = { _, _ -> null },
            resolveWordByWord = { _, _ -> error("word-by-word must not run for an unrecognized two-word group") },
            remainingTrials = { 3 },
            consumeTrial = { calls++; true }
        )

        val result = pipeline.translate("groupe simple", true, AccessLevel.GUEST)

        assertEquals(PhraseTranslationDisposition.FALLBACK, result.disposition)
        assertEquals(null, result.wordByWordTranslation)
        assertEquals(0, calls)
    }

    @Test
    fun supportedTwoWordActionKeepsWordByWordBesideRealGrammarResult() = runBlocking {
        val resolver = FrenchFallbackResolver(
            listOf(FrenchTranslationCandidate("dormir", "duumí", ""))
        )
        val grammar = SaamakaGrammarEngine(resolver::resolve)
        val pipeline = PhraseTranslationPipeline(
            resolvePhrase = { text, _ -> grammar.translate(text) },
            resolveWordByWord = { _, _ ->
                PhraseTranslationResult(
                    translation = "mi • duumí",
                    recognizedSegments = listOf(RecognizedPhraseSegment("dors", "duumí")),
                    untranslatedSegments = emptyList(),
                    isComplete = true,
                    reliability = TranslationReliability.MEDIUM,
                    kind = PhraseTranslationKind.WORD_BY_WORD
                )
            },
            remainingTrials = { 3 },
            consumeTrial = { true }
        )

        val result = pipeline.translate("je dors", true, AccessLevel.GUEST)

        assertEquals("mi • duumí", result.wordByWordTranslation?.translation)
        assertEquals("mi ta duumí", result.grammaticalTranslation?.translation)
    }

    @Test
    fun threeWordPhraseExposesSeparateWordAndGrammarBlocks() = runBlocking {
        val wordByWord = PhraseTranslationResult(
            translation = "mi kë Makandi",
            recognizedSegments = listOf(RecognizedPhraseSegment("je", "mi")),
            untranslatedSegments = emptyList(),
            isComplete = true,
            reliability = TranslationReliability.MEDIUM,
            kind = PhraseTranslationKind.WORD_BY_WORD
        )
        val grammar = complete("mi kë Makandi")
        val pipeline = PhraseTranslationPipeline(
            resolvePhrase = { _, _ -> grammar },
            resolveWordByWord = { _, _ -> wordByWord },
            remainingTrials = { 3 },
            consumeTrial = { true }
        )

        val result = pipeline.resolve("je veux manger", true, AccessLevel.GUEST)

        assertEquals(wordByWord, result.wordByWordTranslation)
        assertEquals(grammar, result.grammaticalTranslation)
        assertEquals(false, result.trialConsumed)
    }

    @Test
    fun oneAndTwoWordsNeverConsumeWhileThreeWordsRequireExplicitTranslate() = runBlocking {
        var consumptions = 0
        val pipeline = PhraseTranslationPipeline(
            resolvePhrase = { _, _ -> null },
            remainingTrials = { 3 - consumptions },
            consumeTrial = { consumptions++; true }
        )

        pipeline.translate("mot", true, AccessLevel.GUEST)
        pipeline.translate("groupe simple", true, AccessLevel.GUEST)
        pipeline.resolve("trois mots saisis", true, AccessLevel.GUEST)
        assertEquals(0, consumptions)

        pipeline.translate("trois mots valides", true, AccessLevel.GUEST)
        assertEquals(1, consumptions)
    }

    @Test
    fun homeAndTranslateUseTheSamePhraseDecisionForAllRequiredCases() = runBlocking {
        val resolver = FrenchFallbackResolver(
            listOf(
                FrenchTranslationCandidate("malade", "síki", ""),
                FrenchTranslationCandidate("vouloir", "kë", ""),
                FrenchTranslationCandidate("devoir", "da", ""),
                FrenchTranslationCandidate("manger", "Makandi", "O"),
                FrenchTranslationCandidate("dormir", "duumí", "")
            )
        )
        val grammar = SaamakaGrammarEngine(resolver::resolve)
        val inputs = listOf(
            "je suis malade",
            "tu es seul",
            "je vais manger",
            "tu vas dormir",
            "je veux manger",
            "tu veux dormir",
            "je suis en train de manger",
            "phrase totalement inconnue"
        )

        inputs.forEach { input ->
            fun pipeline() = PhraseTranslationPipeline(
                resolvePhrase = { text, _ -> grammar.translate(text) },
                resolveWordByWord = { text, _ ->
                    assembleWordByWordPhrase(text) { token ->
                        resolver.resolve(token)?.let {
                            RecognizedPhraseSegment(token, it.saamaka)
                        }
                    }
                },
                remainingTrials = { 3 },
                consumeTrial = { true }
            )

            val homePipeline = pipeline()
            val home = homePipeline.authorizePhraseAttempt(
                homePipeline.resolve(input, true, AccessLevel.PREMIUM),
                AccessLevel.PREMIUM
            )
            val translate = pipeline().translate(input, true, AccessLevel.PREMIUM)

            assertEquals("Disposition différente pour $input", translate.disposition, home.disposition)
            assertEquals("Traduction différente pour $input", translate.translation, home.translation)
            assertEquals("Mot à mot différent pour $input", translate.wordByWordTranslation, home.wordByWordTranslation)
            assertEquals("Grammaire différente pour $input", translate.grammaticalTranslation, home.grammaticalTranslation)
        }
    }

    @Test
    fun everyPhraseAttemptConsumesQuotaIncludingFallbackButSingleWordsStayFree() = runBlocking {
        var remaining = 2
        val pipeline = PhraseTranslationPipeline(
            resolvePhrase = { text, _ -> if (text == "je veux manger") complete("mi kë Makandi") else null },
            remainingTrials = { remaining },
            consumeTrial = {
                if (remaining <= 0) false else {
                    remaining--
                    true
                }
            }
        )

        val translated = pipeline.translate("je veux manger", true, AccessLevel.FREE_ACCOUNT)
        val unknown = pipeline.translate("phrase vraiment inconnue", true, AccessLevel.FREE_ACCOUNT)
        val twoWords = pipeline.translate("phrase inconnue", true, AccessLevel.FREE_ACCOUNT)
        val word = pipeline.translate("manger", true, AccessLevel.FREE_ACCOUNT)

        assertEquals(PhraseTranslationDisposition.TRANSLATED, translated.disposition)
        assertEquals(true, translated.trialConsumed)
        assertEquals(PhraseTranslationDisposition.FALLBACK, unknown.disposition)
        assertEquals(true, unknown.trialConsumed)
        assertEquals(false, twoWords.trialConsumed)
        assertEquals(PhraseTranslationDisposition.FALLBACK, word.disposition)
        assertEquals(false, word.trialConsumed)
        assertEquals(0, remaining)
    }

    @Test
    fun guestFourthUnknownPhraseAttemptIsBlocked() = runBlocking {
        var remaining = 3
        val pipeline = PhraseTranslationPipeline(
            resolvePhrase = { _, _ -> null },
            remainingTrials = { remaining },
            consumeTrial = {
                if (remaining <= 0) false else {
                    remaining--
                    true
                }
            }
        )

        repeat(3) {
            val attempt = pipeline.translate("phrase encore inconnue", true, AccessLevel.GUEST)
            assertEquals(PhraseTranslationDisposition.FALLBACK, attempt.disposition)
            assertEquals(true, attempt.trialConsumed)
        }
        assertEquals(
            PhraseTranslationDisposition.PREMIUM_REQUIRED,
            pipeline.translate("encore une inconnue", true, AccessLevel.GUEST).disposition
        )
    }

    @Test
    fun partialWordByWordResultIsShownAndConsumesThePhraseAttempt() = runBlocking {
        var remaining = 3
        val partial = PhraseTranslationResult(
            translation = "mi pee",
            recognizedSegments = listOf(
                RecognizedPhraseSegment("je", "mi"),
                RecognizedPhraseSegment("papa", "pee")
            ),
            untranslatedSegments = listOf("suis", "ton"),
            isComplete = false,
            reliability = TranslationReliability.LOW,
            kind = PhraseTranslationKind.PARTIAL
        )
        val pipeline = PhraseTranslationPipeline(
            resolvePhrase = { _, _ -> partial },
            remainingTrials = { remaining },
            consumeTrial = { remaining--; true }
        )

        val result = pipeline.translate("je suis ton papa", true, AccessLevel.GUEST)

        assertEquals(PhraseTranslationDisposition.TRANSLATED, result.disposition)
        assertEquals(PhraseTranslationKind.PARTIAL, result.translation?.kind)
        assertEquals("mi pee", result.translation?.translation)
        assertEquals(true, result.trialConsumed)
        assertEquals(2, remaining)
    }

    @Test
    fun homeRecompositionDoesNotConsumeTheSameAttemptTwice() = runBlocking {
        var remaining = 3
        val pipeline = PhraseTranslationPipeline(
            resolvePhrase = { _, _ -> null },
            remainingTrials = { remaining },
            consumeTrial = { remaining--; true }
        )
        val resolved = pipeline.resolve("phrase vraiment inconnue", true, AccessLevel.GUEST)

        val first = pipeline.authorizePhraseAttempt(resolved, AccessLevel.GUEST)
        val repeated = pipeline.authorizePhraseAttempt(
            resolved,
            AccessLevel.GUEST,
            alreadyConsumed = true
        )

        assertEquals(true, first.trialConsumed)
        assertEquals(false, repeated.trialConsumed)
        assertEquals(2, remaining)
    }

    @Test
    fun cancellationAfterAuthorizationCannotBypassGuestQuota() = runBlocking {
        var remaining = 3
        val observedRemaining = mutableListOf<Int>()
        val pipeline = PhraseTranslationPipeline(
            resolvePhrase = { _, _ ->
                observedRemaining += remaining
                null
            },
            remainingTrials = { remaining },
            consumeTrial = {
                if (remaining <= 0) false else {
                    remaining--
                    true
                }
            }
        )

        repeat(3) {
            assertEquals(
                PhraseTranslationDisposition.FALLBACK,
                pipeline.translate("phrase numero $it", true, AccessLevel.GUEST).disposition
            )
        }

        assertEquals(listOf(2, 1, 0), observedRemaining)
        assertEquals(0, remaining)
        assertEquals(
            PhraseTranslationDisposition.PREMIUM_REQUIRED,
            pipeline.translate("phrase numero quatre", true, AccessLevel.GUEST).disposition
        )
        assertEquals(listOf(2, 1, 0), observedRemaining)
    }

    @Test
    fun debouncedPreviewResolutionNeverConsumesATrial() = runBlocking {
        var remaining = 3
        var consumptions = 0
        val pipeline = PhraseTranslationPipeline(
            resolvePhrase = { _, _ -> null },
            remainingTrials = { remaining },
            consumeTrial = {
                consumptions++
                remaining--
                true
            }
        )

        repeat(10) {
            assertEquals(
                PhraseTranslationDisposition.FALLBACK,
                pipeline.resolve("phrase automatique $it", true, AccessLevel.GUEST).disposition
            )
        }

        assertEquals(0, consumptions)
        assertEquals(3, remaining)
    }

    @Test
    fun freeAccountRemainingValuesAreFourToZeroThenSixthAttemptIsBlocked() = runBlocking {
        var remaining = 5
        val values = mutableListOf<Int>()
        val pipeline = PhraseTranslationPipeline(
            resolvePhrase = { _, _ -> null },
            remainingTrials = { remaining },
            consumeTrial = {
                if (remaining <= 0) false else {
                    remaining--
                    values += remaining
                    true
                }
            }
        )

        repeat(5) {
            pipeline.translate("tentative phrase $it", true, AccessLevel.FREE_ACCOUNT)
        }

        assertEquals(listOf(4, 3, 2, 1, 0), values)
        assertEquals(
            PhraseTranslationDisposition.PREMIUM_REQUIRED,
            pipeline.translate("sixieme tentative bloquee", true, AccessLevel.FREE_ACCOUNT).disposition
        )
    }

    @Test
    fun clearingAndRetypingTheSamePhraseNeverResetsGuestQuota() = runBlocking {
        var remaining = 3
        val pipeline = PhraseTranslationPipeline(
            resolvePhrase = { _, _ -> null },
            remainingTrials = { remaining },
            consumeTrial = {
                if (remaining <= 0) false else {
                    remaining--
                    true
                }
            }
        )

        repeat(3) {
            assertEquals(
                PhraseTranslationDisposition.FALLBACK,
                pipeline.translate("même longue phrase", true, AccessLevel.GUEST).disposition
            )
        }
        assertEquals(
            PhraseTranslationDisposition.PREMIUM_REQUIRED,
            pipeline.translate("même longue phrase", true, AccessLevel.GUEST).disposition
        )
    }

    @Test
    fun guestGetsThreeTranslationsAndFreeAccountGetsFive() = runBlocking {
        assertEquals(3, translationTrialLimit(AccessLevel.GUEST))
        assertEquals(5, translationTrialLimit(AccessLevel.FREE_ACCOUNT))
        suspend fun verifyLimit(accessLevel: AccessLevel, limit: Int) {
            var remaining = limit
            val pipeline = PhraseTranslationPipeline(
                resolvePhrase = { _, _ -> complete("mi kë Makandi") },
                remainingTrials = { remaining },
                consumeTrial = {
                    if (remaining == 0) false else {
                        remaining--
                        true
                    }
                }
            )
            repeat(limit) {
                assertEquals(
                    PhraseTranslationDisposition.TRANSLATED,
                    pipeline.translate("je veux manger", true, accessLevel).disposition
                )
            }
            assertEquals(
                PhraseTranslationDisposition.PREMIUM_REQUIRED,
                pipeline.translate("je veux manger", true, accessLevel).disposition
            )
        }

        verifyLimit(AccessLevel.GUEST, 3)
        verifyLimit(AccessLevel.FREE_ACCOUNT, 5)
    }

    @Test
    fun premiumAndTesterNeverConsumeTranslationTrials() = runBlocking {
        listOf(AccessLevel.PREMIUM, AccessLevel.TESTER).forEach { accessLevel ->
            var consumptions = 0
            val pipeline = PhraseTranslationPipeline(
                resolvePhrase = { _, _ -> complete("mi kë Makandi") },
                remainingTrials = { Int.MAX_VALUE },
                consumeTrial = {
                    consumptions++
                    true
                }
            )
            repeat(10) {
                assertEquals(
                    PhraseTranslationDisposition.TRANSLATED,
                    pipeline.translate("je veux manger", true, accessLevel).disposition
                )
            }
            assertEquals(0, consumptions)
        }
    }

    private fun complete(translation: String) = PhraseTranslationResult(
        translation = translation,
        recognizedSegments = listOf(RecognizedPhraseSegment("source", translation)),
        untranslatedSegments = emptyList(),
        isComplete = true,
        reliability = TranslationReliability.HIGH,
        kind = PhraseTranslationKind.GRAMMATICAL
    )
}
