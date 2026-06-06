# UI-Refresh „Warm Editorial Kitchen" Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Die UI von Standard-Material-3 zu einer warmen, magazinartigen Food-App-Anmutung heben — reiner Optik-Refresh (Theme/Typografie + Liste/Detail/Edit umstylen), ohne Änderung an Datenmodell, ViewModels, Repository, Room oder DAO.

**Architecture:** Foundation-first. Zuerst das Theme-Fundament (gebündelte Fonts, warme Palette, Shapes, Typografie), dann wiederverwendbare Composables (`MetaChip`, `RecipeCard`), dann Screen-für-Screen Liste → Detail → Edit. Jede Compose-Änderung wird per erfolgreichem Build (`assembleDebug`) verifiziert (Projektkonvention: Compose-UI wird nicht red-green getestet); die bestehenden JVM-Unit-Tests müssen grün bleiben.

**Tech Stack:** Kotlin 2.0.21, Jetpack Compose (BOM 2024.09.03), Material 3, Coil, Room. Build via Gradle Wrapper.

**Spec:** `docs/superpowers/specs/2026-06-06-ui-redesign-design.md`. Visuelle Referenz: `docs/mockups/ui-redesign.html`.

---

## WICHTIG — Build-Umgebung (vor JEDEM Gradle-Befehl)

Gradle muss auf JDK ≤ 21 laufen. In einer frischen PowerShell-Shell zuerst setzen:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
```

Standard-Build-/Test-Befehle (immer aus dem Projektwurzelverzeichnis, mit gesetztem `JAVA_HOME`):

```powershell
.\gradlew.bat assembleDebug         # Compose-Änderungen verifizieren (muss BUILD SUCCESSFUL)
.\gradlew.bat testDebugUnitTest      # bestehende JVM-Unit-Tests (müssen grün bleiben)
```

> Hinweis zu TDD: Dieses Vorhaben fügt **keine neue reine Logik** hinzu. Daher gibt es keine neuen JVM-Unit-Tests; die „Verifikation" jeder Task ist ein erfolgreicher `assembleDebug` plus weiterhin grüne `testDebugUnitTest`. Das entspricht der Projektkonvention (CLAUDE.md: „Compose UI is verified by a successful build/run, not red-green").

---

## Task 1: Fonts bündeln + Typografie

Fraunces (Display-Serife) und Hanken Grotesk (Body/UI) als statische TTF einbetten und die `Typography` darauf umstellen.

**Files:**
- Create: `app/src/main/res/font/fraunces_semibold.ttf`
- Create: `app/src/main/res/font/hanken_grotesk_regular.ttf`
- Create: `app/src/main/res/font/hanken_grotesk_medium.ttf`
- Create: `app/src/main/res/font/hanken_grotesk_semibold.ttf`
- Create: `app/src/main/res/font/hanken_grotesk_bold.ttf`
- Modify: `app/src/main/java/com/nwe/recipely/ui/theme/Type.kt`

- [ ] **Step 1: Schriftdateien beschaffen und ablegen**

Lade die **statischen** TTF-Schnitte von Google Fonts (nicht die Variable-Font-Datei — minSdk 24 unterstützt keine variablen Fonts in Compose):
- Fraunces: <https://fonts.google.com/specimen/Fraunces> → „Get font" → aus dem `static/`-Ordner `Fraunces_72pt-SemiBold.ttf` (oder `Fraunces-SemiBold.ttf`).
- Hanken Grotesk: <https://fonts.google.com/specimen/Hanken+Grotesk> → statische Schnitte Regular, Medium, SemiBold, Bold.

Lege sie unter `app/src/main/res/font/` mit **exakt diesen kleingeschriebenen Namen** ab (Android-Font-Ressourcennamen erlauben nur `a–z`, `0–9`, `_`):

```
app/src/main/res/font/fraunces_semibold.ttf
app/src/main/res/font/hanken_grotesk_regular.ttf
app/src/main/res/font/hanken_grotesk_medium.ttf
app/src/main/res/font/hanken_grotesk_semibold.ttf
app/src/main/res/font/hanken_grotesk_bold.ttf
```

- [ ] **Step 2: `Type.kt` ersetzen**

Ersetze den **gesamten** Inhalt von `app/src/main/java/com/nwe/recipely/ui/theme/Type.kt`:

```kotlin
package com.nwe.recipely.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.nwe.recipely.R

