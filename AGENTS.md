# AGENTS.md

Compact guide for OpenCode agents working in this repo — a fork of
[snapcast/snapdroid](https://github.com/snapcast/snapdroid.git). Only
repo-specific facts; verify against the cited files before relying on them.

## Project layout

- Single Android app module `:Snapcast` (`settings.gradle`). No other modules.
- AGP 8.3.1 (`build.gradle`); Gradle wrapper 8.4
  (`gradle/wrapper/gradle-wrapper.properties`); Java 17
  (`Snapcast/build.gradle` compileOptions); `compileSdk`/`targetSdk` 35,
  `minSdk` 21; `applicationId` and `namespace` `de.badaix.snapcast`.
- No `src/test` or `src/androidTest` sources, and no separate task-runner or
  typecheck config. Android lint is configured in `Snapcast/build.gradle` to
  disable `MissingTranslation`. Do not invent test commands.

## Required setup

- Native build comes from the submodule `Snapcast/src/main/cpp/snapcast`
  (upstream badaix/snapcast), which supplies the configured CMake build. After a
  fresh checkout run `git submodule update --init --recursive`.
- Android SDK: platform/API 35 and Build-Tools 35.0.0; CMake 3.22.1 and NDK
  27.0.12077973; four ABIs: `armeabi-v7a`, `arm64-v8a`, `x86`, `x86_64`
  (`Snapcast/build.gradle`).
- Gradle needs the SDK location in ignored `local.properties` (`sdk.dir=/path/to/Android/Sdk`)
  or via `ANDROID_HOME`/`ANDROID_SDK_ROOT`; do not commit local SDK paths.
- Dependencies: eight v0.29.0 AARs (boost, flac, oboe, ogg, opus, soxr, tremor,
  vorbis). Either place them in `Snapcast/libs/` (git-ignored) or use the GitHub
  Packages repo with `GITHUB_USER`/`GITHUB_TOKEN` (read:packages). See
  `Snapcast/libs/readme.txt` and `.github/workflows/ci.yml` for the exact
  download list — do not duplicate it here.

## License

- GPL-3.0-or-later (`LICENSE`). Preserve source notices and account for GPL
  obligations when distributing modified builds.

## Build

- CI runs: `JAVA_HOME=$JAVA_HOME_17_X64 ./gradlew build`
  (`JAVA_HOME_17_X64` is a GitHub-hosted-runner env var). The local equivalent
  is `JAVA_HOME=/path/to/jdk-17 ./gradlew build`.
- Gradle cannot start in this checkout without Java installed — install JDK 17
  first.
- Android lint is supplied by the Android Gradle Plugin; run
  `JAVA_HOME=/path/to/jdk-17 ./gradlew lintDebug` for a focused lint check.

## Development tooling

- OpenCode's global config enables `/usr/bin/jdtls` for Java using JDK 21 and
  `/usr/bin/clangd` for native C/C++; restart OpenCode after config changes.
- The Java LSP may report syntax-only diagnostics until it imports the Gradle
  classpath; use Gradle build/lint output for authoritative Android checks.

## Architecture

- `MainActivity`: UI/control entry point; discovers servers via mDNS
  `_snapcast._tcp.` and drives the control connection.
- `control/RemoteControl`: speaks Snapcast JSON-RPC to the server's control port
  (default 1705); the server audio stream port defaults to 1704.
- `SnapclientService`: foreground service that launches the native
  `libsnapclient.so` (built from the snapcast submodule) and owns playback.
- `BroadcastReceiver`: handles `BOOT_COMPLETED` (when autostart is enabled) and
  explicit `de.badaix.snapcast.START_SERVICE`/`STOP_SERVICE` broadcasts.

## Fork gotchas

- Inherited upstream files still say snapdroid/badaix (submodule and dependency
  URLs, upstream repo/docs; the package/application id remain `de.badaix.snapcast`).
  README badge/release links and CI paths now use the fork name `cracklepop`.
  Verify fork-specific URLs/names before changing them.

## CI ordering

- `.github/workflows/ci.yml` order matters: checkout, `git submodule update
  --init --recursive`, download the AARs into `Snapcast/libs/`, then build.
  Signing and artifact upload run only on pushes (they require secrets).
