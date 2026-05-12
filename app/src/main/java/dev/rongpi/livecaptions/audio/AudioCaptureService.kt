package dev.rongpi.livecaptions.audio

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import dev.rongpi.livecaptions.stt.SttEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AudioCaptureService : Service() {
    private val binder = LocalBinder()
    private var sttEngine: SttEngine? = null

    private var mediaProjectionManager: MediaProjectionManager? = null
    private var mediaProjection: MediaProjection? = null
    private var audioRecord: AudioRecord? = null

    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private var captureJob: Job? = null

    inner class LocalBinder : Binder() {
        fun getService(): AudioCaptureService = this@AudioCaptureService
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    fun setSttEngine(engine: SttEngine) {
        this.sttEngine = engine
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_START_CAPTURE) {
            val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
            @Suppress("DEPRECATION")
            val resultData: Intent? = intent.getParcelableExtra(EXTRA_RESULT_DATA)

            if (resultCode != 0 && resultData != null) {
                startForegroundServiceNotification()
                startAudioCapture(resultCode, resultData)
            } else {
                Log.e(TAG, "Invalid intent data for audio capture")
                stopSelf()
            }
        } else if (intent?.action == ACTION_STOP_CAPTURE) {
            stopAudioCapture()
            stopSelf()
        }

        return START_NOT_STICKY
    }

    private fun startForegroundServiceNotification() {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Live Captions")
            .setContentText("Capturing system audio")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .build()

        startForeground(
            NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
        )
    }

    private fun startAudioCapture(resultCode: Int, resultData: Intent) {
        stopAudioCapture()
        mediaProjection = mediaProjectionManager?.getMediaProjection(resultCode, resultData)
        if (mediaProjection == null) {
            Log.e(TAG, "Failed to get MediaProjection")
            return
        }

        val config = AudioPlaybackCaptureConfiguration.Builder(mediaProjection!!)
            .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
            .addMatchingUsage(AudioAttributes.USAGE_GAME)
            .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
            .build()

        val audioFormat = AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(16000)
            .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
            .build()

        val bufferSize = AudioRecord.getMinBufferSize(
            16000,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        try {
            audioRecord = AudioRecord.Builder()
                .setAudioFormat(audioFormat)
                .setBufferSizeInBytes(bufferSize)
                .setAudioPlaybackCaptureConfig(config)
                .build()

            audioRecord?.startRecording()

            captureJob = serviceScope.launch {
                val buffer = ByteArray(bufferSize)
                while (isActive) {
                    val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (read > 0) {
                        // ⚡ Bolt Optimization: Zero-allocation audio processing
                        // Pass the array directly with offset and length instead of copying the array
                        // to avoid massive GC pressure during high-frequency audio capture loops.
                        sttEngine?.processAudio(buffer, 0, read)
                    }
                }
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException: permission not granted to record audio", e)
        } catch (e: Exception) {
            Log.e(TAG, "Exception starting audio capture", e)
        }
    }

    private fun stopAudioCapture() {
        captureJob?.cancel()
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
        mediaProjection?.stop()
        mediaProjection = null
    }

    override fun onDestroy() {
        stopAudioCapture()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Audio Capture Service",
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)
    }

    companion object {
        private const val TAG = "AudioCaptureService"
        private const val CHANNEL_ID = "AudioCaptureChannel"
        private const val NOTIFICATION_ID = 1

        const val ACTION_START_CAPTURE = "dev.rongpi.livecaptions.START_CAPTURE"
        const val ACTION_STOP_CAPTURE = "dev.rongpi.livecaptions.STOP_CAPTURE"
        const val EXTRA_RESULT_CODE = "EXTRA_RESULT_CODE"
        const val EXTRA_RESULT_DATA = "EXTRA_RESULT_DATA"
    }
}
