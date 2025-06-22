package com.example.silentguardapp.controller

import android.media.AudioRecord
import com.example.silentguardapp.model.AudioRecordModel
import com.example.silentguardapp.services.AudioService

/**
 * AudioController - audio operations
 */
class AudioController(private val audioService: AudioService) {


    /**
     * Create new audio record
     */
    fun createAudioRecord(
        sampleRate: Int,
        channelConfig: Int,
        audioFormat: Int,
        bufferSize: Int
    ): AudioRecord? {
        return audioService.createAudioRecord(sampleRate, channelConfig, audioFormat, bufferSize)
    }

    /**
     * Start recording for event
     */
    fun startRecording(eventId: String): Boolean {
        return audioService.startRecording(eventId)
    }

    /**
     * Stop recording and get audio record
     */
    fun stopRecording(): AudioRecordModel? {
        return audioService.stopRecording()
    }

    /**
     * Convert audio to text
     */
    fun convertAudioToText(audioRecord: AudioRecordModel): String {
        return audioService.convertAudioToText(audioRecord)
    }
}