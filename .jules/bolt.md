## 2025-01-24 - Optimized Progress Reporting in I/O Loops
 **Learning:** Frequent calls to `System.currentTimeMillis()` in tight loops (like an 8KB buffer read loop) can introduce unnecessary CPU overhead. Reducing the frequency of these calls by checking a byte-processed threshold first significantly improves efficiency.
 **Action:** Implemented a `bytesSinceLastCheck` counter in `ModelDownloader.kt` to throttle clock checks to every 512KB of data processed.

## 2025-02-12 - Jetpack Compose State Flow Hoisting Anti-pattern
**Learning:** Reading high-frequency state flow emissions (like STT model download progress `.collectAsState().value`) at the root level of a large Jetpack Compose component (e.g., inside the `setContent` root layout of `MainActivity`) causes severe UI jank. This is because every state emission triggers a full recomposition of the large root layout and all its non-memoized children.
**Action:** Always isolate high-frequency state reads into the lowest possible child `@Composable` functions (e.g., isolating `Start`/`Stop` buttons into their own `LiveCaptionControls` composable). This ensures only that specific small sub-tree recomposes when the state changes.

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
## 2025-05-15 - Eliminate ByteArray.copyOf() Allocation
 **Learning:** In high-frequency loops like `AudioRecord` capture, constantly using `ByteArray.copyOf()` generates significant temporary memory allocations. This increases Garbage Collection (GC) overhead and can cause audio stuttering.
 **Action:** Update processing interfaces like `processAudio(data, offset, length)` to accept the raw array slice instead of making full array copies.

## 2025-02-12 - Eliminate GC Pressure in High-Frequency Audio Capture Loops
**Learning:** Allocating new byte arrays (e.g., using `ByteArray.copyOf()`) inside a tight, high-frequency loop (like reading from an `AudioRecord` input stream) creates massive Garbage Collection pressure. This can cause application stuttering and dropped frames in real-time audio processing.
**Action:** Always design and utilize interfaces that accept the pre-allocated buffer along with an `offset` and `length` (e.g., `processAudio(buffer, 0, read)`) to achieve zero-allocation data processing.
## 2025-05-15 - Zero-Allocation Audio Processing in Capture Loop
 **Learning:** In high-frequency loop systems like `AudioCaptureService`, reading fixed-size chunks of data from a hardware source (`AudioRecord`) and calling `ByteArray.copyOf(read)` allocates a new memory block on every iteration. This creates constant, massive GC pressure and micro-stutters during processing.
 **Action:** Instead of allocating new arrays, pass the pre-allocated reusable `buffer` array directly into the `processAudio` method alongside its `offset` (usually 0) and valid `length` (`read`). This enables zero-allocation processing for continuous I/O streams.
## 2025-05-15 - Zero-Allocation Audio Processing
 **Learning:** In high-frequency capture loops (like `AudioRecord.read`), constantly allocating new objects (e.g., `buffer.copyOf(read)`) creates severe GC pressure and can cause execution stutter.
 **Action:** Instead of creating defensive copies, pass the backing buffer directly down the pipeline along with `offset` and `length` parameters (e.g., `processAudio(data, offset, length)`) to achieve zero-allocation processing.
## 2025-05-15 - Jetpack Compose State Isolation
**Learning:** Extracting STT state updates to a child component isolates recompositions, significantly enhancing performance during rapid state updates and preventing the entire `SttConfigCard` from unnecessary re-rendering. This optimizes UI performance, particularly when STT engine emits progress.
**Action:** Extract the status and progress reporting logic to a custom `@Composable` function inside parents that would otherwise suffer from rapid high-frequency re-rendering.