/** Distinctive display serif — recipe names, screen titles, section headers. */
val Fraunces = FontFamily(
    Font(R.font.fraunces_semibold, FontWeight.SemiBold),
)

/** Clean grotesque — body text, labels, fields, buttons. */
val Hanken = FontFamily(
    Font(R.font.hanken_grotesk_regular, FontWeight.Normal),
    Font(R.font.hanken_grotesk_medium, FontWeight.Medium),
    Font(R.font.hanken_grotesk_semibold, FontWeight.SemiBold),
    Font(R.font.hanken_grotesk_bold, FontWeight.Bold),
)

private val base = Typography()

/**
 * Everything defaults to Hanken Grotesk. The Fraunces serif is applied explicitly via
 * `fontFamily = Fraunces` on headings/titles in the composables that want the serif look.
 */
val AppTypography = Typography(
    displayLarge = base.displayLarge.copy(fontFamily = Hanken),
    displayMedium = base.displayMedium.copy(fontFamily = Hanken),
    displaySmall = base.displaySmall.copy(fontFamily = Hanken),
    headlineLarge = base.headlineLarge.copy(fontFamily = Hanken),
    headlineMedium = base.headlineMedium.copy(fontFamily = Hanken),
    headlineSmall = base.headlineSmall.copy(fontFamily = Hanken),
    titleLarge = base.titleLarge.copy(fontFamily = Hanken),
    titleMedium = base.titleMedium.copy(fontFamily = Hanken),
    titleSmall = base.titleSmall.copy(fontFamily = Hanken),
    bodyLarge = base.bodyLarge.copy(fontFamily = Hanken),
    bodyMedium = base.bodyMedium.copy(fontFamily = Hanken),
    bodySmall = base.bodySmall.copy(fontFamily = Hanken),
    labelLarge = base.labelLarge.copy(fontFamily = Hanken),
    labelMedium = base.labelMedium.copy(fontFamily = Hanken),
    labelSmall = base.labelSmall.copy(fontFamily = Hanken),
)
```

- [ ] **Step 3: Build verifizieren**

Run: `$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat assembleDebug`
Expected: `BUILD SUCCESSFUL`. (Schlägt der Build mit „resource font/… not found" fehl, fehlt eine TTF-Datei aus Step 1 oder ihr Name weicht ab.)

- [ ] **Step 4: Commit**

```powershell
git add app/src/main/res/font/ app/src/main/java/com/nwe/recipely/ui/theme/Type.kt
git commit -m "feat(theme): bundle Fraunces + Hanken Grotesk fonts and typography"
```

---

## Task 2: Warme Farbpalette + Theme + Shapes

Die grüne Palette durch die warme „Warm Editorial Kitchen"-Palette ersetzen (Light + Dark) und größere Eck-Radien als Theme-Shapes definieren. Dynamic Color bleibt deaktiviert.

**Files:**
- Modify (Ersetzen): `app/src/main/java/com/nwe/recipely/ui/theme/Color.kt`
- Modify (Ersetzen): `app/src/main/java/com/nwe/recipely/ui/theme/Theme.kt`

- [ ] **Step 1: `Color.kt` ersetzen**

Ersetze den **gesamten** Inhalt von `app/src/main/java/com/nwe/recipely/ui/theme/Color.kt`:

```kotlin
package com.nwe.recipely.ui.theme

import androidx.compose.ui.graphics.Color

// --- Light ---
val ForestPrimary = Color(0xFF1E3A2B)
val OnForest = Color(0xFFFFFFFF)
val ForestContainer = Color(0xFFC7E6C0)
val OnForestContainer = Color(0xFF06210F)
val Terracotta = Color(0xFFC75D3C)
val OnTerracotta = Color(0xFFFFFFFF)
val TerracottaContainer = Color(0xFFF7D9CC)
val OnTerracottaContainer = Color(0xFF3A1207)
val Honey = Color(0xFFE2A03C)
val OnHoney = Color(0xFF3A2600)
val HoneyContainer = Color(0xFFFBEAC9)
val OnHoneyContainer = Color(0xFF3A2600)
val Cream = Color(0xFFF6F0E6)
val CreamSurface = Color(0xFFFBF7EF)
val Ink = Color(0xFF21201B)
val CreamSurfaceVariant = Color(0xFFE8E0D2)
val OnSurfaceVariantLight = Color(0xFF57534A)
val OutlineLight = Color(0xFF8C8578)
val OutlineVariantLight = Color(0xFFDBD3C4)

