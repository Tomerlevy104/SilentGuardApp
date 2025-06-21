package com.example.silentguardapp.controller

import android.content.Context
import android.util.Log
import com.example.silentguardapp.model.EventModel
import com.example.silentguardapp.services.NotifierService

class NotifierController(context: Context) {

    private val notifierService = NotifierService(context)

    /**
     * Try to send message (SMS and/or Email) to contact from event.
     */
    fun notifyContact(event: EventModel): Boolean {
        val contact = event.sentToContact
        val encryptedMessage = event.encryptedMessage?.encryptedText

        Log.d("NotifierController", "Contact: $contact")
        Log.d("NotifierController", "The encrypted message: $encryptedMessage")
        if (contact == null || encryptedMessage.isNullOrBlank()) {
            Log.w("NotifierController", "Missing contact or message. Cannot notify")
            return false
        }

        var success = false

        // Send SMS if phone number exists
        if (!contact.phoneNumber.isNullOrBlank()) {
            val smsSuccess = notifierService.sendSms(contact, encryptedMessage)
            if (smsSuccess) {
                Log.d("NotifierController", "SMS sent successfully")
                success = true
            } else {
                Log.w("NotifierController", "Failed to send SMS")
            }
        }

        // Send Email if email exists
        if (!contact.email.isNullOrBlank()) {
            val emailSuccess = notifierService.sendEmail(contact, encryptedMessage)
            if (emailSuccess) {
                Log.d("NotifierController", "Email intent launched successfully")
                success = true
            } else {
                Log.w("NotifierController", "Failed to send Email")
            }
        }

        if (!success) {
            Log.e("NotifierController", "Failed to send notification - no valid contact method")
        }

        return success
    }
}


