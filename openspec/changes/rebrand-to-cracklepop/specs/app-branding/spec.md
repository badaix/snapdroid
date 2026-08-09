## Purpose

Establishes `cracklepop` as the fork's consistent visible product identity while preserving the technical Snapcast identity required for server and package compatibility.

## ADDED Requirements

### Requirement: Android app presents the cracklepop product name

The Android application SHALL present `cracklepop` as its product name in the application label and on the main screen's upper-left title area.

#### Scenario: User opens the main screen

- **WHEN** the user launches the application
- **THEN** the upper-left title reads `cracklepop` and does not read `Snapcast` or `Snapdroid`

#### Scenario: Android identifies the application

- **WHEN** the Android launcher or system app UI displays the application name
- **THEN** it displays `cracklepop`

### Requirement: Maintained branding identifies the fork as cracklepop

Repository-facing branding surfaces maintained by this fork SHALL identify the product and repository as `cracklepop` rather than `snapdroid`, and SHALL use fork-appropriate links where a link identifies the fork itself.

#### Scenario: User reads maintained project metadata

- **WHEN** the user reads the project README, privacy policy, release metadata, or CI artifact naming
- **THEN** current product branding identifies `cracklepop` and does not present `snapdroid` as the current fork identity

#### Scenario: Technical Snapcast references remain necessary

- **WHEN** a text or link refers to the Snapcast protocol, server, native submodule, dependency source, or upstream technical project
- **THEN** the reference remains technically accurate and is not renamed solely because the product branding changed

### Requirement: Rebranding preserves compatibility identity

The rebranding SHALL NOT change the Android package/application identity `de.badaix.snapcast`, Snapcast protocol terminology, server connection behavior, or dependency coordinates.

#### Scenario: Existing installation and server compatibility are evaluated

- **WHEN** the rebranded application is built and connected to a Snapcast server
- **THEN** it retains package identity and uses the same Snapcast connection and protocol identifiers as before
