fun main() {
    val buffer = ByteArray(4096) { it.toByte() }
    val iterations = 1_000_000

    // Simulate read loop
    val timeWithCopy = kotlin.system.measureNanoTime {
        var dummy = 0
        for (i in 1..iterations) {
            val read = 2048
            val data = buffer.copyOf(read)
            dummy += data.size
        }
    }

    val timeWithZeroAlloc = kotlin.system.measureNanoTime {
        var dummy = 0
        for (i in 1..iterations) {
            val read = 2048
            dummy += read
        }
    }

    println("Time with copyOf: ${timeWithCopy / 1_000_000.0} ms")
    println("Time with zero-alloc: ${timeWithZeroAlloc / 1_000_000.0} ms")
}
