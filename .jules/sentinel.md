## 2024-04-26 - [Improve Error Messages to Prevent Info Leakage]
**Vulnerability:** The raw exception message (`e.message`) was being passed directly to the UI layer in `TranslationManager.kt` via `TranslationState.Error`. Translation errors were also silently swallowed.
**Learning:** This exposes potentially sensitive internal details to the user and prevents developers from reviewing the actual exceptions causing translation failures.
**Prevention:** Always fail securely by displaying generic, safe error messages to the UI and logging the actual exceptions using system logs (e.g., `Log.e`) for internal debugging purposes.
## 2026-04-29 - [Secure Model Downloads]
**Vulnerability:** The application was not enforcing HTTPS for model downloads and lacked complete path traversal protection on target file and directory names (though Zip Slip during extraction was handled).
**Learning:** Downloading complex binary models (like Vosk or Whisper) over unencrypted HTTP exposes the application to Man-in-the-Middle attacks where a malicious actor could replace the models, leading to arbitrary code execution within the native libraries. Path traversal could also allow overwriting arbitrary files in the app's internal storage.
**Prevention:** Always enforce HTTPS for any external asset downloads (`url.startsWith("https://")`). Pre-compute target file and directory paths and explicitly verify their `canonicalPath` against the safe base directory (`context.filesDir.canonicalPath`) to prevent directory traversal payloads.
