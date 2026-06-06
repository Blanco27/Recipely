# UI-Angleichung an das Mockup — Implementierungsplan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Die laufende Compose-App pixelnah an `docs/mockups/ui-redesign.html` angleichen (Papier-Ton, weiche Terrakotta-Felder, eckige ✕-Buttons, Detail-Sheet + Grabber, Pillen-Kategorien, Moos-Häkchen u. a.).

**Architecture:** Rein kosmetisch. Neue Theme-Token in `ui/theme/Color.kt` + drei wiederverwendbare Composables in einem neuen Paket `ui/components/`, danach werden die drei Screens darauf umgestellt. Keine Änderung an Daten, Navigation, ViewModels.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, Coil. Build via `.\gradlew.bat` mit `JAVA_HOME` = Android-Studio-JBR 21.

**Verifikation:** Compose-UI wird per erfolgreichem Build verifiziert (kein Rot-Grün-TDD, siehe CLAUDE.md). Standard-Buildbefehl in jeder PowerShell-Session:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat assembleDebug
```

---

## Task 1: Theme-Token (Papier + weicher Rahmen)

**Files:**
- Modify: `app/src/main/java/com/nwe/recipely/ui/theme/Color.kt`

- [ ] **Step 1: Token am Ende von `Color.kt` ergänzen**

Direkt nach dem `// --- Accent extras ---`-Block (nach `val Forest2 = ...`) einfügen:

```kotlin
// --- Paper: elevated near-white surface for cards/fields/chips (mockup --paper #FFFDF9) ---
val Paper = Color(0xFFFFFDF9)
val PaperDark = Color(0xFF26281F)

// --- Soft warm border for fields/boxed elements (mockup --line #E6DECF) ---
val FieldBorderLight = Color(0xFFE6DECF)
val FieldBorderDark = Color(0xFF45493D)
```

- [ ] **Step 2: Chip-Hintergrund auf Creme angleichen (#19)**

In `Color.kt` den Wert von `MetaChipBgLight` ändern:

```kotlin
val MetaChipBgLight = Color(0xFFF6F0E6)
```

(vorher `0xFFF3ECDD`).

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/nwe/recipely/ui/theme/Color.kt
git commit -m "feat(theme): add paper tone + soft field border tokens"
```

---

## Task 2: `RecipelyTextField`-Composable

**Files:**
- Create: `app/src/main/java/com/nwe/recipely/ui/components/RecipelyTextField.kt`

- [ ] **Step 1: Datei anlegen**

```kotlin
package com.nwe.recipely.ui.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.nwe.recipely.ui.theme.FieldBorderDark
import com.nwe.recipely.ui.theme.FieldBorderLight
import com.nwe.recipely.ui.theme.Paper
import com.nwe.recipely.ui.theme.PaperDark

/**
 * Outlined text field styled like the mockup `.tf`: near-white paper fill, soft warm
 * border, terracotta focus (border + label). Replaces the raw OutlinedTextField on the
 * edit screen so all fields lift off the cream background uniformly.
 */
@Composable
fun RecipelyTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = false,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    val dark = isSystemInDarkTheme()
    val paper = if (dark) PaperDark else Paper
    val border = if (dark) FieldBorderDark else FieldBorderLight
    val terra = MaterialTheme.colorScheme.secondary
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = singleLine,
        shape = RoundedCornerShape(16.dp),
        keyboardOptions = keyboardOptions,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = paper,
            unfocusedContainerColor = paper,
            focusedBorderColor = terra,
            unfocusedBorderColor = border,
            focusedLabelColor = terra,
        ),
        modifier = modifier,
    )
}
```

---

## Task 3: Icon-Button-Composables + Build-Check

**Files:**
- Create: `app/src/main/java/com/nwe/recipely/ui/components/IconButtons.kt`

- [ ] **Step 1: Datei anlegen**

```kotlin
package com.nwe.recipely.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nwe.recipely.ui.theme.FieldBorderDark
import com.nwe.recipely.ui.theme.FieldBorderLight
import com.nwe.recipely.ui.theme.Ink
import com.nwe.recipely.ui.theme.Paper
import com.nwe.recipely.ui.theme.PaperDark

