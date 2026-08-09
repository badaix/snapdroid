# Verification

Verification results for the `rebrand-to-cracklepop` change. Commands were run on
`feat/rebrand-to-cracklepop` at commit `c1d2330` plus the uncommitted branding changes.

## Fork remote

Confirmed with `git remote -v`:

```
origin   https://github.com/slatkin/cracklepop.git (fetch)
origin   https://github.com/slatkin/cracklepop.git (push)
upstream https://github.com/snapcast/snapdroid.git (fetch)
upstream https://github.com/snapcast/snapdroid.git (push)
```

The fork repository is `https://github.com/slatkin/cracklepop`; links that identify
this repository now use that URL. The `upstream` remote still tracks the inherited
Snapdroid source and is left untouched.

## Checks and build

- `git diff --check` passes (no whitespace errors).
- Debug build: `JAVA_HOME=/usr/lib/jvm/java-17-openjdk ./gradlew assembleDebug` succeeds
  (incremental, current tree; only an unrelated deprecation note is emitted).
- Built package identity (`aapt dump badging` of the debug APK):
  - `package: name='de.badaix.snapcast' versionName='0.29.0.0'`
  - `application-label:'cracklepop'`
  - `launchable-activity: name='de.badaix.snapcast.MainActivity' label='cracklepop'`
- Source identity unchanged: `applicationId "de.badaix.snapcast"` and
  `namespace 'de.badaix.snapcast'` in `Snapcast/build.gradle`; manifest components and
  `de.badaix.snapcast.START_SERVICE`/`STOP_SERVICE` actions untouched.

## Authorized Shield verification (task 4.2)

Device verification was executed on the authorized Shield at `192.168.0.106:5555`
(adb transport, device authorized):

1. `adb install -r Snapcast/build/outputs/apk/debug/Snapcast-debug.apk` -> `Success`.
2. `adb shell am start -n de.badaix.snapcast/.MainActivity` -> `Status: ok`,
   `LaunchState: COLD`, activity `de.badaix.snapcast/.MainActivity`.
3. `uiautomator dump` of the main screen shows `text="cracklepop"` as a
   `android.widget.TextView` at bounds `[32,12][172,50]`, i.e. the action bar's
   upper-left title position.
4. `adb shell cmd package resolve-activity` resolves the launcher activity to
   `de.badaix.snapcast/.MainActivity`; the APK's `application-label` and launcher
   label are both `cracklepop`.
5. Navigating to the About screen (overflow menu -> About) shows the action bar
   title `"About Snapcast"` (the upstream technical identity, intentionally preserved)
   and renders `about.html` with title "Snapcast licenses", the fork source link
   `https://github.com/slatkin/cracklepop`, and the upstream link
   `https://github.com/badaix/snapcast` with BadAix copyright/author lines
   intact.

Both the launcher/app label and the main-screen upper-left title read `cracklepop`;
the About screen title reads `About Snapcast` (upstream identity, not renamed per
spec clause allowing technical Snapcast identity on the About page).
So task 4.2 is verified.

## Intentionally preserved technical/upstream branding (task 4.3)

The built package remains `de.badaix.snapcast` (confirmed in the APK and on the
device). The following are intentionally preserved and were not renamed by this
change:

- Android package/application identity `de.badaix.snapcast`, Java packages, manifest
  component names, and `de.badaix.snapcast.START_SERVICE`/`STOP_SERVICE` broadcasts.
- Snapcast protocol/server terminology and connection identifiers (JSON-RPC methods,
  control/stream ports, `Snapclient` notification strings, `Auto start Snapclient`).
- AboutActivity title "About Snapcast" (upstream technical identity, not renamed).
- Fastlane store titles "Snapcast - ..." and full-description product references
  `<i>Snapcast</i>` (technical product name in store metadata, not fork branding).
- `about.html` title "Snapcast licenses" (upstream license page identity),
  upstream attribution and licensing: BadAix copyright (2014-2024),
  Johannes Pohl author line, GPLv3-or-later notice, "Snapcast is licensed under the
  GPL" paragraph, upstream PayPal donation link, the Snapcast banner asset, the
  external-libraries list, the GPL text, and the Xiph.org license.
- The native snapcast submodule (`Snapcast/src/main/cpp/snapcast` @ `v0.29.0`,
  commit `208066e5`) and all dependency-source URLs.
- The inherited `upstream` git remote (`https://github.com/snapcast/snapdroid.git`).

## Status

- Task 4.2 (`[x]`): verified on the authorized Shield as documented above.
- Task 4.3 (`[x]`): confirmed the built package identity and documented preserved
  branding in this review artifact.
