# Settings Screen — Design Spec

**Date:** 2026-06-07
**Status:** Approved, ready for implementation planning

## Overview

Add a settings screen to Recipely with two user-configurable preferences:

- **Appearance** — System / Light / Dark theme. Default: **System**.
- **Language** — System / English / Deutsch. Default: **System**.

Both preferences persist across app restarts. Theme changes apply instantly (reactive
recompose, no Activity restart). Language changes apply via Activity recreation so string
resources reload from the matching `values/` folder.

**Out of scope:** any further settings, an About screen, export/share, account/cloud sync.

## Decisions (locked in)

| Topic | Decision |
|-------|----------|
| Settings entry point | ⚙️ icon in the list's editorial header (next to "Your collection" title) |
| Theme control | 3-way `SingleChoiceSegmentedButtonRow` (System / Light / Dark) |
| Language control | Radio selection list (System / English / Deutsch) |
| Language switch mechanism | `AppCompatDelegate.setApplicationLocales` (AppCompat locale delegate) |
| Theme persistence | DataStore (Preferences) — reactive `Flow` |
| Language persistence | AppCompat's own locale store (`autoStoreLocales`) — avoids cold-start recreation flash |

## Architecture

Follows the existing lean MVVM: Compose UI → ViewModel → Repository → DataStore. New code
lives under `ui/settings/` and `data/`, wired through the existing `di/AppContainer`.

### New dependencies (add to `gradle/libs.versions.toml`, reference as `libs.*`)

- `androidx.datastore:datastore-preferences` — theme persistence.
- `androidx.appcompat:appcompat` — locale delegate + `AppCompatActivity`.

### 1. Entry point & navigation

- `RecipeListScreen`: the editorial header `Column` (label + "Your collection" title) becomes a
  `Row` with `Arrangement.SpaceBetween`; a trailing `IconButton` (`Icons.Outlined.Settings`,
  content description from a string resource) calls a new `onOpenSettings` lambda.
- `RecipelyNavHost`: add `Routes.SETTINGS = "settings"`. The `list` composable receives
  `onOpenSettings = { navController.navigate(Routes.SETTINGS) }`. New `composable(Routes.SETTINGS)`
  renders `SettingsScreen(onBack = { navController.popBackStack() })`.

### 2. Settings screen UI (`ui/settings/`)

`SettingsScreen.kt`

- `Scaffold` with a `TopAppBar`: back arrow (`onBack`) + title "Settings".
- Two sections, each a titled `Surface`/panel matching the warm-editorial card style:
  - **Appearance**: `SingleChoiceSegmentedButtonRow` with three segments
    (System / Light / Dark), bound to `themeMode`.
  - **Language**: a column of selectable radio rows (System / English / Deutsch),
    bound to `appLanguage`.
- Obtains its ViewModel via the established `viewModelFactory { initializer { ... } }` pattern,
  pulling `container.settingsRepository` from `RecipelyApp` through `LocalContext`.

`SettingsViewModel.kt`

- `themeMode`: `StateFlow<ThemeMode>` from the repository's DataStore flow
  (`stateIn(viewModelScope, WhileSubscribed, SYSTEM)`) — genuinely reactive.
- `appLanguage`: a one-shot value read on screen entry (e.g. exposed via UI state seeded from
  the repository's current `AppLanguage`). It does **not** need a reactive flow: changing the
  language recreates the Activity, so the screen is rebuilt with the fresh value anyway.
- `setThemeMode(mode)` — writes to the repository (DataStore).
- `setLanguage(language)` — writes the choice and calls
  `AppCompatDelegate.setApplicationLocales(...)`. Note: locale application is a
  process/Activity concern; keep the actual `setApplicationLocales` call thin and out of any
  pure-logic path so the ViewModel stays JVM-unit-testable (mirrors how image acquisition is
  kept in the Compose layer, not the ViewModel). The simplest split: ViewModel exposes the
  selected `AppLanguage`; the Compose layer (or a small mapper) maps it to a
  `LocaleListCompat` and calls the delegate.

### 3. Persistence (`data/SettingsRepository.kt`)

- Enums:
  - `enum class ThemeMode { SYSTEM, LIGHT, DARK }`
  - `enum class AppLanguage { SYSTEM, ENGLISH, GERMAN }` (with helper mapping to BCP-47 tags:
    SYSTEM → `""`, ENGLISH → `"en"`, GERMAN → `"de"`).
- `SettingsRepository` wraps a Preferences `DataStore`:
  - `val themeMode: Flow<ThemeMode>` (defaults to `SYSTEM` when unset).
  - `suspend fun setThemeMode(mode: ThemeMode)`.
  - For language, the source of truth is AppCompat's locale store; the repository (or a thin
    helper) reads the current selection via `AppCompatDelegate.getApplicationLocales()` and
    maps it back to `AppLanguage`. Language is therefore **not** stored in DataStore.
