# Settings About Section Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a quiet-footer "About" section to the bottom of the Settings screen showing the app version, a GitHub repo link, and an open-source note.

**Architecture:** A new self-contained `AboutFooter` composable is rendered at the bottom of `SettingsScreen`, pushed down by a `Spacer(weight(1f))`. It reads the version via `PackageManager` (no `BuildConfig`), opens the repo with an `ACTION_VIEW` intent, and uses a GitHub vector drawable + existing theme tokens. Follows the existing `ThemePanel`/`LanguagePanel` decomposition.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, Android `PackageManager`/`Intent`, Android vector drawable. Visual source of truth: `docs/mockups/settings-about.html` (Variant C).

**Testing note:** Per project convention (`CLAUDE.md`), Compose UI is verified by a successful build/run, not red-green TDD. No new pure logic or ViewModel branch is introduced (the version read is a direct platform call in the composable), so no JVM unit test is added. Each task ends with a build and, for the final task, manual on-device verification.

**Environment note:** Every CLI Gradle command must run with the JBR 21 toolchain. In a fresh PowerShell shell, set this once before building:
```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
```

## File Structure

- **Create:** `app/src/main/res/drawable/ic_github.xml` — GitHub octocat logo as a vector drawable (tinted at the call site).
- **Create:** `app/src/main/java/com/nwe/recipely/ui/settings/AboutFooter.kt` — the `AboutFooter` composable + a private `openUrl` helper and the repo URL constant.
- **Modify:** `app/src/main/res/values/strings.xml` — add 4 English `about_*` strings.
- **Modify:** `app/src/main/res/values-de/strings.xml` — add the 4 German translations.
- **Modify:** `app/src/main/java/com/nwe/recipely/ui/settings/SettingsScreen.kt` — add a weight spacer + `AboutFooter()` at the bottom of the root `Column`.

---

### Task 1: GitHub vector drawable

**Files:**
- Create: `app/src/main/res/drawable/ic_github.xml`

- [ ] **Step 1: Create the vector drawable**

The mockup uses the GitHub octocat glyph. Add it as a vector drawable with a white fill so the `Icon` composable can recolor it via `tint`. The path data is the GitHub mark on a 16×16 viewport.

Create `app/src/main/res/drawable/ic_github.xml`:

```xml
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="16"
    android:viewportHeight="16">
    <path
        android:fillColor="#FFFFFFFF"
        android:pathData="M8 0C3.58 0 0 3.58 0 8c0 3.54 2.29 6.53 5.47 7.59.4.07.55-.17.55-.38 0-.19-.01-.82-.01-1.49-2.01.37-2.53-.49-2.69-.94-.09-.23-.48-.94-.82-1.13-.28-.15-.68-.52-.01-.53.63-.01 1.08.58 1.23.82.72 1.21 1.87.87 2.33.66.07-.52.28-.87.51-1.07-1.78-.2-3.64-.89-3.64-3.95 0-.87.31-1.59.82-2.15-.08-.2-.36-1.02.08-2.12 0 0 .67-.21 2.2.82.64-.18 1.32-.27 2-.27.68 0 1.36.09 2 .27 1.53-1.04 2.2-.82 2.2-.82.44 1.1.16 1.92.08 2.12.51.56.82 1.27.82 2.15 0 3.07-1.87 3.75-3.65 3.95.29.25.54.73.54 1.48 0 1.07-.01 1.93-.01 2.2 0 .21.15.46.55.38A8.01 8.01 0 0 0 16 8c0-4.42-3.58-8-8-8z" />
</vector>
```

- [ ] **Step 2: Verify it compiles**

Run:
```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat :app:processDebugResources
```
Expected: `BUILD SUCCESSFUL`. A malformed `pathData` would fail resource processing here.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/res/drawable/ic_github.xml
git commit -m "feat(settings): add GitHub logo vector drawable"
```

---

### Task 2: Localized strings

**Files:**
- Modify: `app/src/main/res/values/strings.xml` (after the `language_german` line, before the `<!-- Cook mode -->` comment)
- Modify: `app/src/main/res/values-de/strings.xml` (after the `language_german` line, before the `<!-- Kochmodus -->` comment)

- [ ] **Step 1: Add the English strings**

In `app/src/main/res/values/strings.xml`, find:

```xml
    <string name="language_german">Deutsch</string>

    <!-- Cook mode -->
