package dev.rongpi.livecaptions

import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
import androidx.compose.ui.Modifier
import dev.rongpi.livecaptions.audio.AudioCaptureService
import dev.rongpi.livecaptions.stt.SttConfig
import dev.rongpi.livecaptions.stt.SttEngine
import dev.rongpi.livecaptions.stt.vosk.VoskSttEngine

class MainActivity : ComponentActivity() {

    private var sttEngine: SttEngine? = null
    private var audioCaptureService: AudioCaptureService? = null
    private var isBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as AudioCaptureService.LocalBinder
            audioCaptureService = binder.getService()
            isBound = true

            sttEngine?.let {
                audioCaptureService?.setSttEngine(it)
            }
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            isBound = false
            audioCaptureService = null
        }
    }

    private val captureAudioResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val intent = Intent(this, AudioCaptureService::class.java).apply {
                action = AudioCaptureService.ACTION_START_CAPTURE
                putExtra(AudioCaptureService.EXTRA_RESULT_CODE, result.resultCode)
                putExtra(AudioCaptureService.EXTRA_RESULT_DATA, result.data)
            }
            startForegroundService(intent)
        } else {
            Log.e("MainActivity", "MediaProjection permission denied")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sttEngine = VoskSttEngine()
        sttEngine?.initialize(SttConfig(this, "model-en"))

        val intent = Intent(this, AudioCaptureService::class.java)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Button(onClick = { startAudioCapture() }) {
                            Text(text = "Start Live Captions")
                        }
                    }
                }
            }
        }
    }

    private fun startAudioCapture() {
        val mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        captureAudioResultLauncher.launch(mediaProjectionManager.createScreenCaptureIntent())
    }

    private fun stopAudioCapture() {
        val intent = Intent(this, AudioCaptureService::class.java).apply {
            action = AudioCaptureService.ACTION_STOP_CAPTURE
        }
        startService(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
        stopAudioCapture()
    }
}