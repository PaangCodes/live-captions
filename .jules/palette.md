## 2024-05-24 - Accessibility improvement for Jetpack Compose RadioButtons
**Learning:** In Jetpack Compose, wrapping a `RadioButton` and its accompanying `Text` inside a `Row` and applying the `Modifier.selectable` modifier to the `Row` makes the entire row clickable and properly exposes the component as a single radio button with text label to screen readers.
**Action:** Always wrap `RadioButton` and its label in a `Modifier.selectable` container to improve touch target size and accessibility.

## 2024-05-24 - Visual affordance for custom dropdowns in Jetpack Compose
**Learning:** Custom dropdown triggers built with standard buttons (like `OutlinedButton`) in Jetpack Compose lack native visual affordance, potentially confusing users who might mistake them for regular action buttons.
**Action:** Always include a trailing `ArrowDropDown` icon with a descriptive `contentDescription` inside the trigger component for custom dropdowns to clearly indicate the expanding functionality.

## 2024-05-25 - Localized language display names
**Learning:** Raw ISO language codes (e.g., "en", "es") are not user-friendly in UI elements like dropdown menus or downloaded language lists.
**Action:** Always format raw ISO language codes into localized, human-readable language names using `java.util.Locale(lang).displayLanguage.replaceFirstChar { it.uppercase() }` to improve the intuitiveness of language selection interfaces.
## 2024-05-18 - Compose State Leak in Dynamic Lists
**Learning:** When iterating over a dynamic list in Jetpack Compose to generate UI components that hold internal mutable state (e.g., a "show dialog" boolean), that state can leak or misalign if the list reorders or changes.
**Action:** Always wrap the content block of the iteration in a `key(itemIdentifier) { ... }` function to bind the internal state strictly to that specific item, ensuring proper lifecycle and tracking across recompositions.
## 2024-05-26 - Action buttons missing visual affordance
**Learning:** Primary call-to-action buttons (like "Start" or "Stop") without icons can lack visual distinctiveness and fail to convey their purpose as quickly as those with accompanying icons.
**Action:** Always add standard icons (e.g., `PlayArrow`, `Stop`/`Close`) to primary action buttons and consider semantic coloring (e.g., error color for stop/destructive actions) to improve at-a-glance recognition and usability.

## 2024-05-27 - Map internal sealed class names to user-friendly text in UI
**Learning:** Exposing internal development class names (like `SttState.Downloading` via `javaClass.simpleName`) directly in the UI creates a jarring user experience. Users need clear, actionable, and localized text instead of technical jargon.
**Action:** Always map internal state definitions (like Sealed Classes) into user-friendly localized text, and optionally apply semantic text styling (e.g., `primary` color for ready states, `error` color for failure states, `onSurfaceVariant` for idle states) to improve the clarity of system status indicators.

## 2024-05-28 - Safely deriving action button availability
**Learning:** Deriving action button `enabled` properties directly from asynchronous state variables can introduce critical UI blocking bugs if nullability (e.g., optional features) or intermediate system states (e.g., loading, errors) are not fully accounted for.
**Action:** When deriving enabled states for complex processes, explicitly handle null cases for optional state variables (e.g., `transState == null || transState.value is Ready`), and ensure cancellation/stop buttons are enabled across all active intermediate or error states to prevent users from being trapped.

## 2024-05-30 - Prevent rendering empty overlays
**Learning:** Rendering overlay boxes when the backing state (e.g., text content) is empty results in unsightly blank rectangles on the screen, degrading the user experience.
**Action:** Always conditionally hide UI elements like text overlays when their content is empty, and enhance them with visual boundaries like rounded corners, appropriate padding, and centered text alignment for a more polished look.

## 2026-05-07 - Add visible loading states for asynchronous tasks
**Learning:** Relying solely on text status changes (e.g., 'Initializing Engine...') during asynchronous operations can be overlooked by users, leaving them uncertain if the app is frozen or actively working.
**Action:** Always provide clear visual feedback, such as an indeterminate `LinearProgressIndicator` or `CircularProgressIndicator`, during asynchronous operations like model downloading or initialization to reassure the user that the background task is progressing.

## 2024-05-31 - Add visual loading states for ML Kit model downloads
**Learning:** Translating language models requires a large download and might take several seconds or minutes. Without explicit visual affordance per item (e.g. a disabled downloading button), users can unknowingly trigger multiple simultaneous downloads or assume the app froze.
**Action:** Always provide explicit visual loading feedback (like a `CircularProgressIndicator` on the button) coupled with a disabled state for long-running item-level downloads, like ML Kit models.

## 2024-06-03 - Add helper text explaining technical trade-offs
**Learning:** When presenting users with technical configuration options (like choosing between "Vosk" or "Whisper" STT engines), using only the technical names can be confusing and alienating for non-technical users. It forces them to guess or research the difference.
**Action:** Always provide descriptive helper text beneath technical options that translates the underlying mechanism into clear, practical user-facing trade-offs (e.g., "Fast / Battery Saver" vs "High Accuracy / Heavy") so users can make informed decisions based on their needs.

## 2024-06-04 - Dynamic Loading States & Contrast for Disabled Buttons
**Learning:** When primary action buttons are disabled due to asynchronous background operations (e.g., STT/Translation model downloads), they should convey what the blocking task is. Also, when adding an inline `CircularProgressIndicator` inside a styled component (like a `Button`), you must explicitly set its `color = LocalContentColor.current`. Otherwise, it won't dynamically inherit the parent's contrasting content color and might not be visible against the background.
**Action:** Always provide an inline loading indicator with explicit `color = LocalContentColor.current` and dynamically update the button text to explicitly describe the blocking state (e.g., 'Downloading STT Model...') to prevent user confusion and ensure visual accessibility.
## 2026-05-11 - Inherit disabled colors for inline progress indicators in Jetpack Compose
**Learning:** When adding an inline `CircularProgressIndicator` inside a styled component (like a `Button`), hardcoding the color (e.g., `MaterialTheme.colorScheme.onPrimary`) can cause severe contrast issues when the component enters a disabled state. The background changes to a disabled color (e.g., light grey), but the hardcoded text/icon color remains unchanged, rendering it invisible.
**Action:** Always explicitly set the `color` parameter of inline loading indicators to `LocalContentColor.current` to ensure it dynamically inherits the parent's contrasting content color across all component states.

## 2024-06-04 - Dynamic explanations for disabled primary action buttons
**Learning:** When primary action buttons (like "Start") are disabled due to asynchronous background operations (e.g., downloading or initializing models), leaving the static text unchanged creates confusion. Users may wonder why they cannot click the button and might assume the app is broken.
**Action:** Always provide inline loading indicators (like a `CircularProgressIndicator` inside the button) and dynamically update the button text to explicitly describe the current blocking state (e.g., "Downloading STT Model...") so users understand exactly why the action is temporarily unavailable.
## 2026-05-14 - Improve contrast of disabled CircularProgressIndicator
**Learning:** By default in Jetpack Compose, a `CircularProgressIndicator` uses the primary theme color. When placed inside a disabled component (like a disabled `Button` during a download), it does not automatically dim to match the disabled text color, creating a jarring, high-contrast spinner against a muted background.
**Action:** Always explicitly set `color = LocalContentColor.current` when using a `CircularProgressIndicator` inline within a text component (like a `Button`), ensuring it gracefully inherits the parent's current active or disabled content color for a cohesive UX.
