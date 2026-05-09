package dev.rongpi.livecaptions.translation

import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.common.model.RemoteModelManager
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.TranslateRemoteModel
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
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
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

    private val _downloadedLanguages = MutableStateFlow<Set<String>>(emptySet())
    val downloadedLanguages: StateFlow<Set<String>> = _downloadedLanguages.asStateFlow()

    private val _downloadingLanguages = MutableStateFlow<Set<String>>(emptySet())
    val downloadingLanguages: StateFlow<Set<String>> = _downloadingLanguages.asStateFlow()

    private var translator: Translator? = null
    private var translateJob: Job? = null
    private val modelManager = RemoteModelManager.getInstance()

    init {
        refreshDownloadedLanguages()
    }

    private fun refreshDownloadedLanguages() {
        coroutineScope.launch {
            try {
                val models = modelManager.getDownloadedModels(TranslateRemoteModel::class.java).await()
                _downloadedLanguages.value = models.map { it.language }.toSet()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get downloaded models", e)
            }
        }
    }

    fun downloadLanguage(language: String) {
        coroutineScope.launch {
            _downloadingLanguages.value = _downloadingLanguages.value + language
            try {
                val model = TranslateRemoteModel.Builder(language).build()
                val conditions = DownloadConditions.Builder().build()
                modelManager.download(model, conditions).await()
                refreshDownloadedLanguages()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to download language model $language", e)
            } finally {
                _downloadingLanguages.value = _downloadingLanguages.value - language
            }
        }
    }

    fun deleteLanguage(language: String) {
        coroutineScope.launch {
            try {
                val model = TranslateRemoteModel.Builder(language).build()
                modelManager.deleteDownloadedModel(model).await()
                refreshDownloadedLanguages()

                if (sourceLanguage == language) {
                    sourceLanguage = TranslateLanguage.ENGLISH
                    updateLanguages(sourceLanguage, targetLanguage)
                }
                if (targetLanguage == language) {
                    targetLanguage = TranslateLanguage.ENGLISH
                    updateLanguages(sourceLanguage, targetLanguage)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete language model $language", e)
            }
        }
    }

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
            // Use distinctUntilChanged to avoid translating the exact same text again.
            // Use conflate so if translation is slow, we drop stale intermediate strings and translate the latest.
            textStream.distinctUntilChanged().conflate().collect { text ->
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
