## 2024-05-10 - Disable Application Backup
**Vulnerability:** Android application backup enabled by default.
**Learning:** `android:allowBackup="true"` allows users to use `adb backup` to extract application data, potentially leading to unauthorized data extraction if sensitive data is stored.
**Prevention:** Set `android:allowBackup="false"` in the `AndroidManifest.xml` unless explicitly required and carefully managed.

## 2024-05-12 - Incomplete cleanup of partially extracted files on failure
**Vulnerability:** When extracting downloaded archives, the `downloadAndExtractZip` function correctly removed the temporary zip file via a `finally` block but failed to delete partially extracted files from the destination directory `targetDir` if the process failed midway (e.g., due to a security constraint violation or I/O error).
**Learning:** This could lead to a Denial of Service (DoS) vulnerability via disk space exhaustion or persistent corrupted states if an archive bombs mid-extraction or connection fails, leaving behind potentially large and incomplete data that isn't cleaned up automatically.
**Prevention:** Track extraction success explicitly (e.g., `var success = false`) inside a try block. Update the `finally` block to delete the `targetDir` recursively (`targetDir.deleteRecursively()`) if the operation did not complete successfully.