/**
 * Mockup `.del` / `.x`: a paper square (or circle) with a soft warm border and a tinted
 * icon. Used for ingredient/step delete (terracotta icon) and the edit top-bar close
 * (circle, ink icon).
 */
@Composable
fun BoxedIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    circle: Boolean = false,
    size: Dp = 42.dp,
    tint: Color = MaterialTheme.colorScheme.secondary,
) {
    val dark = isSystemInDarkTheme()
    val paper = if (dark) PaperDark else Paper
    val border = if (dark) FieldBorderDark else FieldBorderLight
    val shape = if (circle) CircleShape else RoundedCornerShape(13.dp)
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(paper)
            .border(1.dp, border, shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint)
    }
}

/**
 * Mockup `.hero-nav .icon` / `.titledrop .ctl`: a light translucent circle that reads over
 * a photo. `solid = true` is the near-opaque paper variant with a dark icon (title-image
 * controls); the default is a frosted white-translucent circle with a white icon (hero nav).
 */
@Composable
fun FrostedIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    solid: Boolean = false,
) {
    val dark = isSystemInDarkTheme()
    val bg = when {
        solid && dark -> PaperDark.copy(alpha = 0.85f)
        solid -> Paper.copy(alpha = 0.85f)
        else -> Color.White.copy(alpha = 0.22f)
    }
    val borderColor = if (solid) Color.White.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.35f)
    val tint = if (solid) (if (dark) Color.White else Ink) else Color.White
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(bg)
            .border(1.dp, borderColor, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint)
    }
}
```

- [ ] **Step 2: Build, um die neuen Composables zu prüfen**

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat assembleDebug
```
Erwartet: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/nwe/recipely/ui/components/
git commit -m "feat(ui): add RecipelyTextField, BoxedIconButton, FrostedIconButton"
```

---

## Task 4: Bearbeiten-Screen umstellen

**Files:**
- Modify: `app/src/main/java/com/nwe/recipely/ui/edit/RecipeEditScreen.kt`

- [ ] **Step 1: Imports ergänzen**

Folgende Imports oben hinzufügen:

```kotlin
import androidx.compose.foundation.isSystemInDarkTheme
import com.nwe.recipely.ui.components.BoxedIconButton
import com.nwe.recipely.ui.components.FrostedIconButton
import com.nwe.recipely.ui.components.RecipelyTextField
import com.nwe.recipely.ui.theme.Forest2
import com.nwe.recipely.ui.theme.Moss
import com.nwe.recipely.ui.theme.Paper
import com.nwe.recipely.ui.theme.PaperDark
```

- [ ] **Step 2: Schließen-Button (Top-Bar) auf umrandeten Papier-Kreis umstellen (#7)**

Den `navigationIcon`-Block im `TopAppBar` ersetzen:

```kotlin
                navigationIcon = {
                    BoxedIconButton(
                        icon = Icons.Default.Close,
                        contentDescription = stringResource(R.string.cancel),
                        onClick = {
                            vm.discardChanges()
                            onClose()
                        },
                        modifier = Modifier.padding(start = 8.dp),
                        circle = true,
                        size = 38.dp,
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                },
```

- [ ] **Step 3: Alle Eingabefelder auf `RecipelyTextField` umstellen (#1–#4)**

Name-Feld:

```kotlin
            item {
                RecipelyTextField(
                    value = state.name,
                    onValueChange = vm::setName,
                    label = stringResource(R.string.label_name),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
```

Zeit/Portionen-Zeile:

```kotlin
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    RecipelyTextField(
                        value = state.prepTime,
                        onValueChange = vm::setPrepTime,
                        label = stringResource(R.string.label_time),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                    RecipelyTextField(
                        value = state.servings,
                        onValueChange = vm::setServings,
                        label = stringResource(R.string.label_servings),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
```

Nährwerte-Zeile 1 (Kalorien/Kohlenhydrate):

```kotlin
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    RecipelyTextField(
                        value = state.calories,
                        onValueChange = vm::setCalories,
                        label = stringResource(R.string.label_calories_input),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                    RecipelyTextField(
                        value = state.carbs,
                        onValueChange = vm::setCarbs,
                        label = stringResource(R.string.label_carbs_input),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
```

Nährwerte-Zeile 2 (Eiweiß/Fett):

```kotlin
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    RecipelyTextField(
                        value = state.protein,
                        onValueChange = vm::setProtein,
                        label = stringResource(R.string.label_protein_input),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                    )
                    RecipelyTextField(
                        value = state.fat,
                        onValueChange = vm::setFat,
                        label = stringResource(R.string.label_fat_input),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
```

Zutaten-Feld + eckiger ✕-Button (#5) — den `items(state.ingredients.size)`-Block ersetzen:

```kotlin
            items(state.ingredients.size) { index ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    RecipelyTextField(
                        value = state.ingredients[index].text,
                        onValueChange = { vm.setIngredient(index, it) },
                        label = stringResource(R.string.ingredient_n, index + 1),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    BoxedIconButton(
                        icon = Icons.Default.Close,
                        contentDescription = stringResource(R.string.remove_ingredient),
                        onClick = { vm.removeIngredient(index) },
                    )
                }
            }
```

- [ ] **Step 4: `StepEditor` umstellen — Feld, eckiger ✕ (#5), 45dp-Einrückung (#10)**

Die `StepEditor`-Funktion komplett ersetzen:

```kotlin
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
    val dark = isSystemInDarkTheme()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(if (dark) PaperDark else Paper)
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
            RecipelyTextField(
                value = text,
                onValueChange = onTextChange,
                label = stringResource(R.string.step_n, number),
                modifier = Modifier.weight(1f),
            )
            BoxedIconButton(
                icon = Icons.Default.Close,
                contentDescription = stringResource(R.string.remove_step),
                onClick = onRemove,
                size = 34.dp,
            )
        }
        // Image / add-image are inset by 45dp to line up under the text field (past the number badge).
        if (imagePath != null) {
            Box(modifier = Modifier.fillMaxWidth().padding(start = 45.dp)) {
                AsyncImage(
                    model = File(imagePath),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(14.dp)),
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
            AddImageButton(onClick = onAddImage, modifier = Modifier.padding(start = 45.dp))
        }
    }
}
```

- [ ] **Step 5: `AddImageButton` um `modifier`-Parameter erweitern**

Signatur und `Row`-Modifier von `AddImageButton` anpassen:

```kotlin
@Composable
private fun AddImageButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val tint = MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = modifier
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
```

- [ ] **Step 6: `AddButton` auf Moos-Grün umstellen (#11)**

Die `AddButton`-Funktion ersetzen:

```kotlin
@Composable
private fun AddButton(text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Moss.copy(alpha = 0.07f))
            .dashedBorder(Moss.copy(alpha = 0.6f), 16.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.Add, contentDescription = null, tint = Moss)
        Spacer(Modifier.width(8.dp))
        Text(text, color = Forest2, fontWeight = FontWeight.Medium)
    }
}
```

- [ ] **Step 7: Kategorie-Chips auf volle Pillen umstellen (#6)**

In `CategoryChip` die `border`-Zeile, `Surface`-Form und unselektierten Hintergrund anpassen:

```kotlin
@Composable
private fun CategoryChip(emoji: String, label: String, selected: Boolean, onClick: () -> Unit) {
    val dark = isSystemInDarkTheme()
    val bg = if (selected) MaterialTheme.colorScheme.primary else (if (dark) PaperDark else Paper)
    val fg = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val border = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
    Surface(
        color = bg,
        contentColor = fg,
        shape = RoundedCornerShape(100.dp),
        border = BorderStroke(1.5.dp, border),
        modifier = Modifier.toggleable(
            value = selected,
            role = Role.Checkbox,
            onValueChange = { onClick() },
        ),
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

- [ ] **Step 8: Titelbild — helle Steuer-Buttons + Label + Scrim (#8)**

Im `TitleImagePicker` den `if (imagePath != null) { ... }`-Zweig (AsyncImage + die beiden `FilledIconButton`-Row) ersetzen durch:

```kotlin
        if (imagePath != null) {
            AsyncImage(
                model = File(imagePath),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            // Bottom scrim for label legibility (mockup .scrim2).
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        androidx.compose.ui.graphics.Brush.verticalGradient(
                            0.45f to Color.Transparent,
                            1f to Color(0x8C141E16),
                        )
                    )
            )
            Text(
                text = "📷 " + stringResource(R.string.title_image_label),
                color = Color.White,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(14.dp),
            )
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FrostedIconButton(
                    icon = Icons.Default.Edit,
                    contentDescription = stringResource(R.string.add_image_dialog_title),
                    onClick = onPick,
                    solid = true,
                )
                FrostedIconButton(
                    icon = Icons.Default.Close,
                    contentDescription = stringResource(R.string.remove_image),
                    onClick = onRemove,
                    solid = true,
                )
            }
        } else {
```

(Der `else { Column(...) }`-Block für den leeren Zustand bleibt unverändert.)

- [ ] **Step 9: Neuen String `title_image_label` anlegen**

In `app/src/main/res/values/strings.xml` im Edit-Block ergänzen:

```xml
    <string name="title_image_label">Title image</string>
```

In `app/src/main/res/values-de/strings.xml` im Edit-Block ergänzen:

```xml
    <string name="title_image_label">Titelbild</string>
```

- [ ] **Step 10: Build prüfen**

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat assembleDebug
```
Erwartet: `BUILD SUCCESSFUL`.

- [ ] **Step 11: Commit**

```bash
git add app/src/main/java/com/nwe/recipely/ui/edit/RecipeEditScreen.kt app/src/main/res/values/strings.xml app/src/main/res/values-de/strings.xml
git commit -m "feat(edit): paper terracotta fields, boxed X buttons, pill categories, moss add-buttons, inset step images, frosted title controls"
```

---

## Task 5: Detail-Screen umstellen

**Files:**
- Modify: `app/src/main/java/com/nwe/recipely/ui/detail/RecipeDetailScreen.kt`

- [ ] **Step 1: Imports ergänzen**

```kotlin
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.filled.Check
import androidx.compose.ui.semantics.Role
import com.nwe.recipely.data.RecipeCategory
import com.nwe.recipely.ui.components.FrostedIconButton
import com.nwe.recipely.ui.theme.Honey
import com.nwe.recipely.ui.theme.Moss
import com.nwe.recipely.ui.theme.Paper
import com.nwe.recipely.ui.theme.PaperDark
```

- [ ] **Step 2: Overlay-Nav auf `FrostedIconButton` umstellen (#14)**

Die `OverlayIcon`-Funktion ersetzen:

```kotlin
@Composable
private fun OverlayIcon(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    FrostedIconButton(icon = icon, contentDescription = contentDescription, onClick = onClick)
}
```

- [ ] **Step 3: `DetailContent` auf Hero + Sheet umbauen (#12)**

Die `DetailContent`-Funktion ersetzen:

```kotlin
@Composable
private fun DetailContent(details: RecipeWithDetails, modifier: Modifier = Modifier) {
    val sortedIngredients = details.ingredients.sortedBy { it.position }
    val sortedSteps = details.steps.sortedBy { it.position }
    // Ephemeral check state (not persisted) — resets when leaving the screen.
    val checked = remember { mutableStateMapOf<Long, Boolean>() }
    val category = RecipeCategory.fromKey(details.recipe.category)

    LazyColumn(modifier = modifier.fillMaxSize()) {
        item {
            Hero(
                name = details.recipe.name,
                imageUri = details.recipe.imageUri,
                categoryLabel = category?.let { stringResource(it.labelRes) },
            )
        }
        item {
            DetailSheet(
                details = details,
                ingredients = sortedIngredients,
                steps = sortedSteps,
                checked = checked,
            )
        }
    }
}

/** Rounded content sheet pulled up over the hero (mockup .sheet) with a grabber handle. */
@Composable
private fun DetailSheet(
    details: RecipeWithDetails,
    ingredients: List<com.nwe.recipely.data.Ingredient>,
    steps: List<Step>,
    checked: SnapshotStateMap<Long, Boolean>,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = (-26).dp)
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(top = 14.dp, bottom = 8.dp),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 16.dp)
                .size(width = 42.dp, height = 5.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(MaterialTheme.colorScheme.outlineVariant),
        )

        StatGrid(
            prepTime = details.recipe.prepTimeMinutes,
            servings = details.recipe.servings,
            calories = details.recipe.calories,
            protein = details.recipe.proteinGrams,
        )

        val facts = details.recipe.nutritionFacts()
        if (facts.hasAny) {
            SectionHeader(stringResource(R.string.section_nutrition))
            NutritionCard(facts = facts, servings = details.recipe.servings)
        }

        if (ingredients.isNotEmpty()) {
            val done = ingredients.count { checked[it.id] == true }
            SectionHeader(
                text = stringResource(R.string.section_ingredients),
                meta = stringResource(R.string.ingredients_done, done, ingredients.size),
            )
            IngredientsCard(items = ingredients.map { it.id to it.text }, checked = checked)
        }

        if (steps.isNotEmpty()) {
            SectionHeader(
                text = stringResource(R.string.section_steps),
                meta = pluralStringResource(R.plurals.steps_count, steps.size, steps.size),
            )
            StepsColumn(steps)
        }

        Spacer(Modifier.height(24.dp))
    }
}
```

Hinweis: `Ingredient` wird hier voll qualifiziert (`com.nwe.recipely.data.Ingredient`), um keinen weiteren Import nötig zu machen. `SnapshotStateMap` ist bereits importiert.

- [ ] **Step 4: `Hero` um Eyebrow, Größe und oberen Scrim erweitern (#13, #16, #17)**

Die `Hero`-Funktion ersetzen:

```kotlin
@Composable
private fun Hero(name: String, imageUri: String?, categoryLabel: String?) {
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
                        0.0f to Color(0x66101610), // top scrim for status/nav icon legibility
                        0.25f to Color.Transparent,
                        0.55f to Color.Transparent,
                        1.0f to Color(0xCC101610),
                    )
                ),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(SidePadding),
        ) {
            if (categoryLabel != null) {
                Text(
                    text = categoryLabel.uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    color = Honey,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
            }
            Text(
                text = name,
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 32.sp),
                fontFamily = Fraunces,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
            )
        }
    }
}
```

- [ ] **Step 5: Stat-Karten auf Paper (#17)**

In `StatCard` die `bg`-Zeile ersetzen:

```kotlin
    val bg = if (stat.accent) cs.primary else (if (isSystemInDarkTheme()) PaperDark else Paper)
