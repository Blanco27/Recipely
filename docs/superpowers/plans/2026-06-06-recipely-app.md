# Recipely App Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a simple, offline Android app to create, view, edit and delete recipes (name, optional image, prep time, servings, ingredient list, and ordered steps with optional per-step image) with a modern Material 3 UI.

**Architecture:** Single-Activity Jetpack Compose app, lean MVVM (Compose UI → ViewModel → Repository → Room DAO). Local-only persistence via Room. Images are copied into app-internal storage; only the file path is stored in the DB. Manual DI through an `AppContainer` held by the `Application`.

**Tech Stack:** Kotlin, Jetpack Compose + Material 3, Navigation-Compose, Room (with KSP), Coil for image loading, Coroutines/Flow. Tests: JUnit4 + kotlinx-coroutines-test (JVM ViewModel/mapping), instrumented Room DAO test.

**Conventions for this plan:**
- The machine is Windows; run Gradle as `.\gradlew.bat` from `D:\Projects\Recipely`.
- Reference spec: `docs/superpowers/specs/2026-06-06-recipely-app-design.md`.
- TDD applies to pure logic (mapping, ViewModel) and the DAO. UI composables are verified by a successful build/run (and optional preview), not red-green — this is called out per task.
- Package root: `com.nwe.recipely`. Source root: `app/src/main/java/com/nwe/recipely/`.
- Theme primary color is green (fixed Material 3 scheme, light + dark). No dynamic color.

---

## File Structure

**Build / config**
- Modify `gradle/libs.versions.toml` — add Compose/Room/KSP/Coil/test versions, libraries, plugins
- Modify `build.gradle.kts` (root) — register `kotlin.compose` and `ksp` plugins (apply false)
- Modify `app/build.gradle.kts` — apply plugins, enable Compose, swap dependencies
- Modify `app/src/main/AndroidManifest.xml` — `Application` name, `MainActivity` (launcher), `FileProvider`
- Modify `app/src/main/res/values/themes.xml` and `app/src/main/res/values-night/themes.xml` — platform parent theme (no AppCompat)
- Create `app/src/main/res/xml/file_paths.xml` — FileProvider path config

**App shell / DI / theme**
- Create `app/src/main/java/com/nwe/recipely/RecipelyApp.kt` — `Application` + holds `AppContainer`
- Create `app/src/main/java/com/nwe/recipely/di/AppContainer.kt` — wiring (DB → DAO → ImageStore → Repository)
- Create `app/src/main/java/com/nwe/recipely/MainActivity.kt` — `ComponentActivity`, `setContent`
- Create `app/src/main/java/com/nwe/recipely/ui/theme/Color.kt`
- Create `app/src/main/java/com/nwe/recipely/ui/theme/Theme.kt`
- Create `app/src/main/java/com/nwe/recipely/ui/theme/Type.kt`

**Data layer**
- Create `app/src/main/java/com/nwe/recipely/data/RecipeEntities.kt` — `Recipe`, `Ingredient`, `Step`, `RecipeWithDetails`
- Create `app/src/main/java/com/nwe/recipely/data/RecipeDao.kt`
- Create `app/src/main/java/com/nwe/recipely/data/RecipeDatabase.kt`
- Create `app/src/main/java/com/nwe/recipely/data/ImageStore.kt`
- Create `app/src/main/java/com/nwe/recipely/data/RecipeRepository.kt` — interface + `RoomRecipeRepository`

**UI layer**
- Create `app/src/main/java/com/nwe/recipely/ui/edit/EditUiState.kt` — form state + mapping functions
- Create `app/src/main/java/com/nwe/recipely/ui/edit/RecipeEditViewModel.kt`
- Create `app/src/main/java/com/nwe/recipely/ui/edit/RecipeEditScreen.kt`
- Create `app/src/main/java/com/nwe/recipely/ui/list/RecipeListViewModel.kt`
- Create `app/src/main/java/com/nwe/recipely/ui/list/RecipeListScreen.kt`
- Create `app/src/main/java/com/nwe/recipely/ui/detail/RecipeDetailViewModel.kt`
- Create `app/src/main/java/com/nwe/recipely/ui/detail/RecipeDetailScreen.kt`
- Create `app/src/main/java/com/nwe/recipely/navigation/RecipelyNavHost.kt`

**Tests**
- Create `app/src/test/java/com/nwe/recipely/MainDispatcherRule.kt`
- Create `app/src/test/java/com/nwe/recipely/FakeRecipeRepository.kt`
- Create `app/src/test/java/com/nwe/recipely/RecipeMappingTest.kt`
- Create `app/src/test/java/com/nwe/recipely/RecipeEditViewModelTest.kt`
- Create `app/src/androidTest/java/com/nwe/recipely/RecipeDaoTest.kt`
- Delete `app/src/test/java/com/nwe/recipely/ExampleUnitTest.kt` and `app/src/androidTest/java/com/nwe/recipely/ExampleInstrumentedTest.kt`

---

## Task 1: Initialize git and commit baseline

**Files:** none (repo currently has no git history)

- [ ] **Step 1: Initialize the repository**

Run:
```bash
git init
git add .
git commit -m "chore: baseline Android scaffold + Recipely design spec"
```
Expected: a first commit containing the existing scaffold, the `CLAUDE.md`, and `docs/superpowers/specs/...`.

> Note: `.gitignore` already ignores `/build`, `local.properties`, and `.superpowers/`.

---

## Task 2: Add Compose, Room, KSP, Coil to the build

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `build.gradle.kts`
- Modify: `app/build.gradle.kts`

- [ ] **Step 1: Replace the version catalog**

Replace the full contents of `gradle/libs.versions.toml` with:

```toml
[versions]
agp = "8.13.2"
kotlin = "2.0.21"
coreKtx = "1.17.0"
junit = "4.13.2"
junitVersion = "1.3.0"
espressoCore = "3.7.0"
ksp = "2.0.21-1.0.28"
composeBom = "2024.09.03"
activityCompose = "1.9.3"
lifecycleViewmodelCompose = "2.8.7"
navigationCompose = "2.8.4"
room = "2.6.1"
coil = "2.7.0"
coroutinesTest = "1.9.0"

[libraries]
androidx-core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "coreKtx" }
# Compose
androidx-compose-bom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
androidx-ui = { group = "androidx.compose.ui", name = "ui" }
androidx-ui-graphics = { group = "androidx.compose.ui", name = "ui-graphics" }
androidx-ui-tooling = { group = "androidx.compose.ui", name = "ui-tooling" }
androidx-ui-tooling-preview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
androidx-material3 = { group = "androidx.compose.material3", name = "material3" }
androidx-material-icons-extended = { group = "androidx.compose.material", name = "material-icons-extended" }
androidx-activity-compose = { group = "androidx.activity", name = "activity-compose", version.ref = "activityCompose" }
androidx-lifecycle-viewmodel-compose = { group = "androidx.lifecycle", name = "lifecycle-viewmodel-compose", version.ref = "lifecycleViewmodelCompose" }
androidx-navigation-compose = { group = "androidx.navigation", name = "navigation-compose", version.ref = "navigationCompose" }
# Room
androidx-room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
androidx-room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
androidx-room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
# Image loading
coil-compose = { group = "io.coil-kt", name = "coil-compose", version.ref = "coil" }
# Test
junit = { group = "junit", name = "junit", version.ref = "junit" }
kotlinx-coroutines-test = { group = "org.jetbrains.kotlinx", name = "kotlinx-coroutines-test", version.ref = "coroutinesTest" }
androidx-junit = { group = "androidx.test.ext", name = "junit", version.ref = "junitVersion" }
androidx-espresso-core = { group = "androidx.test.espresso", name = "espresso-core", version.ref = "espressoCore" }
androidx-compose-ui-test-junit4 = { group = "androidx.compose.ui", name = "ui-test-junit4" }
androidx-compose-ui-test-manifest = { group = "androidx.compose.ui", name = "ui-test-manifest" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
```

(Removed the now-unused `appcompat` and Views `material` entries — the app uses Compose Material 3 only.)

- [ ] **Step 2: Register the new plugins at the root**

Replace `build.gradle.kts` (root) with:

```kotlin
// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
}
```

- [ ] **Step 3: Configure the app module**

