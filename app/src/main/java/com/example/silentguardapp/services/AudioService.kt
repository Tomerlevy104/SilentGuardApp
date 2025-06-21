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
import com.google.cloud.speech.v1.*
import com.google.auth.oauth2.GoogleCredentials
import com.google.api.gax.core.FixedCredentialsProvider
import com.google.protobuf.ByteString

/**
 * AudioService - business logic
 */
class AudioService(private val context: Context) {

    private var isRecording = false
    private var currentEventId: String? = null
    private var currentFilePath: String? = null
    private var mediaRecorder: MediaRecorder? = null
    private var speechToTextConverter = SpeechToTextConverter(context)
    private val FILEPATH : String = "SilentGuard/EmergencyRecords"
    private val MAXRECLEN: Int = 30

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

        return try {
            val audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )

            if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
                Log.e("AudioService", "Failed to initialize AudioRecord")
                null
            } else {
                audioRecord
            }

        } catch (e: SecurityException) {
            Log.e("AudioService", "SecurityException: Missing permission for AudioRecord", e)
            null
        } catch (e: Exception) {
            Log.e("AudioService", "Error creating AudioRecord: ${e.message}", e)
            null
        }
    }

    // Create proper file path for internal app storage
    private fun getEmergencyRecordsDirectory(): String {
        // Use public external storage (accessible from file manager)
        val publicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) // publicDir = Documents
//        val emergencyDir = File(publicDir, "SilentGuard/EmergencyRecords") // Documents -> SilentGuard -> EmergencyRecords

        val emergencyDir = File(publicDir, FILEPATH) // Documents -> SilentGuard -> EmergencyRecords

        // Create directory if it doesn't exist
        if (!emergencyDir.exists()) {
            emergencyDir.mkdirs()
            Log.d("AudioService", "Created directory: ${emergencyDir.absolutePath}")
        }

        return emergencyDir.absolutePath
    }

    /**
     * Start recording audio for an event
     */

    fun startRecording(eventId: String): Boolean {
        if (isRecording) return false

        try {
            currentEventId = eventId

            val preferencesManager = PreferencesManager(context)
            val recordingDuration = preferencesManager.loadAppSettings().recordingDuration // recording duration

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

        Log.d("AudioService","Enter to stopRecording function")
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
     * Convert audio to text using Google Speech-to-Text API
     */
    fun convertAudioToText(audioRecord: AudioRecordModel): String {
        val textFromAudio = speechToTextConverter.convertAudioToText(audioRecord)
        return textFromAudio
    }

    /**
     * Save audio record to event
     */
    fun saveAudioToEvent(eventId: String, audioRecord: AudioRecordModel): Boolean {
        try {
            val preferencesManager = PreferencesManager(context)
            val success = preferencesManager.saveAudioRecordForEvent(eventId, audioRecord)

            if (success) {
                Log.d("AudioService", "Audio saved successfully for event: $eventId")
            } else {
                Log.e("AudioService", "Failed to save audio for event: $eventId - Event might not exist")
            }

            return success

        } catch (e: Exception) {
            Log.e("AudioService", "Error saving audio to event: ${e.message}", e)
            return false
        }
    }

    /**
     * Delete audio file
     */
    fun deleteAudioFile(audioRecord: AudioRecordModel): Boolean {
        return try {
            File(audioRecord.filePath).delete()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Clean old audio files
     */
    fun cleanOldAudioFiles(): Int {
        // TODO: Delete files older than X days
        return 0
    }


    /**
     * Force stop recording
     */
    fun forceStopRecording(): Boolean {
        if (isRecording) {
            isRecording = false
            currentEventId = null
            currentFilePath = null
            return true
        }
        return false
    }
}