// --- Dark ---
val ForestPrimaryDark = Color(0xFF9CD49A)
val OnForestDark = Color(0xFF06380F)
val ForestContainerDark = Color(0xFF2C5238)
val OnForestContainerDark = Color(0xFFC7E6C0)
val TerracottaDark = Color(0xFFF0A184)
val OnTerracottaDark = Color(0xFF5A1B0A)
val TerracottaContainerDark = Color(0xFF7A3422)
val OnTerracottaContainerDark = Color(0xFFF7D9CC)
val HoneyDark = Color(0xFFE9C07A)
val OnHoneyDark = Color(0xFF3A2600)
val HoneyContainerDark = Color(0xFF5E4316)
val OnHoneyContainerDark = Color(0xFFFBEAC9)
val DarkBg = Color(0xFF15170F)
val DarkSurface = Color(0xFF1F211A)
val OnDark = Color(0xFFE7E2D6)
val DarkSurfaceVariant = Color(0xFF44483D)
val OnSurfaceVariantDark = Color(0xFFC6C2B4)
val OutlineDark = Color(0xFF8F8B7E)
val OutlineVariantDark = Color(0xFF44483D)
```

- [ ] **Step 2: `Theme.kt` ersetzen**

Ersetze den **gesamten** Inhalt von `app/src/main/java/com/nwe/recipely/ui/theme/Theme.kt`:

```kotlin
package com.nwe.recipely.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = ForestPrimary,
    onPrimary = OnForest,
    primaryContainer = ForestContainer,
    onPrimaryContainer = OnForestContainer,
    secondary = Terracotta,
    onSecondary = OnTerracotta,
    secondaryContainer = TerracottaContainer,
    onSecondaryContainer = OnTerracottaContainer,
    tertiary = Honey,
    onTertiary = OnHoney,
    tertiaryContainer = HoneyContainer,
    onTertiaryContainer = OnHoneyContainer,
    background = Cream,
    onBackground = Ink,
    surface = CreamSurface,
    onSurface = Ink,
    surfaceVariant = CreamSurfaceVariant,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
)

