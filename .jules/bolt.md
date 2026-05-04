## 2025-01-24 - Optimized Progress Reporting in I/O Loops
 **Learning:** Frequent calls to `System.currentTimeMillis()` in tight loops (like an 8KB buffer read loop) can introduce unnecessary CPU overhead. Reducing the frequency of these calls by checking a byte-processed threshold first significantly improves efficiency.
 **Action:** Implemented a `bytesSinceLastCheck` counter in `ModelDownloader.kt` to throttle clock checks to every 512KB of data processed.
## 2024-05-04 - State Reading Deferral in Jetpack Compose
**Learning:** Reading fast-emitting Flow states (like STT process/progress updates) at the top level of a large layout (e.g., `Scaffold`) causes the entire layout to recompose rapidly, leading to severe UI jank.
**Action:** Defer state reads by pushing them down the component tree into smaller, isolated Composables (e.g., extracting buttons into a `ControlButtons` composable that reads the state internally), ensuring only the UI elements that actually depend on the state are recomposed.
