# Recipe Export & Import — Design

**Date:** 2026-06-07
**Status:** Approved (design)

## Overview

Add the ability to **export all recipes to a single backup file and import them back**, surfaced as a new
**"Data"** section on the Settings screen. The backup is a **ZIP bundle** that carries full fidelity —
recipe data *and* all images — so a user can move their collection to a new device or keep an offline
backup. Import uses **merge** semantics: imported recipes are added as new entries; existing recipes are
never touched or overwritten.

This stays true to Recipely's nature: offline-only, no network, no cloud. Everything happens locally
through the Android Storage Access Framework (SAF) — the user picks where to save and which file to load.

## Goals

- Export every recipe (name, prep time, servings, nutrition, category, ordered ingredients, ordered
  steps) plus the title image and all step images into one `.zip` file the user saves via SAF.
- Import a previously exported `.zip`, adding its recipes (with images) to the local database.
- Both actions live in a new **Data** panel on the Settings screen, styled **exactly** like the approved
  mockup (see *Visual specification*).
- Clear, lightweight feedback: progress overlay during the operation, success/error snackbars afterward.

## Out of scope (by design — YAGNI)

- Cloud sync, auto-/scheduled backups.
- Selective export (single recipe / subset), encryption, password protection.
- Alternative formats (CSV, PDF, plain JSON without images).
- "Replace all" import mode and duplicate detection/merge-by-identity. Merge = always add. Re-importing
  the same file therefore creates duplicates — this is accepted.

## File format

A ZIP archive (recommended default name `recipely-backup-YYYY-MM-DD.zip`) with this layout:

```
recipely-backup-2026-06-07.zip
├── recipes.json
└── images/
    ├── img_173045.jpg
    ├── img_173112.jpg
    └── …
```

### `recipes.json`

Serialized with **kotlinx.serialization**. `image` fields hold a **ZIP-relative path** (`images/<file>`)
or `null`. Image filenames inside the ZIP are taken from the source file's basename (already unique —
`ImageStore` names files `img_<nanoTime>.jpg`); on the rare collision the writer disambiguates with a
numeric suffix.

```json
{
  "schemaVersion": 1,
  "exportedAt": "2026-06-07T10:15:30Z",
  "recipes": [
    {
      "name": "Lasagne",
      "prepTimeMinutes": 60,
      "servings": 4,
      "calories": 850,
      "carbsGrams": 60.0,
      "proteinGrams": 35.0,
      "fatGrams": 40.0,
      "category": "MAIN",
      "image": "images/img_173045.jpg",
      "ingredients": [
        { "text": "500 g minced beef", "position": 0 },
        { "text": "250 g lasagne sheets", "position": 1 }
      ],
      "steps": [
        { "text": "Brown the beef.", "image": null, "position": 0 },
        { "text": "Layer and bake.", "image": "images/img_173112.jpg", "position": 1 }
      ]
    }
  ]
}
```

- `schemaVersion` (currently `1`) lets future versions detect/upgrade. On import, an **unknown/newer**
  `schemaVersion` is treated as an invalid file (graceful error), not a crash.
- `exportedAt` is informational only (ISO-8601 UTC).
- Database `id`/`recipeId` are **not** exported — import always inserts fresh rows.
- `category` is the locale-independent `RecipeCategory.key` (or `null`). An unknown key on import is
  stored as-is and simply renders as uncategorized (matches `RecipeCategory.fromKey` returning null).

## Architecture

Follows the existing layering (Compose → ViewModel → data) and the established image pattern: the
**Compose layer owns SAF** (it produces a `Uri`, exactly as `RecipeEditScreen` owns the photo
picker/camera), the **data layer owns the bytes** (zip/json/image IO), and the **ViewModel** only
orchestrates and exposes UI state — keeping it free of Android `Uri` plumbing where practical.

### New units

