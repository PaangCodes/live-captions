## 2024-05-10 - Disable Application Backup
**Vulnerability:** Android application backup enabled by default.
**Learning:** `android:allowBackup="true"` allows users to use `adb backup` to extract application data, potentially leading to unauthorized data extraction if sensitive data is stored.
**Prevention:** Set `android:allowBackup="false"` in the `AndroidManifest.xml` unless explicitly required and carefully managed.
## 2026-05-15 - Cleanup extracted directories upon failure
**Vulnerability:** Extracted partial files may persist on disk after an extraction exception.
**Learning:** If the extraction process fails or is interrupted, the partially extracted target directory is left in a corrupted state, potentially leading to disk resource exhaustion or a persistent corrupted state within the application's file storage.
**Prevention:** Track extraction success and ensure the entire partially extracted target directory is deleted (e.g., using a `finally` block with `deleteRecursively()`) if the process fails to complete.
