## 2025-01-24 - Optimized Progress Reporting in I/O Loops
 **Learning:** Frequent calls to `System.currentTimeMillis()` in tight loops (like an 8KB buffer read loop) can introduce unnecessary CPU overhead. Reducing the frequency of these calls by checking a byte-processed threshold first significantly improves efficiency.
 **Action:** Implemented a `bytesSinceLastCheck` counter in `ModelDownloader.kt` to throttle clock checks to every 512KB of data processed.

## 2025-02-12 - Jetpack Compose State Flow Hoisting Anti-pattern
**Learning:** Reading high-frequency state flow emissions (like STT model download progress `.collectAsState().value`) at the root level of a large Jetpack Compose component (e.g., inside the `setContent` root layout of `MainActivity`) causes severe UI jank. This is because every state emission triggers a full recomposition of the large root layout and all its non-memoized children.
**Action:** Always isolate high-frequency state reads into the lowest possible child `@Composable` functions (e.g., isolating `Start`/`Stop` buttons into their own `LiveCaptionControls` composable). This ensures only that specific small sub-tree recomposes when the state changes.

## 2026-05-19 - Reduce System.currentTimeMillis Overhead
## 2026-05-17 - Reduce System.currentTimeMillis Overhead
## 2026-05-24 - Reduce System.currentTimeMillis Overhead
## 2025-05-15 - Reduce System.currentTimeMillis Overhead
**Learning:** Initializing the tracking timestamp (`lastEmitTime`) to 0 instead of the current system time in a throttling logic bypasses rate limiting for the very first event, executing it instantly.
**Action:** When implementing time-throttling in high-frequency loops (like download progress), always initialize `lastEmitTime = System.currentTimeMillis()`.

## 2025-02-12 - OkHttpClient Instance Sharing
**Learning:** Instantiating a new `OkHttpClient` per network request creates redundant connection pools and thread pools, which significantly increases memory overhead and connection latency.
**Action:** Always share a single, lazily initialized `OkHttpClient` instance across the application or module (e.g., using a `companion object` and `lazy` delegate) to reuse connection resources efficiently.
## 2026-05-08 - O(1) Membership lookups for Compose List Rendering
 **Learning:** When a UI layer renders a dynamic list that requires verifying if each item exists within a collection of known states (e.g. checking if a language is downloaded out of an entire list of available languages), passing that collection down from the StateHolder as a `List` creates an O(N^2) scaling problem during rendering.
 **Action:** Instead of exposing `StateFlow<List<T>>` from the viewmodel/manager and locally converting it inside `remember { list.toSet() }` in Jetpack Compose, the backend StateHolder should inherently maintain and expose the collection as a `Set` (e.g. `StateFlow<Set<T>>`). This achieves O(1) lookups during recomposition directly, eliminating redundant transformation overhead and making list scaling efficient.
## 2024-05-19 - Pre-compute Canonical Path in Zip Extraction Loop
 **Learning:** Resolving `File.canonicalPath` inside a tight loop (like extracting a zip archive) causes severe performance degradation due to redundant file system I/O.
 **Action:** Pre-computed the `targetDir.canonicalPath + File.separator` outside the `while` loop in `ModelDownloader.kt` and used the cached variable for the Zip Slip validation check inside the loop. Reduced extraction time by ~11.7% (~110ms improvement on a 5000-file mock zip).
## 2026-05-19 - Eliminate ByteArray.copyOf() Allocation
## 2026-05-17 - Eliminate ByteArray.copyOf() Allocation
## 2026-05-24 - Eliminate ByteArray.copyOf() Allocation
## 2025-05-15 - Eliminate ByteArray.copyOf() Allocation
 **Learning:** In high-frequency loops like `AudioRecord` capture, constantly using `ByteArray.copyOf()` generates significant temporary memory allocations. This increases Garbage Collection (GC) overhead and can cause audio stuttering.
 **Action:** Update processing interfaces like `processAudio(data, offset, length)` to accept the raw array slice instead of making full array copies.

## 2025-02-12 - Eliminate GC Pressure in High-Frequency Audio Capture Loops
**Learning:** Allocating new byte arrays (e.g., using `ByteArray.copyOf()`) inside a tight, high-frequency loop (like reading from an `AudioRecord` input stream) creates massive Garbage Collection pressure. This can cause application stuttering and dropped frames in real-time audio processing.
**Action:** Always design and utilize interfaces that accept the pre-allocated buffer along with an `offset` and `length` (e.g., `processAudio(buffer, 0, read)`) to achieve zero-allocation data processing.
## 2026-05-19 - Zero-Allocation Audio Processing in Capture Loop
 **Learning:** In high-frequency loop systems like `AudioCaptureService`, reading fixed-size chunks of data from a hardware source (`AudioRecord`) and calling `ByteArray.copyOf(read)` allocates a new memory block on every iteration. This creates constant, massive GC pressure and micro-stutters during processing.
 **Action:** Instead of allocating new arrays, pass the pre-allocated reusable `buffer` array directly into the `processAudio` method alongside its `offset` (usually 0) and valid `length` (`read`). This enables zero-allocation processing for continuous I/O streams.
