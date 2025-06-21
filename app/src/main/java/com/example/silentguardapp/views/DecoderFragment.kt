package com.example.silentguardapp.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.silentguardapp.R
import com.example.silentguardapp.controller.MessageController
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import android.widget.TextView

class DecoderFragment : Fragment() {

    private lateinit var messageController: MessageController
    private lateinit var encryptedInput: TextInputEditText
    private lateinit var decodeButton: MaterialButton
    private lateinit var decodedOutput: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_decoder, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize controller
        messageController = MessageController(requireContext())

        // Bind UI elements
        encryptedInput = view.findViewById(R.id.encryptedInput)
        decodeButton = view.findViewById(R.id.decodeButton)
        decodedOutput = view.findViewById(R.id.decodedOutput)

        // Set button click listener
        decodeButton.setOnClickListener {
            val inputText = encryptedInput.text?.toString().orEmpty()

            if (inputText.isBlank()) {
                Toast.makeText(requireContext(), "Please enter an encoded message", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Call controller to decode
            val decoded = messageController.decodeMessage(inputText)
            decodedOutput.text = decoded
        }
    }
}
