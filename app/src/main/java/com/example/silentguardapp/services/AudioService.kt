package com.example.silentguardapp.services

import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Environment
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.silentguardapp.model.AudioRecordModel
import com.example.silentguardapp.utils.PreferencesManager
import com.example.silentguardapp.utils.SpeechToTextConverter
import java.util.UUID
import java.io.File

/**
 * AudioService - business logic
 */
class AudioService(private val context: Context) {

    private var isRecording = false
    private var currentEventId: String? = null
    private var currentFilePath: String? = null
    private var mediaRecorder: MediaRecorder? = null
    private var speechToTextConverter = SpeechToTextConverter(context)
    private val FILEPATH: String = "SilentGuard/EmergencyRecords"

    /**
     * Create audio record
     */
    fun createAudioRecord(
        sampleRate: Int,
        channelConfig: Int,
        audioFormat: Int,
        bufferSize: Int
    ): AudioRecord? {
        // Check if RECORD_AUDIO permission is granted
        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("AudioService", "Missing RECORD_AUDIO permission")
            return null
        }

        try {
            val audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )

            if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
                Log.e("AudioService", "Failed to initialize AudioRecord")
                return null
            } else {
                return audioRecord
            }

        } catch (e: SecurityException) {
            Log.e("AudioService", "SecurityException: Missing permission for AudioRecord", e)
            return null
        } catch (e: Exception) {
            Log.e("AudioService", "Error creating AudioRecord: ${e.message}", e)
            return null
        }
    }

    /**
     * Start recording audio for an event
     */
    fun startRecording(eventId: String): Boolean {
        if (isRecording) return false

        try {
            currentEventId = eventId

            val preferencesManager = PreferencesManager(context)
            val recordingDuration =
                preferencesManager.loadAppSettings().recordingDuration // recording duration

            // Use proper external storage path
            val directoryPath = getEmergencyRecordsDirectory()
            currentFilePath = "$directoryPath/$eventId.3gp"

            Log.d("AudioService", "Recording to: $currentFilePath")

            // Create MediaRecorder
            @Suppress("DEPRECATION")
            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setOutputFile(currentFilePath)
                setMaxDuration(recordingDuration * 1000)
                prepare()
                start()
            }

            isRecording = true
            Log.d("AudioService", "Recording started successfully")
            return true

        } catch (e: Exception) {
            Log.e("AudioService", "Failed to start recording: ${e.message}")
            isRecording = false
            return false
        }
    }

    /**
     * Stop recording and return audio record
     * Actually this function create a new record
     */
    fun stopRecording(): AudioRecordModel? {
        val duration = PreferencesManager(context).loadAppSettings().recordingDuration
        if (!isRecording || currentFilePath == null) return null

        Log.d("AudioService", "Enter to stopRecording function")
        // Stop the record
        mediaRecorder?.stop()
        mediaRecorder?.release()
        mediaRecorder = null
        isRecording = false

        return AudioRecordModel(
            id = UUID.randomUUID().toString(),
            filePath = currentFilePath!!,
            duration = duration,
        )
    }

    /**
     * Create proper file path for internal app storage
     */
    private fun getEmergencyRecordsDirectory(): String {
        // Use public external storage
        val publicDir =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) // publicDir = Documents
        val emergencyDir = File(publicDir, FILEPATH) // Documents -> SilentGuard -> EmergencyRecords

        // Create directory if it doesn't exist
        if (!emergencyDir.exists()) {
            emergencyDir.mkdirs()
            Log.d("AudioService", "Created directory: ${emergencyDir.absolutePath}")
        }

        return emergencyDir.absolutePath
    }

    /**
     * Convert audio to text using Google Speech-to-Text API
     */
    fun convertAudioToText(audioRecord: AudioRecordModel): String {
        val textFromAudio = speechToTextConverter.convertAudioToText(audioRecord)
        return textFromAudio
    }
}