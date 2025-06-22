package com.example.silentguardapp.utils

import android.content.Context
import android.content.SharedPreferences
import com.example.silentguardapp.model.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

/**
 * PreferencesManager - Handles local storage of app data
 */
class PreferencesManager(private val context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        private const val PREFS_NAME = "silent_guard_prefs"
        private const val KEY_APP_SETTINGS = "app_settings"
        private const val KEY_EVENTS = "emergency_events"
        private const val AUDIO_DIR_NAME = "emergency_audio"
    }

    /**
     * Save app settings
     */
    fun saveAppSettings(settings: AppSettingsModel): Boolean {
        return try {
            val json = gson.toJson(settings)
            sharedPreferences.edit()
                .putString(KEY_APP_SETTINGS, json)
                .apply()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Load app settings
     */
    fun loadAppSettings(): AppSettingsModel {
        return try {
            val json = sharedPreferences.getString(KEY_APP_SETTINGS, null)
            if (json != null) {
                gson.fromJson(json, AppSettingsModel::class.java)
            } else {
                getDefaultAppSettings()
            }
        } catch (e: Exception) {
            getDefaultAppSettings()
        }
    }

    /**
     * Get default app settings
     */
     fun getDefaultAppSettings(): AppSettingsModel {
        return AppSettingsModel(
            noiseThreshold = 0.7f,
            recordingDuration = 30,
            emergencyContact = ContactModel(
                name = "",
                phoneNumber = null,
                email = null
            ),
            coverMessage = "Hey, how are you? I'm doing fine here."
        )
    }

    /**
     * Save emergency event
     */
    fun saveEmergencyEvent(event: EventModel): Boolean {
        return try {
            val events = loadAllEvents().toMutableList()

            // Remove existing event with same ID if exists
            events.removeAll { it.id == event.id }

            // Add new/updated event
            events.add(event)

            // Keep only recent events (max 50)
            val recentEvents = events.sortedByDescending { it.timestamp }.take(50)

            val json = gson.toJson(recentEvents)
            sharedPreferences.edit()
                .putString(KEY_EVENTS, json)
                .apply()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Load all emergency events
     */
    private fun loadAllEvents(): List<EventModel> {
        return try {
            val json = sharedPreferences.getString(KEY_EVENTS, null)
            if (json != null) {
                val type = object : TypeToken<List<EventModel>>() {}.type
                gson.fromJson<List<EventModel>>(json, type) ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Get emergency event by ID
     */
    fun getEventById(eventId: String): EventModel? {
        return try {
            loadAllEvents().find { it.id == eventId }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Delete emergency event
     */
    fun deleteEvent(eventId: String): Boolean {
        return try {
            val events = loadAllEvents().toMutableList()
            val removed = events.removeAll { it.id == eventId }

            if (removed) {
                val json = gson.toJson(events)
                sharedPreferences.edit()
                    .putString(KEY_EVENTS, json)
                    .apply()
            }

            removed
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Save audio record for specific event
     */
    fun saveAudioRecordForEvent(eventId: String, audioRecord: AudioRecordModel): Boolean {
        return try {
            val event = getEventById(eventId)
            if (event != null) {
                val updatedEvent = event.copy(audioRecord = audioRecord)
                saveEmergencyEvent(updatedEvent)
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get recent events (limited number)
     */
    fun getRecentEvents(limit: Int = 10): List<EventModel> {
        return try {
            loadAllEvents()
                .sortedByDescending { it.timestamp }
                .take(limit)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Clear all events
     */
    fun clearAllEvents(): Boolean {
        return try {
            sharedPreferences.edit()
                .remove(KEY_EVENTS)
                .apply()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get audio storage directory
     */
    private fun getAudioStorageDirectory(): String {
        val audioDir = File(context.filesDir, AUDIO_DIR_NAME)
        if (!audioDir.exists()) {
            audioDir.mkdirs()
        }
        return audioDir.absolutePath
    }

    /**
     * Clean old events (older than specified days)
     */
    fun cleanOldEvents(maxAgeDays: Int = 7): Int {
        return try {
            val cutoffTime = System.currentTimeMillis() - (maxAgeDays * 24 * 60 * 60 * 1000L)
            val allEvents = loadAllEvents()
            val recentEvents = allEvents.filter { it.timestamp >= cutoffTime }
            val deletedCount = allEvents.size - recentEvents.size

            if (deletedCount > 0) {
                val json = gson.toJson(recentEvents)
                sharedPreferences.edit()
                    .putString(KEY_EVENTS, json)
                    .apply()
            }

            deletedCount
        } catch (e: Exception) {
            0
        }
    }

    /**
     * Get total storage usage
     */
    fun getTotalStorageUsage(): Long {
        return try {
            val audioDir = File(getAudioStorageDirectory())
            var totalSize = 0L

            audioDir.listFiles()?.forEach { file ->
                totalSize += file.length()
            }

            totalSize
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * Check if emergency contact is configured
     */
    fun hasEmergencyContact(): Boolean {
        return try {
            val settings = loadAppSettings()
            val contact = settings.emergencyContact
            contact.name.isNotBlank() &&
            (!contact.phoneNumber.isNullOrBlank() && !contact.email.isNullOrBlank())
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Update only the emergency contact
     */
    fun updateEmergencyContact(contact: ContactModel): Boolean {
        return try {
            val currentSettings = loadAppSettings()
            val updatedSettings = currentSettings.copy(emergencyContact = contact)
            saveAppSettings(updatedSettings)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Reset all settings to default
     */
    fun resetToDefaults(): Boolean {
        return try {
            sharedPreferences.edit().clear().apply()
            true
        } catch (e: Exception) {
            false
        }
    }
}