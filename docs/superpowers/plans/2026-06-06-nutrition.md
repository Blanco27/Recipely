# Nutrition Values Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add optional per-recipe nutrition values (kcal, carbs, protein, fat); show them per-portion (with totals as a secondary line) in the detail view, editable in the edit view.

**Architecture:** Four nullable columns on the existing `Recipe` Room entity (no migration — DB version stays 1, the local test DB is reset once). Pure, JVM-testable logic computes per-portion values; Compose screens render them. Input parsing accepts both comma and period decimal separators; read-only display is locale-formatted to one decimal.

**Tech Stack:** Kotlin, Jetpack Compose (Material 3), Room, JUnit 4 + kotlinx-coroutines-test.

---

## Spec

Design: `docs/superpowers/specs/2026-06-06-nutrition-design.md`

Key decisions:
- Macros (carbs/protein/fat) are `Double?` shown with **1 decimal**; calories is `Int?` (kcal).
- Values are entered as the **total for the whole recipe**.
- Detail view (Variante C): when `servings` is set, show **Per portion** values prominently + a small **Total:** summary line. When `servings` is missing, show **only the totals**.
- Scope: detail + edit screens only. The recipe list is unchanged.
- **No DB migration.** DB version stays `1`; the new schema is created fresh after the local test DB is cleared.

## Build/Test Environment (read first)

Every CLI Gradle command needs JDK ≤ 21. In each fresh PowerShell shell, set:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
```

Run a single unit-test class, e.g.:

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.nwe.recipely.NutritionDisplayTest"
```

## File Structure

- **Modify** `app/src/main/java/com/nwe/recipely/data/RecipeEntities.kt` — 4 nullable columns on `Recipe`.
- **Create** `app/src/main/java/com/nwe/recipely/ui/detail/NutritionDisplay.kt` — pure per-portion logic + value formatting.
- **Modify** `app/src/main/java/com/nwe/recipely/ui/edit/EditUiState.kt` — 4 form fields + parse/prefill mapping.
- **Modify** `app/src/main/java/com/nwe/recipely/ui/edit/RecipeEditViewModel.kt` — 4 setters.
- **Modify** `app/src/main/java/com/nwe/recipely/ui/edit/RecipeEditScreen.kt` — nutrition input section.
- **Modify** `app/src/main/java/com/nwe/recipely/ui/detail/RecipeDetailScreen.kt` — nutrition display section.
- **Modify** `app/src/main/res/values/strings.xml` + `app/src/main/res/values-de/strings.xml` — new strings.
- **Modify** `app/src/test/java/com/nwe/recipely/RecipeMappingTest.kt` — nutrition mapping tests.
- **Modify** `app/src/test/java/com/nwe/recipely/RecipeEditViewModelTest.kt` — nutrition setter/save test.
- **Create** `app/src/test/java/com/nwe/recipely/NutritionDisplayTest.kt` — per-portion + format tests.

`RecipeDatabase.kt`, `AppContainer.kt`, `RecipeDao.kt`, `RecipeRepository.kt`, and `FakeRecipeRepository.kt` need **no changes** — adding nullable fields with defaults to `Recipe` does not change any method signature, and the DB version is unchanged.

---

## Task 1: Add nutrition columns to `Recipe`

**Files:**
- Modify: `app/src/main/java/com/nwe/recipely/data/RecipeEntities.kt:11-17`

- [ ] **Step 1: Add the four nullable fields to `Recipe`**

Replace the `Recipe` data class (lines 11-17) with:

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
)
```

- [ ] **Step 2: Verify it compiles**

Run:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat :app:compileDebugKotlin
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```powershell
git add app/src/main/java/com/nwe/recipely/data/RecipeEntities.kt
git commit -m "feat(data): add optional nutrition columns to Recipe"
```

---

## Task 2: Per-portion logic + value formatting (pure, TDD)

**Files:**
- Create: `app/src/main/java/com/nwe/recipely/ui/detail/NutritionDisplay.kt`
- Test: `app/src/test/java/com/nwe/recipely/NutritionDisplayTest.kt`

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/com/nwe/recipely/NutritionDisplayTest.kt`:

```kotlin
package com.nwe.recipely

import com.nwe.recipely.data.Recipe
import com.nwe.recipely.ui.detail.NutritionFacts
import com.nwe.recipely.ui.detail.NutritionFormat
import com.nwe.recipely.ui.detail.nutritionFacts
import com.nwe.recipely.ui.detail.perPortion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class NutritionDisplayTest {

    @Test
    fun hasAny_isFalseWhenAllNull_trueWhenAnySet() {
        assertFalse(NutritionFacts().hasAny)
        assertTrue(NutritionFacts(calories = 100).hasAny)
        assertTrue(NutritionFacts(fatGrams = 1.0).hasAny)
    }

    @Test
    fun nutritionFacts_readsFromRecipe() {
        val facts = Recipe(name = "X", calories = 100, carbsGrams = 10.0).nutritionFacts()
        assertEquals(100, facts.calories)
        assertEquals(10.0, facts.carbsGrams!!, 0.0001)
        assertNull(facts.proteinGrams)
    }

    @Test
    fun perPortion_dividesAndRounds() {
        val facts = NutritionFacts(calories = 1400, carbsGrams = 160.0, proteinGrams = 62.0, fatGrams = 48.0)
        val pp = facts.perPortion(4)!!
        assertEquals(350, pp.calories)
        assertEquals(40.0, pp.carbsGrams!!, 0.0001)
        assertEquals(15.5, pp.proteinGrams!!, 0.0001)
        assertEquals(12.0, pp.fatGrams!!, 0.0001)
    }

    @Test
    fun perPortion_roundsMacrosToOneDecimal() {
        val pp = NutritionFacts(carbsGrams = 160.0).perPortion(3)!!
        assertEquals(53.3, pp.carbsGrams!!, 0.0001) // 53.333... -> 53.3
    }

    @Test
    fun perPortion_isNullWhenServingsMissingOrNonPositive() {
        val facts = NutritionFacts(calories = 100)
        assertNull(facts.perPortion(null))
        assertNull(facts.perPortion(0))
        assertNull(facts.perPortion(-2))
    }

    @Test
    fun perPortion_keepsNullValues() {
        val pp = NutritionFacts(calories = 200, carbsGrams = null).perPortion(2)!!
        assertEquals(100, pp.calories)
        assertNull(pp.carbsGrams)
    }

    @Test
    fun grams_formatsOneDecimalPerLocale() {
        assertEquals("15.5", NutritionFormat.grams(15.5, Locale.US))
        assertEquals("15,5", NutritionFormat.grams(15.5, Locale.GERMANY))
        assertEquals("53.3", NutritionFormat.grams(53.3, Locale.US))
        assertEquals("160.0", NutritionFormat.grams(160.0, Locale.US))
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat testDebugUnitTest --tests "com.nwe.recipely.NutritionDisplayTest"
```

Expected: FAIL — compilation error, `NutritionFacts` / `perPortion` / `NutritionFormat` unresolved.

- [ ] **Step 3: Write the implementation**

Create `app/src/main/java/com/nwe/recipely/ui/detail/NutritionDisplay.kt`:

