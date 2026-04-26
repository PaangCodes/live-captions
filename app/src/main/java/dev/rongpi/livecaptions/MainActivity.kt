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
import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.google.mlkit.nl.translate.TranslateLanguage
import dev.rongpi.livecaptions.audio.AudioCaptureService
import dev.rongpi.livecaptions.overlay.OverlayManager
import dev.rongpi.livecaptions.stt.SttConfig
import dev.rongpi.livecaptions.stt.SttEngine
import dev.rongpi.livecaptions.stt.vosk.VoskSttEngine
import dev.rongpi.livecaptions.translation.TranslationManager

class MainActivity : ComponentActivity() {

    private var sttEngine: SttEngine? = null
    private var audioCaptureService: AudioCaptureService? = null
    private var isBound = false
    private var translationManager: TranslationManager? = null
    private var overlayManager: OverlayManager? = null

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

            sttEngine?.let {
                audioCaptureService?.setSttEngine(it)
                it.start()
            }
        } else {
            Log.e("MainActivity", "MediaProjection permission denied")
        }
    }

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        checkPermissionsAndStart()
    }

    private val audioPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            checkPermissionsAndStart()
        } else {
            Log.e("MainActivity", "Audio permission denied")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sttEngine = VoskSttEngine()
        sttEngine?.initialize(SttConfig(this, "model-en"))

        translationManager = TranslationManager(TranslateLanguage.ENGLISH, TranslateLanguage.SPANISH)
        translationManager?.initialize()

        overlayManager = OverlayManager(this)

        sttEngine?.let { stt ->
            translationManager?.let { trans ->
                trans.translate(stt.partialResults)
                overlayManager?.showOverlay(trans.translatedText)
            }
        }

        val intent = Intent(this, AudioCaptureService::class.java)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Button(onClick = { startLiveCaptions() }) {
                            Text(text = "Start Live Captions")
                        }
                    }
                }
            }
        }
    }

    private fun startLiveCaptions() {
        checkPermissionsAndStart()
    }

    private fun checkPermissionsAndStart() {
        val hasAudioPermission = ContextCompat.checkSelfPermission(
            this, Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        val hasOverlayPermission = Settings.canDrawOverlays(this)

        if (!hasAudioPermission) {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            return
        }

        if (!hasOverlayPermission) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            overlayPermissionLauncher.launch(intent)
            return
        }

        requestMediaProjection()
    }

    private fun requestMediaProjection() {
        val mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        captureAudioResultLauncher.launch(mediaProjectionManager.createScreenCaptureIntent())
    }

    private fun stopLiveCaptions() {
        val intent = Intent(this, AudioCaptureService::class.java).apply {
            action = AudioCaptureService.ACTION_STOP_CAPTURE
        }
        startService(intent)
        sttEngine?.stop()
        overlayManager?.hideOverlay()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
        }
        stopLiveCaptions()
        translationManager?.close()
    }
}