private val DarkColors = darkColorScheme(
    primary = ForestPrimaryDark,
    onPrimary = OnForestDark,
    primaryContainer = ForestContainerDark,
    onPrimaryContainer = OnForestContainerDark,
    secondary = TerracottaDark,
    onSecondary = OnTerracottaDark,
    secondaryContainer = TerracottaContainerDark,
    onSecondaryContainer = OnTerracottaContainerDark,
    tertiary = HoneyDark,
    onTertiary = OnHoneyDark,
    tertiaryContainer = HoneyContainerDark,
    onTertiaryContainer = OnHoneyContainerDark,
    background = DarkBg,
    onBackground = OnDark,
    surface = DarkSurface,
    onSurface = OnDark,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun RecipelyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
```

- [ ] **Step 3: Build verifizieren**

Run: `$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

```powershell
git add app/src/main/java/com/nwe/recipely/ui/theme/Color.kt app/src/main/java/com/nwe/recipely/ui/theme/Theme.kt
git commit -m "feat(theme): warm color palette (light+dark) and rounded shapes"
```

---

## Task 3: Liste — `RecipeCard` + Listscreen umbauen

Zeilen-Liste durch Foto-Karten ersetzen; wiederverwendbaren `MetaChip` einführen; `LargeTopAppBar` + Extended FAB.

**Files:**
- Create: `app/src/main/java/com/nwe/recipely/ui/list/RecipeCard.kt`
- Modify (Ersetzen): `app/src/main/java/com/nwe/recipely/ui/list/RecipeListScreen.kt`

- [ ] **Step 1: `RecipeCard.kt` anlegen**

Erstelle `app/src/main/java/com/nwe/recipely/ui/list/RecipeCard.kt`:

```kotlin
package com.nwe.recipely.ui.list

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.nwe.recipely.R
import com.nwe.recipely.data.Recipe
import com.nwe.recipely.ui.theme.Fraunces
import java.io.File

@Composable
fun RecipeCard(recipe: Recipe, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f),
                contentAlignment = Alignment.Center,
            ) {
                val image = recipe.imageUri
                if (image != null) {
                    AsyncImage(
                        model = File(image),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Outlined.Restaurant,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(40.dp),
                        )
                    }
                }
            }
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = recipe.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontFamily = Fraunces,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                val hasMeta = recipe.prepTimeMinutes != null ||
                    recipe.servings != null ||
                    recipe.calories != null
                if (hasMeta) {
                    Row(
                        modifier = Modifier.padding(top = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        recipe.prepTimeMinutes?.let { MetaChip(stringResource(R.string.meta_time, it)) }
                        recipe.servings?.let { MetaChip(stringResource(R.string.meta_servings, it)) }
                        recipe.calories?.let {
                            MetaChip(stringResource(R.string.nutrition_value_kcal, it), accent = true)
                        }
                    }
                }
            }
        }
    }
}

/** Small rounded pill for a single meta fact. `accent` uses the honey tertiary tint (kcal). */
@Composable
fun MetaChip(text: String, accent: Boolean = false, modifier: Modifier = Modifier) {
    val bg = if (accent) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val fg = if (accent) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(color = bg, contentColor = fg, shape = RoundedCornerShape(10.dp), modifier = modifier) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        )
    }
}
```

- [ ] **Step 2: `RecipeListScreen.kt` ersetzen**

Ersetze den **gesamten** Inhalt von `app/src/main/java/com/nwe/recipely/ui/list/RecipeListScreen.kt`:

```kotlin
package com.nwe.recipely.ui.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nwe.recipely.R
import com.nwe.recipely.RecipelyApp
import com.nwe.recipely.ui.theme.Fraunces

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
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.list_title), fontFamily = Fraunces) },
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAdd,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.add_recipe)) },
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary,
            )
        },
    ) { padding ->
        if (recipes.isEmpty()) {
            EmptyState(Modifier.padding(padding).fillMaxSize())
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(recipes, key = { it.id }) { recipe ->
                    RecipeCard(recipe = recipe, onClick = { onOpen(recipe.id) })
                }
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
            modifier = Modifier.size(72.dp),
        )
        Text(
            text = stringResource(R.string.empty_title),
            style = MaterialTheme.typography.headlineSmall,
            fontFamily = Fraunces,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 16.dp),
        )
        Text(
            text = stringResource(R.string.empty_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}
```

> Hinweis: Die alten privaten Composables `RecipeRow` und die Funktion `recipeMeta` entfallen ersatzlos (durch `RecipeCard`/`MetaChip` ersetzt) — sie sind im obigen Ersatzinhalt bewusst nicht mehr enthalten.

- [ ] **Step 3: Build verifizieren**

Run: `$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

```powershell
git add app/src/main/java/com/nwe/recipely/ui/list/
git commit -m "feat(list): photo-card list with chips, large app bar and extended FAB"
```

---

## Task 4: Detail — Hero, Quick-Facts, abhakbare Zutaten, Schritt-Karten

Detailansicht umbauen. **Wichtig:** Die Nährwert-Logik (`NutritionFacts`, `perPortion`, `NutritionFormat` in `NutritionDisplay.kt`) bleibt **unverändert** — nur die Darstellung wird in eine Karte gehoben. Die „Stat-Leiste" wird als Quick-Facts-Chiprow (Zeit/Portionen) realisiert; die volle Nährwert-Anzeige mit Pro-Portion + Gesamt bleibt als eigene, gestylte Karte erhalten. Es werden **keine neuen Strings** gebraucht.

**Files:**
- Modify (Ersetzen): `app/src/main/java/com/nwe/recipely/ui/detail/RecipeDetailScreen.kt`
- Unverändert: `app/src/main/java/com/nwe/recipely/ui/detail/NutritionDisplay.kt`

- [ ] **Step 1: `RecipeDetailScreen.kt` ersetzen**

Ersetze den **gesamten** Inhalt von `app/src/main/java/com/nwe/recipely/ui/detail/RecipeDetailScreen.kt`:

```kotlin
package com.nwe.recipely.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import coil.compose.AsyncImage
import com.nwe.recipely.R
import com.nwe.recipely.RecipelyApp
import com.nwe.recipely.data.RecipeWithDetails
import com.nwe.recipely.data.Step
import com.nwe.recipely.ui.list.MetaChip
import com.nwe.recipely.ui.theme.Fraunces
import java.io.File

private val SidePadding = 20.dp

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

    Box(Modifier.fillMaxSize()) {
        val current = details
        if (current == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.recipe_not_found))
            }
        } else {
            DetailContent(current)
        }

        // Pinned, translucent nav overlay over the hero.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            OverlayIcon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back), onBack)
            if (current != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OverlayIcon(Icons.Default.Edit, stringResource(R.string.edit), onEdit)
                    OverlayIcon(Icons.Default.Delete, stringResource(R.string.delete)) { showDeleteDialog = true }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_dialog_title)) },
            text = { Text(stringResource(R.string.delete_dialog_text)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    vm.delete(onDeleted)
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@Composable
private fun OverlayIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    FilledIconButton(
        onClick = onClick,
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = Color(0x55000000),
            contentColor = Color.White,
        ),
    ) {
        Icon(icon, contentDescription = contentDescription)
    }
}

