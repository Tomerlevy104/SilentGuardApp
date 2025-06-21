package com.example.silentguardapp.services

class MessageService {
    /**
     * Encodes transcribed text using Zero Width characters
     * and hides it inside the user-defined cover message.
     * @param transcribedText The actual text captured from audio
     * @param coverMessage The message defined in settings to hide the text in
     */
    fun generateCoverMessage(transcribedText: String, coverMessage: String): String {
        val binary =
            transcribedText.toByteArray(Charsets.UTF_8) // Converts each character to a series of bytes (each character is 8 Bits = 1 Byte)
                .joinToString("") { byte ->
                    String.format(
                        "%8s",
                        Integer.toBinaryString(byte.toInt() and 0xFF) // Converts each byte to 8-bit binary code.
                    ).replace(" ", "0")
                }

        val zeroWidthEncoded = binary.map {
            when (it) {
                '0' -> '\u200B'  // Zero Width Space
                '1' -> '\u200C'  // Zero Width Non-Joiner
                else -> ' '
            }
        }.joinToString("")

        return "$coverMessage$zeroWidthEncoded"
    }

    /**
     * Decodes a Zero Width-encoded message back into its original text.
     *
     * @param encodedMessage The full message with cover text and hidden binary
     * @return The decoded hidden message (original transcribed text)
     */
    fun decodeCoverMessage(encodedMessage: String): String {
        // Extract all zero width characters
        val zeroWidthChars = encodedMessage.filter {
            it == '\u200B' || it == '\u200C'
        }

        // Build the binary string
        val binary = zeroWidthChars.map {
            when (it) {
                '\u200B' -> '0'
                '\u200C' -> '1'
                else -> null
            }
        }.joinToString("")

        // We will unpack every 8 bits into a byte and convert it to a character
        val bytes = binary.chunked(8)
            .mapNotNull { byteStr ->
                byteStr.toIntOrNull(2)?.toByte()
            }

        return bytes.toByteArray().toString(Charsets.UTF_8)
    }
}

