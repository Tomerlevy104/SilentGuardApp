package com.example.silentguardapp.views

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.silentguardapp.R
import com.example.silentguardapp.controller.MonitoringController
import com.example.silentguardapp.utils.PreferencesManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.textview.MaterialTextView

class HomeFragment : Fragment() {

    private lateinit var monitoringController: MonitoringController
    private lateinit var statusText: MaterialTextView
    private lateinit var statusIndicator: View
    private lateinit var btnStartMonitoring: MaterialButton
    private lateinit var btnStopMonitoring: MaterialButton
    private lateinit var preferencesManager: PreferencesManager
    private var hasPromptedAccessibility = false

    // Permission launcher for microphone access
    private val requestMicPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
//        if (!isGranted) {
//            Toast.makeText(requireContext(), getString(R.string.microphone_permission_is_required), Toast.LENGTH_LONG).show()
//        }
        checkAllPermissionsAndStartMonitoring()
    }

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            Log.w("HomeFragment", "Notification permission denied")
        }
        checkAllPermissionsAndStartMonitoring()
    }

    private val requestSmsPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            Toast.makeText(requireContext(), "SMS permission is required for emergency alerts", Toast.LENGTH_SHORT).show()
        }
        checkAllPermissionsAndStartMonitoring()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initializeViews(view)
        preferencesManager = PreferencesManager(requireContext())
        setupMonitoringController()
        setupButtonListeners()
        updateUI()
    }

    private fun initializeViews(view: View) {
        statusText = view.findViewById(R.id.status_text)
        statusIndicator = view.findViewById(R.id.status_indicator)
        btnStartMonitoring = view.findViewById(R.id.btn_start_monitoring)
        btnStopMonitoring = view.findViewById(R.id.btn_stop_monitoring)
    }

    private fun setupMonitoringController() {
        monitoringController = MonitoringController(requireContext())
    }

    private fun setupButtonListeners() {
        btnStartMonitoring.setOnClickListener {
            checkAllPermissionsAndStartMonitoring()
        }

        btnStopMonitoring.setOnClickListener {
            stopMonitoring()
        }
    }

    private fun checkAllPermissionsAndStartMonitoring() {
        // Check microphone permission
        val hasMicPermission = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        // Check notifications permission
        val hasNotificationPermission = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        // Check SMS permission
        val hasSmsPermission = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED

        // Check contact settings

        val hasContact = preferencesManager.hasEmergencyContact()
        Log.d("HomeFragment", "Has contact: $hasContact")

        // Permission checks
        when {
            !hasMicPermission -> {
                requestMicPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }

            !hasNotificationPermission && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU -> {
                requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }

            !hasSmsPermission -> {
                requestSmsPermissionLauncher.launch(Manifest.permission.SEND_SMS)
            }

            !hasContact -> {
                Toast.makeText(
                    requireContext(),
                    "Emergency contact must have at least phone number or email.",
                    Toast.LENGTH_LONG
                ).show()
            }

            !isAccessibilityServiceEnabled() -> {
                if (!hasPromptedAccessibility) {
                    hasPromptedAccessibility = true
                    Toast.makeText(
                        requireContext(),
                        "Accessibility service is required for automatic email/SMS sending.",
                        Toast.LENGTH_LONG
                    ).show()

                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    startActivity(intent)
                }
            }

            else -> {
                // All permissions and settings are valid – proceed
                startMonitoringProcess()
            }
        }
    }


    /**
     * Checks if the GmailAutomationService accessibility service is enabled.
     */
    private fun isAccessibilityServiceEnabled(): Boolean {
        val expectedComponent = ComponentName(requireContext(), com.example.silentguardapp.services.GmailAutomationService::class.java)
        val enabledServicesSetting = Settings.Secure.getString(
            requireContext().contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServicesSetting)

        for (service in colonSplitter) {
            if (ComponentName.unflattenFromString(service) == expectedComponent) {
                return true
            }
        }
        return false
    }

    /**
     * Prompts user to enable the accessibility service if not active.
     */
    private fun checkAccessibilityService() {
        if (!isAccessibilityServiceEnabled()) {
            Toast.makeText(
                requireContext(),
                "Accessibility service is not enabled. Please enable it for automatic Gmail sending.",
                Toast.LENGTH_LONG
            ).show()

            // Opens the device Accessibility Settings screen
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
        }
    }



    private fun startMonitoringProcess() {
        val thresholdFromSettings = preferencesManager.loadAppSettings().noiseThreshold
        // Request for start listening
        val successStart = monitoringController.startMonitoring(thresholdFromSettings)

        if (successStart) {
            Log.d("HomeFragment", "Monitoring started successfully with threshold $thresholdFromSettings")
            Toast.makeText(requireContext(),
                getString(R.string.monitoring_started_successfully), Toast.LENGTH_SHORT).show()
        } else {
            Log.e("HomeFragment", "Failed to start monitoring")
            Toast.makeText(requireContext(),
                getString(R.string.error_starting_monitoring), Toast.LENGTH_SHORT).show()
        }

        updateUI()
    }

    private fun stopMonitoring() {
        // Request for stop listening
        val successStop = monitoringController.stopMonitoring()

        if (successStop) {
            Log.d("HomeFragment", "Monitoring stopped successfully")
            Toast.makeText(requireContext(),
                getString(R.string.monitoring_stopped), Toast.LENGTH_SHORT).show()
        } else {
            Log.e("HomeFragment", "Failed to stop monitoring")
            Toast.makeText(requireContext(),
                getString(R.string.error_in_stopping_monitoring), Toast.LENGTH_SHORT).show()
        }

        updateUI()
    }

    private fun updateUI() {
        val isActive = monitoringController.isMonitoringActive()
        // Update status text
        statusText.text = if (isActive) {
            getString(R.string.active)
        } else {
            getString(R.string.not_active)
        }

        // Update status text color and indicator
        if (isActive) {
            statusText.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark))
            statusIndicator.setBackgroundResource(R.drawable.circle_green)
        } else {
            statusText.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.holo_red_dark))
            statusIndicator.setBackgroundResource(R.drawable.circle_red)
        }

        // Update buttons state
        btnStartMonitoring.isEnabled = !isActive
        btnStopMonitoring.isEnabled = isActive

        Log.d("HomeFragment", "UI updated - is active: $isActive")
    }

    override fun onResume() {
        super.onResume()
        // Update UI when returning to fragment
        updateUI()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Optional: Stop monitoring when fragment is destroyed
        // monitoringController.forceStopMonitoring()
    }
}