@Composable
private fun DetailContent(details: RecipeWithDetails, modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        item { Hero(name = details.recipe.name, imageUri = details.recipe.imageUri) }

        item { Spacer(Modifier.height(16.dp)) }

        item { QuickFacts(prepTime = details.recipe.prepTimeMinutes, servings = details.recipe.servings) }

        val facts = details.recipe.nutritionFacts()
        if (facts.hasAny) {
            item { SectionHeader(stringResource(R.string.section_nutrition)) }
            item { NutritionCard(facts = facts, servings = details.recipe.servings) }
        }

        if (details.ingredients.isNotEmpty()) {
            item { SectionHeader(stringResource(R.string.section_ingredients)) }
            val sorted = details.ingredients.sortedBy { it.position }
            item { IngredientsCard(items = sorted.map { it.id to it.text }) }
        }

        if (details.steps.isNotEmpty()) {
            item { SectionHeader(stringResource(R.string.section_steps)) }
            val sorted = details.steps.sortedBy { it.position }
            item { StepsColumn(sorted) }
        }

        item { Spacer(Modifier.height(32.dp)) }
    }
}

@Composable
private fun Hero(name: String, imageUri: String?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp),
    ) {
        if (imageUri != null) {
            AsyncImage(
                model = File(imageUri),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.Restaurant,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(72.dp),
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Transparent,
                        0.55f to Color.Transparent,
                        1.0f to Color(0xCC101610),
                    )
                ),
        )
        Text(
            text = name,
            style = MaterialTheme.typography.headlineMedium,
            fontFamily = Fraunces,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(SidePadding),
        )
    }
}

@Composable
private fun QuickFacts(prepTime: Int?, servings: Int?) {
    if (prepTime == null && servings == null) return
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = SidePadding),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        prepTime?.let { MetaChip(stringResource(R.string.meta_time, it)) }
        servings?.let { MetaChip(stringResource(R.string.chip_servings, it)) }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        fontFamily = Fraunces,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(start = SidePadding, end = SidePadding, top = 20.dp, bottom = 8.dp),
    )
}

