package com.example.silentguardapp.utils

import android.content.Context
import android.util.Log
import com.example.silentguardapp.model.AudioRecordModel
import com.google.api.gax.core.FixedCredentialsProvider
import com.google.auth.oauth2.GoogleCredentials
import com.google.cloud.speech.v1.RecognitionAudio
import com.google.cloud.speech.v1.RecognitionConfig
import com.google.cloud.speech.v1.SpeechClient
import com.google.cloud.speech.v1.SpeechSettings
import com.google.protobuf.ByteString
import java.io.File

class SpeechToTextConverter(private val context: Context) {

    fun convertAudioToText(audioRecord: AudioRecordModel): String {
        return try {
            // Check if audio file exists
            val audioFile = File(audioRecord.filePath)
            if (!audioFile.exists()) {
                return "Error: Audio file not found"
            }

            // Read the Google Cloud credentials
            val credentialsStream = context.assets.open("google-services-speech.json")
            val credentials = GoogleCredentials.fromStream(credentialsStream)

            // Initialize Speech client
            val speechSettings = SpeechSettings.newBuilder()
                .setCredentialsProvider(FixedCredentialsProvider.create(credentials))
                .build()

            val speechClient = SpeechClient.create(speechSettings)

            // Read audio file
            val audioBytes = audioFile.readBytes()
            val audioContent = ByteString.copyFrom(audioBytes)

            // Configure recognition
            val recognitionConfig = RecognitionConfig.newBuilder()
                .setEncoding(RecognitionConfig.AudioEncoding.AMR)  // Match your recording format
                .setSampleRateHertz(8000)    // AMR sample rate
//                .setLanguageCode("en-US")  // For English
                .setLanguageCode("he-IL")    // For Hebrew
                .setMaxAlternatives(1)
                .setEnableAutomaticPunctuation(true)
                .build()

            // Create the audio object
            val audio = RecognitionAudio.newBuilder()
                .setContent(audioContent)
                .build()

            // Perform the recognition
            val response = speechClient.recognize(recognitionConfig, audio)
            speechClient.close()

            // Extract the transcription
            val results = response.resultsList
            if (results.isNotEmpty() && results[0].alternativesCount > 0) {
                val transcription = results[0].getAlternatives(0).transcript
                if (transcription.isNotBlank()) {
                    return transcription.trim()
                }
            }

            // Return default message if no transcription
            "Emergency situation detected - unable to transcribe audio"

        } catch (e: Exception) {
            // Log error and return fallback message
            Log.d("AudioService", "Speech-to-Text error: ${e.message}", e)
            "Emergency situation - transcription failed: ${e.message}"
        }
    }
}