# Recipely 🍃

Eine schlanke, native **Android-App** zum Anlegen, Ansehen, Bearbeiten und Löschen von Kochrezepten — komplett **offline**, mit einem modernen Material-3-UI in frischem Grün.

Jedes Rezept hat einen Namen (Pflicht), optional ein Titelbild, optional Zubereitungszeit und Portionen, eine Zutatenliste sowie nummerierte Zubereitungsschritte mit optionalem Bild pro Schritt.

## Screenshots

<p align="center">
  <img src="docs/screenshots/01-list.png" width="30%" alt="Rezeptliste – Meine Rezepte" />
  &nbsp;&nbsp;
  <img src="docs/screenshots/02-detail.png" width="30%" alt="Rezeptdetail – Spaghetti Carbonara" />
  &nbsp;&nbsp;
  <img src="docs/screenshots/03-edit.png" width="30%" alt="Rezept bearbeiten" />
</p>

<p align="center">
  <em>Rezeptliste &nbsp;·&nbsp; Detailansicht &nbsp;·&nbsp; Editor &nbsp;— hier mit Demodaten</em>
</p>

> Helles und dunkles Theme werden automatisch unterstützt (die Screenshots zeigen den Light-Mode).

## Features

- 📋 **Rezeptliste** mit kompakten Einträgen (Thumbnail, Name, „⏱ Zeit · 🍽 Portionen"), alphabetisch sortiert
- 👀 **Detailansicht**: Titelbild, Zeit-/Portionen-Chips, Zutatenliste, nummerierte Schritte mit optionalem Schrittbild
- ✏️ **Anlegen & Bearbeiten** über ein dynamisches Formular (Zutaten und Schritte beliebig hinzufügen/entfernen)
- 🖼️ **Bilder** aus **Galerie** (Photo Picker) oder **Kamera** — für das Titelbild und je Schritt
- 🗑️ **Löschen** mit Bestätigungsdialog
- 💾 **Offline-First**: lokale Speicherung via Room; Bilder werden in den App-internen Speicher kopiert, die Datenbank hält nur Pfade — verwaiste Bilddateien werden automatisch aufgeräumt
- 🎨 Festes grünes **Material-3**-Theme (Hell/Dunkel automatisch)

## Tech-Stack

- **Kotlin** 2.0.21
- **Jetpack Compose** + **Material 3** (Compose BOM 2024.09.03), Navigation-Compose
- **Room** (lokale SQLite-Persistenz) mit **KSP**
- **Coil** für das Laden von Bildern
- Coroutines / `StateFlow`
- Architektur: **Single-Activity, lean MVVM** mit manueller DI (kein Hilt/Dagger)
- Tests: JUnit 4 + `kotlinx-coroutines-test` (JVM), AndroidX Test (instrumentierter Room-DAO-Test)

## Architektur

Single-Activity-Compose-App nach **lean MVVM**: Compose UI → ViewModel → Repository → Room DAO.

```
RecipelyApp (Application)
  └─ AppContainer (manuelle DI)
       ├─ RecipeDatabase (Room)  → RecipeDao
       ├─ ImageStore (interner Bildspeicher)
       └─ RoomRecipeRepository
            ▲
   ViewModels (List / Detail / Edit)
            ▲
   Compose Screens  ── RecipelyNavHost (list · detail/{id} · edit?id={id})
```

Details und projektweite Konventionen stehen in [`CLAUDE.md`](CLAUDE.md); Design-Spec und Implementierungsplan unter [`docs/superpowers/`](docs/superpowers/).

## Build & Start

**Voraussetzungen**

- Android SDK (compileSdk/targetSdk **36**, minSdk **24**); `local.properties` mit `sdk.dir` (von Android Studio erzeugt)
- **Gradle läuft mit einem JDK ≤ 21.** Das Java-11-Level in der Build-Konfig ist nur die *Bytecode-Ebene* und nicht das JDK, das Gradle ausführt. Android Studio nutzt seine gebündelte JBR (21) automatisch. Für **CLI-Builds** ggf. das JDK setzen, falls der System-Default neuer ist (z. B. JDK 24, das Gradle 8.13 nicht unterstützt):

  ```powershell
  $env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
  ```

**Bauen / Installieren** (Windows/PowerShell — sonst `./gradlew`):

```powershell
.\gradlew.bat assembleDebug     # Debug-APK bauen
.\gradlew.bat installDebug      # Auf verbundenem Gerät/Emulator installieren
```

Alternativ das Projekt in **Android Studio** öffnen und auf ▶ Run drücken.

## Tests

```powershell
.\gradlew.bat testDebugUnitTest          # JVM-Unit-Tests (Mapping + ViewModels)
.\gradlew.bat connectedDebugAndroidTest  # Instrumentierter Room-DAO-Test (Gerät/Emulator nötig)
```

Einzelne Tests:

```powershell
.\gradlew.bat testDebugUnitTest --tests "com.nwe.recipely.RecipeMappingTest"
.\gradlew.bat connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.nwe.recipely.RecipeDaoTest
```

## Projektstruktur

```
app/src/main/java/com/nwe/recipely/
├─ RecipelyApp.kt            # Application + AppContainer-Halter
├─ MainActivity.kt           # Single Activity → Compose + NavHost
├─ di/AppContainer.kt        # manuelle DI (DB, ImageStore, Repository)
├─ data/                     # Room: Entities, DAO, Database, ImageStore, Repository
├─ navigation/               # RecipelyNavHost + Routes
└─ ui/
   ├─ theme/                 # grünes Material-3-Theme (Color/Type/Theme)
   ├─ list/                  # RecipeListScreen + ViewModel
   ├─ detail/                # RecipeDetailScreen + ViewModel
   └─ edit/                  # RecipeEditScreen + ViewModel + Formular-State/Mapping
```

## Nicht im Umfang (bewusst)

Suche, Tags/Kategorien, Cloud-Sync, Export/Teilen und Mengen-Skalierung sind bewusst nicht enthalten — die App bleibt einfach und auf das Wesentliche fokussiert.
