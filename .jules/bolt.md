## 2025-01-24 - Optimized Progress Reporting in I/O Loops
 **Learning:** Frequent calls to `System.currentTimeMillis()` in tight loops (like an 8KB buffer read loop) can introduce unnecessary CPU overhead. Reducing the frequency of these calls by checking a byte-processed threshold first significantly improves efficiency.
 **Action:** Implemented a `bytesSinceLastCheck` counter in `ModelDownloader.kt` to throttle clock checks to every 512KB of data processed.

## 2025-01-24 - Isolate High-Frequency Flow Emissions to Child Components
**Learning:** Recomposing the entire `MainActivity` and `Scaffold` on every high-frequency `StateFlow` progress update (from `currentEngine.state.collectAsState()`) causes significant UI jank. Even if the state is observed at the top level, pulling the exact variable reads into smaller, localized child components (like `SttConfigCard` and `ControlButtons`) avoids recomposing parts of the UI that don't depend on the state.
**Action:** Move the `val sttState by currentEngine.state.collectAsState()` observation entirely into child components (like `ControlButtons` and `SttConfigCard`) instead of hoisting it into `MainActivity` root, preventing top-level recompositions on every byte-progress update.
