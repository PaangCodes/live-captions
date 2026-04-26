package dev.rongpi.livecaptions.stt

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface SttEngine {
    val state: StateFlow<SttState>
    val partialResults: SharedFlow<String>
    val finalResults: StateFlow<String>

    fun initialize(config: SttConfig)
    fun start()
    fun stop()
    fun processAudio(data: ByteArray)
}
