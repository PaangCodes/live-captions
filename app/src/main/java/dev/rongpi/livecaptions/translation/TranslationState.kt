package dev.rongpi.livecaptions.translation

sealed class TranslationState {
    object Idle : TranslationState()
    object DownloadingModel : TranslationState()
    data class Downloading(val downloadedBytes: Long, val totalBytes: Long) : TranslationState()
    object Ready : TranslationState()
    data class Error(val message: String) : TranslationState()
}
