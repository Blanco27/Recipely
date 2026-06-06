# Recipely – Design-Spec

**Datum:** 2026-06-06
**Status:** Genehmigt (Brainstorming abgeschlossen)
**Paket / Namespace:** `com.nwe.recipely`

## 1. Ziel & Umfang

Recipely ist eine bewusst **einfache, lokale Android-App** zum Speichern, Anlegen und
Bearbeiten von Kochrezepten. Ein Rezept besteht aus:

- **Name** (Pflichtfeld)
- **Titelbild** (optional)
- **Zubereitungszeit** (optional)
- **Portionsangabe** (optional)
- **Zutatenliste** (Liste aus Freitext-Zeilen)
- **Zubereitung** als geordnete **Schritte**, jeder Schritt mit Text und **optionalem Bild**

Die App ist offline-only (kein Login, kein Server). Oberste UI-Maxime: **so modern wie
möglich, aber simpel** (Jetpack Compose + Material 3).

### Bewusst NICHT im ersten Wurf (YAGNI)

Suche/Filter, Kategorien/Tags, Cloud-Sync/Backup, Export/Teilen, Mengen-Skalierung,
Mehrbenutzer. Alles später nachrüstbar, ohne das hier beschriebene Modell zu brechen.

## 2. Technische Entscheidungen

| Thema           | Entscheidung |
|-----------------|--------------|
| Sprache         | Kotlin |
| UI-Toolkit      | Jetpack Compose + Material 3 |
| Architektur     | Single-Activity, schlankes MVVM (UI → ViewModel → Repository → DAO) |
| Persistenz      | Room (lokal, SQLite), kein Cloud-Sync |
| Navigation      | Navigation-Compose (`NavHost`) |
| Bilder laden    | Coil (`coil-compose`) |
| DI              | Manuell über `AppContainer` in der `Application`-Klasse + ViewModel-Factory (kein Hilt) |
| Bildquelle      | Galerie (Photo Picker) **und** Kamera (`FileProvider`) |
| Min/Target SDK  | min 24 / target 36 (wie Scaffold) |
| JDK             | 11 |

### Farb-/Theme-Entscheidung

Festes **grünes** Material-3-Seed-Farbschema ("Frisches Grün"). Hell- und Dunkelmodus
werden über das Material-3-Theme automatisch abgeleitet. **Kein** dynamisches Material-You
(bewusst feste Markenfarbe für vorhersehbares Aussehen).

### Neue Abhängigkeiten (über Gradle Version-Catalog `gradle/libs.versions.toml`)

- Compose: `androidx-compose-bom`, `androidx-ui`, `androidx-ui-graphics`,
  `androidx-ui-tooling`(debug)/`androidx-ui-tooling-preview`, `androidx-material3`,
  `androidx-activity-compose`, `androidx-lifecycle-viewmodel-compose`,
  `androidx-navigation-compose`
- Room: `androidx-room-runtime`, `androidx-room-ktx`, `androidx-room-compiler` (via **KSP**)
- Bilder: `coil-compose`
- KSP-Gradle-Plugin (passend zur Kotlin-Version 2.0.21)

Build-Anpassungen in `app/build.gradle.kts`: `buildFeatures { compose = true }`,
Compose-Compiler über das Kotlin-2.0-Compose-Plugin (`org.jetbrains.kotlin.plugin.compose`),
KSP-Plugin. Alle Versionen ausschließlich im Version-Catalog pflegen.

## 3. Datenmodell (Room)

Drei Entities, eine zusammengesetzte Lese-POJO.

### `Recipe`
| Feld              | Typ      | Hinweis |
|-------------------|----------|---------|
| `id`              | `Long`   | PK, autogeneriert |
| `name`            | `String` | Pflicht (nicht leer) |
| `imageUri`        | `String?`| Pfad im App-internen Speicher, sonst `null` |
| `prepTimeMinutes` | `Int?`   | optional |
| `servings`        | `Int?`   | optional |

### `Ingredient`
| Feld       | Typ      | Hinweis |
|------------|----------|---------|
| `id`       | `Long`   | PK, autogeneriert |
| `recipeId` | `Long`   | FK → `Recipe.id`, `onDelete = CASCADE`, indiziert |
| `text`     | `String` | eine Freitext-Zeile, z. B. „200 g Spaghetti" |
| `position` | `Int`    | Reihenfolge |

