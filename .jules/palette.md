## 2024-05-24 - Accessibility improvement for Jetpack Compose RadioButtons
**Learning:** In Jetpack Compose, wrapping a `RadioButton` and its accompanying `Text` inside a `Row` and applying the `Modifier.selectable` modifier to the `Row` makes the entire row clickable and properly exposes the component as a single radio button with text label to screen readers.
**Action:** Always wrap `RadioButton` and its label in a `Modifier.selectable` container to improve touch target size and accessibility.

## 2024-05-24 - Visual affordance for custom dropdowns in Jetpack Compose
**Learning:** Custom dropdown triggers built with standard buttons (like `OutlinedButton`) in Jetpack Compose lack native visual affordance, potentially confusing users who might mistake them for regular action buttons.
**Action:** Always include a trailing `ArrowDropDown` icon with a descriptive `contentDescription` inside the trigger component for custom dropdowns to clearly indicate the expanding functionality.

## 2024-05-25 - Localized language display names
**Learning:** Raw ISO language codes (e.g., "en", "es") are not user-friendly in UI elements like dropdown menus or downloaded language lists.
**Action:** Always format raw ISO language codes into localized, human-readable language names using `java.util.Locale(lang).displayLanguage.replaceFirstChar { it.uppercase() }` to improve the intuitiveness of language selection interfaces.

## 2024-05-26 - Confirmation dialog for destructive actions
**Learning:** Destructive actions without confirmation (like deleting downloaded models) lead to poor user experiences and accidental data loss. Furthermore, when managing states like dialog visibility inside a dynamic list in Jetpack Compose, the `key(identifier)` function must be used so that local state tracks correctly across recompositions.
**Action:** Always add an `AlertDialog` to prompt the user before performing destructive actions. Always wrap components in a `key()` block when mapping lists to Compose elements that contain their own mutable state.