@Composable
private fun NutritionCard(facts: NutritionFacts, servings: Int?) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = SidePadding),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val perPortion = facts.perPortion(servings)
            if (perPortion != null) {
                Text(
                    text = stringResource(R.string.nutrition_per_portion),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                NutritionRows(perPortion)
                Spacer(Modifier.height(8.dp))
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
            add(stringResource(R.string.nutrition_summary_part,
                stringResource(R.string.label_calories),
                stringResource(R.string.nutrition_value_kcal, it)))
        }
        facts.carbsGrams?.let {
            add(stringResource(R.string.nutrition_summary_part,
                stringResource(R.string.label_carbs),
                stringResource(R.string.nutrition_value_grams, NutritionFormat.grams(it))))
        }
        facts.proteinGrams?.let {
            add(stringResource(R.string.nutrition_summary_part,
                stringResource(R.string.label_protein),
                stringResource(R.string.nutrition_value_grams, NutritionFormat.grams(it))))
        }
        facts.fatGrams?.let {
            add(stringResource(R.string.nutrition_summary_part,
                stringResource(R.string.label_fat),
                stringResource(R.string.nutrition_value_grams, NutritionFormat.grams(it))))
        }
    }
    return "${stringResource(R.string.nutrition_total_prefix)} ${parts.joinToString(" · ")}"
}

@Composable
private fun IngredientsCard(items: List<Pair<Long, String>>) {
    // Ephemeral check state (not persisted) — resets when leaving the screen.
    val checked = remember { mutableStateMapOf<Long, Boolean>() }
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = SidePadding),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            items.forEach { (id, text) ->
                val isChecked = checked[id] ?: false
                IngredientRow(text = text, checked = isChecked, onToggle = { checked[id] = !isChecked })
            }
        }
    }
}

@Composable
private fun IngredientRow(text: String, checked: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = { onToggle() })
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = if (checked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            textDecoration = if (checked) TextDecoration.LineThrough else null,
        )
    }
}

@Composable
private fun StepsColumn(steps: List<Step>) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = SidePadding)) {
        steps.forEachIndexed { index, step ->
            StepRow(number = index + 1, step = step, isLast = index == steps.lastIndex)
        }
    }
}

@Composable
private fun StepRow(number: Int, step: Step, isLast: Boolean) {
    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.secondary),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = number.toString(),
                    color = MaterialTheme.colorScheme.onSecondary,
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = Fraunces,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.outlineVariant),
                )
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 14.dp, bottom = 20.dp),
        ) {
            if (step.text.isNotBlank()) {
                Text(step.text, style = MaterialTheme.typography.bodyLarge)
            }
            if (step.imageUri != null) {
                AsyncImage(
                    model = File(step.imageUri),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .fillMaxWidth()
                        .height(170.dp)
                        .clip(RoundedCornerShape(16.dp)),
                )
            }
        }
    }
}
```

- [ ] **Step 2: Build verifizieren**

Run: `$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```powershell
git add app/src/main/java/com/nwe/recipely/ui/detail/RecipeDetailScreen.kt
git commit -m "feat(detail): hero with title overlay, quick-facts, checkable ingredients, step cards"
```

---

## Task 5: Bearbeiten — Maske umstylen

Edit-Screen restylen. **Funktion bleibt identisch** (alle Felder inkl. Nährwert-Eingabe, Galerie/Kamera-Dialog, `discardChanges`/`BackHandler`, Speicherlogik unverändert). Nur Titelbild-Picker, Section-Header, „+"-Buttons und Schritt-Editor werden visuell überarbeitet.

**Files:**
- Modify (Ersetzen): `app/src/main/java/com/nwe/recipely/ui/edit/RecipeEditScreen.kt`

- [ ] **Step 1: `RecipeEditScreen.kt` ersetzen**

Ersetze den **gesamten** Inhalt von `app/src/main/java/com/nwe/recipely/ui/edit/RecipeEditScreen.kt`:

