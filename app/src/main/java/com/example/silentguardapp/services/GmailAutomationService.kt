package com.example.silentguardapp.services

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * GmailAutomationService - Accessibility service that automates the process of:
 * 1. Clicking Gmail in the chooser screen
 * 2. Sending the composed email silently
 * 3. Closing the Gmail compose screen
 */
class GmailAutomationService : AccessibilityService() {

    private var hasClickedGmail = false // Prevent repeated chooser clicks
    private var hasClickedSend = false  // Prevent repeated send clicks

    /**
     * Triggered when the system generates accessibility events (UI changes, etc).
     */
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null ||
            (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
                    event.eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED)) return

        val rootNode = rootInActiveWindow ?: return

        // Step 1: Automatically click on Gmail option in chooser screen
        if (!hasClickedGmail) {
            val gmailNodes = rootNode.findAccessibilityNodeInfosByText("Gmail")
            for (node in gmailNodes) {
                if (node.className == "android.widget.TextView" && node.text == "Gmail") {
                    var parent: AccessibilityNodeInfo? = node
                    var level = 0

                    // Traverse upward to find a clickable parent
                    while (parent != null && !parent.isClickable && level < 5) {
                        parent = parent.parent
                        level++
                    }

                    if (parent != null && parent.isClickable && parent.isEnabled) {
                        Log.d("GmailAutomationService", "Clicking Gmail option...")
                        parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        hasClickedGmail = true

                        // Add short delay before trying to send
                        Handler(Looper.getMainLooper()).postDelayed({
                            attemptSend()
                        }, 1500)

                        return
                    }
                }
            }
            return
        }

        // Step 2 (fallback): If Gmail already clicked, continuously try to send
        if (hasClickedGmail && !hasClickedSend) {
            attemptSend()
        }
    }

    /**
     * Attempts to find and click the "SEND" button inside Gmail compose screen.
     */
    private fun attemptSend() {
        val rootNode = rootInActiveWindow ?: return

        val sendButtons = rootNode.findAccessibilityNodeInfosByViewId("com.google.android.gm:id/send")
        for (node in sendButtons) {
            if (node.className == "android.widget.Button" &&
                node.isClickable && node.isEnabled &&
                node.contentDescription == "שליחה") {

                Log.d("GmailAutomationService", "Clicking SEND button...")
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                hasClickedSend = true

                // Step 3: After sending, attempt to close the compose screen
                Handler(Looper.getMainLooper()).postDelayed({
                    closeComposeScreen()
                }, 1500)

                return
            }
        }
    }

    /**
     * Attempts to find and click the "navigate up" (back) button in Gmail
     * to close the compose screen after sending.
     */
    private fun closeComposeScreen() {
        val rootNode = rootInActiveWindow ?: return

        val navUpNodes = rootNode.findAccessibilityNodeInfosByText("ניווט למעלה")
        for (node in navUpNodes) {
            if (node.className == "android.widget.ImageButton" &&
                node.isClickable && node.isEnabled) {

                Log.d("GmailAutomationService", "Clicking Back button to close Gmail compose...")
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                return
            }
        }
    }

    /**
     * Called when the service is connected and ready.
     */
    override fun onServiceConnected() {
        super.onServiceConnected()
        hasClickedGmail = false
        hasClickedSend = false
    }

    /**
     * Required override for accessibility service interruptions.
     */
    override fun onInterrupt() {
        hasClickedGmail = false
        hasClickedSend = false
    }
}