## 2026-05-19 - Zero-Allocation Audio Processing
 **Learning:** In high-frequency capture loops (like `AudioRecord.read`), constantly allocating new objects (e.g., `buffer.copyOf(read)`) creates severe GC pressure and can cause execution stutter.
 **Action:** Instead of creating defensive copies, pass the backing buffer directly down the pipeline along with `offset` and `length` parameters (e.g., `processAudio(data, offset, length)`) to achieve zero-allocation processing.
## 2025-02-12 - Optimize Blank String Processing in JNI Bridges
**Learning:** Sending empty/blank payloads to JNI-bound APIs like ML Kit Translator during rapid async emissions (e.g. speech pauses in STT streams) incurs unnecessary overhead for crossing the coroutine suspension and JNI boundaries, decreasing overall performance without meaningful output.
**Action:** When handling continuous translation streams, add an early return for blank payloads. Crucially, always emit the original blank string to the downstream collectors to ensure the UI components properly clear any stale captions.
## 2026-05-25 - Bypass JNI for Blank Payloads
**Learning:** When sending data to ML Kit or other JNI-bound APIs, unnecessary coroutine suspension and JNI boundary crossing overhead occurs for empty/blank payloads often emitted by STT engines during speech pauses.
**Action:** Bypassed ML Kit JNI and coroutine suspension for empty text with an early return, still emitting the blank text downstream so the UI correctly clears stale captions.
## $(date +%Y-%m-%d) - Prevent JNI Overhead for Empty Data Streams
 **Learning:** In high-frequency, stream-based text processing (like translating STT outputs), sending blank or empty strings to native libraries (e.g., via JNI or coroutine bridges like `translator?.translate(text)?.await()`) incurs massive, redundant computational overhead. STT engines frequently emit empty strings during speech pauses.
 **Action:** Always implement an early return condition (`if (text.isBlank())`) before hitting heavy asynchronous processing boundaries to dramatically reduce CPU wake-ups and unnecessary task scheduling. Ensure the blank result is still emitted down the pipeline so UI components can clear out stale data accurately.
## 2025-02-12 - Eliminate Translation JNI Bottleneck for Blank Texts
 **Learning:** When sending data to ML Kit or other JNI-bound APIs, processing blank or empty payloads (often emitted by STT engines during speech pauses) incurs unnecessary coroutine suspension and JNI boundary crossing overhead, slowing down the processing pipeline.
 **Action:** Bypass unnecessary JNI boundary crossing overhead by using an early return for empty/blank payloads. Crucially, still emit the blank text downstream (e.g., `_translatedText.emit(text)`) before returning so the UI correctly clears stale captions.

## 2026-05-21 - JNI Overhead Avoidance for Empty Text
**Learning:** When sending data to ML Kit or other JNI-bound APIs, sending blank/empty payloads incurs unnecessary coroutine suspension and JNI boundary crossing overhead.
**Action:** Add an early return for empty/blank payloads (often emitted by STT engines during speech pauses) to bypass this overhead, while still emitting the blank text downstream so the UI correctly clears stale captions.
## 2024-05-20 - Avoid JNI boundary crossing overhead for blank text
 **Learning:** Translation libraries usually rely on JNI bindings for model inference. Sending empty/blank payloads often emitted by STT engines during speech pauses to the translator introduces unnecessary coroutine suspension and JNI boundary crossing overhead.
 **Action:** Add early return `if (text.isBlank()) return` to prevent unnecessary method calls.
## 2026-05-19 - JNI Boundary Crossing Overhead for Empty Payloads
**Learning:** Calling JNI-bound ML Kit translation APIs (like `translator?.translate(text)?.await()`) for blank or empty text payloads incurs unnecessary coroutine suspension and native boundary crossing overhead (measured at ~1ms per call, even if the result is empty). STT engines emit empty strings frequently during pauses in speech.
**Action:** When integrating with native ML libraries, always bypass the JNI boundary with an early return (e.g., `if (text.isBlank()) return`) for simple, predictable edge cases like empty strings.
## 2026-05-17 - Zero-Allocation Audio Processing in Capture Loop
 **Learning:** In high-frequency loop systems like `AudioCaptureService`, reading fixed-size chunks of data from a hardware source (`AudioRecord`) and calling `ByteArray.copyOf(read)` allocates a new memory block on every iteration. This creates constant, massive GC pressure and micro-stutters during processing.
 **Action:** Instead of allocating new arrays, pass the pre-allocated reusable `buffer` array directly into the `processAudio` method alongside its `offset` (usually 0) and valid `length` (`read`). This enables zero-allocation processing for continuous I/O streams.
