package com.example.silentguardapp.model

/*
* Emergency Event Model
*/
data class EventModel(
    val id: String,                                  // Unique identifier
    val timestamp: Long,                             // Event time
    var audioRecord: AudioRecordModel?,              // Audio recording
    var transcribedText: String,                     // Transcribed text
    var encryptedMessage: EncryptedMessageModel?,    // Encrypted message
    val location: String?,                           // Location
    var sentToContact: ContactModel?                 // Contact that received the message
)