Replace `app/build.gradle.kts` with:

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.nwe.recipely"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.nwe.recipely"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.coil.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
}
```

- [ ] **Step 4: Sync/verify the build resolves dependencies**

Run: `.\gradlew.bat help`
Expected: `BUILD SUCCESSFUL` (this forces dependency resolution of the new catalog without compiling sources). If a Compose BOM / library version is rejected, bump it to the newest stable Android Studio suggests and re-run.

- [ ] **Step 5: Commit**

```bash
git add gradle/libs.versions.toml build.gradle.kts app/build.gradle.kts
git commit -m "build: add Compose, Room (KSP), Coil and test dependencies"
```

---

## Task 3: Compose theme, Application/AppContainer skeleton, MainActivity, manifest

This task gets a blank Compose app running with the green Material 3 theme. The `AppContainer` is created but its members are added in later tasks — to keep this task compiling, `AppContainer` starts empty.

**Files:**
- Create: `app/src/main/java/com/nwe/recipely/ui/theme/Color.kt`
- Create: `app/src/main/java/com/nwe/recipely/ui/theme/Type.kt`
- Create: `app/src/main/java/com/nwe/recipely/ui/theme/Theme.kt`
- Create: `app/src/main/java/com/nwe/recipely/di/AppContainer.kt`
- Create: `app/src/main/java/com/nwe/recipely/RecipelyApp.kt`
- Create: `app/src/main/java/com/nwe/recipely/MainActivity.kt`
- Modify: `app/src/main/res/values/themes.xml`
- Modify: `app/src/main/res/values-night/themes.xml`
- Modify: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Colors**

Create `app/src/main/java/com/nwe/recipely/ui/theme/Color.kt`:

```kotlin
package com.nwe.recipely.ui.theme

import androidx.compose.ui.graphics.Color

// Light
val GreenPrimary = Color(0xFF2E7D32)
val GreenOnPrimary = Color(0xFFFFFFFF)
val GreenPrimaryContainer = Color(0xFFB7F0AE)
val GreenOnPrimaryContainer = Color(0xFF002106)
val GreenSecondary = Color(0xFF52634F)
val GreenOnSecondary = Color(0xFFFFFFFF)
val LightBackground = Color(0xFFFCFDF6)
val LightSurface = Color(0xFFFCFDF6)
val LightOnSurface = Color(0xFF1A1C18)

// Dark
val GreenPrimaryDark = Color(0xFF9CD67D)
val GreenOnPrimaryDark = Color(0xFF003910)
val GreenPrimaryContainerDark = Color(0xFF15611C)
val GreenOnPrimaryContainerDark = Color(0xFFB7F0AE)
val GreenSecondaryDark = Color(0xFFB9CCB4)
val GreenOnSecondaryDark = Color(0xFF243424)
val DarkBackground = Color(0xFF1A1C18)
val DarkSurface = Color(0xFF1A1C18)
val DarkOnSurface = Color(0xFFE2E3DC)
```

- [ ] **Step 2: Typography**

Create `app/src/main/java/com/nwe/recipely/ui/theme/Type.kt`:

```kotlin
package com.nwe.recipely.ui.theme

import androidx.compose.material3.Typography

// Material 3 default typography is fine for a simple app.
val AppTypography = Typography()
```

- [ ] **Step 3: Theme**

Create `app/src/main/java/com/nwe/recipely/ui/theme/Theme.kt`:

```kotlin
package com.nwe.recipely.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = GreenPrimary,
    onPrimary = GreenOnPrimary,
    primaryContainer = GreenPrimaryContainer,
    onPrimaryContainer = GreenOnPrimaryContainer,
    secondary = GreenSecondary,
    onSecondary = GreenOnSecondary,
    background = LightBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
)

private val DarkColors = darkColorScheme(
    primary = GreenPrimaryDark,
    onPrimary = GreenOnPrimaryDark,
    primaryContainer = GreenPrimaryContainerDark,
    onPrimaryContainer = GreenOnPrimaryContainerDark,
    secondary = GreenSecondaryDark,
    onSecondary = GreenOnSecondaryDark,
    background = DarkBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
)

@Composable
fun RecipelyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content,
    )
}
```

- [ ] **Step 4: Empty AppContainer**

Create `app/src/main/java/com/nwe/recipely/di/AppContainer.kt`:

```kotlin
package com.nwe.recipely.di

import android.content.Context

/**
 * Manual dependency container. Members (database, dao, imageStore, repository)
 * are added in later tasks. Held by [com.nwe.recipely.RecipelyApp].
 */
class AppContainer(private val context: Context)
```

- [ ] **Step 5: Application class**

Create `app/src/main/java/com/nwe/recipely/RecipelyApp.kt`:

```kotlin
package com.nwe.recipely

import android.app.Application
import com.nwe.recipely.di.AppContainer

class RecipelyApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
```

- [ ] **Step 6: MainActivity**

Create `app/src/main/java/com/nwe/recipely/MainActivity.kt`:

```kotlin
package com.nwe.recipely

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import com.nwe.recipely.ui.theme.RecipelyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RecipelyTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    // Replaced by RecipelyNavHost() in Task 13.
                    Text("Recipely")
                }
            }
        }
    }
}
```

- [ ] **Step 7: Replace the XML themes with platform parents (no AppCompat needed)**

Replace `app/src/main/res/values/themes.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.Recipely" parent="android:Theme.Material.Light.NoActionBar" />
</resources>
```

Replace `app/src/main/res/values-night/themes.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.Recipely" parent="android:Theme.Material.NoActionBar" />
</resources>
```

- [ ] **Step 8: Wire Application + MainActivity in the manifest**

Replace `app/src/main/AndroidManifest.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <application
        android:name=".RecipelyApp"
        android:allowBackup="true"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="@xml/backup_rules"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.Recipely"
        tools:targetApi="31">

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@style/Theme.Recipely">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <!-- FileProvider authority is used by ImageStore (Task 7) for camera capture. -->
        <provider
            android:name="androidx.core.content.FileProvider"
            android:authorities="${applicationId}.fileprovider"
            android:exported="false"
            android:grantUriPermissions="true">
            <meta-data
                android:name="android.support.FILE_PROVIDER_PATHS"
                android:resource="@xml/file_paths" />
        </provider>

    </application>

</manifest>
```

- [ ] **Step 9: Add the FileProvider paths file**

Create `app/src/main/res/xml/file_paths.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<paths>
    <files-path name="images" path="images/" />
</paths>
```

- [ ] **Step 10: Build and run the app**

Run: `.\gradlew.bat assembleDebug`
Expected: `BUILD SUCCESSFUL`.
Then install/run on an emulator or device (`.\gradlew.bat installDebug`, or Android Studio Run). Expected: a blank screen showing "Recipely" with the green theme applied to system surfaces. (`file_paths.xml` referenced by the manifest must exist or the build fails — it was created in Step 9.)

- [ ] **Step 11: Commit**

```bash
git add app/src/main/java/com/nwe/recipely app/src/main/res/values/themes.xml app/src/main/res/values-night/themes.xml app/src/main/AndroidManifest.xml app/src/main/res/xml/file_paths.xml
git commit -m "feat: Compose theme, Application/AppContainer, MainActivity shell"
```

---

## Task 4: Room entities + relation POJO

**Files:**
- Create: `app/src/main/java/com/nwe/recipely/data/RecipeEntities.kt`

- [ ] **Step 1: Create the entities**

Create `app/src/main/java/com/nwe/recipely/data/RecipeEntities.kt`:

```kotlin
package com.nwe.recipely.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "recipes")
data class Recipe(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val imageUri: String? = null,
    val prepTimeMinutes: Int? = null,
    val servings: Int? = null,
)

