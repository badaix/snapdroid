## Context

The project is a single Android app module with Java UI code, Android string/resources, repository metadata, Fastlane store metadata, and CI configuration. The current application/package identity is `de.badaix.snapcast`; Snapcast remains the underlying protocol and native player. See `proposal.md` and `specs/app-branding/spec.md` for the required user-visible behavior.

## Goals / Non-Goals

**Goals:**

- Establish one exact visible product name, lowercase `cracklepop`, for the launcher/app label and main-screen title.
- Replace inherited fork/product branding in maintained documentation, metadata, and CI naming while retaining technically accurate upstream references.
- Make the branding inventory auditable so no `snapdroid` occurrence is changed accidentally when it names an upstream source.

**Non-Goals:**

- Changing `applicationId`, Java package names, Snapcast JSON-RPC methods, network ports, native library names, dependency coordinates, or server terminology.
- Redesigning the UI beyond the title/branding text or changing release/signing behavior.

## Decisions

### Use centralized Android resources for app-visible names

Find the existing application-label and toolbar/title string sources and change their displayed value to `cracklepop`, reusing the existing resource flow rather than hard-coding a new title in activity code. This keeps launcher and in-app branding consistent and localizable where the project already supports localization.

**Alternative considered:** hard-code `cracklepop` in `MainActivity`; rejected because it can diverge from the launcher label and bypasses the existing resource structure.

### Classify branding occurrences before changing them

Search tracked text and resource files for `snapdroid`, `Snapdroid`, `Snapcast`, and related upstream URLs. Change occurrences that identify this fork or product, but preserve references that identify the Snapcast protocol/server, the native submodule, or upstream dependency sources. Update a repository URL only when the fork's actual URL is known; do not invent one from the local directory name.

**Alternative considered:** global case-insensitive replacement; rejected because it would corrupt technical names, package compatibility, and valid upstream links.

### Validate both source branding and the built app

Use targeted searches to review remaining inherited branding, run the existing Gradle build, and install/launch the debug APK on the authorized Shield to verify the visible title and launcher label. Keep the package ID unchanged during validation.

## Risks / Trade-offs

- [Risk] An inherited URL may refer to the upstream project rather than this fork → classify each occurrence by ownership before editing and leave technical upstream URLs accurate.
- [Risk] Launcher and toolbar labels may be sourced from different resources → verify both the Android manifest/application label and the rendered main-screen title after the build.
- [Risk] Replacing the installed signed release with a debug APK can require uninstalling the release → use the authorized test device only and record the package identity/version during deployment.

## Migration Plan

1. Inventory and update app resources, documentation, metadata, and CI branding according to the classification rules.
2. Build the debug APK and run focused source/manifest checks.
3. Install the debug APK on the authorized Shield and verify the launcher/main-screen branding.
4. Roll back by reverting the branding-only commit if the visual or compatibility checks fail; package and persisted app identity remain unchanged.
