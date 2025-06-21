package com.example.silentguardapp.services

import android.content.Context
import android.util.Log
import com.example.silentguardapp.controller.AudioController
import com.example.silentguardapp.controller.MessageController
import com.example.silentguardapp.controller.NotifierController
import com.example.silentguardapp.model.EncryptedMessageModel
import com.example.silentguardapp.model.EventModel
import com.example.silentguardapp.utils.PreferencesManager
import java.util.UUID

/**
 * EventService - business logic
 */
class EventService(private val context: Context) {

    private val audioController = AudioController(AudioService(context))
    private val messageController = MessageController(context)
    private val preferencesManager = PreferencesManager(context)

    fun createEmergencyEvent(): EventModel {
        val event = EventModel(
            id = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            audioRecord = null,
            transcribedText = "",
            encryptedMessage = null,
            location = null,
            sentToContact = null
        )
        preferencesManager.saveEmergencyEvent(event)
        return event
    }

    fun startRecordingForEvent(eventId: String): Boolean {
        return audioController.startRecording(eventId)
    }

    /*
    * Finalize event function
    */
    fun finalizeEvent(eventId: String): Boolean {
        val audioRecordModel = audioController.stopRecording() ?: return false
        val transcribedText = audioController.convertAudioToText(audioRecordModel)
        Log.d("EventService", "Transcribed text: $transcribedText")

        // Get the event and settings from shared preference
        val event = preferencesManager.getEventById(eventId) ?: return false
        val settings = preferencesManager.loadAppSettings()

        // Update event
        event.transcribedText = transcribedText
        event.audioRecord = audioRecordModel
        event.sentToContact = settings.emergencyContact // Get a contact from settings

        // Create an encrypted message
        val encodedMessage =
            messageController.getEncodedMessageFromEvent(event) // The Encrypted text
        Log.d("EventService", "Encrypted text with zero width characters: $encodedMessage")

        // Saving the encrypted message within an encrypted message object
        event.encryptedMessage = EncryptedMessageModel(
            encryptedText = encodedMessage,
            originalText = transcribedText,
            coverMessage = settings.coverMessage
        )

        Log.d("EventService", "event after all: $event")

        // Send the encrypted message to the configured contact
        val notifierController = NotifierController(context)
        val notifySuccess = notifierController.notifyContact(event)


        if (!notifySuccess) {
            Log.w("EventService", "Failed to notify contact")
        } else {
            Log.i("EventService", "Contact notified successfully")
        }

        Log.d("EventService", "Finalized event: $event")

        return preferencesManager.saveEmergencyEvent(event)
    }

}