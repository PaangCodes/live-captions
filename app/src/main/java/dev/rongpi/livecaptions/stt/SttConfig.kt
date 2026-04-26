package dev.rongpi.livecaptions.stt

import android.content.Context

data class SttConfig(
    val context: Context,
    val modelPath: String = "",
    val sampleRate: Int = 16000
)
