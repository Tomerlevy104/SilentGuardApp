package com.example.silentguardapp.services

import android.app.*
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.silentguardapp.controller.AudioController
import com.example.silentguardapp.controller.EventController
import kotlinx.coroutines.*
import kotlin.math.log10
import kotlin.math.sqrt

/**
 * MonitoringService - Background noise monitoring service using AudioRecord
 * Purpose: Monitor noise level and create emergency events when threshold is crossed
 */
class MonitoringService : Service() {

    private var isMonitoring = false
    private var audioRecord: AudioRecord? = null
    private var monitoringJob: Job? = null
    private var defaultThreshold: Float = 0.7f
    private var TIMEOFREC: Long = 30_000
    private lateinit var audioController: AudioController
    private lateinit var eventController: EventController

    companion object {
        const val CHANNEL_ID = "monitoring_channel"
        const val NOTIFICATION_ID = 1
        const val ACTION_START_MONITORING = "start_monitoring"
        const val ACTION_STOP_MONITORING = "stop_monitoring"
        const val EXTRA_THRESHOLD = "threshold"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        audioController = AudioController(AudioService(this))
        eventController = EventController(applicationContext)

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_MONITORING -> {
                val threshold = intent.getFloatExtra(EXTRA_THRESHOLD, defaultThreshold)
                startMonitoring(threshold)
            }

            ACTION_STOP_MONITORING -> {
                stopMonitoring()
            }
        }
        return START_STICKY
    }

    /**
     * Start background noise monitoring using AudioRecord
     */
    private fun startMonitoring(threshold: Float) {
        // If already active monitoring
        if (isMonitoring) {
            Log.d("MonitoringService", "Can not start monitoring")
            return
        }

        Log.d("MonitoringService", "Enter to function startMonitoring")
        defaultThreshold = threshold

        try {
            val notification = createNotification()
            startForeground(NOTIFICATION_ID, notification)
            isMonitoring = true
            startEventDetection()
            Log.d("MonitoringService", "Background monitoring started with threshold: $threshold")

        } catch (e: Exception) {
            Log.e("MonitoringService", "Failed to start monitoring: ${e.message}")
            cleanup()
            stopSelf()
        }
    }

    /**
     * Noise detection using AudioRecord
     */
    private fun startEventDetection() {
        // Launch a coroutine on IO thread for background audio processing
        monitoringJob = CoroutineScope(Dispatchers.IO).launch {
            try {
                // Audio configuration parameters
                // Higher sampling rate -> higher audio quality, but more memory space is required to process and store the data.
                val sampleRate =
                    44100                                // Sample rate: 44.1kHz (CD quality)

                val channelConfig =
                    AudioFormat.CHANNEL_IN_MONO          // Record in mono (single channel)

                val audioFormat =
                    AudioFormat.ENCODING_PCM_16BIT       // 16-bit PCM encoding (standard quality)

                // Calculate minimum buffer size required for these audio settings
                val bufferSize =
                    AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

                // Check if buffer size calculation failed
                if (bufferSize == AudioRecord.ERROR_BAD_VALUE || bufferSize == AudioRecord.ERROR) {
                    Log.e("MonitoringService", "Invalid buffer size for AudioRecord")
                    return@launch  // Exit the coroutine if buffer size is invalid
                }

                // Create AudioRecord instance with specified parameters
                audioRecord = audioController.createAudioRecord(
                    sampleRate,                      // Sample rate (44.1kHz)
                    channelConfig,                   // Mono channel configuration
                    audioFormat,                     // 16-bit PCM format
                    bufferSize * 2         // Buffer size (doubled for safety)
                )

                // Validate AudioRecord creation
                if (audioRecord == null) {
                    Log.e("MonitoringService", "Failed to create AudioRecord")
                    return@launch
                }

                // Start recording audio from microphone
                audioRecord?.startRecording()
                Log.d("MonitoringService", "AudioRecord started successfully")

                // Create buffer array to store audio samples
                val buffer = ShortArray(bufferSize)

                // Main monitoring loop - continues while service is monitoring
                while (isMonitoring) {
                    // Read audio data from microphone into buffer
                    val readCount = audioRecord?.read(buffer, 0, bufferSize) ?: 0

                    // Process audio data if we successfully read samples
                    if (readCount > 0) {
                        // Calculate RMS (Root Mean Square) amplitude from audio samples
                        val amplitude = calculateRMS(buffer, readCount)

                        // Convert amplitude to normalized noise level (0.0 to 1.0)
                        val noiseLevel = normalizeAmplitude(amplitude)

                        // Log noise level if it's above minimum threshold (for debugging)
                        if (noiseLevel > 0.1f) {
                            Log.d(
                                "MonitoringService",
                                "Current noise level: $noiseLevel (threshold: $defaultThreshold)"
                            )
                        }

                        // Check if noise level exceeds emergency threshold
                        if (noiseLevel > defaultThreshold) {
                            Log.d(
                                "MonitoringService",
                                "EMERGENCY DETECTED! Noise level: $noiseLevel"
                            )
                            handleEmergencyEventDetected()  // Trigger emergency response
                            break  // Exit monitoring loop after emergency detection
                        }
                    }

                    delay(200) // Wait 200ms before next audio sample check
                }

            } catch (e: SecurityException) {
                // Handle case where microphone permission was denied
                Log.e("MonitoringService", "Permission denied for microphone access: ${e.message}")
            } catch (e: Exception) {
                // Handle any other errors during audio monitoring
                Log.e("MonitoringService", "Error in AudioRecord monitoring: ${e.message}")
            } finally {
                cleanup() // cleanup: Always stop and release AudioRecord resources
            }
        }
    }

    /**
     * Stop monitoring
     */
    private fun stopMonitoring() {
        cleanup()
        stopForeground(true)
        stopSelf()
    }

    /**
     * Handle emergency detection - create event and start recording
     */
    private fun handleEmergencyEventDetected() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Create a new emergency event and get EmergencyEventModel
                val event = eventController.createEmergencyEvent()
                Log.d("MonitoringService", "Created emergency event: ${event.id}")

                // Start recording audio for this event
                val recordingStarted = eventController.startRecordingForEvent(event.id)
                if (recordingStarted) {
                    Log.i("MonitoringService", "Recording started for event: ${event.id}")
                } else {
                    Log.e("MonitoringService", "Failed to start recording for event: ${event.id}")
                    stopMonitoring()
                    return@launch
                }

                // Wait for the recording to complete (30 seconds)
