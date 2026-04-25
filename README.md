# Live Stream Translation App - Finalized Tech Stack & Architecture

This document outlines the finalized technical architecture for the offline live stream translation application.

## 1. Architectural Approach: System-Wide Overlay (Android Native)

We have chosen the **System-Wide Overlay** approach. The application will run entirely in the background and overlay translated captions on top of whichever app the user is currently running (e.g., Twitch, YouTube app).

### Key Characteristics:
*   **Platform:** Android Native (Kotlin)
*   **Offline Capability:** The entire process runs locally on the device. It does not require an active internet connection once the language models are downloaded.

---

## 2. Tech Stack

### Framework & UI:
*   **Android Native (Kotlin):** The app must be built natively to deeply integrate with Android's system permissions.
*   **Jetpack Compose:** Used to build the application's configuration screens and the dynamic floating overlay UI.
*   **WindowManager API (`TYPE_APPLICATION_OVERLAY`):** Essential for drawing the floating captions over other running applications.

### Audio Capture:
*   **Android API:** `AudioPlaybackCapture` API (Introduced in Android 10).
*   **Implementation:** A Foreground Service using `AudioRecord` configured with `AudioPlaybackCaptureConfiguration`. This allows the app to intercept internal device audio (system sound) rather than relying on the microphone.
*   **Permissions Required:** `RECORD_AUDIO` and `FOREGROUND_SERVICE_MEDIA_PROJECTION` (requires explicit user consent via screen recording prompt).

### Speech-to-Text (STT) / Automatic Speech Recognition:
*   **Choice:** **On-Device (Vosk AND Whisper.cpp)**
*   **Reasoning & Flexibility:** Since we require offline functionality, a cloud-based API is not an option. However, users have different devices and preferences. We will implement an abstraction layer (e.g., `SttEngine` interface) that allows the user to switch between Vosk and Whisper in the app settings.
    *   **Vosk (Default/Battery Saver):** Highly recommended for its lightweight nature on mobile devices. It provides very fast real-time streaming recognition, making it ideal for older devices or preserving battery.
    *   **Whisper.cpp (High Accuracy):** A heavily optimized C++ port of OpenAI's Whisper. It provides superior transcription accuracy but is significantly more resource-intensive and battery draining. Allowing users to toggle this on for high-end devices provides the best of both worlds.

### Translation:
*   **Choice:** **Google ML Kit Translation API (On-Device)**
*   **Reasoning:** Runs entirely locally, is completely free, and is heavily optimized by Google for Android devices. It dynamically manages language packs (~30MB per language), reducing the initial app size.

---

## 3. Implementation Flow

1.  **Initialization:** The user opens the app, selects the "Source Stream Language" (e.g., Japanese), the "Target Caption Language" (e.g., English), and their preferred STT engine (Vosk or Whisper). The app downloads any necessary ML Kit or STT models if they aren't already on the device.
2.  **Start Service:** The user taps "Start Captions".
3.  **Permissions & Capture:** The app requests `SYSTEM_ALERT_WINDOW` (Overlay) and `MediaProjection` permissions. The foreground service begins recording internal audio chunks.
4.  **STT Processing:** The `AudioRecord` stream routes chunks through the active `SttEngine` implementation (Vosk or Whisper). If the user changes engines mid-stream, the app hot-swaps the underlying processor.
5.  **Translation:** As the STT engine returns recognized text in the source language, the text is immediately fed into the Google ML Kit Translation client.
6.  **Display:** The translated text is passed to the floating WindowManager overlay and updated on the screen in real-time, allowing the user to read along as they watch the stream in another app.

---

## 4. Infrastructure & Server

**This application is 100% Serverless and Offline.**
Because we utilize entirely on-device models for both Speech-to-Text and Translation, no audio data or transcription data is ever sent to a remote server.
*   **Privacy:** Complete user privacy is maintained as data never leaves the device.
*   **Cost:** $0 backend operational costs.
*   **Network:** Functions offline once the necessary language models (Vosk/Whisper and ML Kit) are downloaded.

---

## 5. Testing Strategy

Testing an application that relies on system overlays and internal audio capture requires a multi-layered approach:

*   **Unit Tests (JUnit):** Used to verify the business logic, state management (Jetpack Compose ViewModels), and the `SttEngine` abstraction layer (ensuring smooth switching between Vosk and Whisper logic).
*   **Instrumentation Tests (Espresso / UI Automator):** Run on emulators to verify UI components, navigation, and the initiation of Foreground Services.
*   **Physical Device Testing:** Critical for this app. Emulators often struggle to accurately simulate `AudioPlaybackCapture` and complex `SYSTEM_ALERT_WINDOW` interactions across different Android OS versions. Manual testing on physical hardware is required to guarantee performance and battery efficiency.

---

## 6. Build Variants & Distribution

The project utilizes Android's standard Gradle build system to manage different environments:

*   **Debug (Development):** Unoptimized builds with full logging and debugging capabilities, deployed directly from Android Studio during active development.
*   **Release (Preview / Beta):** Optimized and minified builds distributed via **Firebase App Distribution** or the **Google Play Internal Testing Track**. This allows beta testers to easily install the app and test the overlay on real devices.
*   **Release (Production):** The signed, fully optimized build published to the public Google Play Store.
