# Recipe Categories Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add one optional, fixed-set category per recipe — chosen in the edit form, shown as a card badge, and usable as a dynamic filter at the top of the list.

**Architecture:** A single `RecipeCategory` enum (key + emoji + localized label) is the source of truth. `Recipe` gains a nullable `category: String?` holding the enum **key**. The list ViewModel filters in memory (selected-category StateFlow combined with the recipes Flow) and derives the set of categories actually present. The edit picker, list filter pills, and card badge all read from the enum.

**Tech Stack:** Kotlin, Jetpack Compose (Material 3), Room, kotlinx-coroutines (Flow/StateFlow), JUnit 4 + kotlinx-coroutines-test (JVM), AndroidX test (instrumented).

**Spec:** `docs/superpowers/specs/2026-06-06-categories-design.md`

---

## Conventions (read once, applies to every task)

- **Branch:** all work is on `feature/categories` (already created and checked out).
- **CLI Gradle JDK:** before any `gradlew` command in a fresh PowerShell, set the JDK (default JDK 24 breaks Gradle 8.13):
  ```powershell
  $env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
  ```
- **Wrapper:** use `.\gradlew.bat` from `D:\Projects\Recipely`.
- **JVM unit tests** (Tasks 1–4, 5) run with:
  ```powershell
  .\gradlew.bat testDebugUnitTest --tests "com.nwe.recipely.<ClassName>"
  ```
- **Compose UI** (Tasks 5–7) is verified by `.\gradlew.bat assembleDebug` succeeding **and** the full JVM suite staying green (`.\gradlew.bat testDebugUnitTest`), not by red-green tests — per the project's testing conventions.
- **DB schema change & pre-release convention:** Task 2 adds a column **without a Room migration and without a `@Database` version bump** (the nutrition feature set this precedent; the DB stays at `version = 1`). Instrumented tests use a fresh in-memory DB, so they are unaffected. **Before running the app on a device/emulator** after Task 2 (e.g. manual verification in Tasks 5–7), reset the local DB:
  ```powershell
  .\gradlew.bat uninstallDebug
  .\gradlew.bat installDebug
  ```
- **Commit after every task** (each task ends green and self-contained).

---

## File map

| File | Task | Responsibility |
|------|------|----------------|
| `app/src/main/res/values/strings.xml`, `values-de/strings.xml` | 1, 5, 6 | New EN/DE strings (category labels, section header + hint, filter "All") |
| `app/src/main/java/com/nwe/recipely/data/RecipeCategory.kt` | 1 | **New** — enum (key/emoji/labelRes) + `fromKey` |
| `app/src/main/java/com/nwe/recipely/data/RecipeEntities.kt` | 2 | `Recipe.category: String?` |
| `app/src/main/java/com/nwe/recipely/ui/edit/EditUiState.kt` | 2 | `category` in state + `toEntities`/`toUiState` |
| `app/src/main/java/com/nwe/recipely/ui/edit/RecipeEditViewModel.kt` | 3 | `setCategory` |
| `app/src/main/java/com/nwe/recipely/ui/edit/RecipeEditScreen.kt` | 5 | Category picker section |
| `app/src/main/java/com/nwe/recipely/ui/list/RecipeListViewModel.kt` | 4 | Filter state, `availableCategories`, filtered `recipes` |
| `app/src/main/java/com/nwe/recipely/ui/list/RecipeListScreen.kt` | 6 | Filter row + stale-selection reset |
| `app/src/main/java/com/nwe/recipely/ui/list/RecipeCard.kt` | 7 | Category badge overlay |
| `app/src/test/java/com/nwe/recipely/RecipeCategoryTest.kt` | 1 | **New** |
| `app/src/test/java/com/nwe/recipely/RecipeMappingTest.kt` | 2 | Extend |
| `app/src/test/java/com/nwe/recipely/RecipeEditViewModelTest.kt` | 3 | Extend |
| `app/src/test/java/com/nwe/recipely/RecipeListViewModelTest.kt` | 4 | Extend |
| `app/src/androidTest/java/com/nwe/recipely/RecipeDaoTest.kt` | 8 | Extend |

---