1. **`data/backup/BackupModels.kt`** — pure Kotlin, JVM-testable.
   - `@Serializable` DTOs: `BackupFile(schemaVersion, exportedAt, recipes)`, `BackupRecipe`,
     `BackupIngredient`, `BackupStep`.
   - Mapping helpers between DTOs and Room entities (`RecipeWithDetails` ↔ `BackupRecipe`). The DTO uses
     ZIP-relative image paths; mapping to entities leaves the absolute path to the manager (which copies
     the file first and supplies the new path).
   - `const val SCHEMA_VERSION = 1`, `const val JSON_ENTRY = "recipes.json"`, `IMAGES_DIR = "images/"`.

2. **`data/backup/RecipeBackupManager.kt`** — Android-facing, lives in `data/` alongside `ImageStore`.
   Constructor deps: `RecipeDao`, `ImageStore`, and `ContentResolver` (or `Context`).
   - `suspend fun export(target: Uri): ExportResult` — on `Dispatchers.IO`:
     1. `dao.getAllRecipesWithDetails()` (new one-shot DAO read, children sorted by `position`).
     2. Open `contentResolver.openOutputStream(target)` → `ZipOutputStream`.
     3. Write each referenced image file as an `images/<name>` entry (dedup by tracking written names);
        build DTOs with the relative paths; write `recipes.json` last.
     4. Return `ExportResult.Success(count)`.
   - `suspend fun import(source: Uri): ImportResult` — on `Dispatchers.IO`:
     1. Open `contentResolver.openInputStream(source)` → read ZIP fully into memory
        (recipes are small; images buffered per entry).
     2. Parse `recipes.json`; missing/unparseable/`schemaVersion != 1` → `ImportResult.Invalid`.
     3. For each `BackupRecipe`: copy each referenced ZIP image into `filesDir/images/` via a new
        `ImageStore.importFromStream(...)` (returns a fresh absolute path); build `Recipe`/`Ingredient`/
        `Step` entities with `id = 0` and the new absolute paths. A referenced image missing from the ZIP
        → that path becomes `null` (recipe still imported).
     4. Insert all recipes in **one DAO transaction** (new `insertImported(...)` looping
        `upsertRecipeWithChildren` with `id = 0`). If the transaction fails, delete the images copied in
        this run (cleanup) and return `ImportResult.Error`.
     5. Return `ImportResult.Success(count)`.
   - Result types: `ExportResult { Success(count) | Empty | Error }`,
     `ImportResult { Success(count) | Invalid | Error }`.

3. **DAO additions** (`RecipeDao`):
   - `@Transaction @Query("SELECT * FROM recipes") suspend fun getAllRecipesWithDetails(): List<RecipeWithDetails>`
   - `@Transaction suspend fun insertImported(recipes: List<RecipeWithDetails>)` — loops
     `upsertRecipeWithChildren` so all imports commit atomically.

4. **`ImageStore.importFromStream(input: InputStream): String?`** — mirrors the existing
   `importFromUri`, copying an arbitrary stream (a ZIP entry) into `filesDir/images/` and returning the
   new absolute path. Reuses `newImageFile()`.

5. **`AppContainer`** — construct and expose `val backupManager: RecipeBackupManager` (built from
   `database.recipeDao()`, `imageStore`, `context.contentResolver`).

### Wiring into the existing screen

- **`SettingsViewModel`** gains the backup manager + the recipe count (to guard empty export):
  - `recipeCount: StateFlow<Int>` derived from `RecipeRepository.observeRecipes().map { it.size }`
    (so the export row can short-circuit before opening the file picker).
  - `backupState: StateFlow<BackupState>` where `BackupState = Idle | Exporting | Importing`.
  - A one-shot **event** channel/flow `events: Flow<BackupMessage>` for snackbars
    (`ExportDone(count)`, `ImportDone(count)`, `Empty`, `InvalidFile`, `Failed`).
  - `fun export(target: Uri)` / `fun import(source: Uri)` → launch on `viewModelScope`, set state, call
    the manager, emit the matching event, reset to `Idle`.
  - This means the factory now also needs `container.repository` and `container.backupManager`.