@Entity(
    tableName = "ingredients",
    foreignKeys = [
        ForeignKey(
            entity = Recipe::class,
            parentColumns = ["id"],
            childColumns = ["recipeId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("recipeId")],
)
data class Ingredient(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recipeId: Long,
    val text: String,
    val position: Int,
)

@Entity(
    tableName = "steps",
    foreignKeys = [
        ForeignKey(
            entity = Recipe::class,
            parentColumns = ["id"],
            childColumns = ["recipeId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index("recipeId")],
)
data class Step(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recipeId: Long,
    val text: String,
    val imageUri: String? = null,
    val position: Int,
)

data class RecipeWithDetails(
    @Embedded val recipe: Recipe,
    @Relation(parentColumn = "id", entityColumn = "recipeId")
    val ingredients: List<Ingredient>,
    @Relation(parentColumn = "id", entityColumn = "recipeId")
    val steps: List<Step>,
)
```

- [ ] **Step 2: Verify it compiles**

Run: `.\gradlew.bat compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`. (Room annotations are processed when the DAO/Database exist in Task 5; entities alone compile fine.)

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/nwe/recipely/data/RecipeEntities.kt
git commit -m "feat: Room entities (Recipe, Ingredient, Step) + RecipeWithDetails"
```

---

## Task 5: RecipeDao + RecipeDatabase

**Files:**
- Create: `app/src/main/java/com/nwe/recipely/data/RecipeDao.kt`
- Create: `app/src/main/java/com/nwe/recipely/data/RecipeDatabase.kt`

- [ ] **Step 1: Create the DAO**

Create `app/src/main/java/com/nwe/recipely/data/RecipeDao.kt`:

```kotlin
package com.nwe.recipely.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RecipeDao {

    @Query("SELECT * FROM recipes ORDER BY name COLLATE NOCASE ASC")
    fun observeRecipes(): Flow<List<Recipe>>

    @Transaction
    @Query("SELECT * FROM recipes WHERE id = :id")
    fun observeRecipe(id: Long): Flow<RecipeWithDetails?>

    @Insert
    suspend fun insertRecipe(recipe: Recipe): Long

    @Update
    suspend fun updateRecipe(recipe: Recipe)

    @Insert
    suspend fun insertIngredients(items: List<Ingredient>)

    @Insert
    suspend fun insertSteps(items: List<Step>)

    @Query("DELETE FROM ingredients WHERE recipeId = :recipeId")
    suspend fun deleteIngredientsFor(recipeId: Long)

    @Query("DELETE FROM steps WHERE recipeId = :recipeId")
    suspend fun deleteStepsFor(recipeId: Long)

    @Query("DELETE FROM recipes WHERE id = :id")
    suspend fun deleteRecipe(id: Long)

    /**
     * Inserts a new recipe (id == 0) or updates an existing one, then replaces all of its
     * ingredients and steps. Child rows get their [recipeId] reassigned and ids reset so they
     * are inserted fresh. Returns the recipe id.
     */
    @Transaction
    suspend fun upsertRecipeWithChildren(
        recipe: Recipe,
        ingredients: List<Ingredient>,
        steps: List<Step>,
    ): Long {
        val recipeId = if (recipe.id == 0L) {
            insertRecipe(recipe)
        } else {
            updateRecipe(recipe)
            recipe.id
        }
        deleteIngredientsFor(recipeId)
        deleteStepsFor(recipeId)
        insertIngredients(ingredients.map { it.copy(id = 0, recipeId = recipeId) })
        insertSteps(steps.map { it.copy(id = 0, recipeId = recipeId) })
        return recipeId
    }
}
```

- [ ] **Step 2: Create the database**

Create `app/src/main/java/com/nwe/recipely/data/RecipeDatabase.kt`:

```kotlin
package com.nwe.recipely.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [Recipe::class, Ingredient::class, Step::class],
    version = 1,
    exportSchema = false,
)
abstract class RecipeDatabase : RoomDatabase() {
    abstract fun recipeDao(): RecipeDao
}
```

- [ ] **Step 3: Verify Room code generation compiles**

Run: `.\gradlew.bat compileDebugKotlin`
Expected: `BUILD SUCCESSFUL` (KSP generates `RecipeDao_Impl` / `RecipeDatabase_Impl`). If KSP errors, read the message — it usually points to a query/entity mismatch.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/nwe/recipely/data/RecipeDao.kt app/src/main/java/com/nwe/recipely/data/RecipeDatabase.kt
git commit -m "feat: RecipeDao with transactional upsert + RecipeDatabase"
```

---

## Task 6: Instrumented DAO test (requires emulator/device)

Verifies insert, the `RecipeWithDetails` relation, the transactional upsert (replace children), and `CASCADE` delete.

**Files:**
- Create: `app/src/androidTest/java/com/nwe/recipely/RecipeDaoTest.kt`
- Delete: `app/src/androidTest/java/com/nwe/recipely/ExampleInstrumentedTest.kt`

- [ ] **Step 1: Write the test**

Create `app/src/androidTest/java/com/nwe/recipely/RecipeDaoTest.kt`:

```kotlin
package com.nwe.recipely

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.nwe.recipely.data.Ingredient
import com.nwe.recipely.data.Recipe
import com.nwe.recipely.data.RecipeDao
import com.nwe.recipely.data.RecipeDatabase
import com.nwe.recipely.data.Step
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RecipeDaoTest {

    private lateinit var db: RecipeDatabase
    private lateinit var dao: RecipeDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        db = Room.inMemoryDatabaseBuilder(context, RecipeDatabase::class.java)
            .build()
        dao = db.recipeDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun upsert_thenObserve_returnsRecipeWithSortedChildren() = runTest {
        val id = dao.upsertRecipeWithChildren(
            recipe = Recipe(name = "Carbonara", prepTimeMinutes = 30, servings = 4),
            ingredients = listOf(
                Ingredient(recipeId = 0, text = "Spaghetti", position = 0),
                Ingredient(recipeId = 0, text = "Pancetta", position = 1),
            ),
            steps = listOf(
                Step(recipeId = 0, text = "Boil pasta", position = 0),
                Step(recipeId = 0, text = "Fry pancetta", imageUri = "/x.jpg", position = 1),
            ),
        )

        val details = dao.observeRecipe(id).first()!!
        assertEquals("Carbonara", details.recipe.name)
        assertEquals(2, details.ingredients.size)
        assertEquals(2, details.steps.size)
        assertEquals("Spaghetti", details.ingredients.sortedBy { it.position }[0].text)
        assertEquals("/x.jpg", details.steps.sortedBy { it.position }[1].imageUri)
    }

    @Test
    fun upsert_existingRecipe_replacesChildren() = runTest {
        val id = dao.upsertRecipeWithChildren(
            recipe = Recipe(name = "A"),
            ingredients = listOf(Ingredient(recipeId = 0, text = "old", position = 0)),
            steps = emptyList(),
        )
        dao.upsertRecipeWithChildren(
            recipe = Recipe(id = id, name = "A renamed"),
            ingredients = listOf(Ingredient(recipeId = 0, text = "new", position = 0)),
            steps = emptyList(),
        )

        val details = dao.observeRecipe(id).first()!!
        assertEquals("A renamed", details.recipe.name)
        assertEquals(1, details.ingredients.size)
        assertEquals("new", details.ingredients[0].text)
    }

    @Test
    fun deleteRecipe_cascadesToChildren() = runTest {
        val id = dao.upsertRecipeWithChildren(
            recipe = Recipe(name = "ToDelete"),
            ingredients = listOf(Ingredient(recipeId = 0, text = "x", position = 0)),
            steps = listOf(Step(recipeId = 0, text = "y", position = 0)),
        )

        dao.deleteRecipe(id)

        assertNull(dao.observeRecipe(id).first())
        assertEquals(0, dao.observeRecipes().first().size)
    }
}
```

- [ ] **Step 2: Delete the example instrumented test**

Delete `app/src/androidTest/java/com/nwe/recipely/ExampleInstrumentedTest.kt`.

- [ ] **Step 3: Run the test on an emulator/device**

Start an emulator (or connect a device), then run:
`.\gradlew.bat connectedDebugAndroidTest --tests "com.nwe.recipely.RecipeDaoTest"`
Expected: all 3 tests PASS. (Room enables `PRAGMA foreign_keys=ON` automatically, so CASCADE works.)

- [ ] **Step 4: Commit**

```bash
git add app/src/androidTest/java/com/nwe/recipely/RecipeDaoTest.kt
git rm app/src/androidTest/java/com/nwe/recipely/ExampleInstrumentedTest.kt
git commit -m "test: instrumented RecipeDao test (upsert, relation, cascade)"
```

---

## Task 7: ImageStore

Copies picked images into internal storage, provides a camera capture target via FileProvider, and deletes files. Verified by usage in the app (no automated test — it depends on Android `Context`/`Uri`).

**Files:**
- Create: `app/src/main/java/com/nwe/recipely/data/ImageStore.kt`

- [ ] **Step 1: Create ImageStore**

Create `app/src/main/java/com/nwe/recipely/data/ImageStore.kt`:

```kotlin
package com.nwe.recipely.data

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/**
 * Manages recipe images inside app-internal storage (filesDir/images).
 * The DB stores only absolute file paths returned by this class.
 */
class ImageStore(private val context: Context) {

    private val imagesDir: File
        get() = File(context.filesDir, "images").apply { mkdirs() }

    private fun newImageFile(): File = File(imagesDir, "img_${System.nanoTime()}.jpg")

    /** Copies the content at [uri] into internal storage. Returns the new file path, or null on failure. */
    fun importFromUri(uri: Uri): String? = try {
        val target = newImageFile()
        context.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input)
            target.outputStream().use { output -> input.copyTo(output) }
        }
        target.absolutePath
    } catch (e: Exception) {
        null
    }

    /**
     * Creates an empty target file for camera capture and returns the content [Uri] (for the
     * TakePicture contract) together with the absolute path to persist once capture succeeds.
     */
    fun createCameraTarget(): Pair<Uri, String> {
        val file = newImageFile()
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return uri to file.absolutePath
    }

    /** Deletes the file at [path] if it exists. No-op for null. */
    fun delete(path: String?) {
        if (path.isNullOrEmpty()) return
        runCatching { File(path).takeIf { it.exists() }?.delete() }
    }
}
```

- [ ] **Step 2: Verify it compiles**

Run: `.\gradlew.bat compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/nwe/recipely/data/ImageStore.kt
git commit -m "feat: ImageStore for internal image copy/capture/delete"
```

---

## Task 8: RecipeRepository (interface + Room implementation) and AppContainer wiring

**Files:**
- Create: `app/src/main/java/com/nwe/recipely/data/RecipeRepository.kt`
- Modify: `app/src/main/java/com/nwe/recipely/di/AppContainer.kt`

- [ ] **Step 1: Create the repository**

Create `app/src/main/java/com/nwe/recipely/data/RecipeRepository.kt`:

```kotlin
package com.nwe.recipely.data

import kotlinx.coroutines.flow.Flow

interface RecipeRepository {
    fun observeRecipes(): Flow<List<Recipe>>
    fun observeRecipe(id: Long): Flow<RecipeWithDetails?>

    /** Saves the recipe and its children, then deletes any [removedImagePaths] (orphaned files). */
    suspend fun save(
        recipe: Recipe,
        ingredients: List<Ingredient>,
        steps: List<Step>,
        removedImagePaths: List<String>,
    ): Long

    /** Deletes the recipe (cascades children) and removes all of its image files. */
    suspend fun delete(details: RecipeWithDetails)

    /** Deletes freshly-imported images that were never persisted (used on edit cancel). */
    suspend fun discardImages(paths: List<String>)
}

class RoomRecipeRepository(
    private val dao: RecipeDao,
    private val imageStore: ImageStore,
) : RecipeRepository {

    override fun observeRecipes(): Flow<List<Recipe>> = dao.observeRecipes()

    override fun observeRecipe(id: Long): Flow<RecipeWithDetails?> = dao.observeRecipe(id)

    override suspend fun save(
        recipe: Recipe,
        ingredients: List<Ingredient>,
        steps: List<Step>,
        removedImagePaths: List<String>,
    ): Long {
        val id = dao.upsertRecipeWithChildren(recipe, ingredients, steps)
        removedImagePaths.forEach(imageStore::delete)
        return id
    }

    override suspend fun delete(details: RecipeWithDetails) {
        dao.deleteRecipe(details.recipe.id)
        imageStore.delete(details.recipe.imageUri)
        details.steps.forEach { imageStore.delete(it.imageUri) }
    }

    override suspend fun discardImages(paths: List<String>) {
        paths.forEach(imageStore::delete)
    }
}
```

- [ ] **Step 2: Wire the AppContainer**

Replace `app/src/main/java/com/nwe/recipely/di/AppContainer.kt`:

```kotlin
package com.nwe.recipely.di

import android.content.Context
import androidx.room.Room
import com.nwe.recipely.data.ImageStore
import com.nwe.recipely.data.RecipeDatabase
import com.nwe.recipely.data.RecipeRepository
import com.nwe.recipely.data.RoomRecipeRepository

class AppContainer(context: Context) {

    private val database: RecipeDatabase = Room.databaseBuilder(
        context,
        RecipeDatabase::class.java,
        "recipely.db",
    ).build()

    val imageStore: ImageStore = ImageStore(context)

    val repository: RecipeRepository = RoomRecipeRepository(database.recipeDao(), imageStore)
}
```

- [ ] **Step 3: Verify it compiles**

Run: `.\gradlew.bat compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/nwe/recipely/data/RecipeRepository.kt app/src/main/java/com/nwe/recipely/di/AppContainer.kt
git commit -m "feat: RecipeRepository + AppContainer wiring"
```

---

## Task 9: Edit form state + entity mapping (TDD)

Pure Kotlin form model and the mapping form ↔ entities. This is unit-tested first.

**Files:**
- Create: `app/src/main/java/com/nwe/recipely/ui/edit/EditUiState.kt`
- Create: `app/src/test/java/com/nwe/recipely/RecipeMappingTest.kt`
- Delete: `app/src/test/java/com/nwe/recipely/ExampleUnitTest.kt`

- [ ] **Step 1: Write the failing mapping test**

Create `app/src/test/java/com/nwe/recipely/RecipeMappingTest.kt`:

```kotlin
package com.nwe.recipely

import com.nwe.recipely.data.Ingredient
import com.nwe.recipely.data.Recipe
import com.nwe.recipely.data.RecipeWithDetails
import com.nwe.recipely.data.Step
import com.nwe.recipely.ui.edit.EditUiState
import com.nwe.recipely.ui.edit.IngredientRow
import com.nwe.recipely.ui.edit.StepRow
import com.nwe.recipely.ui.edit.referencedPaths
import com.nwe.recipely.ui.edit.toEntities
import com.nwe.recipely.ui.edit.toUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipeMappingTest {

    @Test
    fun canSave_isFalse_whenNameBlank() {
        assertFalse(EditUiState(name = "   ").canSave)
        assertTrue(EditUiState(name = "Soup").canSave)
    }

    @Test
    fun toEntities_trimsAndDropsBlankRows_andAssignsPositions() {
        val state = EditUiState(
            name = "  Carbonara  ",
            prepTime = "30",
            servings = "4",
            imagePath = "/title.jpg",
            ingredients = listOf(
                IngredientRow("  Spaghetti "),
                IngredientRow("   "),
                IngredientRow("Pancetta"),
            ),
            steps = listOf(
                StepRow(text = "Boil pasta"),
                StepRow(text = "   ", imagePath = null),
                StepRow(text = "Fry", imagePath = "/s.jpg"),
            ),
        )

        val (recipe, ingredients, steps) = state.toEntities()

        assertEquals("Carbonara", recipe.name)
        assertEquals(30, recipe.prepTimeMinutes)
        assertEquals(4, recipe.servings)
        assertEquals("/title.jpg", recipe.imageUri)

        assertEquals(listOf("Spaghetti", "Pancetta"), ingredients.map { it.text })
        assertEquals(listOf(0, 1), ingredients.map { it.position })

        // Blank-text step with no image is dropped; the image-only/text steps stay.
        assertEquals(listOf("Boil pasta", "Fry"), steps.map { it.text })
        assertEquals(listOf(0, 1), steps.map { it.position })
        assertEquals("/s.jpg", steps[1].imageUri)
    }

    @Test
    fun toEntities_parsesEmptyNumbersAsNull() {
        val (recipe, _, _) = EditUiState(name = "X", prepTime = "", servings = "abc").toEntities()
        assertEquals(null, recipe.prepTimeMinutes)
        assertEquals(null, recipe.servings)
    }

    @Test
    fun toUiState_mapsDetailsAndFallsBackToOneEmptyRow() {
        val details = RecipeWithDetails(
            recipe = Recipe(id = 7, name = "X", prepTimeMinutes = 10, servings = null, imageUri = null),
            ingredients = emptyList(),
            steps = emptyList(),
        )
        val state = details.toUiState()
        assertEquals(7L, state.id)
        assertEquals("10", state.prepTime)
        assertEquals("", state.servings)
        assertEquals(1, state.ingredients.size)
        assertEquals(1, state.steps.size)
    }

    @Test
    fun referencedPaths_collectsTitleAndStepImages() {
        val state = EditUiState(
            name = "X",
            imagePath = "/t.jpg",
            steps = listOf(StepRow(imagePath = "/a.jpg"), StepRow(imagePath = null), StepRow(imagePath = "/b.jpg")),
        )
        assertEquals(listOf("/t.jpg", "/a.jpg", "/b.jpg"), state.referencedPaths())
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.nwe.recipely.RecipeMappingTest"`
Expected: FAIL — `EditUiState`, `toEntities`, etc. are unresolved (compilation error).

- [ ] **Step 3: Implement the form state + mapping**

Create `app/src/main/java/com/nwe/recipely/ui/edit/EditUiState.kt`:

```kotlin
package com.nwe.recipely.ui.edit

import com.nwe.recipely.data.Ingredient
import com.nwe.recipely.data.Recipe
import com.nwe.recipely.data.RecipeWithDetails
import com.nwe.recipely.data.Step

data class IngredientRow(val text: String = "")

data class StepRow(val text: String = "", val imagePath: String? = null)

data class EditUiState(
    val id: Long = 0,
    val name: String = "",
    val prepTime: String = "",
    val servings: String = "",
    val imagePath: String? = null,
    val ingredients: List<IngredientRow> = listOf(IngredientRow()),
    val steps: List<StepRow> = listOf(StepRow()),
) {
    val canSave: Boolean get() = name.isNotBlank()
}

/** Title image plus every step image, in order. */
fun EditUiState.referencedPaths(): List<String> =
    listOfNotNull(imagePath) + steps.mapNotNull { it.imagePath }

/** Maps the form to entities, trimming text and dropping empty rows. recipeId on children is
 * a placeholder — the DAO reassigns it during upsert. */
fun EditUiState.toEntities(): Triple<Recipe, List<Ingredient>, List<Step>> {
    val recipe = Recipe(
        id = id,
        name = name.trim(),
        imageUri = imagePath,
        prepTimeMinutes = prepTime.trim().toIntOrNull(),
        servings = servings.trim().toIntOrNull(),
    )
    val ingredients = ingredients
        .map { it.text.trim() }
        .filter { it.isNotEmpty() }
        .mapIndexed { index, text -> Ingredient(recipeId = id, text = text, position = index) }
    val steps = steps
        .filter { it.text.isNotBlank() || it.imagePath != null }
        .mapIndexed { index, row ->
            Step(recipeId = id, text = row.text.trim(), imageUri = row.imagePath, position = index)
        }
    return Triple(recipe, ingredients, steps)
}

fun RecipeWithDetails.toUiState(): EditUiState = EditUiState(
    id = recipe.id,
    name = recipe.name,
    prepTime = recipe.prepTimeMinutes?.toString() ?: "",
    servings = recipe.servings?.toString() ?: "",
    imagePath = recipe.imageUri,
    ingredients = ingredients.sortedBy { it.position }
        .map { IngredientRow(it.text) }
        .ifEmpty { listOf(IngredientRow()) },
    steps = steps.sortedBy { it.position }
        .map { StepRow(it.text, it.imageUri) }
        .ifEmpty { listOf(StepRow()) },
)
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.nwe.recipely.RecipeMappingTest"`
Expected: all 5 tests PASS.

- [ ] **Step 5: Delete the example unit test**

Delete `app/src/test/java/com/nwe/recipely/ExampleUnitTest.kt`.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/nwe/recipely/ui/edit/EditUiState.kt app/src/test/java/com/nwe/recipely/RecipeMappingTest.kt
git rm app/src/test/java/com/nwe/recipely/ExampleUnitTest.kt
git commit -m "feat: edit form state + entity mapping (TDD)"
```

---

## Task 10: RecipeEditViewModel (TDD)

**Files:**
- Create: `app/src/test/java/com/nwe/recipely/MainDispatcherRule.kt`
- Create: `app/src/test/java/com/nwe/recipely/FakeRecipeRepository.kt`
- Create: `app/src/main/java/com/nwe/recipely/ui/edit/RecipeEditViewModel.kt`
- Create: `app/src/test/java/com/nwe/recipely/RecipeEditViewModelTest.kt`

- [ ] **Step 1: Add the Main-dispatcher test rule**

Create `app/src/test/java/com/nwe/recipely/MainDispatcherRule.kt`:

```kotlin
package com.nwe.recipely

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val dispatcher: StandardTestDispatcher = StandardTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) = Dispatchers.setMain(dispatcher)
    override fun finished(description: Description) = Dispatchers.resetMain()
}
```

- [ ] **Step 2: Add the fake repository**

Create `app/src/test/java/com/nwe/recipely/FakeRecipeRepository.kt`:

```kotlin
package com.nwe.recipely

import com.nwe.recipely.data.Ingredient
import com.nwe.recipely.data.Recipe
import com.nwe.recipely.data.RecipeRepository
import com.nwe.recipely.data.RecipeWithDetails
import com.nwe.recipely.data.Step
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeRecipeRepository : RecipeRepository {

    val recipes = MutableStateFlow<List<Recipe>>(emptyList())
    var detail = MutableStateFlow<RecipeWithDetails?>(null)

    // Recorded calls for assertions
    var lastSavedRecipe: Recipe? = null
    var lastSavedIngredients: List<Ingredient> = emptyList()
    var lastSavedSteps: List<Step> = emptyList()
    var lastRemovedImagePaths: List<String> = emptyList()
    var discardedImages: List<String> = emptyList()
    var deletedDetails: RecipeWithDetails? = null

    override fun observeRecipes(): Flow<List<Recipe>> = recipes
    override fun observeRecipe(id: Long): Flow<RecipeWithDetails?> = detail

    override suspend fun save(
        recipe: Recipe,
        ingredients: List<Ingredient>,
        steps: List<Step>,
        removedImagePaths: List<String>,
    ): Long {
        lastSavedRecipe = recipe
        lastSavedIngredients = ingredients
        lastSavedSteps = steps
        lastRemovedImagePaths = removedImagePaths
        return if (recipe.id == 0L) 1L else recipe.id
    }

    override suspend fun delete(details: RecipeWithDetails) {
        deletedDetails = details
    }

    override suspend fun discardImages(paths: List<String>) {
        discardedImages = paths
    }
}
```

- [ ] **Step 3: Write the failing ViewModel test**

Create `app/src/test/java/com/nwe/recipely/RecipeEditViewModelTest.kt`:

```kotlin
package com.nwe.recipely

import com.nwe.recipely.data.Recipe
import com.nwe.recipely.data.RecipeWithDetails
import com.nwe.recipely.data.Step
import com.nwe.recipely.ui.edit.RecipeEditViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RecipeEditViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun newViewModel(repo: FakeRecipeRepository, id: Long = 0L) =
        RecipeEditViewModel(repo, id)

    @Test
    fun newRecipe_cannotSaveUntilNameEntered() {
        val vm = newViewModel(FakeRecipeRepository())
        assertFalse(vm.state.value.canSave)
        vm.setName("Soup")
        assertTrue(vm.state.value.canSave)
    }

    @Test
    fun addAndRemoveIngredient() {
        val vm = newViewModel(FakeRecipeRepository())
        assertEquals(1, vm.state.value.ingredients.size)
        vm.addIngredient()
        assertEquals(2, vm.state.value.ingredients.size)
        vm.setIngredient(0, "Salt")
        assertEquals("Salt", vm.state.value.ingredients[0].text)
        vm.removeIngredient(0)
        assertEquals(1, vm.state.value.ingredients.size)
    }

    @Test
    fun addAndRemoveStep() {
        val vm = newViewModel(FakeRecipeRepository())
        vm.addStep()
        assertEquals(2, vm.state.value.steps.size)
        vm.setStepText(1, "Stir")
        assertEquals("Stir", vm.state.value.steps[1].text)
        vm.removeStep(1)
        assertEquals(1, vm.state.value.steps.size)
    }

    @Test
    fun save_passesMappedEntitiesToRepository() = runTest {
        val repo = FakeRecipeRepository()
        val vm = newViewModel(repo)
        vm.setName("Carbonara")
        vm.setPrepTime("30")
        vm.setIngredient(0, "Spaghetti")
        vm.setStepText(0, "Boil")

        var saved = false
        vm.save { saved = true }
        advanceUntilIdle()

        assertTrue(saved)
        assertEquals("Carbonara", repo.lastSavedRecipe?.name)
        assertEquals(30, repo.lastSavedRecipe?.prepTimeMinutes)
        assertEquals(listOf("Spaghetti"), repo.lastSavedIngredients.map { it.text })
        assertEquals(listOf("Boil"), repo.lastSavedSteps.map { it.text })
    }

    @Test
    fun save_doesNothing_whenNameBlank() = runTest {
        val repo = FakeRecipeRepository()
        val vm = newViewModel(repo)
        var saved = false
        vm.save { saved = true }
        advanceUntilIdle()
        assertFalse(saved)
        assertEquals(null, repo.lastSavedRecipe)
    }

    @Test
    fun replacingTitleImage_marksOldImageAsOrphanOnSave() = runTest {
        val repo = FakeRecipeRepository()
        val vm = newViewModel(repo)
        vm.setName("X")
        vm.setTitleImage("/a.jpg")   // newly imported
        vm.setTitleImage("/b.jpg")   // replaces it; /a.jpg now orphaned
        vm.save {}
        advanceUntilIdle()
        assertTrue(repo.lastRemovedImagePaths.contains("/a.jpg"))
        assertFalse(repo.lastRemovedImagePaths.contains("/b.jpg"))
    }

    @Test
    fun discardChanges_deletesNewlyImportedImages() = runTest {
        val repo = FakeRecipeRepository()
        val vm = newViewModel(repo)
        vm.setName("X")
        vm.setTitleImage("/a.jpg")
        vm.discardChanges()
        advanceUntilIdle()
        assertEquals(listOf("/a.jpg"), repo.discardedImages)
    }

    @Test
    fun loadingExistingRecipe_populatesState() = runTest {
        val repo = FakeRecipeRepository()
        repo.detail.value = RecipeWithDetails(
            recipe = Recipe(id = 5, name = "Loaded", prepTimeMinutes = 12),
            ingredients = emptyList(),
            steps = listOf(Step(recipeId = 5, text = "step", imageUri = "/s.jpg", position = 0)),
        )
        val vm = newViewModel(repo, id = 5L)
        advanceUntilIdle()
        assertEquals("Loaded", vm.state.value.name)
        assertEquals("12", vm.state.value.prepTime)
        assertEquals("/s.jpg", vm.state.value.steps[0].imagePath)
    }
}
```

- [ ] **Step 4: Run the test to verify it fails**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.nwe.recipely.RecipeEditViewModelTest"`
Expected: FAIL — `RecipeEditViewModel` is unresolved.

- [ ] **Step 5: Implement the ViewModel**

Create `app/src/main/java/com/nwe/recipely/ui/edit/RecipeEditViewModel.kt`:

```kotlin
package com.nwe.recipely.ui.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nwe.recipely.data.RecipeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class RecipeEditViewModel(
    private val repository: RecipeRepository,
    private val recipeId: Long,
) : ViewModel() {

    private val _state = MutableStateFlow(EditUiState())
    val state: StateFlow<EditUiState> = _state.asStateFlow()

    /** Image paths that already existed when the recipe was loaded. */
    private var originalImagePaths: Set<String> = emptySet()

    /** Image paths imported during this editing session (may or may not survive). */
    private val pendingNewImages = mutableSetOf<String>()

    init {
        if (recipeId != 0L) {
            viewModelScope.launch {
                val details = repository.observeRecipe(recipeId).first() ?: return@launch
                originalImagePaths = (listOfNotNull(details.recipe.imageUri) +
                    details.steps.mapNotNull { it.imageUri }).toSet()
                _state.value = details.toUiState()
            }
        }
    }

    fun setName(value: String) = update { it.copy(name = value) }
    fun setPrepTime(value: String) = update { it.copy(prepTime = value) }
    fun setServings(value: String) = update { it.copy(servings = value) }

    fun setTitleImage(path: String?) {
        if (path != null) pendingNewImages += path
        update { it.copy(imagePath = path) }
    }

    fun addIngredient() = update { it.copy(ingredients = it.ingredients + IngredientRow()) }

    fun setIngredient(index: Int, text: String) = update { s ->
        s.copy(ingredients = s.ingredients.mapIndexed { i, row ->
            if (i == index) row.copy(text = text) else row
        })
    }

    fun removeIngredient(index: Int) = update { s ->
        s.copy(ingredients = s.ingredients.filterIndexed { i, _ -> i != index })
    }

    fun addStep() = update { it.copy(steps = it.steps + StepRow()) }

    fun setStepText(index: Int, text: String) = update { s ->
        s.copy(steps = s.steps.mapIndexed { i, row -> if (i == index) row.copy(text = text) else row })
    }

    fun setStepImage(index: Int, path: String?) {
        if (path != null) pendingNewImages += path
        update { s ->
            s.copy(steps = s.steps.mapIndexed { i, row ->
                if (i == index) row.copy(imagePath = path) else row
            })
        }
    }

    fun removeStep(index: Int) = update { s ->
        s.copy(steps = s.steps.filterIndexed { i, _ -> i != index })
    }

    fun save(onSaved: () -> Unit) {
        val current = _state.value
        if (!current.canSave) return
        val (recipe, ingredients, steps) = current.toEntities()
        val referenced = current.referencedPaths().toSet()
        val orphans = (originalImagePaths + pendingNewImages) - referenced
        viewModelScope.launch {
            repository.save(recipe, ingredients, steps, orphans.toList())
            onSaved()
        }
    }

    /** Call when the user cancels: deletes images imported during this session. */
    fun discardChanges() {
        val toDelete = pendingNewImages.toList()
        if (toDelete.isNotEmpty()) {
            viewModelScope.launch { repository.discardImages(toDelete) }
        }
    }

    private inline fun update(block: (EditUiState) -> EditUiState) {
        _state.value = block(_state.value)
    }
}
```

- [ ] **Step 6: Run the test to verify it passes**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.nwe.recipely.RecipeEditViewModelTest"`
Expected: all 8 tests PASS.

- [ ] **Step 7: Commit**

```bash
git add app/src/test/java/com/nwe/recipely/MainDispatcherRule.kt app/src/test/java/com/nwe/recipely/FakeRecipeRepository.kt app/src/main/java/com/nwe/recipely/ui/edit/RecipeEditViewModel.kt app/src/test/java/com/nwe/recipely/RecipeEditViewModelTest.kt
git commit -m "feat: RecipeEditViewModel with image orphan tracking (TDD)"
```

---

## Task 11: List + Detail ViewModels

Thin ViewModels over repository Flows. The list VM is exercised by a small unit test; the detail VM is trivial and verified via the app.

**Files:**
- Create: `app/src/main/java/com/nwe/recipely/ui/list/RecipeListViewModel.kt`
- Create: `app/src/main/java/com/nwe/recipely/ui/detail/RecipeDetailViewModel.kt`
- Create: `app/src/test/java/com/nwe/recipely/RecipeListViewModelTest.kt`

- [ ] **Step 1: Write the failing list-VM test**

Create `app/src/test/java/com/nwe/recipely/RecipeListViewModelTest.kt`:

```kotlin
package com.nwe.recipely

import com.nwe.recipely.data.Recipe
import com.nwe.recipely.ui.list.RecipeListViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RecipeListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun exposesRecipesFromRepository() = runTest {
        val repo = FakeRecipeRepository()
        val vm = RecipeListViewModel(repo)
        repo.recipes.value = listOf(Recipe(id = 1, name = "A"), Recipe(id = 2, name = "B"))
        advanceUntilIdle()
        assertEquals(listOf("A", "B"), vm.recipes.value.map { it.name })
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.nwe.recipely.RecipeListViewModelTest"`
Expected: FAIL — `RecipeListViewModel` unresolved.

- [ ] **Step 3: Implement the list ViewModel**

Create `app/src/main/java/com/nwe/recipely/ui/list/RecipeListViewModel.kt`:

```kotlin
package com.nwe.recipely.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nwe.recipely.data.Recipe
import com.nwe.recipely.data.RecipeRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class RecipeListViewModel(repository: RecipeRepository) : ViewModel() {
    val recipes: StateFlow<List<Recipe>> = repository.observeRecipes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
```

- [ ] **Step 4: Implement the detail ViewModel**

Create `app/src/main/java/com/nwe/recipely/ui/detail/RecipeDetailViewModel.kt`:

```kotlin
package com.nwe.recipely.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nwe.recipely.data.RecipeRepository
import com.nwe.recipely.data.RecipeWithDetails
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RecipeDetailViewModel(
    private val repository: RecipeRepository,
    recipeId: Long,
) : ViewModel() {

    val recipe: StateFlow<RecipeWithDetails?> = repository.observeRecipe(recipeId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun delete(onDeleted: () -> Unit) {
        val current = recipe.value ?: return
        viewModelScope.launch {
            repository.delete(current)
            onDeleted()
        }
    }
}
```

- [ ] **Step 5: Run the list-VM test to verify it passes**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.nwe.recipely.RecipeListViewModelTest"`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/nwe/recipely/ui/list/RecipeListViewModel.kt app/src/main/java/com/nwe/recipely/ui/detail/RecipeDetailViewModel.kt app/src/test/java/com/nwe/recipely/RecipeListViewModelTest.kt
git commit -m "feat: List and Detail ViewModels"
```

---

## Task 12: List screen UI

Compose UI. Verified by build + running the app (no red-green test).

**Files:**
- Create: `app/src/main/java/com/nwe/recipely/ui/list/RecipeListScreen.kt`

- [ ] **Step 1: Create the list screen**

Create `app/src/main/java/com/nwe/recipely/ui/list/RecipeListScreen.kt`:

```kotlin
package com.nwe.recipely.ui.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import coil.compose.AsyncImage
import com.nwe.recipely.RecipelyApp
import com.nwe.recipely.data.Recipe
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeListScreen(
    onAdd: () -> Unit,
    onOpen: (Long) -> Unit,
) {
    val container = (LocalContext.current.applicationContext as RecipelyApp).container
    val vm: RecipeListViewModel = viewModel(
        factory = viewModelFactory { initializer { RecipeListViewModel(container.repository) } }
    )
    val recipes by vm.recipes.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Meine Rezepte") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) {
                Icon(Icons.Default.Add, contentDescription = "Rezept hinzufügen")
            }
        },
    ) { padding ->
        if (recipes.isEmpty()) {
            EmptyState(Modifier.padding(padding).fillMaxSize())
        } else {
            LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
                items(recipes, key = { it.id }) { recipe ->
                    RecipeRow(recipe = recipe, onClick = { onOpen(recipe.id) })
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun RecipeRow(recipe: Recipe, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (recipe.imageUri != null) {
                AsyncImage(
                    model = File(recipe.imageUri),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Icon(
                    Icons.Outlined.Restaurant,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = recipe.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val meta = recipeMeta(recipe)
            if (meta != null) {
                Text(
                    text = meta,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Outlined.Restaurant,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp),
        )
        Text(
            text = "Noch keine Rezepte",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(top = 16.dp),
        )
        Text(
            text = "Tippe auf +, um dein erstes Rezept anzulegen.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

/** Builds e.g. "⏱ 30 Min · 🍽 4" from whatever fields are set, or null if none. */
fun recipeMeta(recipe: Recipe): String? {
    val parts = buildList {
        recipe.prepTimeMinutes?.let { add("⏱ $it Min") }
        recipe.servings?.let { add("🍽 $it") }
    }
    return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")
}
```

- [ ] **Step 2: Verify it compiles**

Run: `.\gradlew.bat compileDebugKotlin`
Expected: `BUILD SUCCESSFUL`. (The screen is not yet reachable until Task 13 wires navigation.)

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/nwe/recipely/ui/list/RecipeListScreen.kt
git commit -m "feat: recipe list screen (compact rows + empty state)"
```

---

## Task 13: Navigation host + MainActivity wiring

Wires the three routes and makes the list screen reachable. Detail/Edit screens are added next; to keep this task compiling, navigation references them — so this task is implemented together with placeholder-free stubs that are fleshed out in Tasks 14–15. To avoid forward references, this task creates **minimal but complete** Detail/Edit screen entry composables that compile, and Tasks 14–15 replace their bodies.

**Files:**
- Create: `app/src/main/java/com/nwe/recipely/navigation/RecipelyNavHost.kt`
- Create: `app/src/main/java/com/nwe/recipely/ui/detail/RecipeDetailScreen.kt` (temporary minimal body)
- Create: `app/src/main/java/com/nwe/recipely/ui/edit/RecipeEditScreen.kt` (temporary minimal body)
- Modify: `app/src/main/java/com/nwe/recipely/MainActivity.kt`

- [ ] **Step 1: Temporary Detail screen entry point**

Create `app/src/main/java/com/nwe/recipely/ui/detail/RecipeDetailScreen.kt`:

```kotlin
package com.nwe.recipely.ui.detail

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun RecipeDetailScreen(
    recipeId: Long,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDeleted: () -> Unit,
) {
    // Replaced with the full implementation in Task 14.
    Text("Detail $recipeId")
}
```

- [ ] **Step 2: Temporary Edit screen entry point**

Create `app/src/main/java/com/nwe/recipely/ui/edit/RecipeEditScreen.kt`:

```kotlin
package com.nwe.recipely.ui.edit

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun RecipeEditScreen(
    recipeId: Long,
    onClose: () -> Unit,
) {
    // Replaced with the full implementation in Task 15.
    Text("Edit $recipeId")
}
```

- [ ] **Step 3: NavHost**

Create `app/src/main/java/com/nwe/recipely/navigation/RecipelyNavHost.kt`:

```kotlin
package com.nwe.recipely.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.nwe.recipely.ui.detail.RecipeDetailScreen
import com.nwe.recipely.ui.edit.RecipeEditScreen
import com.nwe.recipely.ui.list.RecipeListScreen

object Routes {
    const val LIST = "list"
    const val DETAIL = "detail/{id}"
    const val EDIT = "edit?id={id}"

    fun detail(id: Long) = "detail/$id"
    fun edit(id: Long? = null) = if (id == null) "edit" else "edit?id=$id"
}

@Composable
fun RecipelyNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.LIST) {

        composable(Routes.LIST) {
            RecipeListScreen(
                onAdd = { navController.navigate(Routes.edit()) },
                onOpen = { id -> navController.navigate(Routes.detail(id)) },
            )
        }

        composable(
            route = Routes.DETAIL,
            arguments = listOf(navArgument("id") { type = NavType.LongType }),
        ) { entry ->
            val id = entry.arguments!!.getLong("id")
            RecipeDetailScreen(
                recipeId = id,
                onBack = { navController.popBackStack() },
                onEdit = { navController.navigate(Routes.edit(id)) },
                onDeleted = { navController.popBackStack() },
            )
        }

        composable(
            route = Routes.EDIT,
            arguments = listOf(navArgument("id") {
                type = NavType.LongType
                defaultValue = 0L
            }),
        ) { entry ->
            val id = entry.arguments!!.getLong("id")
            RecipeEditScreen(
                recipeId = id,
                onClose = { navController.popBackStack() },
            )
        }
    }
}
```

- [ ] **Step 4: Use the NavHost in MainActivity**

Replace the `setContent { ... }` body in `app/src/main/java/com/nwe/recipely/MainActivity.kt` so the file reads:

```kotlin
package com.nwe.recipely

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.nwe.recipely.navigation.RecipelyNavHost
import com.nwe.recipely.ui.theme.RecipelyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RecipelyTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    RecipelyNavHost()
                }
            }
        }
    }
}
```

- [ ] **Step 5: Build and run**

Run: `.\gradlew.bat assembleDebug` then install/run.
Expected: app launches to the list screen with empty state + FAB. Tapping the FAB navigates to a placeholder "Edit 0" screen; (no recipes yet to open).

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/nwe/recipely/navigation/RecipelyNavHost.kt app/src/main/java/com/nwe/recipely/ui/detail/RecipeDetailScreen.kt app/src/main/java/com/nwe/recipely/ui/edit/RecipeEditScreen.kt app/src/main/java/com/nwe/recipely/MainActivity.kt
git commit -m "feat: navigation host wiring list/detail/edit routes"
```

---

## Task 14: Detail screen UI

**Files:**
- Modify: `app/src/main/java/com/nwe/recipely/ui/detail/RecipeDetailScreen.kt`

- [ ] **Step 1: Replace the detail screen with the full implementation**

Replace `app/src/main/java/com/nwe/recipely/ui/detail/RecipeDetailScreen.kt`:

```kotlin
package com.nwe.recipely.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import coil.compose.AsyncImage
import com.nwe.recipely.RecipelyApp
import com.nwe.recipely.data.RecipeWithDetails
import com.nwe.recipely.data.Step
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeDetailScreen(
    recipeId: Long,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDeleted: () -> Unit,
) {
    val container = (LocalContext.current.applicationContext as RecipelyApp).container
    val vm: RecipeDetailViewModel = viewModel(
        factory = viewModelFactory { initializer { RecipeDetailViewModel(container.repository, recipeId) } }
    )
    val details by vm.recipe.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(details?.recipe?.name ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                    }
                },
                actions = {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Bearbeiten")
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Löschen")
                    }
                },
            )
        },
    ) { padding ->
        val current = details
        if (current == null) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Rezept nicht gefunden")
            }
        } else {
            DetailContent(current, Modifier.padding(padding))
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Rezept löschen?") },
            text = { Text("Das Rezept wird dauerhaft entfernt.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    vm.delete(onDeleted)
                }) { Text("Löschen") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Abbrechen") }
            },
        )
    }
}

@Composable
private fun DetailContent(details: RecipeWithDetails, modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center,
            ) {
                val image = details.recipe.imageUri
                if (image != null) {
                    AsyncImage(
                        model = File(image),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Icon(
                        Icons.Outlined.Restaurant,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(72.dp),
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                details.recipe.prepTimeMinutes?.let {
                    AssistChip(onClick = {}, label = { Text("⏱ $it Min") })
                }
                details.recipe.servings?.let {
                    AssistChip(onClick = {}, label = { Text("🍽 $it Portionen") })
                }
            }
        }

        if (details.ingredients.isNotEmpty()) {
            item { SectionHeader("Zutaten") }
            items(details.ingredients.sortedBy { it.position }.size) { index ->
                val ingredient = details.ingredients.sortedBy { it.position }[index]
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Box(
                        Modifier.size(6.dp).clip(CircleShape)
                            .let { it },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Outlined.Restaurant,
                            contentDescription = null,
                            modifier = Modifier.size(0.dp),
                        )
                    }
                    Text("•  ${ingredient.text}", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }

        if (details.steps.isNotEmpty()) {
            item { SectionHeader("Zubereitung") }
            val sortedSteps = details.steps.sortedBy { it.position }
            items(sortedSteps.size) { index ->
                StepItem(number = index + 1, step = sortedSteps[index])
            }
        }

        item { Box(Modifier.height(24.dp)) }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun StepItem(number: Int, step: Step) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = number.toString(),
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.labelLarge,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            if (step.text.isNotBlank()) {
                Text(step.text, style = MaterialTheme.typography.bodyLarge)
            }
            if (step.imageUri != null) {
                AsyncImage(
                    model = File(step.imageUri),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(12.dp)),
                )
            }
        }
    }
}
```

> Note: the numbered badge background uses the primary color via a `Modifier.background`. Add the import and modifier: in `StepItem`, change the badge `Box` modifier to include `.background(MaterialTheme.colorScheme.primary, CircleShape)` and add `import androidx.compose.foundation.background`. Likewise the ingredient bullet `Box` above is simplified to a leading "•" in the `Text`, so its inner `Box`/`Icon` can be removed — keep only `Text("•  ${ingredient.text}", ...)`.

- [ ] **Step 2: Apply the two cleanups from the note**

In `StepItem`, the badge `Box` should read:

```kotlin
import androidx.compose.foundation.background
// ...
Box(
    modifier = Modifier
        .size(28.dp)
        .background(MaterialTheme.colorScheme.primary, CircleShape),
    contentAlignment = Alignment.Center,
) {
    Text(
        text = number.toString(),
        color = MaterialTheme.colorScheme.onPrimary,
        style = MaterialTheme.typography.labelLarge,
    )
}
```

And the ingredient item simplifies to:

```kotlin
items(details.ingredients.sortedBy { it.position }.size) { index ->
    val ingredient = details.ingredients.sortedBy { it.position }[index]
    Text(
        text = "•  ${ingredient.text}",
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
    )
}
```

(Remove the now-unused `CircleShape` import only if no longer referenced — it is still used by the badge, so keep it.)

- [ ] **Step 3: Build and run**

Run: `.\gradlew.bat assembleDebug` then install/run.
Expected: `BUILD SUCCESSFUL`. (Detail is reachable once a recipe exists — fully exercised after Task 15.)

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/nwe/recipely/ui/detail/RecipeDetailScreen.kt
git commit -m "feat: recipe detail screen with delete confirmation"
```

---

## Task 15: Edit screen UI (with image picker + camera)

**Files:**
- Modify: `app/src/main/java/com/nwe/recipely/ui/edit/RecipeEditScreen.kt`

- [ ] **Step 1: Replace the edit screen with the full implementation**

Replace `app/src/main/java/com/nwe/recipely/ui/edit/RecipeEditScreen.kt`:

```kotlin
package com.nwe.recipely.ui.edit

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import coil.compose.AsyncImage
import com.nwe.recipely.RecipelyApp
import com.nwe.recipely.data.ImageStore
import java.io.File

private sealed interface ImageTarget {
    data object Title : ImageTarget
    data class StepImage(val index: Int) : ImageTarget
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeEditScreen(
    recipeId: Long,
    onClose: () -> Unit,
) {
    val container = (LocalContext.current.applicationContext as RecipelyApp).container
    val imageStore: ImageStore = container.imageStore
    val vm: RecipeEditViewModel = viewModel(
        factory = viewModelFactory { initializer { RecipeEditViewModel(container.repository, recipeId) } }
    )
    val state by vm.state.collectAsState()

    var imageTarget by remember { mutableStateOf<ImageTarget?>(null) }
    var showSourceDialog by remember { mutableStateOf(false) }
    var cameraTargetPath by remember { mutableStateOf<String?>(null) }

    fun applyImage(path: String?) {
        when (val target = imageTarget) {
            ImageTarget.Title -> vm.setTitleImage(path)
            is ImageTarget.StepImage -> vm.setStepImage(target.index, path)
            null -> Unit
        }
    }

    val pickImage = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) applyImage(imageStore.importFromUri(uri))
    }

    val takePhoto = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) applyImage(cameraTargetPath)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (recipeId == 0L) "Neues Rezept" else "Rezept bearbeiten") },
                navigationIcon = {
                    IconButton(onClick = {
                        vm.discardChanges()
                        onClose()
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "Abbrechen")
                    }
                },
                actions = {
                    TextButton(
                        enabled = state.canSave,
                        onClick = { vm.save(onClose) },
                    ) { Text("Speichern") }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                TitleImagePicker(
                    imagePath = state.imagePath,
                    onPick = { imageTarget = ImageTarget.Title; showSourceDialog = true },
                    onRemove = { vm.setTitleImage(null) },
                )
            }
            item {
                OutlinedTextField(
                    value = state.name,
                    onValueChange = vm::setName,
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = state.prepTime,
                        onValueChange = vm::setPrepTime,
                        label = { Text("Zeit (Min)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = state.servings,
                        onValueChange = vm::setServings,
                        label = { Text("Portionen") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            item { EditSectionHeader("Zutaten") }
            items(state.ingredients.size) { index ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = state.ingredients[index].text,
                        onValueChange = { vm.setIngredient(index, it) },
                        label = { Text("Zutat ${index + 1}") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { vm.removeIngredient(index) }) {
                        Icon(Icons.Default.Close, contentDescription = "Zutat entfernen")
                    }
                }
            }
            item {
                OutlinedButton(onClick = vm::addIngredient) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text("  Zutat hinzufügen")
                }
            }

            item { EditSectionHeader("Zubereitung") }
            items(state.steps.size) { index ->
                StepEditor(
                    number = index + 1,
                    text = state.steps[index].text,
                    imagePath = state.steps[index].imagePath,
                    onTextChange = { vm.setStepText(index, it) },
                    onRemove = { vm.removeStep(index) },
                    onAddImage = { imageTarget = ImageTarget.StepImage(index); showSourceDialog = true },
                    onRemoveImage = { vm.setStepImage(index, null) },
                )
            }
            item {
                OutlinedButton(onClick = vm::addStep) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text("  Schritt hinzufügen")
                }
            }
            item { Box(Modifier.height(24.dp)) }
        }
    }

    if (showSourceDialog) {
        AlertDialog(
            onDismissRequest = { showSourceDialog = false },
            title = { Text("Bild hinzufügen") },
            text = { Text("Woher soll das Bild kommen?") },
            confirmButton = {
                TextButton(onClick = {
                    showSourceDialog = false
                    pickImage.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }) { Text("Galerie") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showSourceDialog = false
                    val (uri, path) = imageStore.createCameraTarget()
                    cameraTargetPath = path
                    takePhoto.launch(uri)
                }) { Text("Kamera") }
            },
        )
    }
}

@Composable
private fun TitleImagePicker(imagePath: String?, onPick: () -> Unit, onRemove: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(14.dp))
            .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(14.dp))
            .clickable(onClick = onPick),
        contentAlignment = Alignment.Center,
    ) {
        if (imagePath != null) {
            AsyncImage(
                model = File(imagePath),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            IconButton(
                onClick = onRemove,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(28.dp)
                    .background(Color(0xAA000000), CircleShape),
            ) {
                Icon(Icons.Default.Close, contentDescription = "Bild entfernen", tint = Color.White)
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Outlined.AddPhotoAlternate,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text("Titelbild · Galerie / Kamera", color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun StepEditor(
    number: Int,
    text: String,
    imagePath: String?,
    onTextChange: (String) -> Unit,
    onRemove: () -> Unit,
    onAddImage: () -> Unit,
    onRemoveImage: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier.size(28.dp).background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(number.toString(), color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.labelLarge)
            }
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                label = { Text("Schritt $number") },
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Close, contentDescription = "Schritt entfernen")
            }
        }
        if (imagePath != null) {
            Box(modifier = Modifier.fillMaxWidth()) {
                AsyncImage(
                    model = File(imagePath),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(10.dp)),
                )
                IconButton(
                    onClick = onRemoveImage,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(26.dp)
                        .background(Color(0xAA000000), CircleShape),
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Schritt-Bild entfernen", tint = Color.White)
                }
            }
        } else {
            OutlinedButton(onClick = onAddImage) {
                Icon(Icons.Default.PhotoCamera, contentDescription = null)
                Text("  Bild zum Schritt")
            }
        }
    }
}

@Composable
private fun EditSectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp),
    )
}
```

- [ ] **Step 2: Build**

Run: `.\gradlew.bat assembleDebug`
Expected: `BUILD SUCCESSFUL`. If an icon (e.g. `AddPhotoAlternate`, `PhotoCamera`) is unresolved, confirm `androidx-material-icons-extended` is in the dependencies (Task 2) — it provides the extended icon set.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/nwe/recipely/ui/edit/RecipeEditScreen.kt
git commit -m "feat: recipe edit screen with dynamic lists + image picker/camera"
```

---

## Task 16: End-to-end manual verification + full test run

**Files:** none (verification only)

- [ ] **Step 1: Run all unit tests**

Run: `.\gradlew.bat testDebugUnitTest`
Expected: all tests PASS (`RecipeMappingTest`, `RecipeEditViewModelTest`, `RecipeListViewModelTest`).

- [ ] **Step 2: Run the instrumented test (emulator/device required)**

Run: `.\gradlew.bat connectedDebugAndroidTest`
Expected: `RecipeDaoTest` PASSES.

- [ ] **Step 3: Manual smoke test on a device/emulator**

Install: `.\gradlew.bat installDebug`. Then verify the full flow:
1. App opens to empty state.
2. Tap **+** → editor. Save is disabled until a name is entered.
3. Enter name "Carbonara", time 30, servings 4. Add two ingredients. Add two steps; on step 2 tap "Bild zum Schritt" → choose **Galerie**, pick an image; it shows a thumbnail.
4. Tap **Speichern** → returns to list; the recipe appears with thumbnail and "⏱ 30 Min · 🍽 4".
5. Open the recipe → detail shows title image (if set), chips, ingredients, numbered steps with the step image.
6. Tap edit (pencil) → change a value → save → detail reflects the change.
7. Rotate the device while editing → entered text is preserved.
8. Delete (trash) → confirm dialog → recipe removed from the list.
9. Add a recipe using **Kamera** for the title image → photo is captured and saved.

Expected: every step behaves as described; no crashes.

- [ ] **Step 4: Final commit**

```bash
git add -A
git commit -m "chore: Recipely v1 complete — verified end-to-end"
```

---

## Self-Review Notes (already applied)

- **Spec coverage:** name/image/time/servings/ingredients/steps-with-optional-image (Tasks 4, 9, 14, 15); local Room storage (Tasks 4–8); gallery + camera (Tasks 7, 15); green Material 3 theme light/dark (Task 3); list/detail/edit screens + navigation (Tasks 12–15); name-required validation, blank-row dropping, delete confirmation, cascade + image cleanup, rotation survival (Tasks 9, 10, 14); tests: ViewModel/mapping unit + DAO instrumented (Tasks 6, 9, 10, 11). Out-of-scope items (search, tags, sync, export, scaling) are intentionally not implemented.
- **Type consistency:** Repository interface methods (`observeRecipes`, `observeRecipe`, `save(..., removedImagePaths)`, `delete(details)`, `discardImages`) match across `RecipeRepository`, `FakeRecipeRepository`, and all ViewModels. `EditUiState`/`IngredientRow`/`StepRow` and the `toEntities`/`toUiState`/`referencedPaths` helpers match between `EditUiState.kt`, the mapping test, and the ViewModel.
- **No placeholders:** Every code step contains full code. The only intentionally-temporary code is the Detail/Edit screen stubs in Task 13, explicitly replaced in Tasks 14–15.