```

- [ ] **Step 6: Zutaten-Häkchen auf Moos-Box umstellen (#15)**

`IngredientRow` ersetzen und eine `MossCheckBox` ergänzen:

```kotlin
@Composable
private fun IngredientRow(text: String, checked: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = { onToggle() })
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        MossCheckBox(checked = checked)
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = if (checked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            textDecoration = if (checked) TextDecoration.LineThrough else null,
        )
    }
}

/** Mockup .ing .box: a 22dp rounded square, moss border, fills moss with a check when done. */
@Composable
private fun MossCheckBox(checked: Boolean) {
    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(if (checked) Moss else Color.Transparent)
            .border(2.dp, Moss, RoundedCornerShape(7.dp)),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}
```

- [ ] **Step 7: Schritt-Bild-Höhe auf 130dp (#17)**

In `StepRow` die `AsyncImage`-Höhe von `170.dp` auf `130.dp` ändern:

```kotlin
                        .height(130.dp)
```

- [ ] **Step 8: Build prüfen**

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat assembleDebug
```
Erwartet: `BUILD SUCCESSFUL`.

- [ ] **Step 9: Commit**

```bash
git add app/src/main/java/com/nwe/recipely/ui/detail/RecipeDetailScreen.kt
git commit -m "feat(detail): sheet + grabber over hero, category eyebrow, top scrim, frosted nav, moss checkboxes, paper stat cards"
```

