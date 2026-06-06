# Recipe Categories — Design Spec

**Date:** 2026-06-06
**Status:** Approved (brainstorming)
**Feature branch:** `feature/categories`

## Goal

Give each recipe one **optional** category from a fixed, curated set. The category is
chosen in the edit form, shown as a badge on the list card, and usable as a filter at the
top of the recipe list. This is "Spec B" of the two-part redesign decomposition (Spec A —
the Warm Editorial Kitchen UI refresh — is already merged).

## Scope

**In scope**
- A fixed set of 6 categories, each with a stable key, an emoji, and a localized label.
- A nullable `category` column on `Recipe`.
- A single-select, deselectable category picker in the edit screen.
- A horizontally-scrolling filter row in the list ("All" + categories), shown dynamically.
- A category badge overlaid on each list card's image.
- EN (default) + DE localization for all new strings.

**Out of scope** (unchanged from the project's standing exclusions)
- Multiple categories per recipe (single-select only).
- Free-text / user-defined categories.
- Search, tags, favorites (the mockup's `Schnell` / `Vegetarisch` / `Favoriten` filter
  pills are illustrative only and are **not** part of this feature).
- Cloud sync, export/share, ingredient scaling.

## The category set

Fixed, in this display order (matches `docs/mockups/ui-redesign.html`):

| Key          | Emoji | Label (EN) | Label (DE)    |
|--------------|-------|------------|---------------|
| `MAIN`       | 🍝    | Main       | Hauptgericht  |
| `BREAKFAST`  | 🥞    | Breakfast  | Frühstück     |
| `SALAD`      | 🥗    | Salad      | Salat         |
| `BAKING`     | 🧁    | Baking     | Backen        |
| `DESSERT`    | 🍰    | Dessert    | Dessert       |
| `SNACK`      | 🥪    | Snack      | Snack         |

The **key** is stored in the database (stable, locale-independent). The emoji lives in code.
The label is a localized string resource.

## Architecture

Chosen approach: **string-key column + a single `RecipeCategory` enum as the source of
truth, with in-memory filtering in the list ViewModel.**

Rationale:
- Keeps `Recipe` primitive-only, consistent with how the nutrition columns were added — **no
  Room `TypeConverter`, no DB version bump.**
- One enum drives the edit picker, the list filter, and the card badge — no duplicated lists.
- `fromKey`, the form mapping, and the VM filtering are all plain JVM-unit-testable.
- In-memory filtering fits a small, offline, single-user dataset and avoids re-querying the DB
  on every filter tap.

Rejected alternatives: storing the enum via a Room `@TypeConverter` with SQL filtering (more
moving parts, couples entity to enum, splits "which categories are present" between SQL and
the VM); free-text categories (inconsistent data, no reliable emoji/filter grouping).

### Components & data flow

```
RecipeCategory (enum)  ──source of truth──┐
   key / emoji / labelRes / fromKey()     │
                                          ▼
Recipe.category: String?  ──►  RecipeWithDetails / Recipe (Flow)
                                          │
   Edit:  EditUiState.category ◄──toUiState / toEntities──► Recipe.category
          RecipeEditViewModel.setCategory(key?)
          RecipeEditScreen: picker chips (FlowRow)
                                          │
   List:  RecipeListViewModel
            selectedCategory: StateFlow<String?>   (null = "All")
            availableCategories: StateFlow<List<RecipeCategory>>
            recipes: StateFlow<List<Recipe>>       (filtered)
          RecipeListScreen: filter row (pills) + RecipeCard badge
```

## Detailed design

### 1. Domain — `data/RecipeCategory.kt` (new)

```kotlin
enum class RecipeCategory(val key: String, val emoji: String, @StringRes val labelRes: Int) {
    MAIN("MAIN", "🍝", R.string.category_main),
    BREAKFAST("BREAKFAST", "🥞", R.string.category_breakfast),
    SALAD("SALAD", "🥗", R.string.category_salad),
    BAKING("BAKING", "🧁", R.string.category_baking),
    DESSERT("DESSERT", "🍰", R.string.category_dessert),
    SNACK("SNACK", "🥪", R.string.category_snack);

    companion object {
        fun fromKey(key: String?): RecipeCategory? =
            entries.firstOrNull { it.key == key }
    }
}
```

- `fromKey(null)`, `fromKey("")`, and `fromKey("BOGUS")` all return `null` (defensive against
  stale/unknown data).
- `entries` (Kotlin enum) provides the canonical display order.

### 2. Data — `data/RecipeEntities.kt`

`Recipe` gains a trailing nullable field:

```kotlin
val category: String? = null,   // RecipeCategory.key, null = uncategorized
```

DAO, `RecipeWithDetails`, and the repository are **unchanged** (DAO reads `SELECT *`, the
column rides along automatically).

### 3. Edit — form state & mapping (`ui/edit/EditUiState.kt`)

- `EditUiState` gains `val category: String? = null`.
- `toEntities()` sets `category = category` on the built `Recipe`.
- `toUiState()` sets `category = recipe.category`.

### 4. Edit — `ui/edit/RecipeEditViewModel.kt`

Add: `fun setCategory(key: String?) = update { it.copy(category = key) }`.

Selecting an already-selected chip in the UI passes `null` (deselect) — the toggle logic
lives in the composable; the VM just stores whatever key (or null) it is given.

### 5. Edit — `ui/edit/RecipeEditScreen.kt`

Insert a new section **between Nutrition and Ingredients** (matching the mockup order):

- `EditSectionHeader(stringResource(R.string.section_category))`
- `EditHint(stringResource(R.string.category_hint))`
- A `FlowRow` (`@OptIn(ExperimentalLayoutApi::class)`) of one chip per `RecipeCategory.entries`.

Chip behaviour: tapping an unselected chip selects it (`vm.setCategory(cat.key)`); tapping the
currently-selected chip clears it (`vm.setCategory(null)`). Selected chip = `primary`
background / `onPrimary` content; unselected = `surface` background, `outline`/`outlineVariant`
border, `onSurface` content. Each chip shows `emoji + " " + stringResource(cat.labelRes)`.

### 6. List — `ui/list/RecipeListViewModel.kt`

```kotlin
class RecipeListViewModel(repository: RecipeRepository) : ViewModel() {
    private val allRecipes = repository.observeRecipes()
    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    val availableCategories: StateFlow<List<RecipeCategory>> = allRecipes
        .map { recipes ->
            val present = recipes.mapNotNull { RecipeCategory.fromKey(it.category) }.toSet()
            RecipeCategory.entries.filter { it in present }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val recipes: StateFlow<List<Recipe>> =
        combine(allRecipes, _selectedCategory) { recipes, selected ->
            if (selected == null) recipes
            else recipes.filter { it.category == selected }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun selectCategory(key: String?) { _selectedCategory.value = key }
}
```

Edge case — **stale selection**: if the user is filtering category X and deletes the last
recipe of X, X disappears from `availableCategories`. The filtered `recipes` then yields an
empty list. To avoid a dead-end empty screen with no matching pill, the UI resets the
selection to "All" when `selectedCategory` is set but not in `availableCategories` (a
`LaunchedEffect` in the screen calling `selectCategory(null)`). Filtering itself stays purely
key-based in the VM; the reset is a UI concern keyed off `availableCategories`.

### 7. List — `ui/list/RecipeListScreen.kt`

- Collect `recipes`, `availableCategories`, `selectedCategory`.
- The empty-state branch is unchanged (no recipes at all → `EmptyState`).
- When there are recipes, render a **filter row** as the item directly after `ListHeader`,
  **only if `availableCategories` is non-empty**. It is a horizontally scrollable `Row`
  (`horizontalScroll`) of pills: first "All" (`filter_all`), then one pill per available
  category (`emoji + label`). Active pill (matching `selectedCategory`, or "All" when null) =
  `primary`/`onPrimary`; inactive = `surface` + `outline` border.
- Add the stale-selection `LaunchedEffect` described in §6.

### 8. List — `ui/list/RecipeCard.kt`

When `recipe.category` resolves to a `RecipeCategory` (via `fromKey`), overlay a badge on the
image `Box`, aligned bottom-start with padding: a small rounded pill (translucent dark-forest
background, light text) showing `emoji + label`. Renders over both a real image and the
`Restaurant` placeholder. No badge when category is null.

### 9. Localization — `res/values/strings.xml` + `res/values-de/strings.xml`

New strings (EN / DE):
- `section_category` — "Category" / "Kategorie"
- `category_hint` — "Optional · choose one category" / "Optional · eine Kategorie wählen"
- `filter_all` — "All" / "Alle"
- `category_main` — "Main" / "Hauptgericht"
- `category_breakfast` — "Breakfast" / "Frühstück"
- `category_salad` — "Salad" / "Salat"
- `category_baking` — "Baking" / "Backen"
- `category_dessert` — "Dessert" / "Dessert"
- `category_snack` — "Snack" / "Snack"

Emoji are not localized (kept in `RecipeCategory`).

### 10. Database / migration

Per the project's pre-release convention (see `MEMORY.md`): the schema change is applied by
**adding the nullable column only — no Room migration and no `@Database` version bump.** The
local development database is reset with `./gradlew uninstallDebug` then `installDebug` (or
reinstall from the IDE). The nutrition feature set this same precedent; the DB stays at
`version = 1`.

## Testing strategy

TDD for pure logic; Compose UI verified by a successful build/run (not red-green).

**JVM unit tests (`app/src/test`)**
1. `RecipeCategoryTest` — `fromKey` returns the right constant for each key; returns `null`
   for `null`, `""`, and an unknown key; `entries` is in the documented order.
2. `RecipeMappingTest` (extend existing) — `toEntities()` carries `category`; `toUiState()`
   reads `recipe.category`; round-trip preserves the key; a null category maps to null both
   ways.
3. `RecipeListViewModelTest` (new) — using `FakeRecipeRepository`, `MainDispatcherRule`, and a
   `backgroundScope` collector:
   - default `selectedCategory` is null and `recipes` returns all;
   - `selectCategory("MAIN")` filters to only `MAIN` recipes;
   - `availableCategories` lists only present categories in enum order;
   - selecting then removing the last recipe of that category leaves it absent from
     `availableCategories` (the UI-level reset is not unit-tested; it's a Compose concern).

**Instrumented test (`app/src/androidTest`)**
4. `RecipeDaoTest` (extend existing) — insert a recipe with a non-null `category`, read it
   back, assert the key persists; insert one with null and assert it stays null.

## File map

| File | Change |
|------|--------|
| `data/RecipeCategory.kt` | **new** — enum + `fromKey` |
| `data/RecipeEntities.kt` | add `category: String?` to `Recipe` |
| `ui/edit/EditUiState.kt` | add `category`; map in `toEntities`/`toUiState` |
| `ui/edit/RecipeEditViewModel.kt` | add `setCategory` |
| `ui/edit/RecipeEditScreen.kt` | new Category picker section |
| `ui/list/RecipeListViewModel.kt` | filter state + `availableCategories` + filtered `recipes` |
| `ui/list/RecipeListScreen.kt` | filter row + stale-selection reset |
| `ui/list/RecipeCard.kt` | category badge overlay |
| `res/values/strings.xml`, `res/values-de/strings.xml` | new strings |
| `app/src/test/.../RecipeCategoryTest.kt` | **new** |
| `app/src/test/.../RecipeMappingTest.kt` | extend |
| `app/src/test/.../RecipeListViewModelTest.kt` | **new** |
| `app/src/androidTest/.../RecipeDaoTest.kt` | extend |
