## 2024-05-10 - Disable Application Backup
**Vulnerability:** Android application backup enabled by default.
**Learning:** `android:allowBackup="true"` allows users to use `adb backup` to extract application data, potentially leading to unauthorized data extraction if sensitive data is stored.
**Prevention:** Set `android:allowBackup="false"` in the `AndroidManifest.xml` unless explicitly required and carefully managed.

## 2024-05-10 - Prevent Resource Exhaustion and Partial Disk Leaks
**Vulnerability:** When extracting zip entries, constraint violations (e.g. zip bomb size exceedance) throw SecurityExceptions leaving the partially uncompressed file on disk. Similarly, when the whole extraction process fails, the half-extracted directory is left on disk. This can cause disk resource exhaustion and persistent corrupted states.
**Learning:** Checking for extraction constraints and throwing exceptions without proper cleanup leaves partial files on the filesystem.
**Prevention:** Wrap file extraction streams inside a `try...finally` block to reliably clean up partial file chunks if a constraint violation happens or extraction fails. Use an `overallSuccess` flag to delete the entire target directory if the overall archive download and extraction process is not fully completed.
