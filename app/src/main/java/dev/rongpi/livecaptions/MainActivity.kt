package dev.rongpi.livecaptions

import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import android.content.ComponentName
import android.content.ServiceConnection
import android.os.IBinder
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
import dev.rongpi.livecaptions.stt.SttState
import dev.rongpi.livecaptions.stt.vosk.VoskSttEngine
import dev.rongpi.livecaptions.stt.whisper.WhisperSttEngine
import dev.rongpi.livecaptions.translation.TranslationManager
import dev.rongpi.livecaptions.translation.TranslationState
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {

    private var sttEngine = MutableStateFlow<SttEngine>(VoskSttEngine())
    private var audioCaptureService: AudioCaptureService? = null
    private var isBound = false
    private var translationManager: TranslationManager? = null
    private var overlayManager: OverlayManager? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as AudioCaptureService.LocalBinder
            audioCaptureService = binder.getService()
            isBound = true

            audioCaptureService?.setSttEngine(sttEngine.value)
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

            audioCaptureService?.setSttEngine(sttEngine.value)
            sttEngine.value.start()

            translationManager?.let { trans ->
                overlayManager?.showOverlay(trans.translatedText)
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

        sttEngine.value.initialize(SttConfig(this, "model-en"))

        translationManager = TranslationManager(TranslateLanguage.ENGLISH, TranslateLanguage.SPANISH)
        translationManager?.initialize()

        overlayManager = OverlayManager(this)

        translationManager?.let { trans ->
            trans.translate(sttEngine.value.partialResults)
        }

        val intent = Intent(this, AudioCaptureService::class.java)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)

        setContent {
            var selectedStt by remember { mutableStateOf("Vosk") }
            var sourceLang by remember { mutableStateOf(TranslateLanguage.ENGLISH) }
            var targetLang by remember { mutableStateOf(TranslateLanguage.SPANISH) }

            val sttState by sttEngine.value.state.collectAsState()
            val transState = translationManager?.state?.collectAsState()

            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("Settings", style = MaterialTheme.typography.headlineMedium)
                        Spacer(modifier = Modifier.height(16.dp))

                        // STT Engine Selection
                        Text("STT Engine:")
                        Row {
                            RadioButton(
                                selected = selectedStt == "Vosk",
                                onClick = {
                                    selectedStt = "Vosk"
                                    switchSttEngine(VoskSttEngine())
                                }
                            )
                            Text("Vosk", modifier = Modifier.align(Alignment.CenterVertically))
                            Spacer(modifier = Modifier.width(8.dp))
                            RadioButton(
                                selected = selectedStt == "Whisper",
                                onClick = {
                                    selectedStt = "Whisper"
                                    switchSttEngine(WhisperSttEngine())
                                }
                            )
                            Text("Whisper", modifier = Modifier.align(Alignment.CenterVertically))
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text("STT State: $sttState")

                        Spacer(modifier = Modifier.height(16.dp))

                        // Language Selection
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Source:")
                            Spacer(modifier = Modifier.width(8.dp))
                            LanguageDropdown(sourceLang) { lang ->
                                sourceLang = lang
                                translationManager?.updateLanguages(sourceLang, targetLang)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Target:")
                            Spacer(modifier = Modifier.width(8.dp))
                            LanguageDropdown(targetLang) { lang ->
                                targetLang = lang
                                translationManager?.updateLanguages(sourceLang, targetLang)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Translation State: ${transState?.value}")

                        Spacer(modifier = Modifier.height(32.dp))

                        Button(onClick = { startLiveCaptions() }) {
                            Text(text = "Start Live Captions")
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(onClick = { stopLiveCaptions() }) {
                            Text(text = "Stop Live Captions")
                        }
                    }
                }
            }
        }
    }

    private fun switchSttEngine(newEngine: SttEngine) {
        val wasListening = sttEngine.value.state.value is SttState.Listening
        if (wasListening) {
            sttEngine.value.stop()
        }
        sttEngine.value = newEngine
        sttEngine.value.initialize(SttConfig(this, "model-en"))

        translationManager?.let { trans ->
            trans.translate(sttEngine.value.partialResults)
        }

        audioCaptureService?.setSttEngine(sttEngine.value)
        if (wasListening) {
            sttEngine.value.start()
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
        sttEngine.value.stop()
        overlayManager?.hideOverlay()
    }

    @Composable
    fun LanguageDropdown(selectedLanguage: String, onLanguageSelected: (String) -> Unit) {
        var expanded by remember { mutableStateOf(false) }
        val languages = listOf(
            TranslateLanguage.ENGLISH,
            TranslateLanguage.SPANISH,
            TranslateLanguage.FRENCH,
            TranslateLanguage.GERMAN,
            TranslateLanguage.JAPANESE,
            TranslateLanguage.KOREAN,
            TranslateLanguage.CHINESE
        )

        Box {
            OutlinedButton(onClick = { expanded = true }) {
                Text(selectedLanguage)
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                languages.forEach { language ->
                    DropdownMenuItem(
                        text = { Text(language) },
                        onClick = {
                            onLanguageSelected(language)
                            expanded = false
                        }
                    )
                }
            }
        }
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