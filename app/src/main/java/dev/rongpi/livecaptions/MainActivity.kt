package dev.rongpi.livecaptions

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
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

    private val sttEngine = MutableStateFlow<SttEngine>(VoskSttEngine())
    private var audioCaptureService: AudioCaptureService? = null
    private var isBound = false
    private var translationManager: TranslationManager? = null
    private var overlayManager: OverlayManager? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as AudioCaptureService.LocalBinder
            audioCaptureService = binder.getService()
            audioCaptureService?.setSttEngine(sttEngine.value)
            isBound = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            isBound = false
        }
    }

    private val captureAudioResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            sttEngine.value.start()
            val intent = Intent(this, AudioCaptureService::class.java).apply {
                action = AudioCaptureService.ACTION_START_CAPTURE
                putExtra(AudioCaptureService.EXTRA_RESULT_CODE, result.resultCode)
                putExtra(AudioCaptureService.EXTRA_RESULT_DATA, result.data)
            }
            startForegroundService(intent)
            translationManager?.let { trans ->
                overlayManager?.showOverlay(trans.translatedText)
            }
        }
    }

    private val audioPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            checkPermissionsAndStart()
        }
    }

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        checkPermissionsAndStart()
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

            val currentEngine by sttEngine.collectAsState()

            // ⚡ Bolt Optimization: Isolate STT state updates to a child component
            // We read sttState inside SttConfigCard so we don't recompose the entire
            // Scaffold and MainActivity layout on every fast-emitting progress update.

            val transState = translationManager?.state?.collectAsState()
            val downloadedLangs = translationManager?.downloadedLanguages?.collectAsState(initial = emptyList())?.value ?: emptyList()

            // ⚡ Bolt Optimization: Convert downloaded languages list to a Set
            // This turns O(N) list lookups inside the UI rendering loop into fast O(1) set lookups,
            // avoiding O(N^2) complexity as the number of languages grows.
            val downloadedLangsSet = remember(downloadedLangs) { downloadedLangs.toSet() }

            MaterialTheme {
                Scaffold(
                    topBar = {
                        @OptIn(ExperimentalMaterial3Api::class)
                        TopAppBar(
                            title = { Text("Live Captions") },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                titleContentColor = MaterialTheme.colorScheme.primary,
                            )
                        )
                    }
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(horizontal = 16.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))

                        // STT Configuration
                        SttConfigCard(
                            selectedStt = selectedStt,
                            onSttSelected = { newStt ->
                                selectedStt = newStt
                                if (newStt == "Vosk") switchSttEngine(VoskSttEngine())
                                else switchSttEngine(WhisperSttEngine())
                            },
                            engine = currentEngine
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Translation Configuration
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Translation Settings", style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Source:")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    LanguageDropdown(sourceLang) { lang ->
                                        sourceLang = lang
                                        translationManager?.updateLanguages(sourceLang, targetLang)
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Target:")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    LanguageDropdown(targetLang) { lang ->
                                        targetLang = lang
                                        translationManager?.updateLanguages(sourceLang, targetLang)
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                val (transStatusText, transStatusColor) = when (val tState = transState?.value) {
                                    is TranslationState.Idle -> "Idle" to MaterialTheme.colorScheme.onSurfaceVariant
                                    is TranslationState.DownloadingModel -> "Downloading Language Model..." to MaterialTheme.colorScheme.primary
                                    is TranslationState.Ready -> "Ready" to MaterialTheme.colorScheme.primary
                                    is TranslationState.Error -> "Error: ${tState.message}" to MaterialTheme.colorScheme.error
                                    else -> "Unknown" to MaterialTheme.colorScheme.onSurfaceVariant
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Status: ", style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        text = transStatusText,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = transStatusColor,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("ML Kit Language Models", style = MaterialTheme.typography.titleMedium)
                                Spacer(modifier = Modifier.height(8.dp))

                                // ⚡ Bolt Optimization: Memoize Locale instantiation and list sorting.
                                // Prevents hundreds of redundant object allocations per second during high-frequency
                                // download progress StateFlow emissions, significantly reducing UI jank.
                                val allLangs = remember {
                                    TranslateLanguage.getAllLanguages().map { lang ->
                                        lang to java.util.Locale(lang).displayLanguage.replaceFirstChar { it.uppercase() }
                                    }.sortedBy { it.second }
                                }
                                allLangs.forEach { (lang, displayName) ->
                                    key(lang) {
                                        val isDownloaded = downloadedLangsSet.contains(lang)
                                        var showDeleteDialog by remember { mutableStateOf(false) }

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(displayName)
                                            if (isDownloaded) {
                                                Button(
                                                    onClick = { showDeleteDialog = true },
                                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                                ) {
                                                    Text("Delete")
                                                }
                                            } else {
                                                Button(onClick = { translationManager?.downloadLanguage(lang) }) {
                                                    Text("Download")
                                                }
                                            }
                                        }

                                        if (showDeleteDialog) {
                                            AlertDialog(
                                                onDismissRequest = { showDeleteDialog = false },
                                                title = { Text("Delete Language") },
                                                text = { Text("Are you sure you want to delete the $displayName model?") },
                                                confirmButton = {
                                                    TextButton(
                                                        onClick = {
                                                            translationManager?.deleteLanguage(lang)
                                                            showDeleteDialog = false
                                                        }
                                                    ) {
                                                        Text("Confirm", color = MaterialTheme.colorScheme.error)
                                                    }
                                                },
                                                dismissButton = {
                                                    TextButton(onClick = { showDeleteDialog = false }) {
                                                        Text("Cancel")
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))

                        LiveCaptionControls(currentEngine, transState)

                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }

    @Composable
    private fun LiveCaptionControls(engine: SttEngine, transState: androidx.compose.runtime.State<TranslationState>?) {
        val sttState by engine.state.collectAsState()

        Button(
            onClick = { startLiveCaptions() },
            modifier = Modifier.fillMaxWidth(),
            enabled = sttState is SttState.Ready && (transState == null || transState.value is TranslationState.Ready)
        ) {
            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(text = "Start Live Captions")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { stopLiveCaptions() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            enabled = sttState !is SttState.Uninitialized && sttState !is SttState.Ready
        ) {
            Icon(imageVector = Icons.Default.Close, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(text = "Stop Live Captions")
        }
    }

    @Composable
    private fun SttConfigCard(selectedStt: String, onSttSelected: (String) -> Unit, engine: SttEngine) {
        val sttState by engine.state.collectAsState()

        ElevatedCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Speech-to-Text Settings", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Row {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.selectable(
                            selected = selectedStt == "Vosk",
                            onClick = { onSttSelected("Vosk") },
                            role = Role.RadioButton
                        )
                    ) {
                        RadioButton(selected = selectedStt == "Vosk", onClick = null)
                        Text("Vosk", modifier = Modifier.padding(start = 4.dp, end = 8.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.selectable(
                            selected = selectedStt == "Whisper",
                            onClick = { onSttSelected("Whisper") },
                            role = Role.RadioButton
                        )
                    ) {
                        RadioButton(selected = selectedStt == "Whisper", onClick = null)
                        Text("Whisper", modifier = Modifier.padding(start = 4.dp, end = 8.dp))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                val (sttStatusText, sttStatusColor) = when (sttState) {
                    is SttState.Uninitialized -> "Uninitialized" to MaterialTheme.colorScheme.onSurfaceVariant
                    is SttState.Initializing -> "Initializing Engine..." to MaterialTheme.colorScheme.onSurfaceVariant
                    is SttState.Downloading -> "Downloading Model..." to MaterialTheme.colorScheme.primary
                    is SttState.Ready -> "Ready" to MaterialTheme.colorScheme.primary
                    is SttState.Listening -> "Listening" to MaterialTheme.colorScheme.primary
                    is SttState.Error -> "Error: ${(sttState as SttState.Error).message}" to MaterialTheme.colorScheme.error
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Status: ", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = sttStatusText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = sttStatusColor,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Medium
                    )
                }

                if (sttState is SttState.Downloading) {
                    val dl = sttState as SttState.Downloading
                    val progress = if (dl.totalBytes > 0) dl.downloadedBytes.toFloat() / dl.totalBytes else 0f
                    val mbDownloaded = dl.downloadedBytes / (1024 * 1024)
                    val mbTotal = dl.totalBytes / (1024 * 1024)

                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = progress,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("$mbDownloaded MB / $mbTotal MB")
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

        // ⚡ Bolt Optimization: Memoize the static language list and Locale instantiation.
        // Prevents redundant string formatting when the dropdown expands or parent recomposes.
        val languages = remember {
            listOf(
                TranslateLanguage.ENGLISH,
                TranslateLanguage.SPANISH,
                TranslateLanguage.FRENCH,
                TranslateLanguage.GERMAN,
                TranslateLanguage.JAPANESE,
                TranslateLanguage.KOREAN,
                TranslateLanguage.CHINESE
            ).map { lang ->
                lang to java.util.Locale(lang).displayLanguage.replaceFirstChar { it.uppercase() }
            }
        }

        // ⚡ Bolt Optimization: Memoize the locale instantiation and formatting based on selectedLanguage.
        // This avoids expensive String manipulations on every layout recomposition triggered
        // by rapid flow state emissions.
        val selectedDisplayName = remember(selectedLanguage) {
            java.util.Locale(selectedLanguage).displayLanguage.replaceFirstChar { it.uppercase() }
        }

        Box {
            OutlinedButton(onClick = { expanded = true }) {
                Text(selectedDisplayName)
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Expand language options"
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                languages.forEach { (language, displayName) ->
                    DropdownMenuItem(
                        text = { Text(displayName) },
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
