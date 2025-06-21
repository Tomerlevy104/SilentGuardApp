package com.example.silentguardapp.controller

import android.content.Context
import com.example.silentguardapp.model.EventModel
import com.example.silentguardapp.services.EventService

class EventController(context: Context) {

    private val eventService = EventService(context)

    fun createEmergencyEvent(): EventModel {
        return eventService.createEmergencyEvent()
    }

    fun startRecordingForEvent(eventId: String): Boolean {
        return eventService.startRecordingForEvent(eventId)
    }

    fun finalizeEvent(eventId: String): Boolean {
        return eventService.finalizeEvent(eventId)
    }
}