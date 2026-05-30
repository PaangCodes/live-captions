package dev.rongpi.livecaptions.translation

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.Before
import org.junit.After
import kotlin.system.measureNanoTime
import org.mockito.Mockito.*
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

class TranslationManagerBenchmarkTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun benchmarkBlankTranslation() = runTest(testDispatcher) {
        // Create an uninitialized manager to test isolated logic
        val manager = TranslationManager(TranslateLanguage.ENGLISH, TranslateLanguage.SPANISH)

        val mockTranslator = mock(Translator::class.java)
        `when`(mockTranslator.translate(anyString())).thenAnswer { invocation ->
            val text = invocation.getArgument<String>(0)
            // Simulate ML Kit overhead and delay
            Thread.sleep(10)
            Tasks.forResult(text)
        }

        // We inject translator through reflection for testing purposes
        // since it's private and initialized internally
        val translatorField = TranslationManager::class.java.getDeclaredField("translator")
        translatorField.isAccessible = true
        translatorField.set(manager, mockTranslator)

        val stateField = TranslationManager::class.java.getDeclaredField("_state")
        stateField.isAccessible = true
        val _state = stateField.get(manager) as kotlinx.coroutines.flow.MutableStateFlow<TranslationState>
        _state.value = TranslationState.Ready

        val iterations = 100
        val blankText = "   "
        val stream = MutableSharedFlow<String>()

        manager.translate(stream)

        var totalTime = 0L

        // Warmup
        stream.emit(blankText)
        delay(50)

        totalTime = measureNanoTime {
            for (i in 1..iterations) {
                stream.emit(blankText)
                // Small delay to let conflate and distinctUntilChanged process
                delay(20)
            }
        }

        println("================ PERF RESULTS ================")
        println("Blank text processing for $iterations iterations: ${totalTime / 1_000_000} ms")
        println("==============================================")

        manager.close()
    }
}
