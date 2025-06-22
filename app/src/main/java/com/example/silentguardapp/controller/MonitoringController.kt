package com.example.silentguardapp.controller

import com.example.silentguardapp.services.MonitoringService
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * MonitoringController - Simple controller for managing background noise monitoring
 * Purpose: Start/stop monitoring service and manage monitoring settings
 */
class MonitoringController(private val context: Context) {

    private var isMonitoringActive = false
    private var defaultThreshold = 0.7f

    /**
     * Start background noise monitoring
     */
    fun startMonitoring(threshold: Float = defaultThreshold): Boolean {
        try {
            // If already monitoring
            if (isMonitoringActive) {
                Log.d("MonitoringController", "Monitoring is already active")
                return false
            }

            val intent = Intent(context, MonitoringService::class.java).apply {
                action =
                    MonitoringService.ACTION_START_MONITORING // calling to "onStartCommand" function
                putExtra(MonitoringService.EXTRA_THRESHOLD, threshold)
            }

            context.startForegroundService(intent) // ! ! ! Start foreground service - monitoring background ! ! !
            isMonitoringActive = true

            Log.d("MonitoringController", "Monitoring started with threshold: $threshold")
            return true

        } catch (e: Exception) {
            Log.e("MonitoringController", "Failed to start monitoring: ${e.message}")
            return false
        }
    }

    /**
     * Stop background noise monitoring
     */
    fun stopMonitoring(): Boolean {
        try {
            if (!isMonitoringActive) {
                Log.d("MonitoringController", "Monitoring is not active")
                return false
            }

            val intent = Intent(context, MonitoringService::class.java).apply {
                action = MonitoringService.ACTION_STOP_MONITORING // calling to "onStartCommand" function
            }

            context.startService(intent)
            isMonitoringActive = false

            Log.d("MonitoringController", "Monitoring stopped")
            return true

        } catch (e: Exception) {
            Log.e("MonitoringController", "Failed to stop monitoring: ${e.message}")
            return false
        }
    }

    /**
     * Check if monitoring is currently active
     */
    fun isMonitoringActive(): Boolean {
        return isMonitoringActive
    }
}