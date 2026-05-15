## 2024-05-31 - [Resource Exhaustion via Partial Zip Extraction]
**Vulnerability:** During ZIP archive extraction, if an error occurred (such as a network failure, lack of disk space, or a Zip Bomb detection thrown as an Exception), the previously extracted files inside the target directory remained on disk without being cleared.
**Learning:** Partially extracted archives create a potential vector for disk space exhaustion, leaving garbage files and corrupted configurations that can cause persistent app instability. A `finally` block is needed not only to close the ZipInputStream but also to ensure atomicity by destroying incomplete target directories when success isn't tracked.
**Prevention:** Track extraction success using a boolean flag set at the very end of the `try` block. In the `finally` block, verify `!success` and invoke `deleteRecursively()` on the target directory to enforce clean rollbacks.
## 2024-05-10 - Disable Application Backup
**Vulnerability:** Android application backup enabled by default.
**Learning:** `android:allowBackup="true"` allows users to use `adb backup` to extract application data, potentially leading to unauthorized data extraction if sensitive data is stored.
**Prevention:** Set `android:allowBackup="false"` in the `AndroidManifest.xml` unless explicitly required and carefully managed.

## 2024-05-12 - Incomplete cleanup of partially extracted files on failure
**Vulnerability:** When extracting downloaded archives, the `downloadAndExtractZip` function correctly removed the temporary zip file via a `finally` block but failed to delete partially extracted files from the destination directory `targetDir` if the process failed midway (e.g., due to a security constraint violation or I/O error).
**Learning:** This could lead to a Denial of Service (DoS) vulnerability via disk space exhaustion or persistent corrupted states if an archive bombs mid-extraction or connection fails, leaving behind potentially large and incomplete data that isn't cleaned up automatically.
**Prevention:** Track extraction success explicitly (e.g., `var success = false`) inside a try block. Update the `finally` block to delete the `targetDir` recursively (`targetDir.deleteRecursively()`) if the operation did not complete successfully.
## 2024-05-10 - Prevent Resource Exhaustion and Partial Disk Leaks
**Vulnerability:** When extracting zip entries, constraint violations (e.g. zip bomb size exceedance) throw SecurityExceptions leaving the partially uncompressed file on disk. Similarly, when the whole extraction process fails, the half-extracted directory is left on disk. This can cause disk resource exhaustion and persistent corrupted states.
**Learning:** Checking for extraction constraints and throwing exceptions without proper cleanup leaves partial files on the filesystem.
**Prevention:** Wrap file extraction streams inside a `try...finally` block to reliably clean up partial file chunks if a constraint violation happens or extraction fails. Use an `overallSuccess` flag to delete the entire target directory if the overall archive download and extraction process is not fully completed.
## 2024-05-11 - File Extraction Cleanup
**Vulnerability:** Denial of Service (DoS) via disk space exhaustion from persistent partially extracted files.
**Learning:** If a zip extraction fails (e.g., due to a Zip bomb, path traversal, or network error), leaving the partially extracted directory on disk consumes storage and leaves the application state corrupted.
**Prevention:** Always track the overall success of the extraction process and recursively delete the target extraction directory in a `finally` block if an error occurs or the process is interrupted.
## 2026-05-15 - Cleanup extracted directories upon failure
**Vulnerability:** Extracted partial files may persist on disk after an extraction exception.
**Learning:** If the extraction process fails or is interrupted, the partially extracted target directory is left in a corrupted state, potentially leading to disk resource exhaustion or a persistent corrupted state within the application's file storage.
**Prevention:** Track extraction success and ensure the entire partially extracted target directory is deleted (e.g., using a `finally` block with `deleteRecursively()`) if the process fails to complete.
