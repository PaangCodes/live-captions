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
## 2024-05-29 - Caption Overlay UX Improvements
**Learning:** Hardcoded full-width black backgrounds for overlays result in unsightly empty black boxes obstructing the screen when there is no text. Additionally, floating captions lack a cohesive feel without proper visual boundaries (like margins and rounded corners) and centered text alignment.
**Action:** Always conditionally hide UI elements when their backing state (e.g., text content) is empty. Ensure text overlays use a `RoundedCornerShape`, appropriate padding (margins), and `TextAlign.Center` to improve readability and visual polish.
