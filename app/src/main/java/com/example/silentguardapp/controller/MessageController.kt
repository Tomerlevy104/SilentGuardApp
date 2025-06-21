package com.example.silentguardapp.controller

import android.content.Context
import com.example.silentguardapp.model.EventModel
import com.example.silentguardapp.services.MessageService
import com.example.silentguardapp.utils.PreferencesManager

class MessageController(context: Context) {

    private val preferencesManager = PreferencesManager(context)
    private val messageService = MessageService()

    /**
     * Generates the full encoded message from event
     * using transcribed text and cover message from settings.
     */
    fun getEncodedMessageFromEvent(event: EventModel): String {
        val transcribed = event.transcribedText
        val coverMessage = preferencesManager.loadAppSettings().coverMessage
        return messageService.generateCoverMessage(transcribed, coverMessage)
    }

    /**
     * Decode a received message
     */
    fun decodeMessage(encodedMessage: String): String {
        return messageService.decodeCoverMessage(encodedMessage)
    }
}
