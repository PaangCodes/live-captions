## 2024-04-26 - [Improve Error Messages to Prevent Info Leakage]
**Vulnerability:** The raw exception message (`e.message`) was being passed directly to the UI layer in `TranslationManager.kt` via `TranslationState.Error`. Translation errors were also silently swallowed.
**Learning:** This exposes potentially sensitive internal details to the user and prevents developers from reviewing the actual exceptions causing translation failures.
**Prevention:** Always fail securely by displaying generic, safe error messages to the UI and logging the actual exceptions using system logs (e.g., `Log.e`) for internal debugging purposes.
