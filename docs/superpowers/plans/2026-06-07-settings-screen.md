# Settings Screen Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a settings screen letting the user choose theme (System/Light/Dark, default System) and language (System/English/Deutsch, default System), both persisted.

**Architecture:** Lean MVVM consistent with the codebase. Theme is persisted in a Preferences DataStore and applied reactively by recomposing `RecipelyTheme` (no restart). Language is applied via the AppCompat locale delegate (`AppCompatDelegate.setApplicationLocales`), persisted by AppCompat's own `autoStoreLocales` store, and applied entirely in the Compose layer to keep the ViewModel JVM-testable. Entry point is a ⚙️ icon in the recipe list's editorial header → a new `settings` route.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, AndroidX DataStore (Preferences), AndroidX AppCompat (locale delegate), JUnit 4 + kotlinx-coroutines-test.

---

## Build environment (every CLI Gradle command)

This machine's default JDK (24) breaks Gradle 8.13. In each fresh PowerShell, set the JBR first:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
```

All `./gradlew` commands below assume `.\gradlew.bat` on Windows with that env var set.

## File structure

- Create `app/src/main/java/com/nwe/recipely/data/SettingsRepository.kt` — `ThemeMode` enum + `SettingsRepository` interface (pure Kotlin, theme only).
- Create `app/src/main/java/com/nwe/recipely/data/DataStoreSettingsRepository.kt` — DataStore-backed implementation.
- Create `app/src/main/java/com/nwe/recipely/ui/settings/SettingsViewModel.kt` — exposes `themeMode`, `setThemeMode`.
- Create `app/src/main/java/com/nwe/recipely/ui/settings/AppLanguage.kt` — language enum + AppCompat apply/read helpers (Compose/Android layer).
- Create `app/src/main/java/com/nwe/recipely/ui/settings/SettingsScreen.kt` — the screen UI.
- Create `app/src/test/java/com/nwe/recipely/FakeSettingsRepository.kt` — in-memory fake.
- Create `app/src/test/java/com/nwe/recipely/SettingsViewModelTest.kt` — JVM unit test.
- Modify `gradle/libs.versions.toml`, `app/build.gradle.kts` — new deps.
- Modify `app/src/main/java/com/nwe/recipely/di/AppContainer.kt` — build/hold `settingsRepository`.
- Modify `app/src/main/java/com/nwe/recipely/MainActivity.kt` — `AppCompatActivity` + theme wiring.
- Modify `app/src/main/res/values/themes.xml`, `app/src/main/res/values-night/themes.xml` — AppCompat parent.
- Modify `app/src/main/AndroidManifest.xml` — `autoStoreLocales` service.
- Modify `app/src/main/java/com/nwe/recipely/navigation/RecipelyNavHost.kt` — `settings` route.
- Modify `app/src/main/java/com/nwe/recipely/ui/list/RecipeListScreen.kt` — settings icon + `onOpenSettings`.
- Modify `app/src/main/res/values/strings.xml`, `app/src/main/res/values-de/strings.xml` — new strings.
- Modify `CLAUDE.md` — relax the "no AppCompat" note.

---

## Task 1: Add DataStore + AppCompat dependencies

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`

- [ ] **Step 1: Add versions to the catalog**

In `gradle/libs.versions.toml`, under `[versions]` add:

```toml
appcompat = "1.7.0"
datastorePreferences = "1.1.1"
```

- [ ] **Step 2: Add library entries**

In `gradle/libs.versions.toml`, under `[libraries]` add:

```toml
androidx-appcompat = { group = "androidx.appcompat", name = "appcompat", version.ref = "appcompat" }
androidx-datastore-preferences = { group = "androidx.datastore", name = "datastore-preferences", version.ref = "datastorePreferences" }
```

- [ ] **Step 3: Reference them in the module build**

In `app/build.gradle.kts`, in the `dependencies { }` block right after `implementation(libs.androidx.core.ktx)` add:

```kotlin
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.datastore.preferences)
```

- [ ] **Step 4: Sync/build to verify dependencies resolve**

Run: `.\gradlew.bat help`
Expected: `BUILD SUCCESSFUL` (dependencies resolve, no catalog errors).

