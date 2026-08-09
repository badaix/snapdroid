## 1. Add the palette and theme

- [x] 1.1 Add centralized Everforest dark hard color resources from the pinned upstream palette.
- [x] 1.2 Replace the default `DayNight` theme with a dark theme and update night/API-specific system-bar resources.
- [x] 1.3 Update active styles and direct UI colors needed for readable surfaces, controls, dialogs, settings, and system bars.

## 2. Preserve the existing app

- [x] 2.1 Confirm the package ID, Snapcast behavior, launcher artwork, and localized strings are unchanged.

## 3. Verify

- [x] 3.1 Check active resources for stale blue/gray colors and compare the palette with the pinned upstream source.
- [x] 3.2 Run the configured JDK 17 Gradle build and focused Android lint/resource checks.
- [ ] 3.3 If a device is available, inspect the main screen, dialogs, settings, controls, and system bars for dark-theme legibility.