```kotlin
package com.nwe.recipely.ui.edit

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import coil.compose.AsyncImage
import com.nwe.recipely.R
import com.nwe.recipely.RecipelyApp
import com.nwe.recipely.data.ImageStore
import com.nwe.recipely.ui.theme.Fraunces
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

    BackHandler {
        vm.discardChanges()
        onClose()
    }

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
                title = {
                    Text(
                        if (recipeId == 0L) stringResource(R.string.new_recipe) else stringResource(R.string.edit_recipe),
                        fontFamily = Fraunces,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        vm.discardChanges()
                        onClose()
                    }) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cancel))
                    }
                },
                actions = {
                    Button(
                        enabled = state.canSave,
                        onClick = { vm.save(onClose) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary,
                        ),
                        modifier = Modifier.padding(end = 8.dp),
                    ) { Text(stringResource(R.string.save)) }
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
                    label = { Text(stringResource(R.string.label_name)) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = state.prepTime,
                        onValueChange = vm::setPrepTime,
                        label = { Text(stringResource(R.string.label_time)) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = state.servings,
                        onValueChange = vm::setServings,
                        label = { Text(stringResource(R.string.label_servings)) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            item { EditSectionHeader(stringResource(R.string.nutrition_optional)) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = state.calories,
                        onValueChange = vm::setCalories,
                        label = { Text(stringResource(R.string.label_calories_input)) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = state.carbs,
                        onValueChange = vm::setCarbs,
                        label = { Text(stringResource(R.string.label_carbs_input)) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
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
                        shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = state.fat,
                        onValueChange = vm::setFat,
                        label = { Text(stringResource(R.string.label_fat_input)) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            item { EditSectionHeader(stringResource(R.string.section_ingredients)) }
            items(state.ingredients.size) { index ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = state.ingredients[index].text,
                        onValueChange = { vm.setIngredient(index, it) },
                        label = { Text(stringResource(R.string.ingredient_n, index + 1)) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { vm.removeIngredient(index) }) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.remove_ingredient))
                    }
                }
            }
            item { AddButton(stringResource(R.string.add_ingredient), onClick = vm::addIngredient) }

            item { EditSectionHeader(stringResource(R.string.section_steps)) }
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
            item { AddButton(stringResource(R.string.add_step), onClick = vm::addStep) }

            item { Box(Modifier.height(24.dp)) }
        }
    }

    if (showSourceDialog) {
        AlertDialog(
            onDismissRequest = { showSourceDialog = false },
            title = { Text(stringResource(R.string.add_image_dialog_title)) },
            text = { Text(stringResource(R.string.add_image_dialog_text)) },
            confirmButton = {
                TextButton(onClick = {
                    showSourceDialog = false
                    pickImage.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }) { Text(stringResource(R.string.gallery)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    showSourceDialog = false
                    val (uri, path) = imageStore.createCameraTarget()
                    cameraTargetPath = path
                    takePhoto.launch(uri)
                }) { Text(stringResource(R.string.camera)) }
            },
        )
    }
}

/** Dashed rounded outline used by the "add" buttons. */
private fun Modifier.dashedBorder(color: Color, cornerRadius: Dp, strokeWidth: Dp = 1.5.dp): Modifier =
    this.drawBehind {
        val r = cornerRadius.toPx()
        drawRoundRect(
            color = color,
            cornerRadius = CornerRadius(r, r),
            style = Stroke(
                width = strokeWidth.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f),
            ),
        )
    }

@Composable
private fun AddButton(text: String, onClick: () -> Unit) {
    val tint = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .dashedBorder(tint.copy(alpha = 0.6f), 16.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.Add, contentDescription = null, tint = tint)
        Spacer(Modifier.width(8.dp))
        Text(text, color = tint, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun TitleImagePicker(imagePath: String?, onPick: () -> Unit, onRemove: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
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
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilledIconButton(
                    onClick = onPick,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Color(0x66000000),
                        contentColor = Color.White,
                    ),
                ) { Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.add_image_dialog_title)) }
                FilledIconButton(
                    onClick = onRemove,
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Color(0x66000000),
                        contentColor = Color.White,
                    ),
                ) { Icon(Icons.Default.Close, contentDescription = stringResource(R.string.remove_image)) }
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Outlined.AddPhotoAlternate,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(36.dp),
                )
                Text(
                    stringResource(R.string.title_image_hint),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
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
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.secondary),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    number.toString(),
                    color = MaterialTheme.colorScheme.onSecondary,
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = Fraunces,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                label = { Text(stringResource(R.string.step_n, number)) },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.remove_step))
            }
        }
        if (imagePath != null) {
            Box(modifier = Modifier.fillMaxWidth()) {
                AsyncImage(
                    model = File(imagePath),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(14.dp)),
                )
                FilledIconButton(
                    onClick = onRemoveImage,
                    modifier = Modifier.align(Alignment.TopEnd).padding(6.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Color(0x66000000),
                        contentColor = Color.White,
                    ),
                ) { Icon(Icons.Default.Close, contentDescription = stringResource(R.string.remove_step_image)) }
            }
        } else {
            AddImageButton(onClick = onAddImage)
        }
    }
}

@Composable
private fun AddImageButton(onClick: () -> Unit) {
    val tint = MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .dashedBorder(tint.copy(alpha = 0.5f), 14.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = tint)
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.add_step_image), color = tint, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun EditSectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        fontFamily = Fraunces,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(top = 8.dp),
    )
}
```

