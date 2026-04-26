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

## 3. Current Implementation Status

The core components of the application are now successfully integrated into a cohesive pipeline orchestrated by `MainActivity`:

1.  **Initialization:** `MainActivity` initializes the `VoskSttEngine`, the `TranslationManager` (using Google ML Kit), and the `OverlayManager`. Coroutine `Flow`s are mapped in `onCreate` to seamlessly pass partial recognition results to the translator, and translated text to the overlay.
2.  **Permissions & Capture:** When the user taps "Start Live Captions", the app sequentially requests and validates `RECORD_AUDIO` and `SYSTEM_ALERT_WINDOW` permissions. Once granted, it launches the `MediaProjection` intent.
3.  **STT Processing:** `AudioCaptureService` (a Foreground Service) intercepts internal device audio using `AudioPlaybackCapture` and feeds the byte array chunks directly into the injected `SttEngine`.
4.  **Translation:** The `TranslationManager` collects the text from the `SttEngine`'s Flow and processes it locally using ML Kit models.
5.  **Display:** The `OverlayManager` creates a custom `LifecycleOwner` and displays a Jetpack Compose floating window over other apps (`TYPE_APPLICATION_OVERLAY`) to show the translated captions in real-time.

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

*   **Unit Tests (JUnit):** Used to verify business logic and state transitions. We use Mockito and Coroutine testing to verify the `SttEngine` logic (e.g., `VoskSttEngineTest`). Run them using `./gradlew test`.
*   **Instrumentation Tests (Espresso/Compose):** Used to verify UI elements (like `MainActivityTest`). Run them using `./gradlew connectedAndroidTest` on an active emulator or physical device.
*   **Physical Device Testing:** Critical for this app. Emulators often struggle to accurately simulate `AudioPlaybackCapture` and complex `SYSTEM_ALERT_WINDOW` interactions across different Android OS versions. Manual testing on physical hardware is required to guarantee performance and battery efficiency.

---

## 6. Build Variants & Distribution

The project utilizes Android's standard Gradle build system to manage different environments:

*   **Debug (Development):** Unoptimized builds with full logging and debugging capabilities, deployed directly from Android Studio during active development.
*   **Release (Preview / Beta):** Optimized and minified builds distributed via **Firebase App Distribution** or the **Google Play Internal Testing Track**. This allows beta testers to easily install the app and test the overlay on real devices.
*   **Release (Production):** The signed, fully optimized build published to the public Google Play Store.

## 7. Releases

You can download the latest debug APK from the root of the repository:
[Download live-captions.apk](./live-captions.apk)
