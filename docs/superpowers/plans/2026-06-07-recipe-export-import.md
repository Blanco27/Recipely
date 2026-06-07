# Recipe Export & Import Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let the user export all recipes (including images) to a single `.zip` backup and import one back, surfaced as a new "Data" section on the Settings screen.

**Architecture:** Compose owns the Android Storage Access Framework (produces a `Uri`), the data layer owns the bytes (a `RecipeBackupManager` does JSON via kotlinx.serialization + ZIP via `java.util.zip` + image file IO through the existing `ImageStore`), and `SettingsViewModel` orchestrates state/commands/events. Import uses merge semantics (always insert new rows). No schema change → no Room migration.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, Room, kotlinx.serialization (new), `java.util.zip`, AndroidX Activity Result (SAF).

---

## Build environment (read before any CLI Gradle command)

Every `.\gradlew.bat` invocation in a fresh PowerShell shell MUST first set the JDK (default JDK 24 breaks Gradle 8.13):

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
```

JVM unit tests: `.\gradlew.bat testDebugUnitTest`. Instrumented tests need a running emulator/device: `.\gradlew.bat connectedDebugAndroidTest`. Single unit test class: `.\gradlew.bat testDebugUnitTest --tests "com.nwe.recipely.BackupModelsTest"`.

## File structure

- **Create** `app/src/main/java/com/nwe/recipely/data/backup/BackupModels.kt` — `@Serializable` DTOs + pure entity↔DTO mapping + constants. One responsibility: the serializable backup shape.
- **Create** `app/src/main/java/com/nwe/recipely/data/backup/RecipeBackupManager.kt` — the `BackupManager` interface, `ExportResult`/`ImportResult`, and the Android implementation (ZIP/JSON/image IO).
- **Modify** `app/src/main/java/com/nwe/recipely/data/RecipeDao.kt` — add `getAllRecipesWithDetails()` and `insertImported(...)`.
- **Modify** `app/src/main/java/com/nwe/recipely/data/ImageStore.kt` — add `importFromStream(...)`.
- **Modify** `app/src/main/java/com/nwe/recipely/di/AppContainer.kt` — expose `backupManager`.
- **Modify** `app/src/main/java/com/nwe/recipely/ui/settings/SettingsViewModel.kt` — backup state/commands/events + `onExportClicked`/`onImportClicked`/`export`/`import`.
- **Modify** `app/src/main/java/com/nwe/recipely/ui/settings/SettingsScreen.kt` — Data panel, SAF launchers, progress overlay, snackbar host.
- **Modify** `app/src/main/java/com/nwe/recipely/ui/theme/Color.kt` — icon-tile color tokens.
- **Modify** `app/src/main/res/values/strings.xml` and `app/src/main/res/values-de/strings.xml` — new strings + plurals.
- **Modify** `gradle/libs.versions.toml` and `app/build.gradle.kts` — kotlinx.serialization.
- **Tests:** `app/src/test/java/com/nwe/recipely/BackupModelsTest.kt` (JVM), `app/src/test/java/com/nwe/recipely/FakeBackupManager.kt`, additions to `app/src/test/java/com/nwe/recipely/SettingsViewModelTest.kt`, `app/src/androidTest/java/com/nwe/recipely/RecipeBackupManagerTest.kt` (instrumented), additions to `app/src/androidTest/java/com/nwe/recipely/RecipeDaoTest.kt`.

---

## Task 1: Add kotlinx.serialization dependency

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts:1-6` (plugins) and `:70-71` (test deps area)

- [ ] **Step 1: Add version, library, and plugin to the catalog**

In `gradle/libs.versions.toml`, under `[versions]` add:

```toml
kotlinxSerialization = "1.7.3"
```

Under `[libraries]` add:

```toml
kotlinx-serialization-json = { group = "org.jetbrains.kotlinx", name = "kotlinx-serialization-json", version.ref = "kotlinxSerialization" }
```

Under `[plugins]` add:

```toml
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
```

- [ ] **Step 2: Apply the plugin and add the dependency in the app build script**

In `app/build.gradle.kts`, the `plugins { }` block becomes:

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}
```

In the `dependencies { }` block, add next to the other `implementation` lines (e.g. after `implementation(libs.coil.compose)`):

```kotlin
    implementation(libs.kotlinx.serialization.json)
```

- [ ] **Step 3: Verify the project still configures and compiles**

Run:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat assembleDebug
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

```bash
git add gradle/libs.versions.toml app/build.gradle.kts
git commit -m "build: add kotlinx.serialization for recipe backup"
```

---

## Task 2: Backup DTOs + mapping (pure, TDD)

**Files:**
- Create: `app/src/main/java/com/nwe/recipely/data/backup/BackupModels.kt`
- Test: `app/src/test/java/com/nwe/recipely/BackupModelsTest.kt`

- [ ] **Step 1: Write the failing test**

Create `app/src/test/java/com/nwe/recipely/BackupModelsTest.kt`:

```kotlin
package com.nwe.recipely