```

Insert a new About block between them so it reads:

```xml
    <string name="language_german">Deutsch</string>

    <!-- About -->
    <string name="about_app_name">Recipely</string>
    <string name="about_version">v%1$s</string>
    <string name="about_github">View source on GitHub</string>
    <string name="about_open_source">Open source · made with ❤</string>

    <!-- Cook mode -->
```

- [ ] **Step 2: Add the German strings**

In `app/src/main/res/values-de/strings.xml`, find:

```xml
    <string name="language_german">Deutsch</string>

    <!-- Kochmodus -->
```

Insert between them so it reads:

```xml
    <string name="language_german">Deutsch</string>

    <!-- Über -->
    <string name="about_app_name">Recipely</string>
    <string name="about_version">v%1$s</string>
    <string name="about_github">Quellcode auf GitHub ansehen</string>
    <string name="about_open_source">Open Source · mit ❤ gemacht</string>

    <!-- Kochmodus -->
```

- [ ] **Step 3: Verify resources compile**

Run:
```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat :app:processDebugResources
```
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/res/values/strings.xml app/src/main/res/values-de/strings.xml
git commit -m "feat(settings): add About-section strings (EN/DE)"
```

---

### Task 3: AboutFooter composable

**Files:**
- Create: `app/src/main/java/com/nwe/recipely/ui/settings/AboutFooter.kt`

- [ ] **Step 1: Create the composable**

This file owns the whole footer: the repo URL constant, the version read, the layout (app mark → name+version → GitHub link → OSS note), and the intent helper. The brand mark uses the fixed `Moss → ForestPrimary` gradient in both light and dark mode (per spec). The name uses `colorScheme.primary` (forest in light, light-green in dark). The link + heart-bearing line use `colorScheme.secondary` (terracotta). The `❤` renders as a colored emoji glyph, so no per-character styling is needed.

Create `app/src/main/java/com/nwe/recipely/ui/settings/AboutFooter.kt`:

```kotlin
package com.nwe.recipely.ui.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nwe.recipely.R
import com.nwe.recipely.ui.theme.Fraunces
import com.nwe.recipely.ui.theme.ForestPrimary
import com.nwe.recipely.ui.theme.Moss

private const val REPO_URL = "https://github.com/Blanco27/Recipely"

/** Quiet-footer About section (mockup Variant C): app mark, name + version, GitHub link, OSS note. */
@Composable
fun AboutFooter(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val versionName = remember {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull()
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 40.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Brush.linearGradient(listOf(Moss, ForestPrimary))),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "🍲", fontSize = 22.sp)
        }

        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = stringResource(R.string.about_app_name),
                fontFamily = Fraunces,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.primary,
            )
            if (versionName != null) {
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.about_version, versionName),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        val linkLabel = stringResource(R.string.about_github)
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClickLabel = linkLabel) { openUrl(context, REPO_URL) }
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_github),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(15.dp),
            )
            Text(
                text = linkLabel,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.5f.sp,
            )
        }

        Text(
            text = stringResource(R.string.about_open_source),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Opens [url] in the user's browser; no-ops if no browser is installed. */
private fun openUrl(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (e: ActivityNotFoundException) {
        // No browser available — ignore rather than crash.
    }
}
```

- [ ] **Step 2: Verify it compiles**