```kotlin
package com.nwe.recipely.ui.detail

import com.nwe.recipely.data.Recipe
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/** The four optional energy/macro values, all nullable. */
data class NutritionFacts(
    val calories: Int? = null,        // kcal
    val carbsGrams: Double? = null,
    val proteinGrams: Double? = null,
    val fatGrams: Double? = null,
) {
    val hasAny: Boolean
        get() = calories != null || carbsGrams != null || proteinGrams != null || fatGrams != null
}

/** Reads the recipe's stored (total) nutrition values. */
fun Recipe.nutritionFacts(): NutritionFacts =
    NutritionFacts(calories, carbsGrams, proteinGrams, fatGrams)

/**
 * Per-portion values: each total divided by [servings] (kcal rounded to a whole number,
 * macros rounded to one decimal). Returns null when [servings] is missing or non-positive —
 * the caller then shows only the totals.
 */
fun NutritionFacts.perPortion(servings: Int?): NutritionFacts? {
    if (servings == null || servings <= 0) return null
    return NutritionFacts(
        calories = calories?.let { (it.toDouble() / servings).roundToInt() },
        carbsGrams = carbsGrams?.let { roundTo1(it / servings) },
        proteinGrams = proteinGrams?.let { roundTo1(it / servings) },
        fatGrams = fatGrams?.let { roundTo1(it / servings) },
    )
}

private fun roundTo1(value: Double): Double = (value * 10.0).roundToLong() / 10.0

object NutritionFormat {
    /** A gram value with exactly one decimal, using [locale]'s decimal separator. */
    fun grams(value: Double, locale: Locale = Locale.getDefault()): String =
        String.format(locale, "%.1f", value)
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat testDebugUnitTest --tests "com.nwe.recipely.NutritionDisplayTest"
```

Expected: `BUILD SUCCESSFUL`, all tests pass.

- [ ] **Step 5: Commit**

```powershell
git add app/src/main/java/com/nwe/recipely/ui/detail/NutritionDisplay.kt app/src/test/java/com/nwe/recipely/NutritionDisplayTest.kt
git commit -m "feat(detail): add per-portion nutrition logic and value formatting"
```

---

## Task 3: Edit form mapping (TDD)

**Files:**
- Modify: `app/src/main/java/com/nwe/recipely/ui/edit/EditUiState.kt`
- Test: `app/src/test/java/com/nwe/recipely/RecipeMappingTest.kt`

- [ ] **Step 1: Write the failing tests**

Append these tests inside the `RecipeMappingTest` class in `app/src/test/java/com/nwe/recipely/RecipeMappingTest.kt` (before the final closing brace):

```kotlin
    @Test
    fun toEntities_parsesNutrition_acceptingCommaAndPeriod() {
        val state = EditUiState(
            name = "X",
            calories = "350",
            carbs = "40,5",   // comma decimal
            protein = "15.5", // period decimal
            fat = "",         // empty -> null
        )
        val (recipe, _, _) = state.toEntities()
        assertEquals(350, recipe.calories)
        assertEquals(40.5, recipe.carbsGrams!!, 0.0001)
        assertEquals(15.5, recipe.proteinGrams!!, 0.0001)
        assertEquals(null, recipe.fatGrams)
    }

    @Test
    fun toEntities_parsesInvalidNutritionAsNull() {
        val (recipe, _, _) = EditUiState(name = "X", calories = "abc", carbs = "x").toEntities()
        assertEquals(null, recipe.calories)
        assertEquals(null, recipe.carbsGrams)
    }

    @Test
    fun toUiState_formatsNutrition_strippingTrailingZero() {
        val details = RecipeWithDetails(
            recipe = Recipe(id = 1, name = "X", calories = 350, carbsGrams = 160.0, proteinGrams = 15.5, fatGrams = null),
            ingredients = emptyList(),
            steps = emptyList(),
        )
        val state = details.toUiState()
        assertEquals("350", state.calories)
        assertEquals("160", state.carbs)   // 160.0 -> "160"
        assertEquals("15.5", state.protein)
        assertEquals("", state.fat)
    }
```

- [ ] **Step 2: Run the tests to verify they fail**

Run:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat testDebugUnitTest --tests "com.nwe.recipely.RecipeMappingTest"
```

Expected: FAIL — `calories`/`carbs`/`protein`/`fat` are not parameters of `EditUiState`.

- [ ] **Step 3: Add the form fields**

In `app/src/main/java/com/nwe/recipely/ui/edit/EditUiState.kt`, add four fields to `EditUiState` (after `val servings: String = "",`, line 17):

```kotlin
    val calories: String = "",
    val carbs: String = "",
    val protein: String = "",
    val fat: String = "",
