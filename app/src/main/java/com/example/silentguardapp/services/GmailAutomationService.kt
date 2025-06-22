package com.example.silentguardapp.services

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.silentguardapp.R

/**
 * GmailAutomationService - Accessibility service that automates the process of:
 * 1. Clicking Gmail in the chooser screen
 * 2. Sending the composed email silently
 * 3. Closing the Gmail compose screen
 */
class GmailAutomationService : AccessibilityService() {

    private var hasClickedGmail = false // Prevent repeated chooser clicks
    private var hasClickedSend = false  // Prevent repeated send clicks
    private var sendAttempts = 0
    private val maxSendAttempts = 5
    private val sendRetryInterval = 500L // 500 milliseconds
    private val handler = Handler(Looper.getMainLooper())


    /**
     * Triggered when the system generates accessibility events (UI changes, etc).
     */
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null ||
            (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
                    event.eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED)) return

        val rootNode = rootInActiveWindow ?: return

        // Look for exact Gmail option and avoid clicking on Quick Share
        if (!hasClickedGmail) {
            val nodes = rootNode.findAccessibilityNodeInfosByText("Gmail")
            for (node in nodes) {
                // Ensure the node is exactly Gmail, and not inside "Quick Share"
                if (node.className == "android.widget.TextView" && node.text == "Gmail") {
                    val parent = findClickableParent(node, 5)

                    // Extra validation: parent should contain only one text child saying "Gmail"
                    val siblingTextNodes = parent?.findAccessibilityNodeInfosByText(getString(R.string.fast_share))
                    if (parent != null && parent.isClickable && parent.isEnabled &&
                        (siblingTextNodes == null || siblingTextNodes.isEmpty())) {

                        Log.d("GmailAutomationService", "Clicking real Gmail option")
                        parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        hasClickedGmail = true
                        sendAttempts = 0
                        attemptSendLoop()
                        return
                    }
                }
            }
            return
        }

        // Step 2: Retry loop to send email
        if (hasClickedGmail && !hasClickedSend && sendAttempts == 0) {
            sendAttempts = 0
            attemptSendLoop()
        }
    }

    /**
     * Helper function to find clickable parent within a number of levels.
     */
    private fun findClickableParent(node: AccessibilityNodeInfo?, maxLevels: Int): AccessibilityNodeInfo? {
        var current = node
        var level = 0
        while (current != null && !current.isClickable && level < maxLevels) {
            current = current.parent
            level++
        }
        return current
    }



    /**
     * Attempts to find and click the "SEND" button inside Gmail compose screen.
     */
//    private fun attemptSend() {
//        val rootNode = rootInActiveWindow ?: return
//
//        val sendButtons = rootNode.findAccessibilityNodeInfosByViewId("com.google.android.gm:id/send")
//        for (node in sendButtons) {
//            if (node.className == "android.widget.Button" &&
//                node.isClickable && node.isEnabled &&
//                node.contentDescription == getString(R.string.send)) {
//
//                Log.d("GmailAutomationService", "Clicking SEND button...")
//                node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
//                hasClickedSend = true
//
//                // After sending, attempt to close the compose screen
//                Handler(Looper.getMainLooper()).postDelayed({
//                    closeComposeScreen()
//                }, 1500)
//
//                return
//            }
//        }
//    }

    private fun attemptSendLoop() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                val rootNode = rootInActiveWindow ?: return

                val sendButtons = rootNode.findAccessibilityNodeInfosByViewId("com.google.android.gm:id/send")
                for (node in sendButtons) {
                    if (node.className == "android.widget.Button" &&
                        node.isClickable && node.isEnabled &&
                        node.contentDescription == getString(R.string.send)) {

                        Log.d("GmailAutomationService", "Clicking SEND button (attempt $sendAttempts)...")
                        node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        hasClickedSend = true

                        handler.postDelayed({
                            closeComposeScreen()
                        }, 1500)

                        return
                    }
                }

                sendAttempts++
                if (sendAttempts < maxSendAttempts && !hasClickedSend) {
                    handler.postDelayed(this, sendRetryInterval)
                } else {
                    Log.w("GmailAutomationService", "SEND button not found after $sendAttempts attempts.")
                }
            }
        }, sendRetryInterval)
    }

    /**
     * Attempts to find and click the "navigate up" (back) button in Gmail
     * to close the compose screen after sending.
     */
    private fun closeComposeScreen() {
        val rootNode = rootInActiveWindow ?: return

        val navUpNodes = rootNode.findAccessibilityNodeInfosByText(getString(R.string.navigate_up))
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
