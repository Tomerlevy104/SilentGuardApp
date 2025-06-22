package com.example.silentguardapp.services

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * SmsAutomationService - Accessibility service that automates the process of:
 * 1. Clicking the "Send" button in the default SMS app (e.g., Samsung Messages)
 * 2. Returning to the original application after sending the SMS
 */
class SmsAutomationService : AccessibilityService() {

    // Flag to prevent repeated clicks
    private var hasClickedSend = false

    /**
     * Triggered when the system generates accessibility events (UI changes, etc).
     */
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null ||
            (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
                    event.eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED)) return

        val rootNode = rootInActiveWindow ?: return

        // Search for the "Send" button in the SMS app
        val sendButtons = rootNode.findAccessibilityNodeInfosByViewId("com.samsung.android.messaging:id/send_button")
        for (node in sendButtons) {
            if (node.className == "android.widget.ImageButton" &&
                node.isClickable && node.isEnabled &&
                node.contentDescription == "שלח") {

                Log.d("SmsAutomationService", "Clicking SMS SEND button...")
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                hasClickedSend = true

                // Try to go back to the app after sending (optional)
                Handler(Looper.getMainLooper()).postDelayed({
                    returnToApp()
                }, 1500)

                return
            }
        }
    }

    /**
     * Attempts to return to the original app after sending SMS by simulating a back action.
     */
    private fun returnToApp() {
        Log.d("SmsAutomationService", "Returning to SilentGuard app via global back action")
        performGlobalAction(GLOBAL_ACTION_BACK)
        hasClickedSend = false
    }

    /**
     * Called when the service is connected and ready.
     */
    override fun onServiceConnected() {
        super.onServiceConnected()
        hasClickedSend = false
        Log.d("SmsAutomationService", "Accessibility service connected")
    }

    /**
     * Required override for accessibility service interruptions.
     */
    override fun onInterrupt() {
        hasClickedSend = false
    }
}
