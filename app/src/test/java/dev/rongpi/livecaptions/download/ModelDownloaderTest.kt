package dev.rongpi.livecaptions.download

import android.content.Context
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import okio.Buffer

@OptIn(ExperimentalCoroutinesApi::class)
class ModelDownloaderTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var downloader: ModelDownloader
    private lateinit var mockContext: Context
    private lateinit var tempDir: File

    @Before
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        downloader = ModelDownloader()

        mockContext = mock(Context::class.java)

        tempDir = File.createTempFile("downloader_test", "dir")
        tempDir.delete()
        tempDir.mkdir()
        tempDir.deleteOnExit()

        `when`(mockContext.filesDir).thenReturn(tempDir)
    }

    @After
    fun teardown() {
        mockWebServer.shutdown()
        tempDir.deleteRecursively()
    }

    @Test
    fun `downloadFile enforces HTTPS`() = runTest {
        val url = "http://example.com/model.bin"
        try {
            downloader.downloadFile(mockContext, url, "model.bin").toList()
            assert(false) { "Should throw SecurityException" }
        } catch (e: SecurityException) {
            assertTrue(e.message?.contains("Insecure HTTP connections") == true)
        }
    }

    @Test
    fun `downloadFile prevents bypass of HTTPS check`() = runTest {
        val url = "http://evil.com/model.bin?localhost"
        try {
            downloader.downloadFile(mockContext, url, "model.bin").toList()
            assert(false) { "Should throw SecurityException" }
        } catch (e: SecurityException) {
            assertTrue(e.message?.contains("Insecure HTTP connections") == true)
        }
    }

    @Test
    fun `downloadFile prevents path traversal`() = runTest {
        val url = "https://example.com/model.bin"
        try {
            downloader.downloadFile(mockContext, url, "../model.bin").toList()
            assert(false) { "Should throw SecurityException" }
        } catch (e: SecurityException) {
            assertTrue(e.message?.contains("path traversal") == true)
        }
    }

    @Test
    fun `downloadAndExtractZip enforces HTTPS`() = runTest {
        val url = "http://example.com/model.zip"
        try {
            downloader.downloadAndExtractZip(mockContext, url, "model_dir").toList()
            assert(false) { "Should throw SecurityException" }
        } catch (e: SecurityException) {
            assertTrue(e.message?.contains("Insecure HTTP connections") == true)
        }
    }

    @Test
    fun `downloadAndExtractZip prevents bypass of HTTPS check`() = runTest {
        val url = "http://evil.com/model.zip?127.0.0.1"
        try {
            downloader.downloadAndExtractZip(mockContext, url, "model_dir").toList()
            assert(false) { "Should throw SecurityException" }
        } catch (e: SecurityException) {
            assertTrue(e.message?.contains("Insecure HTTP connections") == true)
        }
    }

    @Test
    fun `downloadAndExtractZip prevents path traversal`() = runTest {
        val url = "https://example.com/model.zip"
        try {
            downloader.downloadAndExtractZip(mockContext, url, "../model_dir").toList()
            assert(false) { "Should throw SecurityException" }
        } catch (e: SecurityException) {
            assertTrue(e.message?.contains("path traversal") == true)
        }
    }

    @Test
    fun `downloadFile successfully downloads file and emits progress`() = runTest {
        val testData = "Test file content".toByteArray()
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody(Buffer().write(testData))
        )

        val url = mockWebServer.url("/model.bin").toString()

        val progressList = downloader.downloadFile(mockContext, url, "model.bin").toList()

        val targetFile = File(tempDir, "model.bin")
        assertTrue(targetFile.exists())
        assertEquals("Test file content", targetFile.readText())

        assertTrue(progressList.isNotEmpty())
        assertEquals(testData.size.toLong(), progressList.last().downloadedBytes)
    }

    @Test
    fun `downloadAndExtractZip successfully downloads and extracts`() = runTest {
        // Create a dummy zip in memory
        val zipFile = File(tempDir, "dummy.zip")
        ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
            zos.putNextEntry(ZipEntry("extracted_file.txt"))
            zos.write("Extracted content".toByteArray())
            zos.closeEntry()
        }

        mockWebServer.enqueue(MockResponse()
            .setResponseCode(200)
            .setBody(Buffer().write(zipFile.readBytes()))
        )

        val url = mockWebServer.url("/model.zip").toString()

        val progressList = downloader.downloadAndExtractZip(mockContext, url, "model_dir").toList()

        val extractedFile = File(tempDir, "model_dir/extracted_file.txt")
        assertTrue(extractedFile.exists())
        assertEquals("Extracted content", extractedFile.readText())

        assertTrue(progressList.isNotEmpty())
    }
}
