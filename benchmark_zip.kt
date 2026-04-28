import java.io.*
import java.util.zip.*
import kotlin.system.measureTimeMillis

fun main() {
    val tempDir = File("temp_benchmark")
    tempDir.mkdirs()
    val zipFile = File(tempDir, "test.zip")

    // Create a dummy zip file with 1000 small entries
    ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
        for (i in 1..1000) {
            zos.putNextEntry(ZipEntry("file_$i.txt"))
            zos.write("Hello world! This is a test file number $i".toByteArray())
            zos.closeEntry()
        }
    }

    val timeUnbuffered = measureTimeMillis {
        ZipInputStream(FileInputStream(zipFile)).use { zis ->
            var entry = zis.nextEntry
            val buffer = ByteArray(8192)
            while (entry != null) {
                while (zis.read(buffer) > 0) {}
                entry = zis.nextEntry
            }
        }
    }

    val timeBuffered = measureTimeMillis {
        ZipInputStream(BufferedInputStream(FileInputStream(zipFile))).use { zis ->
            var entry = zis.nextEntry
            val buffer = ByteArray(8192)
            while (entry != null) {
                while (zis.read(buffer) > 0) {}
                entry = zis.nextEntry
            }
        }
    }

    println("Unbuffered: $timeUnbuffered ms")
    println("Buffered: $timeBuffered ms")
}
