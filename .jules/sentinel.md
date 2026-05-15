## 2024-05-10 - Disable Application Backup
**Vulnerability:** Android application backup enabled by default.
**Learning:** `android:allowBackup="true"` allows users to use `adb backup` to extract application data, potentially leading to unauthorized data extraction if sensitive data is stored.
**Prevention:** Set `android:allowBackup="false"` in the `AndroidManifest.xml` unless explicitly required and carefully managed.
## 2024-05-11 - File Extraction Cleanup
**Vulnerability:** Denial of Service (DoS) via disk space exhaustion from persistent partially extracted files.
**Learning:** If a zip extraction fails (e.g., due to a Zip bomb, path traversal, or network error), leaving the partially extracted directory on disk consumes storage and leaves the application state corrupted.
**Prevention:** Always track the overall success of the extraction process and recursively delete the target extraction directory in a `finally` block if an error occurs or the process is interrupted.
## 2026-05-15 - Cleanup extracted directories upon failure
**Vulnerability:** Extracted partial files may persist on disk after an extraction exception.
**Learning:** If the extraction process fails or is interrupted, the partially extracted target directory is left in a corrupted state, potentially leading to disk resource exhaustion or a persistent corrupted state within the application's file storage.
**Prevention:** Track extraction success and ensure the entire partially extracted target directory is deleted (e.g., using a `finally` block with `deleteRecursively()`) if the process fails to complete.
