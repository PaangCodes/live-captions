package dev.rongpi.livecaptions

import org.junit.Test
import kotlin.system.measureNanoTime

class TranslationManagerPerfTest {
    @Test
    fun benchmarkListVsSet() {
        val langs = (1..100).map { "lang_$it" }
        val listState = langs.toList()
        val setState = langs.toSet()

        val iterations = 100000

        var timeWithListLookup = 0L
        var timeWithSetDirectly = 0L

        // Warmup
        for (i in 1..10000) {
            langs.forEach { lang -> listState.contains(lang) }
            langs.forEach { lang -> setState.contains(lang) }
        }

        for (i in 1..iterations) {
            timeWithListLookup += measureNanoTime {
                langs.forEach { lang ->
                    listState.contains(lang)
                }
            }
            timeWithSetDirectly += measureNanoTime {
                langs.forEach { lang ->
                    setState.contains(lang)
                }
            }
        }

        println("================ PERF RESULTS ================")
        println("With List (O(N)): ${timeWithListLookup / iterations} ns per iteration")
        println("With Set (O(1)): ${timeWithSetDirectly / iterations} ns per iteration")
        println("==============================================")
    }
}
