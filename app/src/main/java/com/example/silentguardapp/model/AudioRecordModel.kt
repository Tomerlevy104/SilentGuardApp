package com.example.silentguardapp.model

/*
* Audio Record Model
*/
data class AudioRecordModel(
    val id: String,              // Unique identifier
    val filePath: String,        // File path on device
    val duration: Int,           // Recording duration in seconds
)
