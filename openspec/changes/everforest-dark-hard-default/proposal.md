## Why

The app currently uses a blue/gray AppCompat palette. Adopt Everforest dark hard as the default so the app has a consistent dark visual identity.

## What Changes

- Replace the active Android theme colors with the Everforest dark hard palette.
- Ensure the main app surfaces and dialogs remain readable in the dark theme.
- Preserve app identity, Snapcast behavior, and launcher artwork.

## Capabilities

### New Capabilities

- `everforest-dark-hard-theme`: Provides the default Everforest dark hard Android appearance.

### Modified Capabilities

<!-- No existing main capabilities are defined. -->

## Impact

- Android color, theme, and style resources under `Snapcast/src/main/res/`.
- No API, dependency, package ID, native, or protocol changes.