## 2026-05-17 - Zero-Allocation Audio Processing
 **Learning:** In high-frequency capture loops (like `AudioRecord.read`), constantly allocating new objects (e.g., `buffer.copyOf(read)`) creates severe GC pressure and can cause execution stutter.
 **Action:** Instead of creating defensive copies, pass the backing buffer directly down the pipeline along with `offset` and `length` parameters (e.g., `processAudio(data, offset, length)`) to achieve zero-allocation processing.
## $(date +%Y-%m-%d) - Bypass JNI and Coroutine Overhead for Blank Text Payloads
**Learning:** STT engines frequently emit empty or blank strings during speech pauses. Passing these empty strings to ML Kit's translation engine (`translator?.translate(text)?.await()`) incurs unnecessary JNI boundary crossing and coroutine suspension overhead without yielding useful translations.
**Action:** When connecting a high-frequency STT string stream to a translation backend, always add an early return bypass (`if (text.isBlank())`) to emit the blank text directly and skip the heavy translation API calls.
## 2026-05-17 - Bypass JNI Overhead for STT Pauses
 **Learning:** When STT engines emit empty or blank strings during speech pauses, sending these through `translator?.translate()` incurs unnecessary Coroutine suspension and JNI boundary crossing overhead, slowing down the processing loop.
 **Action:** Implemented an early return (`if (text.isBlank())`) in `TranslationManager.kt` to bypass ML Kit's translation entirely for blank payloads.
## 2026-05-24 - Zero-Allocation Audio Processing in Capture Loop
 **Learning:** In high-frequency loop systems like `AudioCaptureService`, reading fixed-size chunks of data from a hardware source (`AudioRecord`) and calling `ByteArray.copyOf(read)` allocates a new memory block on every iteration. This creates constant, massive GC pressure and micro-stutters during processing.
 **Action:** Instead of allocating new arrays, pass the pre-allocated reusable `buffer` array directly into the `processAudio` method alongside its `offset` (usually 0) and valid `length` (`read`). This enables zero-allocation processing for continuous I/O streams.
## 2026-05-24 - Zero-Allocation Audio Processing
 **Learning:** In high-frequency capture loops (like `AudioRecord.read`), constantly allocating new objects (e.g., `buffer.copyOf(read)`) creates severe GC pressure and can cause execution stutter.
 **Action:** Instead of creating defensive copies, pass the backing buffer directly down the pipeline along with `offset` and `length` parameters (e.g., `processAudio(data, offset, length)`) to achieve zero-allocation processing.
## 2026-05-24 - Bypass JNI overhead for empty STT emissions
**Learning:** STT engines often emit empty or blank text during speech pauses. Sending these empty strings through the ML Kit Translation API incurs unnecessary coroutine suspension and costly JNI boundary crossing overhead.
**Action:** Added an early return `if (text.isBlank())` in `TranslationManager.kt`'s translation stream processing. It directly emits the blank text downstream (to clear stale captions) and skips the JNI translation call, improving battery and performance during silence.
## 2025-05-15 - Zero-Allocation Audio Processing in Capture Loop
 **Learning:** In high-frequency loop systems like `AudioCaptureService`, reading fixed-size chunks of data from a hardware source (`AudioRecord`) and calling `ByteArray.copyOf(read)` allocates a new memory block on every iteration. This creates constant, massive GC pressure and micro-stutters during processing.
 **Action:** Instead of allocating new arrays, pass the pre-allocated reusable `buffer` array directly into the `processAudio` method alongside its `offset` (usually 0) and valid `length` (`read`). This enables zero-allocation processing for continuous I/O streams.
## 2025-05-15 - Zero-Allocation Audio Processing
 **Learning:** In high-frequency capture loops (like `AudioRecord.read`), constantly allocating new objects (e.g., `buffer.copyOf(read)`) creates severe GC pressure and can cause execution stutter.
 **Action:** Instead of creating defensive copies, pass the backing buffer directly down the pipeline along with `offset` and `length` parameters (e.g., `processAudio(data, offset, length)`) to achieve zero-allocation processing.
## 2025-05-15 - Jetpack Compose State Isolation
**Learning:** Extracting STT state updates to a child component isolates recompositions, significantly enhancing performance during rapid state updates and preventing the entire `SttConfigCard` from unnecessary re-rendering. This optimizes UI performance, particularly when STT engine emits progress.
**Action:** Extract the status and progress reporting logic to a custom `@Composable` function inside parents that would otherwise suffer from rapid high-frequency re-rendering.
