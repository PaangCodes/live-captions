package dev.rongpi.livecaptions.stt

import android.content.Context
import dev.rongpi.livecaptions.stt.vosk.VoskSttEngine
import dev.rongpi.livecaptions.download.ModelDownloader
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class VoskSttEngineTest {

    private lateinit var sttEngine: VoskSttEngine
    private lateinit var testScope: TestScope
    private lateinit var fakeDownloader: FakeModelDownloader
    private lateinit var mockContext: Context
    private lateinit var mockConfig: SttConfig

    @Before
    fun setup() {
        val testDispatcher = StandardTestDispatcher()
        testScope = TestScope(testDispatcher)
        fakeDownloader = FakeModelDownloader()
        mockContext = mock(Context::class.java)

        // Java IO File gets very angry if we mock the root File.
        // Let's create a real temporary directory for testing instead of mocking Context.filesDir
        val tempDir = File.createTempFile("test", "dir")
        tempDir.delete()
        tempDir.mkdir()
        tempDir.deleteOnExit()

        `when`(mockContext.filesDir).thenReturn(tempDir)

        mockConfig = SttConfig(context = mockContext, modelPath = "test", sampleRate = 16000)

        sttEngine = VoskSttEngine(coroutineScope = testScope, modelDownloader = fakeDownloader)
    }

    @Test
    fun `initial state is Uninitialized`() {
        assertEquals(SttState.Uninitialized, sttEngine.state.value)
    }

    @Test
    fun `initialize logic runs and transitions state to Ready using fake downloader`() = testScope.runTest {
        sttEngine.initialize(mockConfig)

        advanceUntilIdle()

        assertEquals(SttState.Ready, sttEngine.state.value)
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
        sttEngine.processAudio(dummyData, 0, dummyData.size)

        job.join()
        assertEquals(1, results.size)
        assertTrue(results[0].startsWith("Vosk partial"))
    }
}