- [ ] **Step 5: Commit**

```bash
git add gradle/libs.versions.toml app/build.gradle.kts
git commit -m "build: add datastore-preferences and appcompat deps"
```

---

## Task 2: SettingsRepository + DataStore implementation + AppContainer wiring

**Files:**
- Create: `app/src/main/java/com/nwe/recipely/data/SettingsRepository.kt`
- Create: `app/src/main/java/com/nwe/recipely/data/DataStoreSettingsRepository.kt`
- Modify: `app/src/main/java/com/nwe/recipely/di/AppContainer.kt`

- [ ] **Step 1: Create the interface + enum**

Create `app/src/main/java/com/nwe/recipely/data/SettingsRepository.kt`:

```kotlin
package com.nwe.recipely.data

import kotlinx.coroutines.flow.Flow

/** How the app resolves its light/dark appearance. */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/**
 * Persisted user preferences. Only the theme lives here; the language is owned by AppCompat's
 * locale store (see ui/settings/AppLanguage.kt), which keeps this interface pure-Kotlin and the
 * ViewModel JVM-unit-testable.
 */
interface SettingsRepository {
    val themeMode: Flow<ThemeMode>
    suspend fun setThemeMode(mode: ThemeMode)
}
```

- [ ] **Step 2: Create the DataStore-backed implementation**

Create `app/src/main/java/com/nwe/recipely/data/DataStoreSettingsRepository.kt`:

```kotlin
package com.nwe.recipely.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

class DataStoreSettingsRepository(context: Context) : SettingsRepository {

    private val dataStore = context.applicationContext.settingsDataStore
    private val themeKey = stringPreferencesKey("theme_mode")

    override val themeMode: Flow<ThemeMode> = dataStore.data.map { prefs ->
        prefs[themeKey]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[themeKey] = mode.name }
    }
}
```

- [ ] **Step 3: Wire it into AppContainer**

In `app/src/main/java/com/nwe/recipely/di/AppContainer.kt`, add after the `repository` declaration:

```kotlin
    val settingsRepository: SettingsRepository = DataStoreSettingsRepository(context)
```

The required imports (`com.nwe.recipely.data.SettingsRepository`, `com.nwe.recipely.data.DataStoreSettingsRepository`) are in the same package as the other `data` imports already listed; add them alongside.

- [ ] **Step 4: Build to verify it compiles**

Run: `.\gradlew.bat assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/nwe/recipely/data/SettingsRepository.kt app/src/main/java/com/nwe/recipely/data/DataStoreSettingsRepository.kt app/src/main/java/com/nwe/recipely/di/AppContainer.kt
git commit -m "feat(data): SettingsRepository backed by Preferences DataStore"
```

---

## Task 3: SettingsViewModel + fake + unit test (TDD)

**Files:**
- Create: `app/src/test/java/com/nwe/recipely/FakeSettingsRepository.kt`
- Create: `app/src/test/java/com/nwe/recipely/SettingsViewModelTest.kt`
- Create: `app/src/main/java/com/nwe/recipely/ui/settings/SettingsViewModel.kt`

- [ ] **Step 1: Create the in-memory fake**

Create `app/src/test/java/com/nwe/recipely/FakeSettingsRepository.kt`:

```kotlin
package com.nwe.recipely

import com.nwe.recipely.data.SettingsRepository
import com.nwe.recipely.data.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeSettingsRepository : SettingsRepository {
    val theme = MutableStateFlow(ThemeMode.SYSTEM)
    override val themeMode: Flow<ThemeMode> = theme
    override suspend fun setThemeMode(mode: ThemeMode) {
        theme.value = mode
    }
}
```

- [ ] **Step 2: Write the failing test**

Create `app/src/test/java/com/nwe/recipely/SettingsViewModelTest.kt`:

```kotlin
package com.nwe.recipely

import com.nwe.recipely.data.ThemeMode
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

    @Test
    fun defaultThemeMode_isSystem() = runTest {
        val vm = SettingsViewModel(FakeSettingsRepository())
        backgroundScope.launch { vm.themeMode.collect {} }
        advanceUntilIdle()
        assertEquals(ThemeMode.SYSTEM, vm.themeMode.value)
    }

    @Test
    fun setThemeMode_propagatesToFlow() = runTest {
        val repo = FakeSettingsRepository()
        val vm = SettingsViewModel(repo)
        backgroundScope.launch { vm.themeMode.collect {} }
        vm.setThemeMode(ThemeMode.DARK)
        advanceUntilIdle()
        assertEquals(ThemeMode.DARK, vm.themeMode.value)
    }
}
```

- [ ] **Step 3: Run test to verify it fails**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.nwe.recipely.SettingsViewModelTest"`
Expected: FAIL — `SettingsViewModel` is unresolved (does not compile yet).

- [ ] **Step 4: Create the ViewModel**

Create `app/src/main/java/com/nwe/recipely/ui/settings/SettingsViewModel.kt`:

```kotlin
package com.nwe.recipely.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nwe.recipely.data.SettingsRepository
import com.nwe.recipely.data.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = repository.themeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeMode.SYSTEM)

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { repository.setThemeMode(mode) }
    }
}
```

- [ ] **Step 5: Run test to verify it passes**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.nwe.recipely.SettingsViewModelTest"`
Expected: PASS (2 tests).

- [ ] **Step 6: Commit**

```bash
git add app/src/test/java/com/nwe/recipely/FakeSettingsRepository.kt app/src/test/java/com/nwe/recipely/SettingsViewModelTest.kt app/src/main/java/com/nwe/recipely/ui/settings/SettingsViewModel.kt
git commit -m "feat(settings): SettingsViewModel with theme-mode flow (TDD)"
```

---

## Task 4: AppCompat window theme + autoStoreLocales service

**Files:**
- Modify: `app/src/main/res/values/themes.xml`
- Modify: `app/src/main/res/values-night/themes.xml`
- Modify: `app/src/main/AndroidManifest.xml`

- [ ] **Step 1: Switch the light theme parent to AppCompat**

Replace the entire contents of `app/src/main/res/values/themes.xml` with:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.Recipely" parent="Theme.AppCompat.DayNight.NoActionBar" />
</resources>
```

- [ ] **Step 2: Switch the night theme parent to AppCompat**

Replace the entire contents of `app/src/main/res/values-night/themes.xml` with:

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.Recipely" parent="Theme.AppCompat.DayNight.NoActionBar" />
</resources>
```

- [ ] **Step 3: Register the AppCompat locale auto-store service**

In `app/src/main/AndroidManifest.xml`, inside `<application>` (e.g. right after the closing `</provider>`), add:

```xml
        <service
            android:name="androidx.appcompat.app.AppLocalesMetadataHolderService"
            android:enabled="false"
            android:exported="false">
            <meta-data
                android:name="autoStoreLocales"
                android:value="true" />
        </service>
```

- [ ] **Step 4: Build to verify resources/manifest are valid**

Run: `.\gradlew.bat assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/res/values/themes.xml app/src/main/res/values-night/themes.xml app/src/main/AndroidManifest.xml
git commit -m "build(theme): AppCompat DayNight window theme + autoStoreLocales service"
```

---

## Task 5: MainActivity → AppCompatActivity + reactive theme

**Files:**
- Modify: `app/src/main/java/com/nwe/recipely/MainActivity.kt`

- [ ] **Step 1: Rewrite MainActivity**

Replace the entire contents of `app/src/main/java/com/nwe/recipely/MainActivity.kt` with:

```kotlin
package com.nwe.recipely

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.nwe.recipely.data.ThemeMode
import com.nwe.recipely.navigation.RecipelyNavHost
import com.nwe.recipely.ui.theme.RecipelyTheme

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val settings = (application as RecipelyApp).container.settingsRepository
        setContent {
            val themeMode by settings.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            RecipelyTheme(darkTheme = darkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    RecipelyNavHost()
                }
            }
        }
    }
}
```

- [ ] **Step 2: Build to verify it compiles**

