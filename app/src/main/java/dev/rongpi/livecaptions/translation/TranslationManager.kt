package dev.rongpi.livecaptions.translation

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import android.util.Log
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class TranslationManager(
    private var sourceLanguage: String,
    private var targetLanguage: String,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main)
) {
    private val _state = MutableStateFlow<TranslationState>(TranslationState.Idle)
    val state: StateFlow<TranslationState> = _state.asStateFlow()

    private val _translatedText = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val translatedText: SharedFlow<String> = _translatedText.asSharedFlow()

    private var translator: Translator? = null
    private var translateJob: Job? = null

    fun updateLanguages(source: String, target: String) {
        if (sourceLanguage == source && targetLanguage == target) return
        sourceLanguage = source
        targetLanguage = target
        close()
        initialize()
    }

    fun initialize() {
        coroutineScope.launch {
            try {
                _state.value = TranslationState.DownloadingModel

                val options = TranslatorOptions.Builder()
                    .setSourceLanguage(sourceLanguage)
                    .setTargetLanguage(targetLanguage)
                    .build()

                translator = Translation.getClient(options)

                val conditions = DownloadConditions.Builder()
                    .requireWifi()
                    .build()

                // Download models if not present
                translator?.downloadModelIfNeeded(conditions)?.await()

                _state.value = TranslationState.Ready
            } catch (e: Exception) {
                Log.e(TAG, "Exception during translation initialization", e)
                _state.value = TranslationState.Error("Failed to initialize translation model")
            }
        }
    }

    fun translate(textStream: SharedFlow<String>) {
        translateJob?.cancel()
        translateJob = coroutineScope.launch {
            textStream.collect { text ->
                if (_state.value is TranslationState.Ready) {
                    try {
                        val translated = translator?.translate(text)?.await()
                        if (translated != null) {
                            _translatedText.emit(translated)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Exception during translation", e)
                        // Handle or log translation error, don't crash stream
                    }
                }
            }
        }
    }

    fun close() {
        translator?.close()
        _state.value = TranslationState.Idle
    }

    companion object {
        private const val TAG = "TranslationManager"
    }
}
