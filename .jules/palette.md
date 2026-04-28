## 2024-05-24 - Accessibility improvement for Jetpack Compose RadioButtons
**Learning:** In Jetpack Compose, wrapping a `RadioButton` and its accompanying `Text` inside a `Row` and applying the `Modifier.selectable` modifier to the `Row` makes the entire row clickable and properly exposes the component as a single radio button with text label to screen readers.
**Action:** Always wrap `RadioButton` and its label in a `Modifier.selectable` container to improve touch target size and accessibility.

## 2024-05-24 - Visual affordance for custom dropdowns in Jetpack Compose
**Learning:** Custom dropdown triggers built with standard buttons (like `OutlinedButton`) in Jetpack Compose lack native visual affordance, potentially confusing users who might mistake them for regular action buttons.
**Action:** Always include a trailing `ArrowDropDown` icon with a descriptive `contentDescription` inside the trigger component for custom dropdowns to clearly indicate the expanding functionality.

## 2024-05-24 - Human-readable language names
**Learning:** Using raw ISO language codes (like "en", "es") in UI elements such as dropdowns and lists provides poor UX. Users expect human-readable display names for language selections.
**Action:** Always format language codes using `Locale(lang).displayLanguage.replaceFirstChar { it.uppercase() }` to display localized, human-readable language names to the user.
