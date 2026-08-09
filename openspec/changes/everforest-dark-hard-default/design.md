## Context

The app is a small single-module Android application using AppCompat and Material Components. Its active theme currently uses `DayNight` with a small blue/gray palette, while some styles and layouts contain direct colors.

## Goals / Non-Goals

**Goals:**

- Make Everforest dark hard the default appearance.
- Centralize the palette and update the active theme/styles and any conflicting direct UI colors.
- Keep common surfaces, dialogs, settings, and system bars readable.

**Non-Goals:**

- Adding a theme picker or other Everforest variants.
- Redesigning layouts, typography, launcher artwork, or strings.
- Changing package identity, dependencies, native code, or Snapcast behavior.

## Decisions

- Use named color resources for the Everforest palette and reference them from existing themes/styles.
- Replace the `DayNight` default with a dark AppCompat theme so light system mode cannot switch the app to a light appearance.
- Update `values-night` and API-specific system-bar resources consistently.
- Review settings and dialogs because they may use platform defaults, but make only the resource/style changes needed for dark legibility.
- Use the pinned upstream palette source for reproducible color values: [palette.md](https://raw.githubusercontent.com/sainnhe/everforest/85a86eb62409e3ec88713bff3d1b9d7374e112e4/palette.md), commit `85a86eb62409e3ec88713bff3d1b9d7374e112e4`.

## Risks / Trade-offs

- Platform dialogs or preferences may retain light defaults; inspect them and add only targeted style overrides.
- A forced dark default may not suit every user, but no theme preference is requested in this change.
- Direct colors may be missed; review active resources before finalizing.

## Migration Plan

1. Add the palette and update active theme/style resources.
2. Fix only conflicting active UI colors and platform surfaces.
3. Run the existing build/lint checks.
4. Revert the resource-only change if visual or build checks fail.