## Task 1: `RecipeCategory` enum + category label strings

**Files:**
- Create: `app/src/main/java/com/nwe/recipely/data/RecipeCategory.kt`
- Modify: `app/src/main/res/values/strings.xml`, `app/src/main/res/values-de/strings.xml`
- Test: `app/src/test/java/com/nwe/recipely/RecipeCategoryTest.kt` (new)

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/com/nwe/recipely/RecipeCategoryTest.kt`:

```kotlin
package com.nwe.recipely

import com.nwe.recipely.data.RecipeCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RecipeCategoryTest {

    @Test
    fun fromKey_returnsMatchingConstant() {
        assertEquals(RecipeCategory.MAIN, RecipeCategory.fromKey("MAIN"))
        assertEquals(RecipeCategory.SNACK, RecipeCategory.fromKey("SNACK"))
    }

    @Test
    fun fromKey_returnsNull_forNullBlankOrUnknown() {
        assertNull(RecipeCategory.fromKey(null))
        assertNull(RecipeCategory.fromKey(""))
        assertNull(RecipeCategory.fromKey("BOGUS"))
    }

    @Test
    fun entries_areInDocumentedOrder() {
        assertEquals(
            listOf("MAIN", "BREAKFAST", "SALAD", "BAKING", "DESSERT", "SNACK"),
            RecipeCategory.entries.map { it.key },
        )
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat testDebugUnitTest --tests "com.nwe.recipely.RecipeCategoryTest"
```
Expected: compilation failure — `RecipeCategory` is unresolved.

- [ ] **Step 3: Add the category label strings**

In `app/src/main/res/values/strings.xml`, add before `</resources>`:

```xml
    <!-- Categories -->
    <string name="category_main">Main</string>
    <string name="category_breakfast">Breakfast</string>
    <string name="category_salad">Salad</string>
    <string name="category_baking">Baking</string>
    <string name="category_dessert">Dessert</string>
    <string name="category_snack">Snack</string>
```

In `app/src/main/res/values-de/strings.xml`, add before `</resources>`:

```xml
    <!-- Categories -->
    <string name="category_main">Hauptgericht</string>
    <string name="category_breakfast">Frühstück</string>
    <string name="category_salad">Salat</string>
    <string name="category_baking">Backen</string>
    <string name="category_dessert">Dessert</string>
    <string name="category_snack">Snack</string>
```

- [ ] **Step 4: Create the enum**

Create `app/src/main/java/com/nwe/recipely/data/RecipeCategory.kt`:

```kotlin
package com.nwe.recipely.data

import androidx.annotation.StringRes
import com.nwe.recipely.R

/**
 * Fixed set of recipe categories. The [key] is the locale-independent value stored in the DB
 * ([Recipe.category]); [emoji] and [labelRes] are for display only.
 */
enum class RecipeCategory(
    val key: String,
    val emoji: String,
    @StringRes val labelRes: Int,
) {
    MAIN("MAIN", "🍝", R.string.category_main),
    BREAKFAST("BREAKFAST", "🥞", R.string.category_breakfast),
    SALAD("SALAD", "🥗", R.string.category_salad),
    BAKING("BAKING", "🧁", R.string.category_baking),
    DESSERT("DESSERT", "🍰", R.string.category_dessert),
    SNACK("SNACK", "🥪", R.string.category_snack);

    companion object {
        /** Resolves a stored key to a constant; returns null for null/blank/unknown keys. */
        fun fromKey(key: String?): RecipeCategory? =
            entries.firstOrNull { it.key == key }
    }
}
```

- [ ] **Step 5: Run the test to verify it passes**

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.nwe.recipely.RecipeCategoryTest"
```
Expected: BUILD SUCCESSFUL, 3 tests pass.

- [ ] **Step 6: Commit**

```powershell
git add app/src/main/java/com/nwe/recipely/data/RecipeCategory.kt app/src/main/res/values/strings.xml app/src/main/res/values-de/strings.xml app/src/test/java/com/nwe/recipely/RecipeCategoryTest.kt
git commit -m "feat(data): add RecipeCategory enum and localized labels"
```

---

## Task 2: `Recipe.category` column + form mapping

**Files:**
- Modify: `app/src/main/java/com/nwe/recipely/data/RecipeEntities.kt:11-21`
- Modify: `app/src/main/java/com/nwe/recipely/ui/edit/EditUiState.kt`
- Test: `app/src/test/java/com/nwe/recipely/RecipeMappingTest.kt`

- [ ] **Step 1: Write the failing tests**

In `app/src/test/java/com/nwe/recipely/RecipeMappingTest.kt`, add the import `import org.junit.Assert.assertNull` (next to the existing `org.junit.Assert.*` imports), then add these tests inside the class:

```kotlin
    @Test
    fun toEntities_carriesCategory() {
        val (recipe, _, _) = EditUiState(name = "X", category = "MAIN").toEntities()
        assertEquals("MAIN", recipe.category)
    }

    @Test
    fun toUiState_readsCategory() {
        val details = RecipeWithDetails(
            recipe = Recipe(id = 1, name = "X", category = "DESSERT"),
            ingredients = emptyList(),
            steps = emptyList(),
        )
        assertEquals("DESSERT", details.toUiState().category)
    }

    @Test
    fun toEntities_nullCategoryStaysNull() {
        val (recipe, _, _) = EditUiState(name = "X", category = null).toEntities()
        assertNull(recipe.category)
    }
```

- [ ] **Step 2: Run the tests to verify they fail**

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat testDebugUnitTest --tests "com.nwe.recipely.RecipeMappingTest"
```
Expected: compilation failure — `EditUiState`/`Recipe` have no `category` parameter.

- [ ] **Step 3: Add the column to `Recipe`**

In `app/src/main/java/com/nwe/recipely/data/RecipeEntities.kt`, add a trailing field to `Recipe` (after `fatGrams`):

```kotlin
@Entity(tableName = "recipes")
data class Recipe(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val imageUri: String? = null,
    val prepTimeMinutes: Int? = null,
    val servings: Int? = null,
    val calories: Int? = null,        // kcal, total for the whole recipe
    val carbsGrams: Double? = null,   // carbohydrates in grams, total
    val proteinGrams: Double? = null, // protein in grams, total
    val fatGrams: Double? = null,     // fat in grams, total
    val category: String? = null,     // RecipeCategory.key, null = uncategorized
)
```

- [ ] **Step 4: Thread `category` through the form mapping**

In `app/src/main/java/com/nwe/recipely/ui/edit/EditUiState.kt`:

Add the field to `EditUiState` (after `fat`):

```kotlin
data class EditUiState(
    val id: Long = 0,
    val name: String = "",
    val prepTime: String = "",
    val servings: String = "",
    val calories: String = "",
    val carbs: String = "",
    val protein: String = "",
    val fat: String = "",
    val category: String? = null,
    val imagePath: String? = null,
    val ingredients: List<IngredientRow> = listOf(IngredientRow()),
    val steps: List<StepRow> = listOf(StepRow()),
) {
    val canSave: Boolean get() = name.isNotBlank()
}
```

In `toEntities()`, add `category = category` to the `Recipe(...)` constructor (after `fatGrams = fat.toGramsOrNull(),`):

```kotlin
    val recipe = Recipe(
        id = id,
        name = name.trim(),
        imageUri = imagePath,
        prepTimeMinutes = prepTime.trim().toIntOrNull(),
        servings = servings.trim().toIntOrNull(),
        calories = calories.trim().toIntOrNull(),
        carbsGrams = carbs.toGramsOrNull(),
        proteinGrams = protein.toGramsOrNull(),
        fatGrams = fat.toGramsOrNull(),
        category = category,
    )
```

In `RecipeWithDetails.toUiState(...)`, add `category = recipe.category` (after `fat = ...`):

```kotlin
fun RecipeWithDetails.toUiState(locale: Locale = Locale.getDefault()): EditUiState = EditUiState(
    id = recipe.id,
    name = recipe.name,
    prepTime = recipe.prepTimeMinutes?.toString() ?: "",
    servings = recipe.servings?.toString() ?: "",
    calories = recipe.calories?.toString() ?: "",
    carbs = recipe.carbsGrams?.toEditString(locale) ?: "",
    protein = recipe.proteinGrams?.toEditString(locale) ?: "",
    fat = recipe.fatGrams?.toEditString(locale) ?: "",
    category = recipe.category,
    imagePath = recipe.imageUri,
    ingredients = ingredients.sortedBy { it.position }
        .map { IngredientRow(it.text) }
        .ifEmpty { listOf(IngredientRow()) },
    steps = steps.sortedBy { it.position }
        .map { StepRow(it.text, it.imageUri) }
        .ifEmpty { listOf(StepRow()) },
)
```

- [ ] **Step 5: Run the tests to verify they pass**

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.nwe.recipely.RecipeMappingTest"
```
Expected: BUILD SUCCESSFUL, all `RecipeMappingTest` tests pass (the 3 new ones plus the existing ones).

- [ ] **Step 6: Commit**

```powershell
git add app/src/main/java/com/nwe/recipely/data/RecipeEntities.kt app/src/main/java/com/nwe/recipely/ui/edit/EditUiState.kt app/src/test/java/com/nwe/recipely/RecipeMappingTest.kt
git commit -m "feat(data): add nullable category column and form mapping"
```

---

## Task 3: `RecipeEditViewModel.setCategory`

**Files:**
- Modify: `app/src/main/java/com/nwe/recipely/ui/edit/RecipeEditViewModel.kt:37-43`
- Test: `app/src/test/java/com/nwe/recipely/RecipeEditViewModelTest.kt`

- [ ] **Step 1: Write the failing tests**

In `app/src/test/java/com/nwe/recipely/RecipeEditViewModelTest.kt`, add the import `import org.junit.Assert.assertNull` (next to the existing assert imports), then add these tests inside the class:

```kotlin
    @Test
    fun setCategory_updatesState_andClearsToNull() {
        val vm = newViewModel(FakeRecipeRepository())
        vm.setCategory("MAIN")
        assertEquals("MAIN", vm.state.value.category)
        vm.setCategory(null)
        assertNull(vm.state.value.category)
    }

    @Test
    fun save_passesCategoryToRepository() = runTest {
        val repo = FakeRecipeRepository()
        val vm = newViewModel(repo)
        vm.setName("X")
        vm.setCategory("BAKING")
        vm.save {}
        advanceUntilIdle()
        assertEquals("BAKING", repo.lastSavedRecipe?.category)
    }
```

- [ ] **Step 2: Run the tests to verify they fail**

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat testDebugUnitTest --tests "com.nwe.recipely.RecipeEditViewModelTest"
```
Expected: compilation failure — `setCategory` is unresolved.

- [ ] **Step 3: Add the setter**

In `app/src/main/java/com/nwe/recipely/ui/edit/RecipeEditViewModel.kt`, add after `setFat` (line ~43):

```kotlin
    fun setCategory(key: String?) = update { it.copy(category = key) }
```

- [ ] **Step 4: Run the tests to verify they pass**

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.nwe.recipely.RecipeEditViewModelTest"
```
Expected: BUILD SUCCESSFUL, all tests pass.

- [ ] **Step 5: Commit**

```powershell
git add app/src/main/java/com/nwe/recipely/ui/edit/RecipeEditViewModel.kt app/src/test/java/com/nwe/recipely/RecipeEditViewModelTest.kt
git commit -m "feat(edit): add setCategory to RecipeEditViewModel"
```

---

## Task 4: List filtering in `RecipeListViewModel`

**Files:**
- Modify: `app/src/main/java/com/nwe/recipely/ui/list/RecipeListViewModel.kt` (full rewrite)
- Test: `app/src/test/java/com/nwe/recipely/RecipeListViewModelTest.kt`

- [ ] **Step 1: Write the failing tests**

In `app/src/test/java/com/nwe/recipely/RecipeListViewModelTest.kt`, add the import `import com.nwe.recipely.data.RecipeCategory`, then add these tests inside the class (keep the existing `exposesRecipesFromRepository` test):

```kotlin
    @Test
    fun selectCategory_filtersRecipes() = runTest {
        val repo = FakeRecipeRepository()
        val vm = RecipeListViewModel(repo)
        backgroundScope.launch { vm.recipes.collect {} }
        repo.recipes.value = listOf(
            Recipe(id = 1, name = "Pasta", category = "MAIN"),
            Recipe(id = 2, name = "Cake", category = "DESSERT"),
            Recipe(id = 3, name = "Toast", category = null),
        )
        advanceUntilIdle()
        vm.selectCategory("MAIN")
        advanceUntilIdle()
        assertEquals(listOf("Pasta"), vm.recipes.value.map { it.name })
    }

    @Test
    fun nullSelection_returnsAllRecipes() = runTest {
        val repo = FakeRecipeRepository()
        val vm = RecipeListViewModel(repo)
        backgroundScope.launch { vm.recipes.collect {} }
        repo.recipes.value = listOf(
            Recipe(id = 1, name = "Pasta", category = "MAIN"),
            Recipe(id = 2, name = "Cake", category = "DESSERT"),
        )
        advanceUntilIdle()
        assertEquals(listOf("Pasta", "Cake"), vm.recipes.value.map { it.name })
    }

    @Test
    fun availableCategories_listsPresentCategoriesInEnumOrder() = runTest {
        val repo = FakeRecipeRepository()
        val vm = RecipeListViewModel(repo)
        backgroundScope.launch { vm.availableCategories.collect {} }
        repo.recipes.value = listOf(
            Recipe(id = 1, name = "Cake", category = "DESSERT"),
            Recipe(id = 2, name = "Pasta", category = "MAIN"),
            Recipe(id = 3, name = "Toast", category = null),
            Recipe(id = 4, name = "Junk", category = "BOGUS"),
        )
        advanceUntilIdle()
        // enum order puts MAIN before DESSERT; null and unknown keys are excluded.
        assertEquals(listOf(RecipeCategory.MAIN, RecipeCategory.DESSERT), vm.availableCategories.value)
    }

    @Test
    fun removingLastRecipeOfCategory_dropsItFromAvailable() = runTest {
        val repo = FakeRecipeRepository()
        val vm = RecipeListViewModel(repo)
        backgroundScope.launch { vm.availableCategories.collect {} }
        repo.recipes.value = listOf(Recipe(id = 1, name = "Cake", category = "DESSERT"))
        advanceUntilIdle()
        assertEquals(listOf(RecipeCategory.DESSERT), vm.availableCategories.value)
        repo.recipes.value = emptyList()
        advanceUntilIdle()
        assertEquals(emptyList<RecipeCategory>(), vm.availableCategories.value)
    }
```

- [ ] **Step 2: Run the tests to verify they fail**

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat testDebugUnitTest --tests "com.nwe.recipely.RecipeListViewModelTest"
```
Expected: compilation failure — `selectCategory` / `availableCategories` are unresolved.

- [ ] **Step 3: Rewrite the ViewModel**

Replace the entire contents of `app/src/main/java/com/nwe/recipely/ui/list/RecipeListViewModel.kt` with:

```kotlin
package com.nwe.recipely.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nwe.recipely.data.Recipe
import com.nwe.recipely.data.RecipeCategory
import com.nwe.recipely.data.RecipeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class RecipeListViewModel(repository: RecipeRepository) : ViewModel() {

    private val allRecipes = repository.observeRecipes()

    /** Currently selected filter category key; null means "All". */
    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    /** Categories that currently have at least one recipe, in enum order. */
    val availableCategories: StateFlow<List<RecipeCategory>> = allRecipes
        .map { recipes ->
            val present = recipes.mapNotNull { RecipeCategory.fromKey(it.category) }.toSet()
            RecipeCategory.entries.filter { it in present }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Recipes after applying the selected filter (all when nothing is selected). */
    val recipes: StateFlow<List<Recipe>> =
        combine(allRecipes, _selectedCategory) { recipes, selected ->
            if (selected == null) recipes else recipes.filter { it.category == selected }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun selectCategory(key: String?) {
        _selectedCategory.value = key
    }
}
```

- [ ] **Step 4: Run the tests to verify they pass**

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.nwe.recipely.RecipeListViewModelTest"
```
Expected: BUILD SUCCESSFUL, all 5 tests pass.

- [ ] **Step 5: Commit**

```powershell
git add app/src/main/java/com/nwe/recipely/ui/list/RecipeListViewModel.kt app/src/test/java/com/nwe/recipely/RecipeListViewModelTest.kt
git commit -m "feat(list): filter recipes by category in the ViewModel"
```

---

## Task 5: Category picker in the edit screen

**Files:**
- Modify: `app/src/main/res/values/strings.xml`, `app/src/main/res/values-de/strings.xml`
- Modify: `app/src/main/java/com/nwe/recipely/ui/edit/RecipeEditScreen.kt`

No red-green test (Compose). Verified by build + green unit suite. There IS a manual check at the end.

- [ ] **Step 1: Add the section + hint strings**

In `app/src/main/res/values/strings.xml`, add before `</resources>`:

```xml
    <string name="section_category">Category</string>
    <string name="category_hint">Optional · choose one category</string>
```

In `app/src/main/res/values-de/strings.xml`, add before `</resources>`:

```xml
    <string name="section_category">Kategorie</string>
    <string name="category_hint">Optional · eine Kategorie wählen</string>
```

- [ ] **Step 2: Add imports**

In `app/src/main/java/com/nwe/recipely/ui/edit/RecipeEditScreen.kt`, add these imports (alphabetically among the existing ones):

```kotlin
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.Surface
import com.nwe.recipely.data.RecipeCategory
```

- [ ] **Step 3: Insert the Category section into the form**

In the `LazyColumn`, between the nutrition block and the ingredients header — i.e. directly after the `item { Row { ... protein/fat ... } }` block (the one ending at line ~241) and before `item { EditSectionHeader(stringResource(R.string.section_ingredients)) }` (line ~243) — add:

```kotlin
            item { EditSectionHeader(stringResource(R.string.section_category)) }
            item { EditHint(stringResource(R.string.category_hint)) }
            item { CategoryPicker(selected = state.category, onSelect = vm::setCategory) }
```

- [ ] **Step 4: Add the picker composables**

At the end of `app/src/main/java/com/nwe/recipely/ui/edit/RecipeEditScreen.kt` (after `EditHint`), add:

```kotlin
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryPicker(selected: String?, onSelect: (String?) -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        RecipeCategory.entries.forEach { category ->
            val isSelected = category.key == selected
            CategoryChip(
                emoji = category.emoji,
                label = stringResource(category.labelRes),
                selected = isSelected,
                // Tapping the selected chip clears the category (it is optional).
                onClick = { onSelect(if (isSelected) null else category.key) },
            )
        }
    }
}

@Composable
private fun CategoryChip(emoji: String, label: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val fg = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val border = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    Surface(
        color = bg,
        contentColor = fg,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, border),
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(emoji, style = MaterialTheme.typography.labelLarge)
            Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}
```

- [ ] **Step 5: Build and run the unit suite**

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest
```
Expected: both BUILD SUCCESSFUL.

- [ ] **Step 6: Commit**

```powershell
git add app/src/main/java/com/nwe/recipely/ui/edit/RecipeEditScreen.kt app/src/main/res/values/strings.xml app/src/main/res/values-de/strings.xml
git commit -m "feat(edit): add category picker section"
```

---

## Task 6: Filter row in the list screen

**Files:**
- Modify: `app/src/main/res/values/strings.xml`, `app/src/main/res/values-de/strings.xml`
- Modify: `app/src/main/java/com/nwe/recipely/ui/list/RecipeListScreen.kt`

No red-green test (Compose). Verified by build + green unit suite.

- [ ] **Step 1: Add the "All" filter string**

In `app/src/main/res/values/strings.xml`, add before `</resources>`:

```xml
    <string name="filter_all">All</string>
```

In `app/src/main/res/values-de/strings.xml`, add before `</resources>`:

```xml
    <string name="filter_all">Alle</string>
```

- [ ] **Step 2: Add imports**

In `app/src/main/java/com/nwe/recipely/ui/list/RecipeListScreen.kt`, add these imports (alphabetically among the existing ones):

```kotlin
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import com.nwe.recipely.data.RecipeCategory
```

- [ ] **Step 3: Collect the new state and add the stale-selection reset**

In `RecipeListScreen`, replace the single `val recipes by vm.recipes.collectAsState()` line with:

```kotlin
    val recipes by vm.recipes.collectAsState()
    val availableCategories by vm.availableCategories.collectAsState()
    val selectedCategory by vm.selectedCategory.collectAsState()

    // If the selected category no longer has any recipes (e.g. the last one was deleted),
    // fall back to "All" so the list never shows an empty filtered result with no matching pill.
    LaunchedEffect(availableCategories, selectedCategory) {
        val current = selectedCategory
        if (current != null && availableCategories.none { it.key == current }) {
            vm.selectCategory(null)
        }
    }
```

- [ ] **Step 4: Insert the filter row into the list**

In the `LazyColumn`, directly after `item { ListHeader(count = recipes.size) }` and before `items(recipes, key = { it.id }) { ... }`, add:

```kotlin
                if (availableCategories.isNotEmpty()) {
                    item {
                        FilterRow(
                            categories = availableCategories,
                            selected = selectedCategory,
                            onSelect = vm::selectCategory,
                        )
                    }
                }
```

Note: `ListHeader(count = recipes.size)` intentionally shows the count of *currently displayed* recipes — that equals the total when "All" is selected and the filtered count otherwise. This is acceptable and informative; no separate total is needed.

- [ ] **Step 5: Add the filter composables**

At the end of `app/src/main/java/com/nwe/recipely/ui/list/RecipeListScreen.kt` (after `EmptyState`), add:

```kotlin
@Composable
private fun FilterRow(
    categories: List<RecipeCategory>,
    selected: String?,
    onSelect: (String?) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterPill(
            label = stringResource(R.string.filter_all),
            selected = selected == null,
            onClick = { onSelect(null) },
        )
        categories.forEach { category ->
            FilterPill(
                label = category.emoji + " " + stringResource(category.labelRes),
                selected = selected == category.key,
                onClick = { onSelect(category.key) },
            )
        }
    }
}

@Composable
private fun FilterPill(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
    val fg = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    val border = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    Surface(
        color = bg,
        contentColor = fg,
        shape = RoundedCornerShape(100.dp),
        border = BorderStroke(1.dp, border),
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        )
    }
}
```

Note: `dp` is already imported in this file; `RoundedCornerShape` is added in Step 2.

- [ ] **Step 6: Build and run the unit suite**

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest
```
Expected: both BUILD SUCCESSFUL.

- [ ] **Step 7: Commit**

```powershell
git add app/src/main/java/com/nwe/recipely/ui/list/RecipeListScreen.kt app/src/main/res/values/strings.xml app/src/main/res/values-de/strings.xml
git commit -m "feat(list): add dynamic category filter row"
```

---

## Task 7: Category badge on the list card

**Files:**
- Modify: `app/src/main/java/com/nwe/recipely/ui/list/RecipeCard.kt`

No red-green test (Compose). Verified by build + green unit suite.

- [ ] **Step 1: Add imports**

In `app/src/main/java/com/nwe/recipely/ui/list/RecipeCard.kt`, add these imports (alphabetically among the existing ones):

```kotlin
import androidx.compose.ui.graphics.Color
import com.nwe.recipely.data.RecipeCategory
import com.nwe.recipely.ui.theme.ForestPrimary
```

- [ ] **Step 2: Overlay the badge on the image box**

In `RecipeCard`, inside the image `Box` (the one with `.aspectRatio(16f / 9f)` and `contentAlignment = Alignment.Center`), after the `if (image != null) { ... } else { ... }` block and still inside that `Box`, add:

```kotlin
                val category = RecipeCategory.fromKey(recipe.category)
                if (category != null) {
                    CategoryBadge(
                        emoji = category.emoji,
                        label = stringResource(category.labelRes),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(12.dp),
                    )
                }
```

- [ ] **Step 3: Add the badge composable**

At the end of `app/src/main/java/com/nwe/recipely/ui/list/RecipeCard.kt` (after `MetaChip`), add:

```kotlin
/**
 * Category pill overlaid on a card image (mockup `.tag`): translucent dark-forest background
 * with light text, so it reads over both a photo and the placeholder, in light and dark themes.
 */
@Composable
private fun CategoryBadge(emoji: String, label: String, modifier: Modifier = Modifier) {
    Surface(
        color = ForestPrimary.copy(alpha = 0.82f),
        contentColor = Color.White,
        shape = RoundedCornerShape(100.dp),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(emoji, style = MaterialTheme.typography.labelSmall)
            Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
    }
}
```

- [ ] **Step 4: Build and run the unit suite**

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest
```
Expected: both BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```powershell
git add app/src/main/java/com/nwe/recipely/ui/list/RecipeCard.kt
git commit -m "feat(list): show category badge on recipe cards"
```

---

## Task 8: DAO persistence test for `category` (instrumented)

**Files:**
- Test: `app/src/androidTest/java/com/nwe/recipely/RecipeDaoTest.kt`

Requires a connected device/emulator. The instrumented DB is in-memory (fresh), so no reset is needed for this test.

- [ ] **Step 1: Add the test**

In `app/src/androidTest/java/com/nwe/recipely/RecipeDaoTest.kt`, add inside the class (`assertNull` is already imported):

```kotlin
    @Test
    fun upsert_persistsCategory_andNullStaysNull() = runTest {
        val withCategory = dao.upsertRecipeWithChildren(
            recipe = Recipe(name = "Cake", category = "DESSERT"),
            ingredients = emptyList(),
            steps = emptyList(),
        )
        val withoutCategory = dao.upsertRecipeWithChildren(
            recipe = Recipe(name = "Plain", category = null),
            ingredients = emptyList(),
            steps = emptyList(),
        )

        assertEquals("DESSERT", dao.observeRecipe(withCategory).first()!!.recipe.category)
        assertNull(dao.observeRecipe(withoutCategory).first()!!.recipe.category)
    }
```

- [ ] **Step 2: Run the instrumented test**

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.nwe.recipely.RecipeDaoTest
```
Expected: BUILD SUCCESSFUL; the new test passes alongside the existing DAO tests.

- [ ] **Step 3: Commit**

```powershell
git add app/src/androidTest/java/com/nwe/recipely/RecipeDaoTest.kt
git commit -m "test(dao): verify category column round-trips"
```

---

## Final manual verification (on device/emulator)

The schema changed, so reset the local DB first:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat uninstallDebug
.\gradlew.bat installDebug
```

Then check:
1. **Edit:** open a recipe → a "Category" section appears between Nutrition and Ingredients with 6 chips; tap one to select (forest fill), tap it again to clear; Save.
2. **List badge:** the saved recipe shows its category badge bottom-left on the card image (and over the placeholder for image-less recipes).
3. **Filter:** with at least one categorized recipe, the filter row appears under the title; "All" is active by default; tapping a category pill shows only those recipes; tapping "All" restores the full list.
4. **Dynamic hide:** with no categorized recipes, the filter row is absent.
5. **Stale reset:** filter to a category, delete its last recipe → the filter falls back to "All".
6. **Locale:** on a German-locale device the labels/headers/"Alle" read in German; default is English.

---

## Self-review notes

- **Spec coverage:** §1 enum → Task 1; §2 column → Task 2; §3 form mapping → Task 2; §4 setCategory → Task 3; §5 edit picker → Task 5; §6 list VM filtering → Task 4; §7 list filter row + reset → Task 6; §8 card badge → Task 7; §9 localization → Tasks 1/5/6; §10 DB (no migration) → Conventions; testing §1–4 → Tasks 1/2/3/4/8.
- **Type consistency:** edit uses `setCategory(key: String?)`; list uses `selectCategory(key: String?)` + `availableCategories: StateFlow<List<RecipeCategory>>` + `selectedCategory: StateFlow<String?>` — names match across tasks. `RecipeCategory.fromKey`, `.key`, `.emoji`, `.labelRes`, `.entries` are used identically in VM (Task 4), edit screen (Task 5), list screen (Task 6), and card (Task 7).
```
