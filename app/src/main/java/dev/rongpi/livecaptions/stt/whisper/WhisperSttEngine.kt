package dev.rongpi.livecaptions.stt.whisper

import android.util.Log
import dev.rongpi.livecaptions.stt.SttConfig
import dev.rongpi.livecaptions.stt.SttEngine
import dev.rongpi.livecaptions.stt.SttState
import dev.rongpi.livecaptions.download.ModelDownloader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class WhisperSttEngine(
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main),
    private val modelDownloader: ModelDownloader = ModelDownloader()
) : SttEngine {
    private val _state = MutableStateFlow<SttState>(SttState.Uninitialized)
    override val state: StateFlow<SttState> = _state.asStateFlow()

    private val _partialResults = MutableSharedFlow<String>(extraBufferCapacity = 10)
    override val partialResults: SharedFlow<String> = _partialResults.asSharedFlow()

    private val _finalResults = MutableStateFlow("")
    override val finalResults: StateFlow<String> = _finalResults.asStateFlow()

    private var initJob: Job? = null

    override fun initialize(config: SttConfig) {
        _state.value = SttState.Initializing
        val modelFile = File(config.context.filesDir, "ggml-tiny.en.bin")

        initJob?.cancel()
        initJob = coroutineScope.launch {
            if (!modelFile.exists()) {
                try {
                    modelDownloader.downloadFile(
                        context = config.context,
                        url = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-tiny.en.bin",
                        targetFileName = "ggml-tiny.en.bin"
                    ).collect { progress ->
                        _state.value = SttState.Downloading(progress.downloadedBytes, progress.totalBytes)
                    }
                    _state.value = SttState.Ready
                } catch (e: Exception) {
                    e.printStackTrace()
                    _state.value = SttState.Error("Failed to initialize Whisper model")
                }
            } else {
                _state.value = SttState.Ready
            }
        }
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
            // TODO: Feed data to Whisper recognizer
            // Dummy logic for stub
            _partialResults.tryEmit("Whisper partial: ${data.size} bytes")
        }
    }
}