//                 delay(30_000)
                delay(TIMEOFREC)

                // Finalize the event: stop recording and transcribe the audio
                val finalized = eventController.finalizeEvent(event.id)
                if (finalized) {
                    Log.i("MonitoringService", "Event finalized successfully: ${event.id}")
                } else {
                    Log.e("MonitoringService", "Failed to finalize event: ${event.id}")
                }

            } catch (e: Exception) {
                // Log any unexpected errors during emergency handling
                Log.e("MonitoringService", "Error handling emergency: ${e.message}", e)
            } finally {
                // Always stop monitoring once emergency handling is done
                stopMonitoring()
            }
        }
    }

    /**
     * Create notification channel for Android 8+
     */
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Silent Guard Monitoring",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Emergency monitoring service"
            setSound(null, null)
        }

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    /**
     * Create notification for foreground service
     */
    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Silent Guard")
            .setContentText("Monitoring for emergency situations")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /**
     * Calculate RMS (Root Mean Square) amplitude from audio buffer
     */
    private fun calculateRMS(buffer: ShortArray, readCount: Int): Double {
        var sum = 0.0
        for (i in 0 until readCount) {
            sum += (buffer[i] * buffer[i]).toDouble()
        }
        return sqrt(sum / readCount)
    }

    /**
     * Normalize amplitude to 0.0-1.0 range
     */
    private fun normalizeAmplitude(amplitude: Double): Float {
        if (amplitude <= 1.0) return 0f

        // Convert to 0-1 range with logarithmic scaling
        val maxAmplitude = 32767.0 // 16-bit PCM max
        val ratio = amplitude / maxAmplitude

        return when {
            ratio <= 0.001 -> 0f
            ratio >= 1.0 -> 1f
            else -> (log10(ratio * 999 + 1) / log10(1000.0)).toFloat()
        }
    }

    /**
     * Clean up resources
     */
    private fun cleanup() {
        isMonitoring = false
        monitoringJob?.cancel()

        try {
            audioRecord?.stop() // Stop recording
            audioRecord?.release() // Release system resources
        } catch (e: Exception) {
            Log.e("MonitoringService", "Error in cleanup: ${e.message}")
        } finally {
            audioRecord = null // Clear reference to prevent memory leaks
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cleanup()
    }
}