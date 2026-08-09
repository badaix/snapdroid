## Purpose

Provide the Android app with a consistent Everforest dark hard appearance by default, using centralized resources and preserving existing application behavior.

## ADDED Requirements

### Requirement: The default theme is Everforest dark hard

The application SHALL use a dark, non-system-adaptive default theme based on Everforest dark hard. Active app surfaces, toolbar, dialogs, settings screens, controls, and system bars SHALL use compatible dark colors and remain readable.

#### Scenario: Application launches

- **WHEN** the app launches without an appearance preference
- **THEN** its main background uses Everforest `bg0` (`#272E33`), primary text uses `fg` (`#D3C6AA`), and the visible UI is dark and legible

### Requirement: Palette values are centralized and authoritative

The implementation SHALL define semantic Android color resources for the Everforest dark hard palette and reference them from themes/styles instead of duplicating active UI color literals. Values SHALL match the pinned official source: [Everforest palette.md](https://raw.githubusercontent.com/sainnhe/everforest/85a86eb62409e3ec88713bff3d1b9d7374e112e4/palette.md), retrieved 2026-08-09.

#### Scenario: Palette is reviewed

- **WHEN** the color resources are compared with the pinned source
- **THEN** the dark hard background colors and shared dark foreground/accent colors match
- **AND** the active theme does not use the old blue/gray palette

### Requirement: Existing behavior and identity are preserved

The theme change SHALL NOT alter the package ID, Snapcast connection/playback behavior, launcher artwork, or localized strings.

#### Scenario: Built app is inspected

- **WHEN** the manifest and relevant runtime configuration are reviewed after the change
- **THEN** the package remains `de.badaix.snapcast` and the existing app behavior is unchanged