```

- [ ] **Step 4: Add the parse/prefill helpers**

In the same file, add the import at the top (with the other imports):

```kotlin
import kotlin.math.roundToLong
```

And add these top-level helpers at the end of the file:

```kotlin
/** Parses a gram value, accepting both comma and period decimals; blank/invalid -> null. */
internal fun String.toGramsOrNull(): Double? =
    trim().replace(',', '.').toDoubleOrNull()

/** Formats a stored gram value for an editable text field: one decimal max, trailing ".0" dropped. */
internal fun Double.toEditString(): String {
    val rounded = (this * 10.0).roundToLong() / 10.0
    return if (rounded % 1.0 == 0.0) rounded.toLong().toString() else rounded.toString()
}
```

- [ ] **Step 5: Map nutrition in `toEntities`**

In `toEntities()`, extend the `Recipe(...)` construction to include the four fields (after `servings = servings.trim().toIntOrNull(),`):

```kotlin
        calories = calories.trim().toIntOrNull(),
        carbsGrams = carbs.toGramsOrNull(),
        proteinGrams = protein.toGramsOrNull(),
        fatGrams = fat.toGramsOrNull(),
```

- [ ] **Step 6: Map nutrition in `toUiState`**

In `RecipeWithDetails.toUiState()`, extend the `EditUiState(...)` construction to include the four fields (after `servings = recipe.servings?.toString() ?: "",`):

```kotlin
    calories = recipe.calories?.toString() ?: "",
    carbs = recipe.carbsGrams?.toEditString() ?: "",
    protein = recipe.proteinGrams?.toEditString() ?: "",
    fat = recipe.fatGrams?.toEditString() ?: "",
```

- [ ] **Step 7: Run the tests to verify they pass**

Run:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat testDebugUnitTest --tests "com.nwe.recipely.RecipeMappingTest"
```

Expected: `BUILD SUCCESSFUL`, all tests pass.

- [ ] **Step 8: Commit**

```powershell
git add app/src/main/java/com/nwe/recipely/ui/edit/EditUiState.kt app/src/test/java/com/nwe/recipely/RecipeMappingTest.kt
git commit -m "feat(edit): map nutrition form fields to and from entities"
```

---

## Task 4: Edit ViewModel setters (TDD)

**Files:**
- Modify: `app/src/main/java/com/nwe/recipely/ui/edit/RecipeEditViewModel.kt:39`
- Test: `app/src/test/java/com/nwe/recipely/RecipeEditViewModelTest.kt`

- [ ] **Step 1: Write the failing test**

Append this test inside the `RecipeEditViewModelTest` class in `app/src/test/java/com/nwe/recipely/RecipeEditViewModelTest.kt` (before the final closing brace):

```kotlin
    @Test
    fun save_passesNutritionToRepository() = runTest {
        val repo = FakeRecipeRepository()
        val vm = newViewModel(repo)
        vm.setName("X")
        vm.setCalories("350")
        vm.setCarbs("40,5")
        vm.setProtein("15.5")
        vm.setFat("12")
        vm.save {}
        advanceUntilIdle()
        assertEquals(350, repo.lastSavedRecipe?.calories)
        assertEquals(40.5, repo.lastSavedRecipe?.carbsGrams!!, 0.0001)
        assertEquals(15.5, repo.lastSavedRecipe?.proteinGrams!!, 0.0001)
        assertEquals(12.0, repo.lastSavedRecipe?.fatGrams!!, 0.0001)
    }
```

- [ ] **Step 2: Run the test to verify it fails**