Run: `.\gradlew.bat assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Install and smoke-test (device/emulator connected)**

Run: `.\gradlew.bat installDebug`
Expected: app launches unchanged (theme still follows system — settings UI not wired yet).

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/nwe/recipely/MainActivity.kt
git commit -m "feat(theme): apply persisted ThemeMode reactively in MainActivity"
```

---

## Task 6: Add settings strings (English + German)

**Files:**
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values-de/strings.xml`

- [ ] **Step 1: Add English strings**

In `app/src/main/res/values/strings.xml`, before the closing `</resources>` add:

```xml
    <!-- Settings -->
    <string name="settings">Settings</string>
    <string name="settings_title">Settings</string>
    <string name="settings_appearance">Appearance</string>
    <string name="settings_language">Language</string>
    <string name="theme_system">System</string>
    <string name="theme_light">Light</string>
    <string name="theme_dark">Dark</string>
    <string name="language_system">System default</string>
    <string name="language_english">English</string>
    <string name="language_german">Deutsch</string>
```

- [ ] **Step 2: Add German strings**

In `app/src/main/res/values-de/strings.xml`, before the closing `</resources>` add:

```xml
    <!-- Settings -->
    <string name="settings">Einstellungen</string>
    <string name="settings_title">Einstellungen</string>
    <string name="settings_appearance">Darstellung</string>
    <string name="settings_language">Sprache</string>
    <string name="theme_system">System</string>
    <string name="theme_light">Hell</string>
    <string name="theme_dark">Dunkel</string>
    <string name="language_system">Systemstandard</string>
    <string name="language_english">English</string>
    <string name="language_german">Deutsch</string>
```

- [ ] **Step 3: Build to verify resources compile**

Run: `.\gradlew.bat assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/res/values/strings.xml app/src/main/res/values-de/strings.xml
git commit -m "i18n: settings screen strings (en + de)"
```

---

## Task 7: AppLanguage helper (AppCompat locale layer)

**Files:**
- Create: `app/src/main/java/com/nwe/recipely/ui/settings/AppLanguage.kt`

- [ ] **Step 1: Create the helper**

Create `app/src/main/java/com/nwe/recipely/ui/settings/AppLanguage.kt`:

```kotlin
package com.nwe.recipely.ui.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/**
 * In-app language choice. The empty tag means "follow the system locale". Persistence and
 * application are delegated to AppCompat (autoStoreLocales), so this lives in the Android layer,
 * not the ViewModel.
 */
enum class AppLanguage(val tag: String) {
    SYSTEM(""),
    ENGLISH("en"),
    GERMAN("de");

    companion object {
        /** The language currently applied by AppCompat, mapped back to an enum value. */
        fun current(): AppLanguage {
            val tags = AppCompatDelegate.getApplicationLocales().toLanguageTags()
            return entries.firstOrNull { it.tag.isNotEmpty() && tags.startsWith(it.tag) } ?: SYSTEM
        }
    }
}

/** Apply this language app-wide; AppCompat recreates the Activity and persists the choice. */
fun AppLanguage.applyNow() {
    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
}
```

- [ ] **Step 2: Build to verify it compiles**

Run: `.\gradlew.bat assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/nwe/recipely/ui/settings/AppLanguage.kt
git commit -m "feat(settings): AppLanguage helper over AppCompat locale delegate"
```

---

## Task 8: SettingsScreen UI

**Files:**
- Create: `app/src/main/java/com/nwe/recipely/ui/settings/SettingsScreen.kt`

- [ ] **Step 1: Create the screen**

Create `app/src/main/java/com/nwe/recipely/ui/settings/SettingsScreen.kt`:

```kotlin
package com.nwe.recipely.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nwe.recipely.R
import com.nwe.recipely.RecipelyApp
import com.nwe.recipely.data.ThemeMode
import com.nwe.recipely.ui.theme.Fraunces

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val container = (LocalContext.current.applicationContext as RecipelyApp).container
    val vm: SettingsViewModel = viewModel(
        factory = viewModelFactory { initializer { SettingsViewModel(container.settingsRepository) } }
    )
    val themeMode by vm.themeMode.collectAsState()
    var language by remember { mutableStateOf(AppLanguage.current()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_title),
                        fontFamily = Fraunces,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            SectionLabel(stringResource(R.string.settings_appearance))
            ThemeSegmentedControl(selected = themeMode, onSelect = vm::setThemeMode)

            Spacer(Modifier.height(16.dp))

            SectionLabel(stringResource(R.string.settings_language))
            LanguageList(
                selected = language,
                onSelect = {
                    language = it
                    it.applyNow()
                },
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.5.sp,
        color = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 8.dp),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemeSegmentedControl(selected: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    val options = listOf(
        ThemeMode.SYSTEM to R.string.theme_system,
        ThemeMode.LIGHT to R.string.theme_light,
        ThemeMode.DARK to R.string.theme_dark,
    )
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (mode, labelRes) ->
            SegmentedButton(
                selected = selected == mode,
                onClick = { onSelect(mode) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
            ) {
                Text(stringResource(labelRes))
            }
        }
    }
}

