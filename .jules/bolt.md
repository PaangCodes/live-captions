## 2025-01-24 - Optimized Progress Reporting in I/O Loops
 **Learning:** Frequent calls to `System.currentTimeMillis()` in tight loops (like an 8KB buffer read loop) can introduce unnecessary CPU overhead. Reducing the frequency of these calls by checking a byte-processed threshold first significantly improves efficiency.
 **Action:** Implemented a `bytesSinceLastCheck` counter in `ModelDownloader.kt` to throttle clock checks to every 512KB of data processed.

## 2025-02-12 - Jetpack Compose State Flow Hoisting Anti-pattern
**Learning:** Reading high-frequency state flow emissions (like STT model download progress `.collectAsState().value`) at the root level of a large Jetpack Compose component (e.g., inside the `setContent` root layout of `MainActivity`) causes severe UI jank. This is because every state emission triggers a full recomposition of the large root layout and all its non-memoized children.
**Action:** Always isolate high-frequency state reads into the lowest possible child `@Composable` functions (e.g., isolating `Start`/`Stop` buttons into their own `LiveCaptionControls` composable). This ensures only that specific small sub-tree recomposes when the state changes.
