package dev.rongpi.livecaptions.stt

sealed interface SttState {
    data object Uninitialized : SttState
    data object Initializing : SttState
    data object Ready : SttState
    data object Listening : SttState
    data class Error(val exception: Throwable) : SttState
}