Run:
```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat :app:compileDebugKotlin
```
Expected: `BUILD SUCCESSFUL`. (Unresolved imports like `Moss`/`ForestPrimary`/`Fraunces` or a missing `R.drawable.ic_github` would fail here.)

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/nwe/recipely/ui/settings/AboutFooter.kt
git commit -m "feat(settings): add AboutFooter composable"
```

---

### Task 4: Wire AboutFooter into SettingsScreen

**Files:**
- Modify: `app/src/main/java/com/nwe/recipely/ui/settings/SettingsScreen.kt` (the root `Column` inside `Scaffold`, currently ending after `LanguagePanel(...)`)

- [ ] **Step 1: Add the spacer + footer**

In `SettingsScreen.kt`, find the end of the root `Column`:

```kotlin
            SectionLabel(stringResource(R.string.settings_language))
            LanguagePanel(
                selected = language,
                onSelect = {
                    language = it
                    it.applyNow()
                },
            )
        }
    }
}
```

Replace it with (adds a weight spacer that pushes the footer to the bottom, then the footer):

```kotlin
            SectionLabel(stringResource(R.string.settings_language))
            LanguagePanel(
                selected = language,
                onSelect = {
                    language = it
                    it.applyNow()
                },
            )

            Spacer(Modifier.weight(1f))

            AboutFooter()
        }
    }
}
```

Note: `Spacer`, `Modifier`, and `weight` are already imported in this file (used elsewhere). `AboutFooter` is in the same package, so no import is needed.

- [ ] **Step 2: Verify it compiles**

Run:
```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat :app:compileDebugKotlin
```
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/nwe/recipely/ui/settings/SettingsScreen.kt
git commit -m "feat(settings): show AboutFooter at bottom of Settings"
```

---

### Task 5: Build, install, and manually verify

**Files:** none (verification only)

- [ ] **Step 1: Assemble the debug APK**

Run:
```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat assembleDebug
```
Expected: `BUILD SUCCESSFUL`.

- [ ] **Step 2: Install on a device/emulator**

Run:
```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat installDebug
```
Expected: `BUILD SUCCESSFUL` and the app installs.

- [ ] **Step 3: Verify against the mockup**

Open the app → Settings, and confirm:
- The About footer sits at the **bottom** of the screen, centered, below Appearance & Language.
- It shows the 🍲 mark in a green gradient square, "Recipely v1.0", a terracotta "View source on GitHub" link with the GitHub glyph, and "Open source · made with ❤".
- Compare side-by-side with `docs/mockups/settings-about.html` (Variant C) — layout, spacing, colors match.
- Toggle the theme to **Dark** (Appearance → Dark): the footer remains legible; the gradient mark is unchanged, the name turns light-green, the link stays terracotta.
- Switch the language to **Deutsch** (Language → Deutsch): the link reads "Quellcode auf GitHub ansehen" and the note "Open Source · mit ❤ gemacht".
- Tap the GitHub link: the browser opens `https://github.com/Blanco27/Recipely`.

- [ ] **Step 4: Run lint (catches unused/missing resources)**

Run:
```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"; .\gradlew.bat lint
```
Expected: `BUILD SUCCESSFUL` with no new errors referencing `about_*` strings or `ic_github`.

- [ ] **Step 5: Final commit (only if verification surfaced fixes)**

If Step 3 or 4 required adjustments, commit them:
```bash
git add -A
git commit -m "fix(settings): About footer polish after manual verification"
```
If nothing changed, skip this step.

---

## Self-Review

**Spec coverage:**
- App version (dynamic, no hardcode) → Task 3 (`PackageManager` read). ✓
- GitHub link via `ACTION_VIEW`, crash-safe → Task 3 (`openUrl`). ✓
- Open-source note → Tasks 2 + 3. ✓
- Variant C layout (mark, name+version, link, OSS line, bottom-anchored) → Tasks 3 + 4. ✓
- GitHub glyph matching mockup → Task 1. ✓
- EN/DE localization → Task 2. ✓
- Terracotta = `colorScheme.secondary`, name = `colorScheme.primary`, gradient = `Moss`/`ForestPrimary` → Task 3. ✓
- `weight(1f)` bottom anchoring → Task 4. ✓
- No `BuildConfig` change → confirmed (not touched). ✓
- No LICENSE file needed (general OSS note) → confirmed (not in scope). ✓

**Placeholder scan:** No TBD/TODO/"handle edge cases" — all code and commands are concrete. ✓

**Type/name consistency:** `REPO_URL`, `openUrl(context, url)`, `AboutFooter()`, `R.drawable.ic_github`, and string keys `about_app_name` / `about_version` / `about_github` / `about_open_source` are used identically across Tasks 1–4. Color vals `Moss`, `ForestPrimary`, `Fraunces` match the names verified in `ui/theme/Color.kt`. ✓
