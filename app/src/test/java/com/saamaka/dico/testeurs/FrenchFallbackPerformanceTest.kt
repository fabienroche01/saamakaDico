package com.saamaka.dico.testeurs

import kotlin.system.measureNanoTime
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FrenchFallbackPerformanceTest {
    @Test
    fun indexedResolverStaysFastWith9812EntriesAndSuccessiveRequests() {
        val candidates = List(9_812) { index ->
            FrenchTranslationCandidate(
                french = "mot${index.toString().padStart(5, '0')}",
                saamaka = "local$index",
                validationStatus = if (index % 7 == 0) "O" else ""
            )
        }

        lateinit var resolver: FrenchFallbackResolver
        val indexNanos = measureNanoTime {
            resolver = FrenchFallbackResolver(candidates)
        }
        val successiveNanos = measureNanoTime {
            repeat(20) {
                assertNotNull(resolver.resolve("mot0000x"))
            }
        }
        val legacyFullScanNanos = measureNanoTime {
            repeat(20) {
                candidates.asSequence()
                    .map { normalizeAttestedPhraseKey(it.french) }
                    .count { legacyDistance("mot0000x", it) == 1 }
            }
        }

        val indexMillis = indexNanos / 1_000_000.0
        val successiveMillis = successiveNanos / 1_000_000.0
        val legacyMillis = legacyFullScanNanos / 1_000_000.0
        println(
            "FrenchFallbackResolver benchmark: index=${indexMillis}ms, " +
                "20 indexed requests=${successiveMillis}ms, " +
                "20 legacy full scans=${legacyMillis}ms"
        )

        assertTrue("Index construction unexpectedly slow: ${indexMillis}ms", indexMillis < 2_000)
        assertTrue("Indexed requests unexpectedly slow: ${successiveMillis}ms", successiveMillis < 1_000)
    }

    private fun legacyDistance(left: String, right: String): Int {
        if (kotlin.math.abs(left.length - right.length) > 1) return 2
        var previous = IntArray(right.length + 1) { it }
        for (i in left.indices) {
            val current = IntArray(right.length + 1)
            current[0] = i + 1
            for (j in right.indices) {
                current[j + 1] = minOf(
                    current[j] + 1,
                    previous[j + 1] + 1,
                    previous[j] + if (left[i] == right[j]) 0 else 1
                )
            }
            previous = current
        }
        return previous[right.length]
    }
}
