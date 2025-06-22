package com.example.silentguardapp.views

import android.os.Bundle
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.silentguardapp.R
import com.example.silentguardapp.model.AppSettingsModel
import com.example.silentguardapp.model.ContactModel
import com.example.silentguardapp.utils.PreferencesManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.slider.Slider
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class SettingsFragment : Fragment() {

    private lateinit var thresholdSlider: Slider
    private lateinit var durationSlider: Slider
    private lateinit var contactNameInput: TextInputEditText
    private lateinit var contactPhoneInput: TextInputEditText
    private lateinit var contactEmailInput: TextInputEditText
    private lateinit var coverMessageInput: TextInputEditText
    private lateinit var contactEmailLayout: TextInputLayout
    private lateinit var contactPhoneLayout: TextInputLayout
    private lateinit var preferencesManager: PreferencesManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        // Bind views
        thresholdSlider = view.findViewById(R.id.thresholdSlider)
        durationSlider = view.findViewById(R.id.durationSlider)
        contactNameInput = view.findViewById(R.id.contactNameInput)
        contactPhoneInput = view.findViewById(R.id.contactPhoneInput)
        contactEmailInput = view.findViewById(R.id.contactEmailInput)
        coverMessageInput = view.findViewById(R.id.coverMessageInput)
        contactEmailLayout = view.findViewById(R.id.contactEmailLayout)
        contactPhoneLayout = view.findViewById(R.id.contactPhoneLayout)

        val saveButton = view.findViewById<MaterialButton>(R.id.saveSettingsButton)
        saveButton.setOnClickListener {
            saveSettings()
        }

        preferencesManager = PreferencesManager(requireContext())
        loadSettings()

        return view
    }

    private fun loadSettings() {
        val settings = preferencesManager.loadAppSettings()

        // Populate UI with loaded values
        thresholdSlider.value = settings.noiseThreshold
        durationSlider.value = settings.recordingDuration.toFloat()
        contactNameInput.setText(settings.emergencyContact.name)
        contactPhoneInput.setText(settings.emergencyContact.phoneNumber ?: "")
        contactEmailInput.setText(settings.emergencyContact.email ?: "")
        coverMessageInput.setText(settings.coverMessage)
    }

    private fun saveSettings() {
        // Get values
        val name = contactNameInput.text.toString().trim()
        val phone = contactPhoneInput.text.toString().trim()
        val email = contactEmailInput.text.toString().trim()
        val message = coverMessageInput.text.toString().trim()
        val threshold = thresholdSlider.value
        val duration = durationSlider.value.toInt()

        var hasError = false

        // Email validation
        if (email.isNotEmpty() && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            contactEmailLayout.error = "Invalid email address"
            hasError = true
        } else {
            contactEmailLayout.error = null
        }

        // Phone validation
        if (phone.isNotEmpty() && !phone.matches(Regex("^\\d{10}$"))) {
            contactPhoneLayout.error = "Phone must be 10 digits"
            hasError = true
        } else {
            contactPhoneLayout.error = null
        }

        if (hasError) {
            Toast.makeText(requireContext(), "Please fix input errors", Toast.LENGTH_SHORT).show()
            return
        }

        val contact = ContactModel(name = name, phoneNumber = phone, email = email)

        val settings = AppSettingsModel(
            noiseThreshold = threshold,
            recordingDuration = duration,
            emergencyContact = contact,
            coverMessage = message
        )

        val saved = preferencesManager.saveAppSettings(settings)

        if (saved) {
            Toast.makeText(requireContext(), "Settings saved", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "Failed to save settings", Toast.LENGTH_SHORT).show()
        }
    }
}
