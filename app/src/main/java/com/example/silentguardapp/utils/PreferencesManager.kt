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
        try {
            val json = gson.toJson(settings)
            sharedPreferences.edit()
                .putString(KEY_APP_SETTINGS, json)
                .apply()
            return true
        } catch (e: Exception) {
            return false
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
    private fun getDefaultAppSettings(): AppSettingsModel {
        return AppSettingsModel(
            noiseThreshold = 0.7f,
            recordingDuration = 15,
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
        try {
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
            return true
        } catch (e: Exception) {
            return false
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
}