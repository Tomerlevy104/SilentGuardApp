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
    private val maxSendAttempts = 10
    private val sendRetryInterval = 1000L // 1 second
    private val handler = Handler(Looper.getMainLooper())

    /**
     * Triggered when the system generates accessibility events (UI changes, etc).
     */
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val packageName = event.packageName?.toString()
        Log.d(
            "GmailAutomationService",
            "Event from package: $packageName, EventType: ${event.eventType}"
        )

        val rootNode = rootInActiveWindow ?: return

        when (packageName) {
            "android" -> {
                // Handle chooser screen
                if (!hasClickedGmail) {
                    handleChooserScreen(rootNode)
                }
            }

            "com.google.android.gm" -> {
                // Handle Gmail app
                if (hasClickedGmail && !hasClickedSend) {
                    Log.d("GmailAutomationService", "In Gmail app, attempting to send...")
                    attemptSendLoop()
                }
            }
        }
    }

    private fun handleChooserScreen(rootNode: AccessibilityNodeInfo) {
        val nodes = rootNode.findAccessibilityNodeInfosByText("Gmail")
        for (node in nodes) {
            // Ensure the node is exactly Gmail, and not inside "Quick Share"
            if (node.className == "android.widget.TextView" && node.text == "Gmail") {
                val parent = findClickableParent(node, 5)

                // Extra validation: parent should contain only one text child saying "Gmail"
                val siblingTextNodes =
                    parent?.findAccessibilityNodeInfosByText(getString(R.string.fast_share))
                if (parent != null && parent.isClickable && parent.isEnabled &&
                    (siblingTextNodes == null || siblingTextNodes.isEmpty())
                ) {

                    Log.d("GmailAutomationService", "Clicking real Gmail option")
                    parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    hasClickedGmail = true
                    sendAttempts = 0
                    return
                }
            }
        }
    }

    /**
     * Helper function to find clickable parent within a number of levels.
     */
    private fun findClickableParent(
        node: AccessibilityNodeInfo?,
        maxLevels: Int
    ): AccessibilityNodeInfo? {
        var current = node
        var level = 0
        while (current != null && !current.isClickable && level < maxLevels) {
            current = current.parent
            level++
        }
        return current
    }

    private fun attemptSendLoop() {
        if (hasClickedSend) return // Already sent

        handler.postDelayed(object : Runnable {
            override fun run() {
                if (hasClickedSend) return

                val rootNode = rootInActiveWindow ?: return

                // Try multiple methods to find the send button
                var sendButton: AccessibilityNodeInfo? = null

                // Method 1: Find by resource ID
                val sendButtonsById =
                    rootNode.findAccessibilityNodeInfosByViewId("com.google.android.gm:id/send")
                for (node in sendButtonsById) {
                    if (node.className == "android.widget.Button" && node.isClickable && node.isEnabled) {
                        sendButton = node
                        Log.d(
                            "GmailAutomationService",
                            "Found send button by ID: ${node.contentDescription}"
                        )
                        break
                    }
                }

                // Method 2: Find by content description if Method 1 failed
                if (sendButton == null) {
                    val sendButtonsByDesc =
                        rootNode.findAccessibilityNodeInfosByText(getString(R.string.send))
                    for (node in sendButtonsByDesc) {
                        if (node.className == "android.widget.Button" && node.isClickable && node.isEnabled) {
                            sendButton = node
                            Log.d("GmailAutomationService", "Found send button by description")
                            break
                        }
                    }
                }

                // Method 3: Find by traversing all buttons
                if (sendButton == null) {
                    sendButton = findSendButtonRecursively(rootNode)
                }

                if (sendButton != null) {
                    Log.d(
                        "GmailAutomationService",
                        "Clicking SEND button (attempt ${sendAttempts + 1})..."
                    )
                    val success = sendButton.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                    Log.d("GmailAutomationService", "Send click result: $success")

                    if (success) {
                        hasClickedSend = true
                        // Give Gmail time to process the send
                        handler.postDelayed({
                            closeComposeScreen()
                        }, 2000)
                        return
                    }
                }

                sendAttempts++
                if (sendAttempts < maxSendAttempts && !hasClickedSend) {
                    Log.d(
                        "GmailAutomationService",
                        "Retrying send button search (attempt $sendAttempts)"
                    )
                    handler.postDelayed(this, sendRetryInterval)
                } else {
                    Log.w(
                        "GmailAutomationService",
                        "SEND button not found after $sendAttempts attempts."
                    )
                }
            }
        }, sendRetryInterval)
    }

    private fun findSendButtonRecursively(node: AccessibilityNodeInfo?): AccessibilityNodeInfo? {
        if (node == null) return null

        // Check if this node is a send button
        if (node.className == "android.widget.Button" && node.isClickable && node.isEnabled) {
            val contentDesc = node.contentDescription?.toString()
            val text = node.text?.toString()
            val viewId = node.viewIdResourceName

            if (contentDesc?.contains(getString(R.string.send_heb)) == true ||
                text?.contains(getString(R.string.send_heb)) == true ||
                viewId?.contains(getString(R.string.send)) == true
            ) {
                Log.d(
                    "GmailAutomationService",
                    "Found send button recursively: desc=$contentDesc, text=$text, id=$viewId"
                )
                return node
            }
        }

        // Recursively search children
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            val result = findSendButtonRecursively(child)
            if (result != null) {
                return result
            }
        }

        return null
    }

    /**
     * Attempts to find and click the "navigate up" (back) button in Gmail
     * to close the compose screen after sending.
     */
    private fun closeComposeScreen() {
        val rootNode = rootInActiveWindow ?: return

        // Try to find the back button by content description
        val navUpNodes = rootNode.findAccessibilityNodeInfosByText(getString(R.string.navigate_up))
        for (node in navUpNodes) {
            if (node.className == "android.widget.ImageButton" &&
                node.isClickable && node.isEnabled
            ) {

                Log.d("GmailAutomationService", "Clicking Back button to close Gmail compose...")
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                return
            }
        }

        // Alternative: Look for back button by class and position
        val imageButtons = findNodesByClass(rootNode, "android.widget.ImageButton")
        for (button in imageButtons) {
            if (button.contentDescription?.contains(getString(R.string.nav)) == true ||
                button.contentDescription?.contains(getString(R.string.up)) == true
            ) {
                Log.d("GmailAutomationService", "Found alternative back button")
                button.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                return
            }
        }
    }

    private fun findNodesByClass(
        node: AccessibilityNodeInfo?,
        className: String
    ): List<AccessibilityNodeInfo> {
        val result = mutableListOf<AccessibilityNodeInfo>()
        if (node == null) return result

        if (node.className == className) {
            result.add(node)
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            result.addAll(findNodesByClass(child, className))
        }

        return result
    }

    /**
     * Called when the service is connected and ready.
     */
    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d("GmailAutomationService", "Service connected")
        hasClickedGmail = false
        hasClickedSend = false
        sendAttempts = 0
    }

    /**
     * Required override for accessibility service interruptions.
     */
    override fun onInterrupt() {
        Log.d("GmailAutomationService", "Service interrupted")
        hasClickedGmail = false
        hasClickedSend = false
        sendAttempts = 0
    }
}