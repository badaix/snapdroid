## Why

This fork still presents inherited Snapdroid/Snapcast branding, making it unclear when the modified fork is running. Establishing `cracklepop` as the visible product identity gives the fork a reliable visual marker on the Shield and removes misleading upstream branding.

## What Changes

- Replace user-visible `snapdroid` and inherited product branding with `cracklepop` across the app and repository-facing branding surfaces.
- Change the main app screen's upper-left title from `Snapcast` to `cracklepop`.
- Preserve the Android package/application identity `de.badaix.snapcast` and Snapcast protocol/server terminology needed for compatibility.
- Update branding-related upstream links, metadata, and text where they identify the product or repository, without changing unrelated technical URLs.

## Capabilities

### New Capabilities

- `app-branding`: Defines the visible product name and fork branding presented by the Android app and its maintained repository metadata.

### Modified Capabilities

<!-- No existing OpenSpec capabilities are present; this change introduces the branding contract. -->

## Impact

- Android resources and activities that supply the application label and main-screen title.
- Repository documentation and metadata containing inherited product/repository branding.
- No API, package-name, network-protocol, or dependency changes are intended.
