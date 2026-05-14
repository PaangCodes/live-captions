## 2024-05-10 - Disable Application Backup
**Vulnerability:** Android application backup enabled by default.
**Learning:** `android:allowBackup="true"` allows users to use `adb backup` to extract application data, potentially leading to unauthorized data extraction if sensitive data is stored.
**Prevention:** Set `android:allowBackup="false"` in the `AndroidManifest.xml` unless explicitly required and carefully managed.
## 2024-05-11 - File Extraction Cleanup
**Vulnerability:** Denial of Service (DoS) via disk space exhaustion from persistent partially extracted files.
**Learning:** If a zip extraction fails (e.g., due to a Zip bomb, path traversal, or network error), leaving the partially extracted directory on disk consumes storage and leaves the application state corrupted.
**Prevention:** Always track the overall success of the extraction process and recursively delete the target extraction directory in a `finally` block if an error occurs or the process is interrupted.