@Composable
private fun LanguageList(selected: AppLanguage, onSelect: (AppLanguage) -> Unit) {
    val options = listOf(
        AppLanguage.SYSTEM to R.string.language_system,
        AppLanguage.ENGLISH to R.string.language_english,
        AppLanguage.GERMAN to R.string.language_german,
    )
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            options.forEach { (lang, labelRes) ->
                Row(
                    selected = selected == lang,
                    label = stringResource(labelRes),
                    onClick = { onSelect(lang) },
                )
            }
        }
    }
}

@Composable
private fun Row(selected: Boolean, label: String, onClick: () -> Unit) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        RadioButton(selected = selected, onClick = null)
    }
}
```

- [ ] **Step 2: Build to verify it compiles**

Run: `.\gradlew.bat assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/nwe/recipely/ui/settings/SettingsScreen.kt
git commit -m "feat(settings): settings screen UI (theme segmented control + language list)"
```

---

## Task 9: Navigation route + list header settings icon

**Files:**
- Modify: `app/src/main/java/com/nwe/recipely/navigation/RecipelyNavHost.kt`
- Modify: `app/src/main/java/com/nwe/recipely/ui/list/RecipeListScreen.kt`

- [ ] **Step 1: Add the settings route**

In `app/src/main/java/com/nwe/recipely/navigation/RecipelyNavHost.kt`:

In the `Routes` object, after `const val EDIT = "edit?id={id}"` add:

```kotlin
    const val SETTINGS = "settings"
