## 2024-04-26 - [Improve Error Messages to Prevent Info Leakage]
**Vulnerability:** The raw exception message (`e.message`) was being passed directly to the UI layer in `TranslationManager.kt` via `TranslationState.Error`. Translation errors were also silently swallowed.
**Learning:** This exposes potentially sensitive internal details to the user and prevents developers from reviewing the actual exceptions causing translation failures.
**Prevention:** Always fail securely by displaying generic, safe error messages to the UI and logging the actual exceptions using system logs (e.g., `Log.e`) for internal debugging purposes.

## 2024-04-30 - [Enforce HTTPS for External Downloads]
**Vulnerability:** The application was not restricting model downloads strictly to HTTPS endpoints (`ModelDownloader.kt`), which could allow HTTP URLs.
**Learning:** This could expose the user to a Man-in-the-Middle (MitM) attack when fetching machine learning models (Vosk or Whisper) from external domains over an unsecured connection.
**Prevention:** Always validate and restrict any remote download requests to use `https://` schemas, especially for applications dealing with potentially sensitive features (like audio and transcription), and enforce this in the base `ModelDownloader` component.
