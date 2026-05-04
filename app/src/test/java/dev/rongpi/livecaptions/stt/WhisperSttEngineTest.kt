package dev.rongpi.livecaptions.stt

import android.content.Context
import android.util.Log
import dev.rongpi.livecaptions.stt.whisper.WhisperSttEngine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.MockedStatic
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockStatic
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class WhisperSttEngineTest {

    private lateinit var sttEngine: WhisperSttEngine
    private lateinit var testScope: TestScope
    private lateinit var fakeDownloader: FakeModelDownloader
    private lateinit var mockContext: Context
    private lateinit var mockConfig: SttConfig
    private lateinit var tempDir: File
    private lateinit var logMock: MockedStatic<Log>

    @Before
    fun setup() {
        val testDispatcher = StandardTestDispatcher()
        testScope = TestScope(testDispatcher)
        fakeDownloader = FakeModelDownloader()
        mockContext = mock(Context::class.java)

        tempDir = File.createTempFile("whisper_test", "dir")
        tempDir.delete()
        tempDir.mkdir()
        tempDir.deleteOnExit()

        `when`(mockContext.filesDir).thenReturn(tempDir)

        mockConfig = SttConfig(context = mockContext, modelPath = "test", sampleRate = 16000)

        sttEngine = WhisperSttEngine(coroutineScope = testScope, modelDownloader = fakeDownloader)

        logMock = mockStatic(Log::class.java)
        logMock.`when`<Int> { Log.e(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any(Throwable::class.java)) }
            .thenReturn(0)
    }

    @After
    fun teardown() {
        logMock.close()
    }

    @Test
    fun `initial state is Uninitialized`() {
        assertEquals(SttState.Uninitialized, sttEngine.state.value)
    }

    @Test
    fun `initialize logic runs and transitions state to Ready when model does not exist`() = testScope.runTest {
        sttEngine.initialize(mockConfig)

        advanceUntilIdle()

        assertEquals(SttState.Ready, sttEngine.state.value)
    }

    @Test
    fun `initialize transitions state to Ready immediately when model exists`() = testScope.runTest {
        val modelFile = File(tempDir, "ggml-tiny.en.bin")
        modelFile.createNewFile()

        sttEngine.initialize(mockConfig)

        advanceUntilIdle()

        assertEquals(SttState.Ready, sttEngine.state.value)
    }

    @Test
    fun `initialize transitions state to Error when download fails`() = testScope.runTest {
        fakeDownloader.shouldThrow = true

        sttEngine.initialize(mockConfig)

        advanceUntilIdle()

        assertTrue(sttEngine.state.value is SttState.Error)
        assertEquals("Failed to initialize Whisper model", (sttEngine.state.value as SttState.Error).message)
    }

    @Test
    fun `start transitions state to Listening`() = testScope.runTest {
        sttEngine.initialize(mockConfig)
        advanceUntilIdle()

        sttEngine.start()
        assertEquals(SttState.Listening, sttEngine.state.value)
    }

    @Test
    fun `stop transitions state to Ready`() = testScope.runTest {
        sttEngine.initialize(mockConfig)
        advanceUntilIdle()
        sttEngine.start()
        sttEngine.stop()

        assertEquals(SttState.Ready, sttEngine.state.value)
    }

    @Test
    fun `processAudio emits partial result when Listening`() = testScope.runTest {
        sttEngine.initialize(mockConfig)
        advanceUntilIdle()
        sttEngine.start()

        val results = mutableListOf<String>()
        val job = launch(UnconfinedTestDispatcher()) {
            sttEngine.partialResults.take(1).toList(results)
        }

        val dummyData = ByteArray(10)
        sttEngine.processAudio(dummyData)

        job.join()
        assertEquals(1, results.size)
        assertTrue(results[0].startsWith("Whisper partial"))
    }
}
