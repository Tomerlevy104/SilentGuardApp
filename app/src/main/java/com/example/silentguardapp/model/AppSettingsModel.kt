package com.example.silentguardapp.model

/*
* App Setting Model
*/
data class AppSettingsModel(
    val noiseThreshold: Float,              // Noise threshold (0.0 - 1.0)
    val recordingDuration: Int,             // Recording duration in seconds
    val emergencyContact: ContactModel,     // Emergency contacts
    val coverMessage: String,               // Default cover message
)