- `AppContainer` constructs and holds `settingsRepository` alongside `repository`/`imageStore`.

### 4. Theme application (reactive, no recreation)

- `MainActivity` collects `themeMode` as Compose state (`collectAsState`) and resolves
  `darkTheme` passed to `RecipelyTheme`:
  - `SYSTEM` → `isSystemInDarkTheme()`
  - `LIGHT` → `false`
  - `DARK` → `true`
- Switching theme triggers an immediate recompose — no Activity restart, no flash.

### 5. Language application

- On change: `AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))`
  (`""` for System). AppCompat recreates the Activity automatically; strings reload from
  `values-de`/`values`.
- Persistence: enable AppCompat `autoStoreLocales` so the choice survives process death without
  a cold-start recreation flash. This requires the manifest service + meta-data entry:
  - `androidx.appcompat.app.AppLocalesMetadataHolderService` with
    `<meta-data android:name="autoStoreLocales" android:value="true" />`.

### 6. AppCompat implication (host changes)

`setApplicationLocales` requires an `AppCompatActivity` on Android < 13, which in turn requires
an AppCompat window theme:

- `MainActivity` extends `AppCompatActivity` instead of `ComponentActivity`. `setContent`,
  `enableEdgeToEdge`, and the Compose tree are unaffected.
- `res/values/themes.xml` and `res/values-night/themes.xml`: change the `Theme.Recipely` parent
  from `android:Theme.Material[.Light].NoActionBar` to **`Theme.AppCompat.DayNight.NoActionBar`**.
  Since Compose draws all UI and `enableEdgeToEdge` governs the system bars, there is no visible
  difference.
- Update `CLAUDE.md`: relax the "no AppCompat" note to reflect that AppCompat is now a
  deliberate dependency for the locale delegate (UI remains Compose-only — no AppCompat views).

## Localization

New string resources in both `values/strings.xml` and `values-de/strings.xml`:

- Screen title, "Appearance" / "Language" section headers.
- Option labels: System, Light, Dark, English, Deutsch.
- Content descriptions: settings icon, back button.

(English and German label "Deutsch"/"English" are the language's own endonym in both locales.)

## Testing

- **JVM unit test** `SettingsViewModelTest` (`app/src/test`) using `MainDispatcherRule` and a
  `FakeSettingsRepository` (in-memory, mirrors `FakeRecipeRepository`):
  - Default `themeMode` is `SYSTEM`.
  - `setThemeMode` propagates to the `themeMode` flow.
  - (Language has no DataStore flow to assert — it lives in AppCompat's store.)
  - Activate `stateIn(WhileSubscribed)` flows with a `backgroundScope.launch { ... collect {} }`
    before asserting.
  - Locale-delegate calls are not unit-tested (Android/process concern); they're verified by a
    successful build/run, per the project's Compose-UI testing convention.
- Compose UI (settings screen, header icon) verified by successful build/run, not red-green.

## Implementation order (suggested)

1. Add dependencies (DataStore, AppCompat) to the version catalog + `app/build.gradle.kts`.
2. `SettingsRepository` + enums; wire into `AppContainer`.
3. Host changes: `AppCompatActivity`, window theme parents, manifest `autoStoreLocales` service.
4. `SettingsViewModel` (+ `FakeSettingsRepository`, unit test) — TDD.
5. `SettingsScreen` UI.
6. Theme wiring in `MainActivity` (collect `themeMode` → `RecipelyTheme`).
7. Header settings icon + `Routes.SETTINGS` navigation.
8. Strings (`values` + `values-de`); update `CLAUDE.md`.