Run:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat testDebugUnitTest --tests "com.nwe.recipely.RecipeEditViewModelTest"
```

Expected: FAIL — `setCalories`/`setCarbs`/`setProtein`/`setFat` unresolved.

- [ ] **Step 3: Add the setters**

In `app/src/main/java/com/nwe/recipely/ui/edit/RecipeEditViewModel.kt`, add after `fun setServings(...)` (line 39):

```kotlin
    fun setCalories(value: String) = update { it.copy(calories = value) }
    fun setCarbs(value: String) = update { it.copy(carbs = value) }
    fun setProtein(value: String) = update { it.copy(protein = value) }
    fun setFat(value: String) = update { it.copy(fat = value) }
```

- [ ] **Step 4: Run the test to verify it passes**

Run:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat testDebugUnitTest --tests "com.nwe.recipely.RecipeEditViewModelTest"
```

Expected: `BUILD SUCCESSFUL`, all tests pass.

- [ ] **Step 5: Commit**

```powershell
git add app/src/main/java/com/nwe/recipely/ui/edit/RecipeEditViewModel.kt app/src/test/java/com/nwe/recipely/RecipeEditViewModelTest.kt
git commit -m "feat(edit): add nutrition setters to RecipeEditViewModel"
```

---

## Task 5: Localized strings (EN + DE)

**Files:**
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-de/strings.xml`

These must exist before the screens reference them (Tasks 6 & 7), or the build fails.

- [ ] **Step 1: Add English strings**

In `app/src/main/res/values/strings.xml`, before the closing `</resources>`, add:

```xml

    <!-- Nutrition -->
    <string name="section_nutrition">Nutrition</string>
    <string name="nutrition_optional">Nutrition (optional)</string>
    <string name="nutrition_per_portion">Per portion</string>
    <string name="nutrition_total_prefix">Total:</string>
    <string name="label_calories">Calories</string>
    <string name="label_carbs">Carbs</string>
    <string name="label_protein">Protein</string>
    <string name="label_fat">Fat</string>
    <string name="label_calories_input">Calories (kcal)</string>
    <string name="label_carbs_input">Carbs (g)</string>
    <string name="label_protein_input">Protein (g)</string>
    <string name="label_fat_input">Fat (g)</string>
    <string name="nutrition_value_kcal">%1$d kcal</string>
    <string name="nutrition_value_grams">%1$s g</string>
```

- [ ] **Step 2: Add German strings**

In `app/src/main/res/values-de/strings.xml`, before the closing `</resources>`, add:

```xml

    <!-- Nutrition -->
    <string name="section_nutrition">Nährwerte</string>
    <string name="nutrition_optional">Nährwerte (optional)</string>
    <string name="nutrition_per_portion">Pro Portion</string>
    <string name="nutrition_total_prefix">Gesamt:</string>
    <string name="label_calories">Kalorien</string>
    <string name="label_carbs">Kohlenhydrate</string>
    <string name="label_protein">Eiweiß</string>
    <string name="label_fat">Fett</string>
    <string name="label_calories_input">Kalorien (kcal)</string>
    <string name="label_carbs_input">Kohlenhydrate (g)</string>
    <string name="label_protein_input">Eiweiß (g)</string>
    <string name="label_fat_input">Fett (g)</string>
    <string name="nutrition_value_kcal">%1$d kcal</string>
    <string name="nutrition_value_grams">%1$s g</string>
