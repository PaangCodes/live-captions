## 2025-01-24 - Optimized Progress Reporting in I/O Loops
 **Learning:** Frequent calls to `System.currentTimeMillis()` in tight loops (like an 8KB buffer read loop) can introduce unnecessary CPU overhead. Reducing the frequency of these calls by checking a byte-processed threshold first significantly improves efficiency.
 **Action:** Implemented a `bytesSinceLastCheck` counter in `ModelDownloader.kt` to throttle clock checks to every 512KB of data processed.

## 2024-05-06 - Optimized StateFlow Reading in Jetpack Compose
**Learning:** Reading `.value` directly from a `StateFlow` inside a Compose component breaks state tracking because it bypasses Compose's state observation mechanism (`collectAsState()`). This means the component won't recompose when the underlying value changes.
**Action:** When working with Compose, ensure that variables bound to `StateFlow` use `.collectAsState()` and are read from the resulting state object or via property delegation (`by`). For variables needed by buttons at the root of the hierarchy, extract those buttons into isolated Composable child components (e.g., `LiveCaptionControls`) where the state can be collected independently without causing the parent layout to recompose rapidly.