```

Add the import near the other screen imports:

```kotlin
import com.nwe.recipely.ui.settings.SettingsScreen
```

Change the `list` composable to pass `onOpenSettings`:

```kotlin
        composable(Routes.LIST) {
            RecipeListScreen(
                onAdd = { navController.navigate(Routes.edit()) },
                onOpen = { id -> navController.navigate(Routes.detail(id)) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }
```

After the `EDIT` composable block (before the closing brace of `NavHost`) add:

```kotlin
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
```

- [ ] **Step 2: Add the `onOpenSettings` parameter + icon to the list screen**

In `app/src/main/java/com/nwe/recipely/ui/list/RecipeListScreen.kt`:

Add imports near the existing ones:

```kotlin
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.IconButton
```

Change the `RecipeListScreen` signature to accept the new lambda:

```kotlin
@Composable
fun RecipeListScreen(
    onAdd: () -> Unit,
    onOpen: (Long) -> Unit,
    onOpenSettings: () -> Unit,
) {
```

In the empty branch, wrap `EmptyState` so the gear stays reachable. Replace:

```kotlin
        if (recipes.isEmpty()) {
            EmptyState(Modifier.padding(padding).fillMaxSize())
        } else {
```

with:

```kotlin
        if (recipes.isEmpty()) {
            Box(Modifier.padding(padding).fillMaxSize()) {
                SettingsButton(onOpenSettings, Modifier.align(Alignment.TopEnd))
                EmptyState(Modifier.fillMaxSize())
            }
        } else {
```

Change `ListHeader(count = recipes.size)` in the `item { }` to pass the lambda:

```kotlin
                item { ListHeader(count = recipes.size, onOpenSettings = onOpenSettings) }
```

Replace the `ListHeader` composable with a version that has a trailing settings icon:

```kotlin
@Composable
private fun ListHeader(count: Int, onOpenSettings: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = pluralStringResource(R.plurals.recipe_count, count, count).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                color = MaterialTheme.colorScheme.secondary,
            )
            Text(
                text = stringResource(R.string.list_title),
                style = MaterialTheme.typography.displaySmall,
                fontFamily = Fraunces,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        SettingsButton(onOpenSettings)
    }
}

@Composable
private fun SettingsButton(onOpenSettings: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(onClick = onOpenSettings, modifier = modifier) {
        Icon(
            Icons.Outlined.Settings,
            contentDescription = stringResource(R.string.settings),
            tint = MaterialTheme.colorScheme.primary,
        )
    }
}
```

- [ ] **Step 3: Build to verify it compiles**

Run: `.\gradlew.bat assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Install and verify the full flow (device/emulator connected)**

Run: `.\gradlew.bat installDebug`
Expected: list shows a ⚙️ icon by the header; tapping opens Settings; theme System/Light/Dark switches instantly; language System/English/Deutsch recreates the screen with translated strings; both survive an app restart.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/nwe/recipely/navigation/RecipelyNavHost.kt app/src/main/java/com/nwe/recipely/ui/list/RecipeListScreen.kt
git commit -m "feat(settings): settings entry point in list header + navigation route"
```

---

## Task 10: Update CLAUDE.md + full verification

**Files:**
- Modify: `CLAUDE.md`

- [ ] **Step 1: Relax the "no AppCompat" note**

In `CLAUDE.md`, in the "Theme" bullet under "Key cross-cutting rules", change:

```
- **Theme:** fixed **green** Material 3 scheme, light + dark, **no dynamic color / Material You** (`ui/theme/`). XML window themes use platform `android:Theme.Material*` parents (no AppCompat/Views-material).
```

to:

```
- **Theme:** fixed **green** Material 3 scheme, light + dark, **no dynamic color / Material You** (`ui/theme/`). User-selectable via Settings (System/Light/Dark), persisted in a Preferences DataStore and applied by recomposing `RecipelyTheme` in `MainActivity`. XML window themes use `Theme.AppCompat.DayNight.NoActionBar` and `MainActivity` is an `AppCompatActivity` — AppCompat is a deliberate dependency for the **per-app language** locale delegate (`AppCompatDelegate.setApplicationLocales`, persisted via `autoStoreLocales`); the UI itself remains Compose-only (no AppCompat views).
```

Also update the last paragraph of "Build configuration" that says "This is a Compose-only stack — no Views/AppCompat, view-binding, or DI framework." to:

```
This is a Compose-only UI stack — no Views/AppCompat *views*, view-binding, or DI framework. (AppCompat is present solely for `AppCompatDelegate` per-app-language support; `androidx.datastore:datastore-preferences` stores the theme preference.)
```

- [ ] **Step 2: Run the full unit-test suite**

Run: `.\gradlew.bat testDebugUnitTest`
Expected: `BUILD SUCCESSFUL`, all tests pass (including `SettingsViewModelTest`).

- [ ] **Step 3: Final debug build**

Run: `.\gradlew.bat assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

```bash
git add CLAUDE.md
git commit -m "docs: note AppCompat/DataStore additions for settings"
```

---

## Self-review notes

- **Spec coverage:** entry point (Task 9), theme segmented control + language list (Task 8), AppCompat locale mechanism (Tasks 4/7/8), DataStore theme persistence (Task 2), reactive theme (Task 5), `AppCompatActivity`/theme implication (Tasks 4/5), strings (Task 6), `SettingsViewModelTest` (Task 3), CLAUDE.md update (Task 10). All covered.
- **Language is intentionally not in the ViewModel/Repository** — it is read/applied via `AppLanguage` (AppCompat) in the Compose layer, which preserves the ViewModel's JVM-testability exactly as the spec requires. The repository interface stays theme-only.
- **Empty-list reachability:** the settings gear is rendered in both the populated header (Task 9, `ListHeader`) and the empty state (Task 9, `Box` + `TopEnd`), so settings are always reachable.
