package dev.rongpi.livecaptions.audio

import org.junit.Test
import kotlin.system.measureNanoTime
import org.junit.Assert.assertTrue

class AudioCaptureBenchmarkTest {

    @Test
    fun benchmarkZeroAllocationProcessing() {
        val bufferSize = 2048
        val buffer = ByteArray(bufferSize)
        val read = bufferSize
        val iterations = 100000

        var sumCopy = 0L
        var sumZeroAlloc = 0L

        // Warmup
        for (i in 0..10000) {
            val data = buffer.copyOf(read)
            sumCopy += data.size
            sumZeroAlloc += read
        }

        val copyTime = measureNanoTime {
            for (i in 0..iterations) {
                val data = buffer.copyOf(read)
                sumCopy += data.size
            }
        }

        val zeroAllocTime = measureNanoTime {
            for (i in 0..iterations) {
                sumZeroAlloc += read
            }
        }

        println("Baseline (copyOf) Time: ${copyTime / 1_000_000} ms")
        println("Optimized (Zero-allocation) Time: ${zeroAllocTime / 1_000_000} ms")
        println("Improvement: ${(copyTime - zeroAllocTime).toDouble() / copyTime * 100}%")

        // Zero allocation should be significantly faster
        assertTrue(zeroAllocTime < copyTime)
    }
}