import com.nwe.recipely.data.Ingredient
import com.nwe.recipely.data.Recipe
import com.nwe.recipely.data.RecipeWithDetails
import com.nwe.recipely.data.Step
import com.nwe.recipely.data.backup.BackupFile
import com.nwe.recipely.data.backup.SCHEMA_VERSION
import com.nwe.recipely.data.backup.toBackupRecipe
import com.nwe.recipely.data.backup.toRecipeWithDetails
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class BackupModelsTest {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private fun sample() = RecipeWithDetails(
        recipe = Recipe(
            id = 7, name = "Lasagne", imageUri = "/data/img_title.jpg",
            prepTimeMinutes = 60, servings = 4, calories = 850,
            carbsGrams = 60.0, proteinGrams = 35.0, fatGrams = 40.0, category = "MAIN",
        ),
        // intentionally out of order to prove sorting by position
        ingredients = listOf(
            Ingredient(recipeId = 7, text = "Sheets", position = 1),
            Ingredient(recipeId = 7, text = "Beef", position = 0),
        ),
        steps = listOf(
            Step(recipeId = 7, text = "Bake", imageUri = "/data/img_step.jpg", position = 1),
            Step(recipeId = 7, text = "Brown beef", imageUri = null, position = 0),
        ),
    )

    @Test
    fun toBackupRecipe_sortsChildrenByPosition_andSetsImages() {
        val backup = sample().toBackupRecipe(
            image = "images/title.jpg",
            stepImages = listOf(null, "images/step.jpg"), // aligned to position-sorted steps
        )

        assertEquals("Lasagne", backup.name)
        assertEquals("MAIN", backup.category)
        assertEquals("images/title.jpg", backup.image)
        assertEquals(listOf("Beef", "Sheets"), backup.ingredients.map { it.text })
        assertEquals(listOf("Brown beef", "Bake"), backup.steps.map { it.text })
        assertEquals(listOf(null, "images/step.jpg"), backup.steps.map { it.image })
    }

    @Test
    fun jsonRoundTrip_preservesAllFields() {
        val original = BackupFile(
            schemaVersion = SCHEMA_VERSION,
            exportedAt = "2026-06-07T10:15:30Z",
            recipes = listOf(sample().toBackupRecipe(null, listOf(null, null))),
        )

        val decoded = json.decodeFromString<BackupFile>(json.encodeToString(original))

        assertEquals(original, decoded)
    }

    @Test
    fun toRecipeWithDetails_buildsFreshEntities_withNewImagePaths() {
        val backup = sample().toBackupRecipe(image = "images/t.jpg", stepImages = listOf(null, "images/s.jpg"))

        val rwd = backup.toRecipeWithDetails(
            image = "/new/title.jpg",
            stepImages = listOf("/new/a.jpg", "/new/b.jpg"),
        )

        assertEquals(0L, rwd.recipe.id) // id reset for insert
        assertEquals("/new/title.jpg", rwd.recipe.imageUri)
        assertEquals(850, rwd.recipe.calories)
        assertEquals(listOf("Beef", "Sheets"), rwd.ingredients.map { it.text })
        assertEquals(listOf("Brown beef", "Bake"), rwd.steps.map { it.text })
        assertEquals(listOf("/new/a.jpg", "/new/b.jpg"), rwd.steps.map { it.imageUri })
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat testDebugUnitTest --tests "com.nwe.recipely.BackupModelsTest"
```

Expected: FAIL — `BackupFile`, `toBackupRecipe`, `toRecipeWithDetails`, `SCHEMA_VERSION` unresolved.

- [ ] **Step 3: Write the implementation**

Create `app/src/main/java/com/nwe/recipely/data/backup/BackupModels.kt`:

```kotlin
package com.nwe.recipely.data.backup

import com.nwe.recipely.data.Ingredient
import com.nwe.recipely.data.Recipe
import com.nwe.recipely.data.RecipeWithDetails
import com.nwe.recipely.data.Step
import kotlinx.serialization.Serializable

/** Current backup schema version. Bump when the JSON shape changes. */
const val SCHEMA_VERSION = 1

/** ZIP entry name of the recipe JSON. */
const val JSON_ENTRY = "recipes.json"

/** ZIP folder (prefix) that holds image files. */
const val IMAGES_DIR = "images/"

@Serializable
data class BackupFile(
    val schemaVersion: Int,
    val exportedAt: String,
    val recipes: List<BackupRecipe>,
)

@Serializable
data class BackupRecipe(
    val name: String,
    val prepTimeMinutes: Int? = null,
    val servings: Int? = null,
    val calories: Int? = null,
    val carbsGrams: Double? = null,
    val proteinGrams: Double? = null,
    val fatGrams: Double? = null,
    val category: String? = null,
    /** ZIP-relative path (e.g. "images/x.jpg") or null. */
    val image: String? = null,
    val ingredients: List<BackupIngredient> = emptyList(),
    val steps: List<BackupStep> = emptyList(),
)

@Serializable
data class BackupIngredient(val text: String, val position: Int)

@Serializable
data class BackupStep(val text: String, val image: String? = null, val position: Int)

/**
 * Maps a stored recipe to its backup DTO. Children are sorted by [position]; [image] is the
 * ZIP-relative title-image path and [stepImages] are the ZIP-relative step-image paths aligned
 * to the position-sorted step order (null where a step has no image).
 */
fun RecipeWithDetails.toBackupRecipe(image: String?, stepImages: List<String?>): BackupRecipe {
    val sortedSteps = steps.sortedBy { it.position }
    return BackupRecipe(
        name = recipe.name,
        prepTimeMinutes = recipe.prepTimeMinutes,
        servings = recipe.servings,
        calories = recipe.calories,
        carbsGrams = recipe.carbsGrams,
        proteinGrams = recipe.proteinGrams,
        fatGrams = recipe.fatGrams,
        category = recipe.category,
        image = image,
        ingredients = ingredients.sortedBy { it.position }
            .map { BackupIngredient(it.text, it.position) },
        steps = sortedSteps.mapIndexed { i, s -> BackupStep(s.text, stepImages.getOrNull(i), s.position) },
    )
}

/**
 * Builds fresh Room entities (id == 0) from a backup DTO. [image] is the new absolute title-image
 * path and [stepImages] the new absolute step-image paths aligned to [BackupRecipe.steps] order.
 */
fun BackupRecipe.toRecipeWithDetails(image: String?, stepImages: List<String?>): RecipeWithDetails =
    RecipeWithDetails(
        recipe = Recipe(
            name = name,
            imageUri = image,
            prepTimeMinutes = prepTimeMinutes,
            servings = servings,
            calories = calories,
            carbsGrams = carbsGrams,
            proteinGrams = proteinGrams,
            fatGrams = fatGrams,
            category = category,
        ),
        ingredients = ingredients.map { Ingredient(recipeId = 0, text = it.text, position = it.position) },
        steps = steps.mapIndexed { i, s ->
            Step(recipeId = 0, text = s.text, imageUri = stepImages.getOrNull(i), position = s.position)
        },
    )
```

- [ ] **Step 4: Run the test to verify it passes**

Run:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat testDebugUnitTest --tests "com.nwe.recipely.BackupModelsTest"
```

Expected: PASS (3 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/nwe/recipely/data/backup/BackupModels.kt app/src/test/java/com/nwe/recipely/BackupModelsTest.kt
git commit -m "feat(backup): serializable backup models and entity mapping"
```

---

## Task 3: DAO reads/writes for backup (instrumented, TDD)

**Files:**
- Modify: `app/src/main/java/com/nwe/recipely/data/RecipeDao.kt`
- Test: `app/src/androidTest/java/com/nwe/recipely/RecipeDaoTest.kt`

- [ ] **Step 1: Write the failing tests**

Append these two tests inside the `RecipeDaoTest` class in `app/src/androidTest/java/com/nwe/recipely/RecipeDaoTest.kt` (before the closing brace). They reference `RecipeWithDetails`, so add the import `import com.nwe.recipely.data.RecipeWithDetails` at the top:

```kotlin
    @Test
    fun getAllRecipesWithDetails_returnsAllWithChildren() = runTest {
        dao.upsertRecipeWithChildren(
            recipe = Recipe(name = "A"),
            ingredients = listOf(Ingredient(recipeId = 0, text = "a1", position = 0)),
            steps = listOf(Step(recipeId = 0, text = "s1", position = 0)),
        )
        dao.upsertRecipeWithChildren(
            recipe = Recipe(name = "B"),
            ingredients = emptyList(),
            steps = emptyList(),
        )

        val all = dao.getAllRecipesWithDetails()

        assertEquals(2, all.size)
        val a = all.first { it.recipe.name == "A" }
        assertEquals(1, a.ingredients.size)
        assertEquals(1, a.steps.size)
    }

    @Test
    fun insertImported_addsRowsWithoutTouchingExisting() = runTest {
        val existing = dao.upsertRecipeWithChildren(
            recipe = Recipe(name = "Existing"),
            ingredients = emptyList(),
            steps = emptyList(),
        )

        dao.insertImported(
            listOf(
                RecipeWithDetails(
                    recipe = Recipe(id = 999, name = "Imported", category = "DESSERT"),
                    ingredients = listOf(Ingredient(recipeId = 999, text = "Sugar", position = 0)),
                    steps = listOf(Step(recipeId = 999, text = "Whisk", position = 0)),
                ),
            ),
        )

        val all = dao.getAllRecipesWithDetails()
        assertEquals(2, all.size)
        // existing untouched
        assertEquals("Existing", dao.observeRecipe(existing).first()!!.recipe.name)
        // imported got a fresh id and kept its children
        val imported = all.first { it.recipe.name == "Imported" }
        assert(imported.recipe.id != 999L)
        assertEquals("Sugar", imported.ingredients.single().text)
        assertEquals("DESSERT", imported.recipe.category)
    }
```

- [ ] **Step 2: Run to verify failure**

Run (emulator/device required):

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.nwe.recipely.RecipeDaoTest
```

Expected: FAIL — `getAllRecipesWithDetails` / `insertImported` unresolved.

- [ ] **Step 3: Implement the DAO methods**

In `app/src/main/java/com/nwe/recipely/data/RecipeDao.kt`, add inside the `RecipeDao` interface (after `observeRecipe`):

```kotlin
    @Transaction
    @Query("SELECT * FROM recipes")
    suspend fun getAllRecipesWithDetails(): List<RecipeWithDetails>
```

And add this default method (after `upsertRecipeWithChildren`):

```kotlin
    /** Inserts each imported recipe as a brand-new row (merge import). All-or-nothing. */
    @Transaction
    suspend fun insertImported(recipes: List<RecipeWithDetails>) {
        recipes.forEach {
            upsertRecipeWithChildren(it.recipe.copy(id = 0), it.ingredients, it.steps)
        }
    }
```

- [ ] **Step 4: Run to verify passing**

Run:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.nwe.recipely.RecipeDaoTest
```

Expected: PASS (all `RecipeDaoTest` tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/nwe/recipely/data/RecipeDao.kt app/src/androidTest/java/com/nwe/recipely/RecipeDaoTest.kt
git commit -m "feat(backup): DAO read-all and atomic merge-insert"
```

---

## Task 4: ImageStore.importFromStream

**Files:**
- Modify: `app/src/main/java/com/nwe/recipely/data/ImageStore.kt`

This small method is exercised by Task 5's round-trip test, so no separate test here.

- [ ] **Step 1: Add the method**

In `app/src/main/java/com/nwe/recipely/data/ImageStore.kt`, add `import java.io.InputStream` at the top, then add this method to the `ImageStore` class (after `importFromUri`):

```kotlin
    /** Copies an arbitrary [input] stream (e.g. a ZIP entry) into internal storage. Returns the new path, or null on failure. */
    fun importFromStream(input: InputStream): String? = try {
        val target = newImageFile()
        target.outputStream().use { output -> input.copyTo(output) }
        target.absolutePath
    } catch (e: Exception) {
        null
    }
```

- [ ] **Step 2: Verify it compiles**

Run:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat compileDebugKotlin
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/nwe/recipely/data/ImageStore.kt
git commit -m "feat(backup): ImageStore.importFromStream for restoring images"
```

---

## Task 5: RecipeBackupManager (export/import, instrumented TDD)

**Files:**
- Create: `app/src/main/java/com/nwe/recipely/data/backup/RecipeBackupManager.kt`
- Test: `app/src/androidTest/java/com/nwe/recipely/RecipeBackupManagerTest.kt`

- [ ] **Step 1: Write the failing test**

Create `app/src/androidTest/java/com/nwe/recipely/RecipeBackupManagerTest.kt`:

```kotlin
package com.nwe.recipely

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nwe.recipely.data.ImageStore
import com.nwe.recipely.data.Ingredient
import com.nwe.recipely.data.Recipe
import com.nwe.recipely.data.RecipeDao
import com.nwe.recipely.data.RecipeDatabase
import com.nwe.recipely.data.Step
import com.nwe.recipely.data.backup.ExportResult
import com.nwe.recipely.data.backup.ImportResult
import com.nwe.recipely.data.backup.JSON_ENTRY
import com.nwe.recipely.data.backup.RecipeBackupManager
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayInputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(AndroidJUnit4::class)
class RecipeBackupManagerTest {

    private lateinit var context: Context
    private lateinit var db: RecipeDatabase
    private lateinit var dao: RecipeDao
    private lateinit var imageStore: ImageStore
    private lateinit var manager: RecipeBackupManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, RecipeDatabase::class.java).build()
        dao = db.recipeDao()
        imageStore = ImageStore(context)
        manager = RecipeBackupManager(dao, imageStore, context.contentResolver)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun export_then_import_roundTrips_andMerges() = runTest {
        val img = imageStore.importFromStream(ByteArrayInputStream(byteArrayOf(1, 2, 3)))!!
        dao.upsertRecipeWithChildren(
            recipe = Recipe(name = "Cake", imageUri = img, calories = 500, category = "DESSERT"),
            ingredients = listOf(Ingredient(recipeId = 0, text = "Flour", position = 0)),
            steps = listOf(Step(recipeId = 0, text = "Mix", imageUri = null, position = 0)),
        )

        val zip = File(context.cacheDir, "backup-test.zip")
        val uri = Uri.fromFile(zip)

        val export = manager.export(uri)
        assertEquals(ExportResult.Success(1), export)
        assertTrue(zip.exists() && zip.length() > 0)

        val import = manager.import(uri)
        assertEquals(ImportResult.Success(1), import)

        val all = dao.getAllRecipesWithDetails()
        assertEquals(2, all.size) // merge: original + imported
        val imported = all.last { it.recipe.name == "Cake" }
        assertEquals("DESSERT", imported.recipe.category)
        assertEquals(500, imported.recipe.calories)
        assertEquals("Flour", imported.ingredients.single().text)
        // restored title image exists on disk and is a fresh copy (different path)
        assertTrue(File(imported.recipe.imageUri!!).exists())
        assertTrue(imported.recipe.imageUri != img)
    }

    @Test
    fun import_wrongSchemaVersion_returnsInvalid() = runTest {
        val zip = File(context.cacheDir, "bad-schema.zip")
        ZipOutputStream(zip.outputStream()).use { z ->
            z.putNextEntry(ZipEntry(JSON_ENTRY))
            z.write("""{"schemaVersion":99,"exportedAt":"x","recipes":[]}""".toByteArray())
            z.closeEntry()
        }

        assertEquals(ImportResult.Invalid, manager.import(Uri.fromFile(zip)))
    }

    @Test
    fun import_missingJson_returnsInvalid() = runTest {
        val zip = File(context.cacheDir, "no-json.zip")
        ZipOutputStream(zip.outputStream()).use { z ->
            z.putNextEntry(ZipEntry("images/x.jpg"))
            z.write(byteArrayOf(9))
            z.closeEntry()
        }

        assertEquals(ImportResult.Invalid, manager.import(Uri.fromFile(zip)))
    }
}
```

- [ ] **Step 2: Run to verify failure**

Run:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.nwe.recipely.RecipeBackupManagerTest
```

Expected: FAIL — `RecipeBackupManager`, `ExportResult`, `ImportResult` unresolved.

- [ ] **Step 3: Implement the manager**

Create `app/src/main/java/com/nwe/recipely/data/backup/RecipeBackupManager.kt`:

```kotlin
package com.nwe.recipely.data.backup

import android.content.ContentResolver
import android.net.Uri
import com.nwe.recipely.data.ImageStore
import com.nwe.recipely.data.RecipeDao
import com.nwe.recipely.data.RecipeWithDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.BufferedOutputStream
import java.io.ByteArrayInputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

sealed interface ExportResult {
    data class Success(val count: Int) : ExportResult
    data object Error : ExportResult
}

sealed interface ImportResult {
    data class Success(val count: Int) : ImportResult
    data object Invalid : ImportResult
    data object Error : ImportResult
}

/** Reads/writes a [Uri] from SAF; both methods run on [Dispatchers.IO]. */
interface BackupManager {
    suspend fun export(target: Uri): ExportResult
    suspend fun import(source: Uri): ImportResult
}

class RecipeBackupManager(
    private val dao: RecipeDao,
    private val imageStore: ImageStore,
    private val contentResolver: ContentResolver,
) : BackupManager {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; prettyPrint = false }

    override suspend fun export(target: Uri): ExportResult = withContext(Dispatchers.IO) {
        try {
            val recipes = dao.getAllRecipesWithDetails()
            val output = contentResolver.openOutputStream(target) ?: return@withContext ExportResult.Error
            ZipOutputStream(BufferedOutputStream(output)).use { zip ->
                val usedNames = HashSet<String>()
                val dtos = recipes.map { rwd ->
                    val titleName = writeImage(zip, rwd.recipe.imageUri, usedNames)
                    val sortedSteps = rwd.steps.sortedBy { it.position }
                    val stepNames = sortedSteps.map { writeImage(zip, it.imageUri, usedNames) }
                    rwd.toBackupRecipe(titleName, stepNames)
                }
                val file = BackupFile(SCHEMA_VERSION, nowIso(), dtos)
                zip.putNextEntry(ZipEntry(JSON_ENTRY))
                zip.write(json.encodeToString(file).toByteArray())
                zip.closeEntry()
            }
            ExportResult.Success(recipes.size)
        } catch (e: Exception) {
            ExportResult.Error
        }
    }

    override suspend fun import(source: Uri): ImportResult = withContext(Dispatchers.IO) {
        val copied = mutableListOf<String>()
        try {
            val contents = readZip(source) ?: return@withContext ImportResult.Error
            val jsonText = contents.json ?: return@withContext ImportResult.Invalid
            val file = try {
                json.decodeFromString<BackupFile>(jsonText)
            } catch (e: Exception) {
                return@withContext ImportResult.Invalid
            }
            if (file.schemaVersion != SCHEMA_VERSION) return@withContext ImportResult.Invalid

            val toInsert = file.recipes.map { br ->
                val titlePath = copyImage(contents.images[br.image], copied)
                val stepPaths = br.steps.map { copyImage(contents.images[it.image], copied) }
                br.toRecipeWithDetails(titlePath, stepPaths)
            }
            dao.insertImported(toInsert)
            ImportResult.Success(toInsert.size)
        } catch (e: Exception) {
            copied.forEach(imageStore::delete)
            ImportResult.Error
        }
    }

    /** Writes the file at [absPath] into the ZIP under images/, returning its entry name (or null). */
    private fun writeImage(zip: ZipOutputStream, absPath: String?, used: MutableSet<String>): String? {
        if (absPath.isNullOrEmpty()) return null
        val src = File(absPath)
        if (!src.exists()) return null
        var base = src.name
        while (!used.add(base)) base = "${System.nanoTime()}_${src.name}"
        val entryName = IMAGES_DIR + base
        zip.putNextEntry(ZipEntry(entryName))
        src.inputStream().use { it.copyTo(zip) }
        zip.closeEntry()
        return entryName
    }

    /** Copies image [bytes] into internal storage, tracking the new path in [copied]. Null-safe. */
    private fun copyImage(bytes: ByteArray?, copied: MutableList<String>): String? {
        if (bytes == null) return null
        return imageStore.importFromStream(ByteArrayInputStream(bytes))?.also { copied.add(it) }
    }

    private class ZipContents(val json: String?, val images: Map<String, ByteArray>)

    private fun readZip(source: Uri): ZipContents? {
        val input = contentResolver.openInputStream(source) ?: return null
        var jsonText: String? = null
        val images = HashMap<String, ByteArray>()
        ZipInputStream(input).use { zip ->
            var entry: ZipEntry? = zip.nextEntry
            while (entry != null) {
                val bytes = zip.readBytes()
                if (entry.name == JSON_ENTRY) jsonText = String(bytes) else images[entry.name] = bytes
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        return ZipContents(jsonText, images)
    }

    private fun nowIso(): String {
        val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        fmt.timeZone = TimeZone.getTimeZone("UTC")
        return fmt.format(Date())
    }
}
```

- [ ] **Step 4: Run to verify passing**

Run:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.nwe.recipely.RecipeBackupManagerTest
```

Expected: PASS (3 tests).

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/nwe/recipely/data/backup/RecipeBackupManager.kt app/src/androidTest/java/com/nwe/recipely/RecipeBackupManagerTest.kt
git commit -m "feat(backup): RecipeBackupManager zip export/import with images"
```

---

## Task 6: Expose backupManager from AppContainer

**Files:**
- Modify: `app/src/main/java/com/nwe/recipely/di/AppContainer.kt`

- [ ] **Step 1: Add the manager**

In `app/src/main/java/com/nwe/recipely/di/AppContainer.kt`, add the import:

```kotlin
import com.nwe.recipely.data.backup.BackupManager
import com.nwe.recipely.data.backup.RecipeBackupManager
```

And add this property after `settingsRepository` (it reuses the existing `database`, `imageStore`, and `context`):

```kotlin
    val backupManager: BackupManager =
        RecipeBackupManager(database.recipeDao(), imageStore, context.contentResolver)
```

- [ ] **Step 2: Verify it compiles**

Run:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat compileDebugKotlin
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/nwe/recipely/di/AppContainer.kt
git commit -m "feat(backup): wire BackupManager into AppContainer"
```

---

## Task 7: Color tokens for the icon tiles

**Files:**
- Modify: `app/src/main/java/com/nwe/recipely/ui/theme/Color.kt`

- [ ] **Step 1: Add the tokens**

At the end of `app/src/main/java/com/nwe/recipely/ui/theme/Color.kt`, add:

```kotlin
// --- Backup action icon tiles (Settings ▸ Data) — match mockup ---
val ExportIconBgLight = Color(0xFFE9F0E7) // soft green
val ExportIconBgDark = Color(0xFF22301F)
val ImportIconBgLight = Color(0xFFFBEEDD) // soft orange
val ImportIconBgDark = Color(0xFF3A2C18)
```

- [ ] **Step 2: Verify it compiles**

Run:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat compileDebugKotlin
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/nwe/recipely/ui/theme/Color.kt
git commit -m "feat(backup): icon-tile color tokens for Data section"
```

---

## Task 8: Strings (EN + DE)

**Files:**
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-de/strings.xml`

- [ ] **Step 1: Add English strings**

In `app/src/main/res/values/strings.xml`, before the closing `</resources>`, add:

```xml
    <!-- Settings: Data / Backup -->
    <string name="settings_data">Data</string>
    <string name="export_recipes_title">Export recipes</string>
    <string name="export_recipes_subtitle">Save all recipes as a .zip backup</string>
    <string name="import_recipes_title">Import recipes</string>
    <string name="import_recipes_subtitle">Restore from a .zip backup</string>
    <string name="backup_exporting">Exporting…</string>
    <string name="backup_importing">Importing…</string>
    <string name="backup_export_empty">No recipes to export</string>
    <string name="backup_error_invalid">Not a valid Recipely backup</string>
    <string name="backup_error_export">Export failed</string>
    <string name="backup_error_import">Import failed</string>
    <plurals name="backup_export_done">
        <item quantity="one">%d recipe exported</item>
        <item quantity="other">%d recipes exported</item>
    </plurals>
    <plurals name="backup_import_done">
        <item quantity="one">%d recipe imported</item>
        <item quantity="other">%d recipes imported</item>
    </plurals>
```

- [ ] **Step 2: Add German strings**

In `app/src/main/res/values-de/strings.xml`, before the closing `</resources>`, add:

```xml
    <!-- Settings: Daten / Sicherung -->
    <string name="settings_data">Daten</string>
    <string name="export_recipes_title">Rezepte exportieren</string>
    <string name="export_recipes_subtitle">Alle Rezepte als .zip sichern</string>
    <string name="import_recipes_title">Rezepte importieren</string>
    <string name="import_recipes_subtitle">Aus einer .zip wiederherstellen</string>
    <string name="backup_exporting">Exportiere…</string>
    <string name="backup_importing">Importiere…</string>
    <string name="backup_export_empty">Keine Rezepte zum Exportieren</string>
    <string name="backup_error_invalid">Keine gültige Recipely-Sicherung</string>
    <string name="backup_error_export">Export fehlgeschlagen</string>
    <string name="backup_error_import">Import fehlgeschlagen</string>
    <plurals name="backup_export_done">
        <item quantity="one">%d Rezept exportiert</item>
        <item quantity="other">%d Rezepte exportiert</item>
    </plurals>
    <plurals name="backup_import_done">
        <item quantity="one">%d Rezept importiert</item>
        <item quantity="other">%d Rezepte importiert</item>
    </plurals>
```

- [ ] **Step 3: Verify resources compile**

Run:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat assembleDebug
```

Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/res/values/strings.xml app/src/main/res/values-de/strings.xml
git commit -m "feat(backup): strings for Data section (EN + DE)"
```

---

## Task 9: SettingsViewModel backup logic (TDD)

**Files:**
- Modify: `app/src/main/java/com/nwe/recipely/ui/settings/SettingsViewModel.kt`
- Create: `app/src/test/java/com/nwe/recipely/FakeBackupManager.kt`
- Test: `app/src/test/java/com/nwe/recipely/SettingsViewModelTest.kt`

- [ ] **Step 1: Create the fake backup manager**

Create `app/src/test/java/com/nwe/recipely/FakeBackupManager.kt`:

```kotlin
package com.nwe.recipely

import android.net.Uri
import com.nwe.recipely.data.backup.BackupManager
import com.nwe.recipely.data.backup.ExportResult
import com.nwe.recipely.data.backup.ImportResult

/** Test double. The ViewModel unit tests exercise click handlers, not the Uri paths. */
class FakeBackupManager(
    private val exportResult: ExportResult = ExportResult.Success(0),
    private val importResult: ImportResult = ImportResult.Success(0),
) : BackupManager {
    override suspend fun export(target: Uri): ExportResult = exportResult
    override suspend fun import(source: Uri): ImportResult = importResult
}
```

- [ ] **Step 2: Write the failing ViewModel tests**

Replace the body of `app/src/test/java/com/nwe/recipely/SettingsViewModelTest.kt` with (keeps the existing theme tests, adds backup tests):

```kotlin
package com.nwe.recipely

import com.nwe.recipely.data.Recipe
import com.nwe.recipely.data.ThemeMode
import com.nwe.recipely.ui.settings.BackupCommand
import com.nwe.recipely.ui.settings.BackupMessage
import com.nwe.recipely.ui.settings.SettingsViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun newVm(
        repo: FakeRecipeRepository = FakeRecipeRepository(),
        backup: FakeBackupManager = FakeBackupManager(),
    ) = SettingsViewModel(FakeSettingsRepository(), repo, backup)

    @Test
    fun defaultThemeMode_isSystem() = runTest {
        val vm = newVm()
        backgroundScope.launch { vm.themeMode.collect {} }
        advanceUntilIdle()
        assertEquals(ThemeMode.SYSTEM, vm.themeMode.value)
    }

    @Test
    fun setThemeMode_propagatesToFlow() = runTest {
        val vm = newVm()
        backgroundScope.launch { vm.themeMode.collect {} }
        vm.setThemeMode(ThemeMode.DARK)
        advanceUntilIdle()
        assertEquals(ThemeMode.DARK, vm.themeMode.value)
    }

    @Test
    fun onExportClicked_whenEmpty_emitsEmptyMessage() = runTest {
        val repo = FakeRecipeRepository() // recipes default to emptyList()
        val vm = newVm(repo)
        val events = mutableListOf<BackupMessage>()
        backgroundScope.launch { vm.events.collect { events.add(it) } }

        vm.onExportClicked()
        advanceUntilIdle()

        assertEquals(listOf(BackupMessage.Empty), events)
    }

    @Test
    fun onExportClicked_whenNotEmpty_emitsPickExportCommand() = runTest {
        val repo = FakeRecipeRepository().apply { recipes.value = listOf(Recipe(name = "X")) }
        val vm = newVm(repo)
        val commands = mutableListOf<BackupCommand>()
        backgroundScope.launch { vm.commands.collect { commands.add(it) } }

        vm.onExportClicked()
        advanceUntilIdle()

        assertEquals(listOf(BackupCommand.PickExportTarget), commands)
    }

    @Test
    fun onImportClicked_emitsPickImportCommand() = runTest {
        val vm = newVm()
        val commands = mutableListOf<BackupCommand>()
        backgroundScope.launch { vm.commands.collect { commands.add(it) } }

        vm.onImportClicked()
        advanceUntilIdle()

        assertEquals(listOf(BackupCommand.PickImportSource), commands)
    }
}
```

Note: the `export(Uri)`/`import(Uri)` paths are intentionally **not** unit-tested — `android.net.Uri` can't be constructed in JVM tests (and the stub `Uri.EMPTY` is null, which would trip Kotlin's non-null parameter check). Those paths are covered by `RecipeBackupManagerTest` (instrumented) and the manual verification in Task 10. The unit tests cover the click handlers, which need no `Uri`.

- [ ] **Step 3: Run to verify failure**

Run:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat testDebugUnitTest --tests "com.nwe.recipely.SettingsViewModelTest"
```

Expected: FAIL — `BackupCommand`, `BackupMessage`, the new `SettingsViewModel` constructor params, and the click/handler methods are unresolved.

- [ ] **Step 4: Implement the ViewModel**

Replace `app/src/main/java/com/nwe/recipely/ui/settings/SettingsViewModel.kt` with:

```kotlin
package com.nwe.recipely.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nwe.recipely.data.RecipeRepository
import com.nwe.recipely.data.SettingsRepository
import com.nwe.recipely.data.ThemeMode
import com.nwe.recipely.data.backup.BackupManager
import com.nwe.recipely.data.backup.ExportResult
import com.nwe.recipely.data.backup.ImportResult
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Whether a backup operation is in flight (drives the progress overlay). */
enum class BackupState { Idle, Exporting, Importing }

/** One-shot requests for the UI to open a SAF picker. */
sealed interface BackupCommand {
    data object PickExportTarget : BackupCommand
    data object PickImportSource : BackupCommand
}

/** One-shot results to show as a snackbar. */
sealed interface BackupMessage {
    data object Empty : BackupMessage
    data class ExportDone(val count: Int) : BackupMessage
    data class ImportDone(val count: Int) : BackupMessage
    data object InvalidFile : BackupMessage
    data object ExportFailed : BackupMessage
    data object ImportFailed : BackupMessage
}

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val recipeRepository: RecipeRepository,
    private val backupManager: BackupManager,
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = settingsRepository.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeMode.SYSTEM)

    private val _backupState = MutableStateFlow(BackupState.Idle)
    val backupState: StateFlow<BackupState> = _backupState

    private val _commands = Channel<BackupCommand>(Channel.BUFFERED)
    val commands = _commands.receiveAsFlow()

    private val _events = Channel<BackupMessage>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    /** Guards empty export before opening the file picker. */
    fun onExportClicked() {
        viewModelScope.launch {
            if (recipeRepository.observeRecipes().first().isEmpty()) {
                _events.send(BackupMessage.Empty)
            } else {
                _commands.send(BackupCommand.PickExportTarget)
            }
        }
    }

    fun onImportClicked() {
        viewModelScope.launch { _commands.send(BackupCommand.PickImportSource) }
    }

    fun export(target: Uri) {
        viewModelScope.launch {
            _backupState.value = BackupState.Exporting
            val message = when (val r = backupManager.export(target)) {
                is ExportResult.Success -> BackupMessage.ExportDone(r.count)
                ExportResult.Error -> BackupMessage.ExportFailed
            }
            _events.send(message)
            _backupState.value = BackupState.Idle
        }
    }

    fun import(source: Uri) {
        viewModelScope.launch {
            _backupState.value = BackupState.Importing
            val message = when (val r = backupManager.import(source)) {
                is ImportResult.Success -> BackupMessage.ImportDone(r.count)
                ImportResult.Invalid -> BackupMessage.InvalidFile
                ImportResult.Error -> BackupMessage.ImportFailed
            }
            _events.send(message)
            _backupState.value = BackupState.Idle
        }
    }
}
```

- [ ] **Step 5: Run to verify passing**

Run:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat testDebugUnitTest --tests "com.nwe.recipely.SettingsViewModelTest"
```

Expected: PASS (5 tests).

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/nwe/recipely/ui/settings/SettingsViewModel.kt app/src/test/java/com/nwe/recipely/FakeBackupManager.kt app/src/test/java/com/nwe/recipely/SettingsViewModelTest.kt
git commit -m "feat(backup): SettingsViewModel export/import orchestration"
```

---

## Task 10: Settings screen Data section + SAF + overlay + snackbar

**Files:**
- Modify: `app/src/main/java/com/nwe/recipely/ui/settings/SettingsScreen.kt`

Compose UI is verified by a successful build/run (project convention), and the rendered result must match the approved mockup (icon tiles with colored emoji, captions, chevron, progress overlay, snackbars). See `docs/superpowers/specs/2026-06-07-recipe-export-import-design.md` → *Visual specification*.

- [ ] **Step 1: Update the ViewModel factory and screen wiring**

In `app/src/main/java/com/nwe/recipely/ui/settings/SettingsScreen.kt`, add these imports:

```kotlin
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import com.nwe.recipely.ui.theme.ExportIconBgDark
import com.nwe.recipely.ui.theme.ExportIconBgLight
import com.nwe.recipely.ui.theme.ImportIconBgDark
import com.nwe.recipely.ui.theme.ImportIconBgLight
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
```

Update the `viewModel(...)` factory in `SettingsScreen` to inject all three dependencies:

```kotlin
    val vm: SettingsViewModel = viewModel(
        factory = viewModelFactory {
            initializer {
                SettingsViewModel(
                    container.settingsRepository,
                    container.repository,
                    container.backupManager,
                )
            }
        },
    )
```

- [ ] **Step 2: Add SAF launchers, command collection, snackbar host, and overlay**

Inside `SettingsScreen`, after the existing `val themeMode by ...` / `var language by ...` lines, add:

```kotlin
    val context = LocalContext.current
    val backupState by vm.backupState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip"),
    ) { uri -> uri?.let(vm::export) }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let(vm::import) }

    LaunchedEffect(Unit) {
        vm.commands.collect { command ->
            when (command) {
                BackupCommand.PickExportTarget -> exportLauncher.launch(defaultExportFileName())
                BackupCommand.PickImportSource ->
                    importLauncher.launch(arrayOf("application/zip", "application/octet-stream", "*/*"))
            }
        }
    }

    LaunchedEffect(Unit) {
        vm.events.collect { message -> snackbarHostState.showSnackbar(message.toText(context)) }
    }
```

Add `snackbarHost = { SnackbarHost(snackbarHostState) },` to the `Scaffold(...)` call (alongside `topBar = { ... }`).

- [ ] **Step 3: Render the Data section and the progress overlay**

Replace the `Scaffold` content lambda so it wraps the existing `Column` in a `Box` (for the overlay) and appends the Data section. The content lambda becomes:

```kotlin
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
            ) {
                SectionLabel(stringResource(R.string.settings_appearance))
                ThemePanel(selected = themeMode, onSelect = vm::setThemeMode)

                Spacer(Modifier.height(20.dp))

                SectionLabel(stringResource(R.string.settings_language))
                LanguagePanel(
                    selected = language,
                    onSelect = {
                        language = it
                        it.applyNow()
                    },
                )

                Spacer(Modifier.height(20.dp))

                SectionLabel(stringResource(R.string.settings_data))
                DataPanel(
                    onExport = vm::onExportClicked,
                    onImport = vm::onImportClicked,
                )
            }

            if (backupState != BackupState.Idle) {
                BackupOverlay(
                    label = stringResource(
                        if (backupState == BackupState.Exporting) R.string.backup_exporting
                        else R.string.backup_importing,
                    ),
                )
            }
        }
    }
```

- [ ] **Step 4: Add the new composables and helpers**

At the bottom of `SettingsScreen.kt` (after the existing private composables), add:

```kotlin
@Composable
private fun DataPanel(onExport: () -> Unit, onImport: () -> Unit) {
    val dark = LocalDarkTheme.current
    SettingsPanel {
        DataRow(
            iconBackground = if (dark) ExportIconBgDark else ExportIconBgLight,
            emoji = "📤",
            title = stringResource(R.string.export_recipes_title),
            subtitle = stringResource(R.string.export_recipes_subtitle),
            onClick = onExport,
        )
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant,
            thickness = 1.dp,
        )
        DataRow(
            iconBackground = if (dark) ImportIconBgDark else ImportIconBgLight,
            emoji = "📥",
            title = stringResource(R.string.import_recipes_title),
            subtitle = stringResource(R.string.import_recipes_subtitle),
            onClick = onImport,
        )
    }
}

@Composable
private fun DataRow(
    iconBackground: Color,
    emoji: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconBackground),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = emoji, fontSize = 18.sp)
        }
        Spacer(Modifier.width(13.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun BackupOverlay(label: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.32f)),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            color = if (LocalDarkTheme.current) PaperDark else Paper,
            shape = RoundedCornerShape(16.dp),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 26.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                Text(text = label, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

private fun defaultExportFileName(): String {
    val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    return "recipely-backup-$date.zip"
}

private fun BackupMessage.toText(context: Context): String = when (this) {
    BackupMessage.Empty -> context.getString(R.string.backup_export_empty)
    is BackupMessage.ExportDone ->
        context.resources.getQuantityString(R.plurals.backup_export_done, count, count)
    is BackupMessage.ImportDone ->
        context.resources.getQuantityString(R.plurals.backup_import_done, count, count)
    BackupMessage.InvalidFile -> context.getString(R.string.backup_error_invalid)
    BackupMessage.ExportFailed -> context.getString(R.string.backup_error_export)
    BackupMessage.ImportFailed -> context.getString(R.string.backup_error_import)
}
```

- [ ] **Step 5: Build and install, then verify on a device/emulator**

Run:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat installDebug
```

Expected: `BUILD SUCCESSFUL`. Then manually verify against the mockup:
1. Settings shows a **Data** section below Language with two rows: 📤 *Export recipes* (green tile) and 📥 *Import recipes* (orange tile), each with caption + chevron, divider between them.
2. With ≥1 recipe, tap **Export** → file picker for `recipely-backup-<date>.zip` → save → progress overlay → green snackbar "N recipes exported".
3. Tap **Import** → pick that `.zip` → overlay → green snackbar "N recipes imported"; the recipe list now contains the duplicates (merge).
4. Delete all recipes, tap **Export** → snackbar "No recipes to export", no picker.
5. Import a non-backup file → snackbar "Not a valid Recipely backup".
6. Toggle dark mode → tiles use the muted dark backgrounds; everything legible.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/nwe/recipely/ui/settings/SettingsScreen.kt
git commit -m "feat(backup): Settings Data section with export/import UI"
```

---

## Task 11: Full verification

**Files:** none (verification only)

- [ ] **Step 1: Run the full JVM unit suite**

Run:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat testDebugUnitTest
```

Expected: PASS (existing tests + `BackupModelsTest` + `SettingsViewModelTest`).

- [ ] **Step 2: Run the instrumented suite**

Run (emulator/device required):

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat connectedDebugAndroidTest
```

Expected: PASS (`RecipeDaoTest` + `RecipeBackupManagerTest`).

- [ ] **Step 3: Lint**

Run:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat lint
```

Expected: no new errors (missing-translation warnings should be clear — EN and DE both updated).

- [ ] **Step 4: Final manual round-trip on device**

Export with several image-bearing recipes, uninstall the app (or use a second device), reinstall, import the `.zip`, and confirm every recipe and image is restored.

---

## Notes / deviations from the spec

- The empty-export guard reads `recipeRepository.observeRecipes().first()` rather than a `recipeCount` StateFlow — this avoids a `stateIn`/subscription pitfall (an unsubscribed `stateIn` would report the initial `0` and falsely block export) and keeps the check trivially unit-testable.
- The spec listed a separate "Couldn't read the file" string; the implementation folds unreadable-input into `ImportResult.Error` → "Import failed" to keep the result types minimal. If you want the distinct message later, split `ImportResult.Error` into `Error`/`Unreadable` and add `backup_error_read`.
```
