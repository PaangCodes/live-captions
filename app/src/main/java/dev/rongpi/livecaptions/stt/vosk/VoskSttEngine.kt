package dev.rongpi.livecaptions.stt.vosk

import dev.rongpi.livecaptions.stt.SttConfig
import dev.rongpi.livecaptions.stt.SttEngine
import dev.rongpi.livecaptions.stt.SttState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

class VoskSttEngine : SttEngine {
    private val _state = MutableStateFlow<SttState>(SttState.Uninitialized)
    override val state: StateFlow<SttState> = _state.asStateFlow()

    private val _partialResults = MutableSharedFlow<String>(extraBufferCapacity = 10)
    override val partialResults: SharedFlow<String> = _partialResults.asSharedFlow()

    private val _finalResults = MutableStateFlow("")
    override val finalResults: StateFlow<String> = _finalResults.asStateFlow()

    override fun initialize(config: SttConfig) {
        _state.value = SttState.Initializing
        // TODO: Load Vosk model using config.context and config.modelPath
        _state.value = SttState.Ready
    }

    override fun start() {
        if (_state.value is SttState.Ready || _state.value is SttState.Listening) {
            _state.value = SttState.Listening
            // TODO: Start processing loop or prepare for incoming audio
        }
    }

    override fun stop() {
        if (_state.value is SttState.Listening) {
            _state.value = SttState.Ready
            // TODO: Cleanup resources for current listening session
        }
    }

    override fun processAudio(data: ByteArray) {
        if (_state.value is SttState.Listening) {
            // TODO: Feed data to Vosk recognizer
            // Dummy logic for stub
            _partialResults.tryEmit("Vosk partial: ${data.size} bytes")
        }
    }
}