- **`SettingsScreen`** (Compose):
  - New `DataPanel` rendered below the Language section (section label `R.string.settings_data`).
  - Two SAF launchers via `rememberLauncherForActivityResult`:
    - Export: `ActivityResultContracts.CreateDocument("application/zip")`, default name
      `recipely-backup-<LocalDate.now()>.zip`; on non-null result → `vm.export(uri)`.
    - Import: `ActivityResultContracts.OpenDocument()` with
      `arrayOf("application/zip", "application/octet-stream", "*/*")`; on non-null result → `vm.import(uri)`.
  - Export row `onClick`: if `recipeCount == 0` → emit/show the *Empty* message and **do not** open the
    picker; else launch the create-document contract.
  - `Scaffold` gains a `SnackbarHost`; a `LaunchedEffect` collects `vm.events` and shows the localized
    message (success = default snackbar, error = same host; color follows theme — see visual spec).
  - While `backupState != Idle`, draw a **progress overlay** (scrim + centered paper card with a
    `CircularProgressIndicator` and label) over the screen.

## Data flow

**Export:** tap *Export* → (count > 0) SAF create-document → user picks location → `vm.export(uri)` →
overlay shown → manager reads all recipes, streams images + `recipes.json` into the ZIP → success
snackbar `"N recipes exported"`.

**Import:** tap *Import* → SAF open-document → user picks `.zip` → `vm.import(uri)` → overlay shown →
manager parses JSON, copies images into internal storage, inserts recipes in one transaction → success
snackbar `"N recipes imported"`. The list screen (observing the DAO `Flow`) updates automatically.

## Error handling

| Situation | Behavior |
|---|---|
| Export with 0 recipes | Guarded before picker: snackbar "No recipes to export"; no file written. |
| Export IO failure (stream/zip) | `ExportResult.Error` → error snackbar "Export failed". |
| Import file unreadable | `ImportResult.Error` → error snackbar "Couldn't read the file". |
| `recipes.json` missing / unparseable / wrong `schemaVersion` | `ImportResult.Invalid` → "Not a valid Recipely backup". |
| Image entry referenced but absent in ZIP | That image becomes `null`; recipe still imported. |
| DB transaction fails mid-import | Copied images from this run are deleted; "Import failed". |

All manager work runs on `Dispatchers.IO`; the UI is never blocked beyond the overlay.

## Visual specification (must match the mockup exactly)

The new section reuses the existing `SectionLabel` + `SettingsPanel` (Paper card, `outlineVariant`
border, 16 dp radius). Inside the panel: **two full-width action rows** (variant A), divided by a
`HorizontalDivider` — structurally the same as the Language rows.

Each **DataRow**:
- Leading **icon tile**: 38 dp square, 10 dp rounded corners, **tinted background**, centered **emoji**
  at 18 sp.
  - Export 📤 on **`#E9F0E7`** (soft green) — light; muted green tile in dark.
  - Import 📥 on **`#FBEEDD`** (soft orange) — light; muted amber tile in dark.
  - These are real, colored emoji glyphs (not monochrome vector icons) — matching the mockup so no
    rework is needed. New color tokens go in `ui/theme/Color.kt`:
    `ExportIconBgLight = #E9F0E7`, `ExportIconBgDark` (≈ `#22301F`),
    `ImportIconBgLight = #FBEEDD`, `ImportIconBgDark` (≈ `#3A2C18`).
- **Text column** (weight 1): title `bodyLarge` (e.g. "Export recipes"), caption `bodySmall` in
  `onSurfaceVariant` ("Save all recipes as a .zip backup").
- **Trailing chevron**: `Icons.AutoMirrored.Filled.KeyboardArrowRight`, `onSurfaceVariant`.
- Row padding ≈ horizontal 10 dp, vertical 13 dp (matches the mockup spacing).

**Progress overlay:** full-screen scrim (`Color.Black` @ ~32% alpha) + centered `Surface` (Paper, 16 dp
radius) containing a `CircularProgressIndicator` (primary) and a label ("Exporting…" / "Importing…").

