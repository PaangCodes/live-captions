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
*   **Choice:** **On-Device (Vosk or Whisper.cpp)**
*   **Reasoning:** Since we require offline functionality, a cloud-based API is not an option.
    *   **Vosk:** Highly recommended for its lightweight nature on mobile devices and ease of integration in Android. It provides good real-time streaming recognition.
    *   **Whisper.cpp:** An alternative if higher accuracy is needed, though it is more resource-intensive and battery draining.

### Translation:
*   **Choice:** **Google ML Kit Translation API (On-Device)**
*   **Reasoning:** Runs entirely locally, is completely free, and is heavily optimized by Google for Android devices. It dynamically manages language packs (~30MB per language), reducing the initial app size.

---

## 3. Implementation Flow

1.  **Initialization:** The user opens the app, selects the "Source Stream Language" (e.g., Japanese) and the "Target Caption Language" (e.g., English). The app downloads any necessary ML Kit or Vosk models if they aren't already on the device.
2.  **Start Service:** The user taps "Start Captions".
3.  **Permissions & Capture:** The app requests `SYSTEM_ALERT_WINDOW` (Overlay) and `MediaProjection` permissions. The foreground service begins recording internal audio chunks.
4.  **STT Processing:** Audio chunks are streamed continuously into the local STT model (e.g., Vosk).
5.  **Translation:** As the STT engine returns recognized text in the source language, the text is immediately fed into the Google ML Kit Translation client.
6.  **Display:** The translated text is passed to the floating WindowManager overlay and updated on the screen in real-time, allowing the user to read along as they watch the stream in another app.
