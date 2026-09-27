## 2024-05-31 - [Resource Exhaustion via Partial Zip Extraction]
**Vulnerability:** During ZIP archive extraction, if an error occurred (such as a network failure, lack of disk space, or a Zip Bomb detection thrown as an Exception), the previously extracted files inside the target directory remained on disk without being cleared.
**Learning:** Partially extracted archives create a potential vector for disk space exhaustion, leaving garbage files and corrupted configurations that can cause persistent app instability. A `finally` block is needed not only to close the ZipInputStream but also to ensure atomicity by destroying incomplete target directories when success isn't tracked.
**Prevention:** Track extraction success using a boolean flag set at the very end of the `try` block. In the `finally` block, verify `!success` and invoke `deleteRecursively()` on the target directory to enforce clean rollbacks.
## 2024-05-10 - Disable Application Backup
**Vulnerability:** Android application backup enabled by default.
**Learning:** `android:allowBackup="true"` allows users to use `adb backup` to extract application data, potentially leading to unauthorized data extraction if sensitive data is stored.
**Prevention:** Set `android:allowBackup="false"` in the `AndroidManifest.xml` unless explicitly required and carefully managed.

## 2024-05-24 - [Critical URL Validation Bypass in File Downloads]
**Vulnerability:** The URL scheme and host checks in `ModelDownloader` could be bypassed using query parameters (e.g., `http://evil.com/model.bin?localhost`). `String.contains()` was used to check for the localhost bypass.
**Learning:** Checking string components manually using `contains()` instead of actually parsing the URL string leaves the application open to manipulation and bypasses.
**Prevention:** Always use `java.net.URL` or `java.net.URI` when evaluating URLs for network connections to properly separate protocol, host, and path elements, rather than performing simple string operations.
## 2024-05-24 - [Enforce Timeouts on Network Clients]
**Vulnerability:** The OkHttpClient was initialized with default configurations, which could lead to stalled network connections hanging the application thread indefinitely when downloading large STT language models (e.g., Vosk/Whisper).
**Learning:** Large external downloads must have explicit timeouts to prevent Denial of Service (DoS) due to resource exhaustion.
**Prevention:** Always explicitly set `connectTimeout`, `readTimeout`, and `writeTimeout` via `OkHttpClient.Builder()` rather than using the default `OkHttpClient()` constructor.
## 2024-05-24 - [Replace printStackTrace with secure logging]
**Vulnerability:** The application was using `e.printStackTrace()` in `WhisperSttEngine.kt`'s initialization catch block.
**Learning:** `printStackTrace()` writes directly to standard error, which is considered insecure as it can leak sensitive system or application structural information (stack traces) to logs or users unintentionally.
**Prevention:** Always use proper secure logging frameworks (like Android's `Log.e`) to handle exceptions securely without leaking stack trace information directly to system output streams.
## 2024-05-24 - [Avoid e.printStackTrace() and Share OkHttpClient Instances]
**Vulnerability:** The application was printing raw exceptions using `e.printStackTrace()` in `WhisperSttEngine.kt`, potentially leaking internal details. Furthermore, `OkHttpClient` was being re-instantiated for every request in `ModelDownloader.kt`, which can lead to connection leaks and resource exhaustion (DoS vulnerability).
**Learning:** Raw stack traces must not be exposed carelessly. In Android, `System.err` outputs from `printStackTrace()` bypass proper logging mechanisms. Additionally, `OkHttpClient` instances create expensive thread and connection pools that must be shared to prevent application crashes under load.
**Prevention:** Always use proper system logging mechanisms like `Log.e(TAG, message, e)` to handle exceptions securely without leaking details to raw standard error. For OkHttpClient, define shared instances using `by lazy { createClient() }` to reuse the underlying connection pools across requests.

## 2026-05-09 - [Prevent Unauthorized Data Extraction via App Backup]
**Vulnerability:** The application had `android:allowBackup="true"` enabled in the `AndroidManifest.xml`.
**Learning:** Enabling application backup allows sensitive user data to be extracted from the device via `adb backup`, which can be exploited if an attacker has physical access to the device or if the device is compromised.
**Prevention:** Always set `android:allowBackup="false"` in the `AndroidManifest.xml` for applications that handle sensitive data to prevent unauthorized data extraction.
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
## 2024-05-31 - Explicitly close OkHttp Responses to prevent DoS
**Vulnerability:** Connection pool resource exhaustion (Denial of Service) risk in download functions.
**Learning:** OkHttp responses must be explicitly closed to return the connection back to the pool, even if the body stream is read.
**Prevention:** Always wrap `execute()` calls in `.use { response -> ... }` blocks.
## 2025-02-12 - [Connection Pool Exhaustion Prevention]
**Vulnerability:** `OkHttpClient` responses (`client.newCall(request).execute()`) were not explicitly closed, causing potential connection leaks and resource exhaustion (Denial of Service risk).
**Learning:** In Kotlin/Java network calls via OkHttp, unclosed responses leak connection resources from the client pool and file descriptors, potentially hanging or crashing the application under repeated network use.
**Prevention:** Always wrap synchronous OkHttp calls `client.newCall(request).execute()` in a `try-with-resources` or Kotlin `.use { response -> }` block to guarantee correct stream termination and resource reclamation.

## 2026-05-24 - [Fix OkHttp Response Leaks in ModelDownloader]
**Vulnerability:** OkHttp `Response` objects were not being closed after downloading files in `ModelDownloader.kt`, leading to potential connection pool exhaustion and DoS.
**Learning:** OkHttp responses must always be explicitly closed, even if the body stream is fully consumed.
**Prevention:** Use a `try-with-resources` block (or Kotlin's `.use { response -> ... }`) to ensure the underlying connection is released back to the pool, regardless of success or intermediate exceptions.
## 2024-05-23 - Prevent Resource Exhaustion with OkHttp
**Vulnerability:** Connection leak / resource exhaustion due to unclosed OkHttp Response objects in `ModelDownloader.kt`.
**Learning:** When using `client.newCall(request).execute()`, the underlying connection is not automatically released. Failing to close the response body can lead to connection pool exhaustion and DoS.
**Prevention:** Always wrap `client.newCall(request).execute()` in a `try-with-resources` block (or Kotlin's `.use { response -> ... }`) to ensure the response body is safely closed and the connection is released.
## 2024-05-25 - [Prevent Connection Leaks in OkHttp]
**Vulnerability:** The application executed network requests with OkHttpClient without closing the returned `Response` objects.
**Learning:** Failing to close OkHttp `Response` objects keeps network connections open and leads to connection pool resource exhaustion, which can result in an application crash (Denial of Service).
**Prevention:** Always wrap `client.newCall(request).execute()` in a `try-with-resources` or Kotlin `.use { response -> ... }` block to ensure connections are released.
## 2026-05-20 - Unclosed OkHttp Responses Causing DoS
**Vulnerability:** OkHttp responses are not being closed in ModelDownloader.kt.
**Learning:** Unclosed OkHttp Response objects can lead to connection pool resource exhaustion and Denial of Service (DoS) vulnerabilities, stalling network connections.
**Prevention:** Always explicitly close OkHttp Response objects by wrapping client.newCall(request).execute() in a .use { response -> ... } block.

## 2024-05-24 - DoS vulnerability via unclosed OkHttp Connections
**Vulnerability:** The application was vulnerable to connection pool exhaustion (Denial of Service) when downloading STT models. `OkHttpClient.newCall(request).execute()` returned an OkHttp `Response` object that was never explicitly closed. If errors occurred during body processing (e.g., download size limits exceeded, network IO exceptions) or when downloads finished naturally, the underlying network connection remained held in the OkHttp connection pool, eventually exhausting available resources.
**Learning:** OkHttp automatically closes the response body *only* if you use specific convenience methods. When operating on raw `InputStream` streams manually via `response.body?.byteStream()`, OkHttp expects the caller to manually release the connection back to the pool by closing the response body.
**Prevention:** Always wrap `client.newCall(request).execute()` in a `.use { response -> ... }` block in Kotlin (or a `try-with-resources` block in Java). This ensures that the response's body is automatically closed, regardless of success, intermediate exceptions, or early returns, preventing resource leak vulnerabilities.
## 2024-05-17 - Unclosed OkHttp Responses
**Vulnerability:** OkHttp responses were not being explicitly closed during model downloads (`client.newCall(request).execute()`), leading to connection pool exhaustion and potential Denial of Service (DoS) under multiple connection attempts.
**Learning:** In Kotlin/Android, network connections via OkHttp must be manually released back to the connection pool by closing the response or its body, even if an exception occurs.
**Prevention:** Always wrap `client.newCall(request).execute()` in a `try-with-resources` or `.use { response -> ... }` block to guarantee connection release.
## 2024-05-24 - [Fix DoS Resource Exhaustion in OkHttp Network Calls]
**Vulnerability:** The OkHttp `Response` body was consumed as an `InputStream`, but the `Response` itself was never explicitly closed. This leads to connection pool resource leaks and potential Denial of Service (DoS) due to unreleased connections.
**Learning:** OkHttp connections are kept alive by default. If a `Response` is not closed (or if its body stream is not fully consumed and closed automatically), the connection is never returned to the shared pool, leading to resource exhaustion, especially when downloading multiple large model files.
**Prevention:** Always wrap OkHttp network calls executing requests inside a `try-with-resources` construct (like Kotlin's `.use { response -> ... }`) to guarantee the underlying response and connection are released properly, even if exceptions are thrown mid-download.

## $(date +%Y-%m-%d) - Fix syntax and undeclared variables
 **Vulnerability:** Code wouldn't compile due to syntax errors (unbalanced `try-finally` blocks) and undeclared variables (`tempZipFile` and `maxDownloadBytes`). This leaves the download mechanisms broken and potentially un-tunable for security limits.
 **Learning:** When a `try` block encapsulates an OkHttp `.use { ... }` lambda, modifying the end to `} } finally {` successfully closes the nested lambda and `try` block. However, the original closing brace for the `try` block remains orphaned further down the file, incorrectly closing the encompassing `flow` builder prematurely and causing "Expecting a top level declaration" errors.
 **Prevention:** Write a Python brace parser script (e.g., `parse_braces.py`) to systematically check for brace imbalances, and always trace structural scopes completely to the end of the method before applying block-level Git merge diffs.
## 2026-06-06 - Prevent stack trace leakage in logs
**Vulnerability:** Passing full exception objects `e` to `Log.e` prints raw stack traces to system logs (`System.err`), potentially leaking sensitive internal implementation details to other apps or physical attackers.
**Learning:** To avoid information leakage, logs should only contain descriptive error messages and `e.message` rather than the full stack trace `e`.
**Prevention:** Always log `e.message` or omit the exception entirely if a static string is sufficient, rather than passing the `Exception` object to Android's logging framework.

## 2026-06-06 - Disable Cleartext HTTP Traffic in AndroidManifest
**Vulnerability:** Cleartext HTTP traffic was permitted application-wide by default because `android:usesCleartextTraffic` was missing in `AndroidManifest.xml`.
**Learning:** Modern Android applications should enforce encrypted HTTPS connections globally by explicitly declaring `android:usesCleartextTraffic="false"` in the `<application>` element of `AndroidManifest.xml`.
**Prevention:** Always include `android:usesCleartextTraffic="false"` in `AndroidManifest.xml` unless unencrypted network traffic is explicitly required and secured via Network Security Configuration.
