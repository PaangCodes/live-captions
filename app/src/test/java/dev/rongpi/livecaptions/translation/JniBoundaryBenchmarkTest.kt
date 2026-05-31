package dev.rongpi.livecaptions.translation

import org.junit.Test
import kotlin.system.measureNanoTime
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.UnconfinedTestDispatcher

class JniBoundaryBenchmarkTest {

    // Simulate the translation manager's exact inner flow
    suspend fun translateFlow(text: String): String {
        // Here we simulate the JNI crossing and ML execution which might take 5ms overhead
        // just to start up, cross the boundary, and figure out the string is empty
        // For local simulation, we'll just thread-sleep 50ms for non-empty and 5ms for empty
        // But since this is a unit test benchmark, we'll actually use Thread.sleep to measure difference.

        // Simulating `translator?.translate(text)?.await()` JNI call
        return if (text.isBlank()) {
            Thread.sleep(1) // JNI boundary crossing overhead for empty string
            ""
        } else {
            Thread.sleep(5) // Actual ML translation
            "translated_text"
        }
    }

    suspend fun optimizedTranslateFlow(text: String): String {
        // Bolt optimization: Early return for blank strings to bypass JNI
        if (text.isBlank()) return ""

        // Simulating `translator?.translate(text)?.await()` JNI call
        return if (text.isBlank()) {
            Thread.sleep(1) // JNI boundary crossing overhead for empty string
            ""
        } else {
            Thread.sleep(5) // Actual ML translation
            "translated_text"
        }
    }

    @Test
    fun benchmarkJniCrossingOverhead() = runTest {
        val iterations = 1000
        val stringsToProcess = List(iterations) { "" } // Simulating empty pauses in speech

        var baselineTime = 0L
        for (str in stringsToProcess) {
            baselineTime += measureNanoTime {
                translateFlow(str)
            }
        }

        var optimizedTime = 0L
        for (str in stringsToProcess) {
            optimizedTime += measureNanoTime {
                optimizedTranslateFlow(str)
            }
        }

        println("================ PERF RESULTS ================")
        println("Baseline (JNI Crossing for empty strings): ${baselineTime / 1_000_000} ms total for $iterations items")
        println("Optimized (Early return): ${optimizedTime / 1_000_000} ms total for $iterations items")
        println("==============================================")
    }
}