```

- [ ] **Step 3: Verify the resources compile**

Run:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat :app:compileDebugKotlin
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

```powershell
git add app/src/main/res/values/strings.xml app/src/main/res/values-de/strings.xml
git commit -m "feat(i18n): add nutrition strings (EN + DE)"
```

---

## Task 6: Nutrition input section in the edit screen

**Files:**
- Modify: `app/src/main/java/com/nwe/recipely/ui/edit/RecipeEditScreen.kt:174-176`

No red-green here (Compose UI is verified by a successful build/run per project convention).

- [ ] **Step 1: Insert the nutrition input items**

In `RecipeEditScreen`, inside the `LazyColumn`, add the following **after** the prep-time/servings `Row` item (which ends at line 174 with `}`) and **before** `item { EditSectionHeader(stringResource(R.string.section_ingredients)) }` (line 176):

```kotlin
            item { EditSectionHeader(stringResource(R.string.nutrition_optional)) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = state.calories,
                        onValueChange = vm::setCalories,
                        label = { Text(stringResource(R.string.label_calories_input)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = state.carbs,
                        onValueChange = vm::setCarbs,
                        label = { Text(stringResource(R.string.label_carbs_input)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = state.protein,
                        onValueChange = vm::setProtein,
                        label = { Text(stringResource(R.string.label_protein_input)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = state.fat,
                        onValueChange = vm::setFat,
                        label = { Text(stringResource(R.string.label_fat_input)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
```

(No new imports needed — `Row`, `Arrangement`, `OutlinedTextField`, `KeyboardOptions`, `KeyboardType`, `Modifier.weight` are already imported.)

- [ ] **Step 2: Verify it compiles**

Run:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat :app:compileDebugKotlin
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```powershell
git add app/src/main/java/com/nwe/recipely/ui/edit/RecipeEditScreen.kt
git commit -m "feat(edit): add nutrition input section to the edit screen"
```

---

## Task 7: Nutrition display section in the detail screen

**Files:**
- Modify: `app/src/main/java/com/nwe/recipely/ui/detail/RecipeDetailScreen.kt`

Variante C: when `servings` is set, show **Per portion** values + a small **Total:** line; otherwise show only totals. The section appears only when at least one value is set, placed after the meta chips and before the ingredients.

- [ ] **Step 1: Add imports**

In `app/src/main/java/com/nwe/recipely/ui/detail/RecipeDetailScreen.kt`, add these imports (with the existing import block):

```kotlin
import androidx.compose.foundation.layout.Spacer
import com.nwe.recipely.ui.detail.NutritionFacts
import com.nwe.recipely.ui.detail.nutritionFacts
import com.nwe.recipely.ui.detail.perPortion
import com.nwe.recipely.ui.detail.NutritionFormat
```

- [ ] **Step 2: Insert the nutrition section into `DetailContent`**

In `DetailContent`'s `LazyColumn`, add the following **after** the meta-chips `item { Row(...) { ... } }` (the block at lines 148-160) and **before** the `if (details.ingredients.isNotEmpty()) {` block (line 162):

```kotlin
        val facts = details.recipe.nutritionFacts()
        if (facts.hasAny) {
            item { SectionHeader(stringResource(R.string.section_nutrition)) }
            item { NutritionBlock(facts = facts, servings = details.recipe.servings) }
        }
```

- [ ] **Step 3: Add the nutrition composables**

Add these private composables to the same file (e.g., after the existing `SectionHeader` function, around line 195):

```kotlin
@Composable
private fun NutritionBlock(facts: NutritionFacts, servings: Int?) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        val perPortion = facts.perPortion(servings)
        if (perPortion != null) {
            Text(
                text = stringResource(R.string.nutrition_per_portion),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            NutritionRows(perPortion)
            Spacer(Modifier.height(6.dp))
            Text(
                text = totalSummary(facts),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            NutritionRows(facts)
        }
    }
}

@Composable
private fun NutritionRows(facts: NutritionFacts) {
    facts.calories?.let {
        NutritionRow(stringResource(R.string.label_calories), stringResource(R.string.nutrition_value_kcal, it))
    }
    facts.carbsGrams?.let {
        NutritionRow(stringResource(R.string.label_carbs), stringResource(R.string.nutrition_value_grams, NutritionFormat.grams(it)))
    }
    facts.proteinGrams?.let {
        NutritionRow(stringResource(R.string.label_protein), stringResource(R.string.nutrition_value_grams, NutritionFormat.grams(it)))
    }
    facts.fatGrams?.let {
        NutritionRow(stringResource(R.string.label_fat), stringResource(R.string.nutrition_value_grams, NutritionFormat.grams(it)))
    }
}

@Composable
private fun NutritionRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun totalSummary(facts: NutritionFacts): String {
    val parts = buildList {
        facts.calories?.let {
            add("${stringResource(R.string.label_calories)} ${stringResource(R.string.nutrition_value_kcal, it)}")
        }
        facts.carbsGrams?.let {
            add("${stringResource(R.string.label_carbs)} ${stringResource(R.string.nutrition_value_grams, NutritionFormat.grams(it))}")
        }
        facts.proteinGrams?.let {
            add("${stringResource(R.string.label_protein)} ${stringResource(R.string.nutrition_value_grams, NutritionFormat.grams(it))}")
        }
        facts.fatGrams?.let {
            add("${stringResource(R.string.label_fat)} ${stringResource(R.string.nutrition_value_grams, NutritionFormat.grams(it))}")
        }
    }
    return "${stringResource(R.string.nutrition_total_prefix)} ${parts.joinToString(" · ")}"
}
```

- [ ] **Step 4: Verify it compiles**

Run:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat :app:compileDebugKotlin
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```powershell
git add app/src/main/java/com/nwe/recipely/ui/detail/RecipeDetailScreen.kt
git commit -m "feat(detail): show per-portion nutrition with totals summary"
```

---

## Task 8: Full build, tests, and on-device verification

**Files:** none (verification only).

- [ ] **Step 1: Run the full unit-test suite**

Run:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat testDebugUnitTest
```

Expected: `BUILD SUCCESSFUL`, all tests pass.

- [ ] **Step 2: Build the debug APK**

Run:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat assembleDebug
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Reset the local test DB and install**

Because the `Recipe` schema changed while the DB version stayed `1`, the existing on-device DB must be dropped (Room would otherwise throw a data-integrity error). `uninstallDebug` removes the app and its data; `installDebug` reinstalls with the fresh schema. Needs a connected device/emulator.

Run:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat uninstallDebug installDebug
```

Expected: `BUILD SUCCESSFUL`. (If `uninstallDebug` reports nothing installed, that's fine — continue.)

- [ ] **Step 4: Manual smoke test on the device**

Verify:
1. Create a recipe with **4 servings**, fill all four nutrition fields (use a comma decimal, e.g. `40,5`, in one macro). Save.
2. Open the recipe: the **Nutrition** section shows **Per portion** values (totals ÷ 4) and a small **Total:** line.
3. Edit the recipe: the nutrition fields are pre-filled; the comma value round-trips. Save again — values unchanged.
4. Create a recipe with nutrition values but **no servings**: the detail view shows **only the totals** (no per-portion line).
5. Create a recipe with **no** nutrition values: no Nutrition section appears.
6. Switch the device to German locale: labels read **Nährwerte / Pro Portion / Kalorien / Kohlenhydrate / Eiweiß / Fett**, and gram values use a comma separator.

- [ ] **Step 5: Final commit (if any cleanup was needed)**

```powershell
git status
# commit only if Step 4 surfaced fixes
```

---

## Self-Review notes

- **Spec coverage:** entity columns (T1), per-portion + null-servings handling (T2, T7), 1-decimal macros / integer kcal (T2 format, T1 types), comma+period input (T3), Variante C display (T7), EN+DE i18n (T5), no migration + DB reset (T8 step 3), list unchanged (no list task). All spec sections map to a task.
- **Type consistency:** `NutritionFacts`, `nutritionFacts()`, `perPortion()`, `NutritionFormat.grams()`, `toGramsOrNull()`, `toEditString()` and the setter names (`setCalories`/`setCarbs`/`setProtein`/`setFat`) are used identically across tasks and tests.
- **No placeholders:** every code step contains complete code; every command lists its expected result.
