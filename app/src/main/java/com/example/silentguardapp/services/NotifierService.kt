package com.example.silentguardapp.services

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.example.silentguardapp.R
import com.example.silentguardapp.model.ContactModel

/**
 * Notifier Service
 */
class NotifierService(private val context: Context) {

    fun sendSms(contact: ContactModel, message: String): Boolean {
        try {
            if (contact.phoneNumber.isNullOrBlank()) return false

            val smsUri = Uri.parse(context.getString(R.string.smsto, contact.phoneNumber))
            val smsIntent = Intent(Intent.ACTION_SENDTO, smsUri).apply {
                putExtra("sms_body", message)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            context.startActivity(smsIntent)

            Log.d("NotifierService", "SMS intent launched to ${contact.phoneNumber}")
            return true
        } catch (e: Exception) {
            Log.e("NotifierService", "Failed to launch SMS intent", e)
            return false
        }
    }

    fun sendEmail(contact: ContactModel, message: String): Boolean {
         try {
            if (contact.email.isNullOrBlank()) return false

            Log.d("NotifierService:", "Contact: $contact")
            Log.d("NotifierService:", "Message: $message")

            val emailIntent = Intent(Intent.ACTION_SEND).apply {
                type = "message/rfc822"
                putExtra(Intent.EXTRA_EMAIL, arrayOf(contact.email))
                putExtra(Intent.EXTRA_SUBJECT, "Emergency Message")
                putExtra(Intent.EXTRA_TEXT, message)
            }

            val chooser = Intent.createChooser(emailIntent, "Send Email").apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            context.startActivity(chooser)

            Log.d("NotifierService", "Email intent launched to ${contact.email}")
            return true
        } catch (e: Exception) {
            Log.e("NotifierService", "Failed to launch email intent", e)
            return false
        }
    }
}