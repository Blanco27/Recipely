---
name: update-readme
description: Use when Recipely's README needs refreshing — its screenshots are stale after a UI redesign or theme change, or its prose (Features, theme description, Out-of-scope list) no longer matches the current code after a feature like categories or nutrition shipped.
---

# Update the Recipely README

## Overview

Recipely has **no in-app demo seed** and the README screenshots are hand-made from a running
emulator. Keeping the README honest means three things, in order: **seed realistic demo data →
recapture the screenshots → reconcile the prose against the actual code.** Skipping the last step
is the usual failure — new screenshots with stale feature/theme text.

## Prerequisites

- An emulator/device is running: `adb devices` shows one `device`.
- CLI Gradle uses JBR 21 (default JDK 24 breaks Gradle 8.13). See [[recipely-build-jdk]]:
  `$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"`
- `adb` lives at `<sdk.dir>\platform-tools\adb.exe` (`sdk.dir` is in `local.properties`).
- **Run adb/push commands from PowerShell, NOT git-bash** — MSYS mangles unix paths like
  `/data/local/tmp` into `C:/Program Files/Git/...`.

## Workflow

### 1. Audit what's stale
Read `README.md` and the current screenshots (`docs/screenshots/*.png`). Diff against reality:
recent commits (`git log --oneline -8`), `app/src/main/res/values/strings.xml`,
`data/RecipeEntities.kt`, `ui/theme/Color.kt`. List every claim that no longer holds (theme
colour, feature bullets, "out of scope" list) — fix these in step 6.

### 2. Build & install the current app
```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat installDebug
```

### 3. Seed demo data straight into the Room DB
There is no seed code and no `sqlite3` on the device or in git-bash — use **Python's** `sqlite3`
(`python -c "import sqlite3"` works). Pre-release means no migrations: reset, don't migrate
([[recipely-prerelease-no-migration]]).

1. `adb shell pm clear com.nwe.recipely`, launch once, force-stop → a fresh DB with the **current**
   schema + Room identity hash (avoids any schema-mismatch on open).
2. Pull it: `adb exec-out run-as com.nwe.recipely cat databases/recipely.db > seed.db`
3. Insert recipes/ingredients/steps with `seed_demo.py` (in this skill dir). **Verify the column
   list against `data/RecipeEntities.kt` first** — the script must match the live schema.
4. Push back (PowerShell): push to `/data/local/tmp`, `run-as ... cp` into `databases/`, then
   `rm -f databases/recipely.db-wal databases/recipely.db-shm`.

Make the demo data **exercise the features the screenshots must prove**: spread categories so the
filter bar is full, give most recipes nutrition (leave one without, to show it's optional), and
keep the hero recipe (Spaghetti Carbonara) fully populated for the detail shot.

### 4. (Optional but preferred) Real title images
The redesigned cards/detail are image-led; the placeholder looks empty. Add free **Unsplash**
photos:
- Find a photo page per dish (`WebSearch ... allowed_domains unsplash.com`), grab the 11-char id
  from the URL, download via the redirect endpoint:
  `Invoke-WebRequest "https://unsplash.com/photos/<ID>/download?w=1200&fit=crop" -OutFile x.jpg`
- Push each into `files/images/` (same `/data/local/tmp` + `run-as cp` dance), then set each
  recipe's `imageUri` to the **device** absolute path `/data/user/0/com.nwe.recipely/files/images/<name>.jpg`
  (Coil loads `File(path)`; the DB stores only paths). Unsplash License = free, no attribution.

### 5. Capture screenshots
```powershell
adb shell am start -n com.nwe.recipely/.MainActivity
adb exec-out screencap -p > C:\...\shot_list.png   # a REAL Windows path, so Read can view it
adb shell input swipe 540 1900 540 350 350         # scroll
adb shell input tap <x> <y>                         # open detail; pencil ≈ (820,150)
```
Capture **list, detail (hero recipe), edit** to mirror the three README slots. After each tap,
screencap and `Read` the PNG to confirm you're on the right screen before moving on. Copy finals
over `docs/screenshots/01-list.png`, `02-detail.png`, `03-edit.png` (keep the names — README
references them).

### 6. Reconcile the prose (the step people skip)
Re-read `README.md` end-to-end and fix everything from the step-1 audit: intro line, **Features**
list, theme description, **Project structure** comments, and the **Out of scope** list (a shipped
feature must leave it). Then check `CLAUDE.md` for the same drift (e.g. its "Out of scope by
design" line) and keep both in sync.

### 7. Commit
Stage **only** README/screenshots/CLAUDE.md — never the untracked `.idea/` or `docs/mockups/`.
Commit to `main` and push (a branch is overkill for a docs refresh, per the user's preference).

## Common mistakes

| Mistake | Fix |
|---|---|
| adb push fails with `C:/Program Files/Git/data/...` | Use PowerShell, not git-bash (MSYS path conversion). |
| `sqlite3: command not found` | Use Python's `sqlite3` module; the device has no sqlite3 binary. |
| `Read` can't open the screenshot | Save to a real Windows dir, not `/tmp` (git-bash temp). |
| Image doesn't render in app | `imageUri` must be the device path `/data/user/0/com.nwe.recipely/files/images/...`. |
| Gradle config fails | `JAVA_HOME` not set to JBR 21. |
| New screenshots, old text | Do step 6 — reconcile Features/theme/scope, and CLAUDE.md too. |
| kcal looks wrong | List card shows **total** calories; detail computes per-portion from `servings`. |

## Quick reference
- Schema: `recipes(id,name,imageUri,prepTimeMinutes,servings,calories,carbsGrams,proteinGrams,fatGrams,category)`,
  `ingredients(id,recipeId,text,position)`, `steps(id,recipeId,text,imageUri,position)`.
- Category keys: `MAIN, BREAKFAST, SALAD, BAKING, DESSERT, SNACK` (`data/RecipeCategory.kt`).
- App data dir: `/data/user/0/com.nwe.recipely` (`files/images/` for photos, `databases/recipely.db`).
- Seeding template: `seed_demo.py` (this dir).
