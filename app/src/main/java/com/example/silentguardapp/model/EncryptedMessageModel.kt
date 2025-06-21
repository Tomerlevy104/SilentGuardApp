package com.example.silentguardapp.model

/*
* Encrypted Message Model
*/
data class EncryptedMessageModel(
    val originalText: String,    // Original text before encryption
    val encryptedText: String,   // Encrypted text with zero-width characters
    val coverMessage: String,    // Visible cover message
)