## 2024-05-24 - Accessibility improvement for Jetpack Compose RadioButtons
**Learning:** In Jetpack Compose, wrapping a `RadioButton` and its accompanying `Text` inside a `Row` and applying the `Modifier.selectable` modifier to the `Row` makes the entire row clickable and properly exposes the component as a single radio button with text label to screen readers.
**Action:** Always wrap `RadioButton` and its label in a `Modifier.selectable` container to improve touch target size and accessibility.
