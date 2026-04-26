package dev.rongpi.livecaptions.stt

import dev.rongpi.livecaptions.stt.vosk.VoskSttEngine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

@OptIn(ExperimentalCoroutinesApi::class)
class VoskSttEngineTest {

    private lateinit var sttEngine: VoskSttEngine

    @Before
    fun setup() {
        sttEngine = VoskSttEngine()
    }

    @Test
    fun `initial state is Uninitialized`() {
        assertEquals(SttState.Uninitialized, sttEngine.state.value)
    }

    @Test
    fun `initialize transitions state to Ready`() = runTest {
        val config = mock(SttConfig::class.java)
        sttEngine.initialize(config)
        assertEquals(SttState.Ready, sttEngine.state.value)
    }

    @Test
    fun `start transitions state to Listening`() = runTest {
        val config = mock(SttConfig::class.java)
        sttEngine.initialize(config)
        sttEngine.start()
        assertEquals(SttState.Listening, sttEngine.state.value)
    }

    @Test
    fun `stop transitions state to Ready`() = runTest {
        val config = mock(SttConfig::class.java)
        sttEngine.initialize(config)
        sttEngine.start()
        sttEngine.stop()
        assertEquals(SttState.Ready, sttEngine.state.value)
    }

    @Test
    fun `processAudio emits partial result when Listening`() = runTest(UnconfinedTestDispatcher()) {
        val config = mock(SttConfig::class.java)
        sttEngine.initialize(config)
        sttEngine.start()

        val results = mutableListOf<String>()
        val job = launch {
            sttEngine.partialResults.take(1).toList(results)
        }

        val dummyData = ByteArray(10)
        sttEngine.processAudio(dummyData)

        job.join()
        assertEquals(1, results.size)
        assertTrue(results[0].startsWith("Vosk partial"))
    }
}