- [ ] **Step 2: Build verifizieren**

Run: `$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat assembleDebug`
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```powershell
git add app/src/main/java/com/nwe/recipely/ui/edit/RecipeEditScreen.kt
git commit -m "feat(edit): restyle edit form (title picker, rounded fields, dashed add buttons, step cards)"
```

---

## Task 6: Gesamtverifikation auf Gerät/Emulator

Sicherstellen, dass Tests grün sind und die App real korrekt läuft (Compose-Konvention: visuelle Verifikation).

**Files:** keine.

- [ ] **Step 1: Unit-Tests laufen lassen**

Run: `$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat testDebugUnitTest`
Expected: `BUILD SUCCESSFUL`, alle bestehenden Tests grün (Mapping-/ViewModel-Tests sind von der UI-Änderung unberührt).

- [ ] **Step 2: Auf Gerät/Emulator installieren und prüfen**

Run: `$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat installDebug`
Dann die App öffnen und visuell gegen `docs/mockups/ui-redesign.html` prüfen:
- Liste: Foto-Karten 16:9, Chips (Zeit/Portionen/kcal), große Fraunces-Headline, Extended FAB in Terrakotta, Karte ohne Bild zeigt Platzhalter.
- Detail: Hero mit Titel-Overlay + Scrim, runde Nav-Buttons, Quick-Facts-Chips, Nährwert-Karte (Pro Portion + Gesamt korrekt), abhakbare Zutaten, Schritt-Karten mit Verbindungslinie.
- Bearbeiten: gerundeter Titelbild-Picker mit Overlay-Steuerung, Nährwert-Felder als 2×2, gestrichelte „+"-Buttons, Schritt-Editor-Karten.
- Dark Mode (Systemeinstellung umschalten) prüfen.

- [ ] **Step 3: Etwaige Feinkorrekturen committen** (nur falls nötig)

```powershell
git add -A
git commit -m "fix(ui): polish after on-device review"
```

---

## Selbstreview (vom Plan-Autor durchgeführt)

- **Spec-Abdeckung:** Schriften (Task 1), Farben/Shapes (Task 2), Liste mit Karten/Chips/LargeTopAppBar/Extended-FAB (Task 3), Detail-Hero/Statleiste-als-Quick-Facts/Nährwert-Karte/abhakbare Zutaten/Schritt-Karten (Task 4), Edit-Restyle inkl. Nährwert-Eingabe-Raster (Task 5), Verifikation (Task 6). ✔
- **Bewusste Interpretationen ggü. Spec/Mockup:**
  - Die Detail-„Stat-Leiste" wird als Quick-Facts-Chiprow (Zeit/Portionen) realisiert; die volle Nährwert-Anzeige bleibt als eigene Karte mit **unveränderter** Pro-Portion-/Gesamt-Logik. So bleibt „keine neuen Strings" und „Logik unverändert" gewahrt.
  - Der Hero ist als fester, mitscrollender Hero mit fixierter, halbtransparenter Nav-Leiste umgesetzt (robuste, abhängigkeitsarme Variante des „Collapsing-Hero"-Konzepts).
- **Keine Datenmodell-Änderung**, keine ViewModel-/Repository-/Room-/DAO-Änderung, keine Navigationsänderung. ✔
- **Platzhalter-Scan:** keine TBD/TODO; vollständiger Code je Schritt. ✔
- **Typkonsistenz:** `MetaChip` (public, in `ui/list/RecipeCard.kt`) wird in Liste und Detail genutzt; `Fraunces`/`Hanken` (public, in `ui/theme/Type.kt`) konsistent referenziert; Farb-Vals in `Color.kt` decken alle in `Theme.kt` gemappten Rollen ab. ✔
```