**Snackbars:** success uses the default snackbar styling; errors are visually distinct (terracotta /
`error` tone). Short, single-line messages as in the mockup.

The Appearance and Language sections are unchanged.

## Strings (new — EN `values/`, DE `values-de/`)

Counts use Android **plurals** (`<plurals>`), so "1 recipe" / "N recipes" read naturally.

| Key | EN | DE |
|---|---|---|
| `settings_data` | Data | Daten |
| `export_recipes_title` | Export recipes | Rezepte exportieren |
| `export_recipes_subtitle` | Save all recipes as a .zip backup | Alle Rezepte als .zip sichern |
| `import_recipes_title` | Import recipes | Rezepte importieren |
| `import_recipes_subtitle` | Restore from a .zip backup | Aus einer .zip wiederherstellen |
| `backup_exporting` | Exporting… | Exportiere… |
| `backup_importing` | Importing… | Importiere… |
| `backup_export_empty` | No recipes to export | Keine Rezepte zum Exportieren |
| `backup_error_read` | Couldn't read the file | Datei konnte nicht gelesen werden |
| `backup_error_invalid` | Not a valid Recipely backup | Keine gültige Recipely-Sicherung |
| `backup_error_export` | Export failed | Export fehlgeschlagen |
| `backup_error_import` | Import failed | Import fehlgeschlagen |
| `backup_export_done` (plural) | %d recipe(s) exported | %d Rezept(e) exportiert |
| `backup_import_done` (plural) | %d recipe(s) imported | %d Rezept(e) importiert |

## Dependencies

Add **kotlinx.serialization** via the version catalog (`gradle/libs.versions.toml`) and reference as
`libs.*` — never hardcode versions:

- `[versions]` `kotlinxSerialization = "1.7.3"` (compatible with Kotlin 2.0.21).
- `[libraries]` `kotlinx-serialization-json = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version.ref = "kotlinxSerialization" }`.
- `[plugins]` `kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }`.

Apply the `kotlin-serialization` plugin in `app/build.gradle.kts` and add the json dependency. The ZIP
work uses `java.util.zip` from the JDK (no extra dependency).

## Testing

Per project conventions (TDD for pure logic + DAO; Compose verified by build/run):

- **JVM unit (`app/src/test`)** — `BackupModels`: entity ↔ DTO mapping and a **JSON round-trip**
  (`RecipeWithDetails` → DTO → `Json.encodeToString` → decode → DTO → entities) asserting all fields,
  ordering by `position`, null images, and `category` keys survive. Pure, no Android.
- **Instrumented (`app/src/androidTest`)** — `RecipeBackupManager` round-trip against an in-memory Room
  DB and a temp `filesDir`: seed recipes (with images), `export` to a temp stream, `import` it back,
  assert the recipe count doubled (merge), fields/order match, and images exist on disk. Plus the
  invalid-file path (`schemaVersion = 99` → `Invalid`).
- **DAO** — `getAllRecipesWithDetails` returns all recipes with children sorted by `position`;
  `insertImported` adds rows without disturbing existing ones.
- **Compose UI** — verified by a successful build/run; the rendered Data section must match the mockup
  (icon tiles, colored emoji, captions, chevron, overlay, snackbars).

## Files touched (summary)

- New: `data/backup/BackupModels.kt`, `data/backup/RecipeBackupManager.kt`.
- Edit: `data/RecipeDao.kt` (two reads), `data/ImageStore.kt` (`importFromStream`),
  `di/AppContainer.kt` (expose `backupManager`), `ui/settings/SettingsViewModel.kt`,
  `ui/settings/SettingsScreen.kt`, `ui/theme/Color.kt` (icon-tile tokens),
  `res/values/strings.xml` + `res/values-de/strings.xml`, `gradle/libs.versions.toml`,
  `app/build.gradle.kts`.
- Tests: JVM `BackupModelsTest`; instrumented `RecipeBackupManagerTest` (+ DAO assertions).
```