---

## Task 6: Liste umstellen

**Files:**
- Modify: `app/src/main/java/com/nwe/recipely/ui/list/RecipeCard.kt`
- Modify: `app/src/main/java/com/nwe/recipely/ui/list/RecipeListScreen.kt`

- [ ] **Step 1: Karten auf Paper + Radius 26 (#18) — `RecipeCard.kt`**

Imports ergänzen:

```kotlin
import com.nwe.recipely.ui.theme.Paper
import com.nwe.recipely.ui.theme.PaperDark
```

Im `Card`-Aufruf `shape` und `containerColor` anpassen:

```kotlin
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSystemInDarkTheme()) PaperDark else Paper,
        ),
```

(`isSystemInDarkTheme` ist in `RecipeCard.kt` bereits importiert.)

- [ ] **Step 2: Inaktive Filter-Pillen auf Paper + FAB-Radius 20 (#21) — `RecipeListScreen.kt`**

Imports ergänzen:

```kotlin
import androidx.compose.foundation.isSystemInDarkTheme
import com.nwe.recipely.ui.theme.Paper
import com.nwe.recipely.ui.theme.PaperDark
```

In `FilterPill` die `bg`-Zeile ersetzen:

```kotlin
    val bg = if (selected) MaterialTheme.colorScheme.primary else (if (isSystemInDarkTheme()) PaperDark else Paper)
```

Im `ExtendedFloatingActionButton` die `shape` ergänzen (z. B. nach `contentColor = ...`):

```kotlin
                shape = RoundedCornerShape(20.dp),
```

Und den Import dafür ergänzen, falls nicht vorhanden:

```kotlin
import androidx.compose.foundation.shape.RoundedCornerShape
```

- [ ] **Step 3: Eyebrow „Offline ·" verifizieren (#20)**

Keine Code-Änderung nötig — `plurals/recipe_count` enthält bereits „Offline · %d …". Nur prüfen, dass `ListHeader` weiterhin `pluralStringResource(R.plurals.recipe_count, count, count).uppercase()` nutzt.

- [ ] **Step 4: Build prüfen**

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat assembleDebug
```
Erwartet: `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/nwe/recipely/ui/list/RecipeCard.kt app/src/main/java/com/nwe/recipely/ui/list/RecipeListScreen.kt
git commit -m "feat(list): paper cards + pills, card radius 26, FAB radius 20"
```

---

## Task 7: Gesamt-Verifikation

**Files:** keine

- [ ] **Step 1: Voller Build + Unit-Tests**

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat assembleDebug testDebugUnitTest
```
Erwartet: `BUILD SUCCESSFUL` und alle bestehenden Unit-Tests grün (reine Logik unverändert).

- [ ] **Step 2: Visuelle Kontrolle (Gerät/Emulator) gegen das Mockup**

`./gradlew installDebug`, dann pro Screen in **Hell- und Dunkelmodus** prüfen:
- Liste: Karten heben sich als Papier ab, Radius weicher, Creme-Chips, FAB-Pille, „OFFLINE · N REZEPTE".
- Detail: Sheet zieht über das Hero mit Grabber; Kategorie-Eyebrow (Honig) über dem Titel; helle, matte Nav-Buttons; Moos-Box-Häkchen haken moosgrün ab; obere Icons trotz hellem Foto lesbar.
- Bearbeiten: Felder weiß auf Creme mit weichem Rahmen, Fokus terrakotta; eckige ✕-Buttons; Schließen-Kreis; Kategorie-Pillen; moosgrüne „Hinzufügen"-Buttons; Schritt-Bild eingerückt; helle Titelbild-Steuerung + „📷 Titelbild"-Label.

- [ ] **Step 3: Branch abschließen**

REQUIRED SUB-SKILL: `superpowers:finishing-a-development-branch` — Optionen für Merge/PR/Cleanup von `ui-mockup-alignment`.
```
