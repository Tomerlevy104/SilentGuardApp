# &#x20;SilentGuard App - Mobile Security Final Project

> **A noise-aware, auto-response security app for emergency scenarios**

---

## 🌐 Project Overview

SilentGuard is a smart Android application designed to **detect emergency situations** based on abnormal noise levels in the environment. Upon detecting such a situation, it automatically records audio, transcribes the content into text using **Google Speech-to-Text**, **encrypts** it using **zero-width characters**, and sends it discreetly via **SMS and/or Email** to a preconfigured emergency contact.

Key goals:

* Trigger response **only** when noise exceeds a defined threshold.
* **Encrypt** sensitive transcribed data inside a cover message.
* **Send notifications silently** using Android's Accessibility Services.
* Maintain user safety through automation and discretion.

---

## 🎥 Demo Video

A full demonstration video of the app in action is available here:

[**Watch the Demo**](https://your-link-here.com)&#x20;

---

## 📂 Project Structure (MVC)

SilentGuard follows the **Model-View-Controller (MVC)** architecture:

```
com.example.silentguardapp
|
|-- controller/       # Handles app logic and coordination between services and UI
|   |-- AudioController.kt         # Manages audio recording lifecycle and buffer
|   |-- EventController.kt         # Coordinates full flow from detection to delivery
|   |-- MessageController.kt       # Creates messages and encodes using zero-width characters
|   |-- MonitoringController.kt    # Monitors audio levels and triggers emergency events
|   |-- NotifierController.kt      # Dispatches messages to contact (email/SMS)
|
|-- model/            # Pure data classes shared across layers
|   |-- AudioRecordModel.kt        # Stores audio file path and duration
|   |-- ContactModel.kt            # Stores contact name, phone, email
|   |-- EncryptedMessageModel.kt   # Combines raw and encoded text
|   |-- EventModel.kt              # Represents a complete emergency event
|   |-- AppSettingsModel.kt        # Holds user-defined configuration
|
|-- services/         # Background tasks, long-running actions
|   |-- EventService.kt            # Orchestrates emergency event creation
|   |-- GmailAutomationService.kt  # Automates Gmail UI to send email
|   |-- SmsAutomationService.kt    # Automates Messages UI to send SMS
|   |-- MessageService.kt          # Formats and encrypts messages
|   |-- MonitoringService.kt       # Captures ambient sound and triggers controller
|   |-- NotifierService.kt         # Routes final message to delivery method
|
|-- utils/            # Shared logic across layers
|   |-- PreferencesManager.kt      # Manages local storage of settings/events
|   |-- SpeechToTextConverter.kt   # Integrates Google API for transcriptions
|
|-- views/            # Fragments for each screen (UI only)
|   |-- HomeFragment.kt            # Displays current status and buttons
|   |-- SettingsFragment.kt        # UI for noise level, contact, message settings
|   |-- DecoderFragment.kt         # UI to paste and decode received message
|
|-- res/layout/       # XML layout definitions
|   |-- activity_main.xml
|   |-- fragment_home.xml
|   |-- fragment_settings.xml
|   |-- fragment_decoder.xml
|
|-- MainActivity.kt   # Hosts navigation via BottomNavigationView
```

---

## 🔒 Security Implementation Details

* The app runs **foreground noise monitoring** via a service with the `RECORD_AUDIO` permission.
* Uses **Google Cloud Speech-to-Text API** to transcribe audio into text.
* Transcribed messages are **encoded inside cover messages** using **zero-width Unicode characters**.
* Final messages are sent using `Intent.ACTION_SENDTO` and `AccessibilityService` for Gmail/SMS automation.

### 📁 Google Cloud Integration

To use Google Speech-to-Text:

1. A Google API key is required.
2. Place your credentials file `google-services-speech.json` inside the `assets/` folder.
3. **Note:** For security reasons, this file is **excluded** from the Git repository.

> ⚠️ If you're cloning this repository, you must create your own credentials file:

* [**Get started with Google Cloud Speech-to-Text**](https://cloud.google.com/speech-to-text/docs/quickstart-client-libraries)

---

## 📸 App Screenshots

Below are key screens from the application:

### 🏠 Home Screen

* Monitoring mode (active/inactive)
* Triggering audio analysis

![Home Active](screenshots/Home%20page%20active.jpg)
![Home Inactive](screenshots/Home%20page%20inactive.jpg)

### ⚙️ Settings Screen

* Noise threshold
* Emergency contact
* Covert message configuration

![Settings 1](screenshots/Settings%20screen%201.jpg)
![Settings 2](screenshots/Settings%20screen%202.jpg)

### 🧩 Decoder Screen

* Paste encrypted message and reveal decoded output

![Decoder](screenshots/Decoder%20Screen.jpg)

> ✅ **Tip:** Store screenshots in `/screenshots/`, use relative Markdown paths and resize to \~500px width for optimal GitHub display.

---

## ⚙️ Installation and Setup

### Requirements

* Android Studio (latest stable)
* Minimum SDK: 26
* Google Cloud API Key for Speech-to-Text

### Steps

1. Clone the repository:

```bash
git clone https://github.com/Tomerlevy104/SilentGuardApp.git
```

2. Create a file named `google-services-speech.json` and place it under:

```
app/src/main/assets/
```

3. Build and run the project on a real device (not an emulator).
4. On first launch, grant the following permissions:

   * `RECORD_AUDIO`
   * `SEND_SMS`
   * `POST_NOTIFICATIONS`
   * Accessibility access for:

      * `GmailAutomationService`
      * `SmsAutomationService`

---

## 💡 Technologies Used

| Purpose              | Technology                      |
| -------------------- | ------------------------------- |
| Programming Language | Kotlin                          |
| Architecture         | MVC                             |
| Speech Recognition   | Google Cloud Speech-to-Text API |
| Storage              | SharedPreferences (Local JSON)  |
| UI Toolkit           | Material Design 3               |
| Background Execution | Foreground Service, Coroutine   |
| Message Sending      | AccessibilityService, Intents   |

---

## ⚡ Permissions Overview

The app requests the following permissions to function securely:

* `RECORD_AUDIO`: To monitor environmental noise.
* `FOREGROUND_SERVICE_MICROPHONE`: Required for long-running audio monitoring.
* `SEND_SMS`: To notify emergency contact.
* `POST_NOTIFICATIONS`: For user alerts.
* `INTERNET`: For communication with Google Speech API.
* `BIND_ACCESSIBILITY_SERVICE`: For Gmail/SMS automation.

---

## 🚀 Future Improvements

* Add support for multiple emergency contacts.
* Use Firebase or external DB to sync events.
* Use biometric authentication for accessing app settings.

---

## 📅 License

This project is for academic and demonstration purposes only. All rights reserved to the author.

---

## 👤 Author

Developed by **Tomer Levy** as a final project in Mobile Security at Afeka College.