### `Step`
| Feld       | Typ      | Hinweis |
|------------|----------|---------|
| `id`       | `Long`   | PK, autogeneriert |
| `recipeId` | `Long`   | FK → `Recipe.id`, `onDelete = CASCADE`, indiziert |
| `text`     | `String` | Schritt-Beschreibung |
| `imageUri` | `String?`| optionales Schritt-Bild (Pfad), sonst `null` |
| `position` | `Int`    | Reihenfolge |

### `RecipeWithDetails` (Lese-POJO)
`@Embedded Recipe` + `@Relation` Listen `ingredients` und `steps` (nach `position` sortiert).
Dient dem Detail- und Editor-Screen.

## 4. Schichten & Komponenten

### DAO – `RecipeDao`
- `observeRecipes(): Flow<List<Recipe>>` – nur Recipe-Felder für die Liste (kein Laden der Kinder).
- `observeRecipe(id: Long): Flow<RecipeWithDetails?>` – `@Transaction`.
- `saveRecipe(...)` als `@Transaction`-Funktion: Recipe upserten, dann zugehörige
  Zutaten/Schritte **ersetzen** (alte löschen, neue mit `position` einfügen).
- `deleteRecipe(id: Long)` – Cascade entfernt Kinder.

### Repository – `RecipeRepository`
- Kapselt den DAO, gibt die Flows weiter und bietet `suspend`-Funktionen
  (`save`, `delete`).
- Verantwortlich für das **transaktionale Speichern** eines kompletten Rezepts und das
  **Aufräumen verwaister Bilddateien** (siehe §6) beim Speichern/Löschen.

### DI – `AppContainer`
- In der `Application`-Subklasse erzeugt: Room-`Database` → `RecipeDao` → `RecipeRepository`
  → `ImageStore` (siehe §6).
- ViewModels erhalten ihre Abhängigkeiten über eine `ViewModelProvider.Factory`.

### ViewModels (je Screen, mit `StateFlow`-UI-State + Coroutines)
- `RecipeListViewModel` – exponiert `Flow<List<Recipe>>` als UI-State.
- `RecipeDetailViewModel` – lädt `RecipeWithDetails` per id; stellt `delete()` bereit.
- `RecipeEditViewModel` – hält den **editierbaren Formular-State** (Name, Zeit, Portionen,
  Titelbild, Listen aus Zutaten und Schritten); Aktionen: Zutat/Schritt hinzufügen &
  entfernen, Reihenfolge implizit über Listenposition, Schritt-Bild setzen/entfernen,
  Titelbild setzen/entfernen, `save()`. Lädt bei vorhandener id den Bestand, sonst leeres
  Formular.

## 5. Screens & Navigation

Single `MainActivity` hostet das Compose-Theme und einen `NavHost` mit drei Routen:

### Liste – Route `list`
- Kompaktes Listen-Layout (gewählte Variante „B"): kleines quadratisches Vorschaubild
  links, Name + Meta-Zeile („⏱ 30 Min · 🍽 4") rechts, Trennlinien.
- **FAB „＋"** unten rechts → Editor (neues Rezept).
- Tipp auf Eintrag → Detail.
- **Leerer Zustand:** freundlicher Hinweistext + Aufforderung, das erste Rezept anzulegen.

### Detail – Route `detail/{id}`
- Titelbild oben (Platzhalter, falls keins) mit Zurück-Pfeil und Bearbeiten-Stift.
- Name, Chips für Zeit & Portionen (nur anzeigen, wenn gesetzt).
- Abschnitt **Zutaten** (Aufzählung).
- Abschnitt **Zubereitung**: nummerierte Schritte (Kreis-Badge), Schritt-Text, darunter
  optionales Schritt-Bild.
- Aktionen: Bearbeiten (→ Editor mit id), Löschen (→ Bestätigungsdialog).

### Editor – Route `edit?id={id}` (id optional ⇒ neu vs. bearbeiten)
- Top-App-Bar mit „Abbrechen (✕)" und „Speichern" (Speichern deaktiviert, solange Name leer).
- Titelbild-Auswahl: tippen öffnet Auswahl **Galerie / Kamera**; gesetztes Bild mit
  Entfernen-Option.
- Felder: Name (Pflicht), Zeit, Portionen.
- **Zutaten:** dynamische Liste editierbarer Zeilen, je Zeile ✕ zum Entfernen,
  „＋ Zutat hinzufügen".
- **Zubereitung:** dynamische Liste von Schritt-Karten; je Karte Nummer, mehrzeiliges
  Textfeld, ✕ zum Entfernen, „＋ Bild zum Schritt" (bzw. Vorschaubild mit Entfernen),
  „＋ Schritt hinzufügen".

## 6. Bilder

- **`ImageStore`-Hilfskomponente** verwaltet App-internen Bildordner (`filesDir/images/`).
- **Galerie:** Compose `rememberLauncherForActivityResult` mit dem **Photo Picker**
  (`PickVisualMedia`). Das ausgewählte Bild wird in den internen Speicher **kopiert**
  (eigener Dateiname), in der DB steht nur der interne Pfad. So entfällt das Problem
  flüchtiger URI-Leseberechtigungen.
- **Kamera:** `TakePicture`-Contract schreibt in eine via **`FileProvider`** bereitgestellte
  Ziel-URI im internen Speicher; Pfad wird gespeichert. `FileProvider` in `AndroidManifest`
  + `res/xml/file_paths.xml` konfigurieren.
- **Anzeige:** Coil `AsyncImage` mit Platzhalter/Fehlerbild.
- **Aufräumen:** Beim Entfernen eines Bildes, beim Ersetzen und beim Löschen eines Rezepts
  werden die nicht mehr referenzierten Bilddateien gelöscht (Repository/`ImageStore`).
- **Berechtigungen:** Photo Picker und `FileProvider`-Kamera benötigen **keine**
  Lese-/Speicher-Laufzeitberechtigung. (Falls später direkte Kamera-Aufnahme ohne
  `TakePicture`-Contract gewünscht: `CAMERA`-Permission – aktuell nicht nötig.)

## 7. Fehlerbehandlung & Randfälle

- **Name leer:** Speichern deaktiviert; Hinweis am Namensfeld.
- **Leere Zutaten-/Schritt-Zeilen** werden beim Speichern verworfen (getrimmt; komplett
  leere Einträge entfernt).
- **Löschen** stets mit Bestätigungsdialog; Cascade entfernt Kinder, `ImageStore` entfernt
  Bilddateien.
- **Konfigurationswechsel** (Rotation/Prozess-Recreation): Eingaben überleben im
  ViewModel-State.
- **Detail einer gelöschten/nicht existenten id:** sauber zurück navigieren bzw.
  „nicht gefunden"-Zustand.
- **Bild-Lade-/Kopierfehler:** Platzhalter anzeigen, App stürzt nicht ab.

## 8. Teststrategie

- **Unit-Tests (JVM, `app/src/test`)**
  - `RecipeEditViewModel`: Validierung (Speichern aktiv/inaktiv), Zutat/Schritt
    hinzufügen & entfernen, Verwerfen leerer Zeilen beim Speichern, Laden-bei-id vs. neu –
    gegen ein **Fake-Repository**.
  - Repository-/Mapping-Logik (Formular-State ↔ Entities), inkl. `position`-Vergabe.
- **Instrumented-Test (`app/src/androidTest`)**
  - `RecipeDao` gegen **In-Memory-Room-DB**: Insert + `RecipeWithDetails`-Relation +
    `onDelete = CASCADE`.
- Umfang bewusst schlank; UI-Tests optional später.

## 9. Grobe Umsetzungsreihenfolge (Hinweis für die Plan-Phase)

1. Build-Setup: Compose + Room (KSP) + Coil im Version-Catalog & `build.gradle.kts`,
   `Application`-Klasse + `AppContainer`, Material-3-Theme (grünes Seed) + `MainActivity`.
2. Datenmodell: Entities, `RecipeWithDetails`, `RecipeDao`, `Database`, `RecipeRepository`.
3. Liste (mit leerem Zustand) + Navigation-Gerüst.
4. Detail-Screen.
5. Editor-Screen inkl. dynamischer Zutaten-/Schritt-Listen.
6. Bilder: `ImageStore`, Photo Picker, Kamera/`FileProvider`, Anzeige, Aufräumen.
7. Tests (ViewModel-Unit, DAO-Instrumented).

Jeder Schritt sollte für sich kompilier- und testbar bleiben.
