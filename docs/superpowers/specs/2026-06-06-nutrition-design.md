# Design: Nährwerte pro Rezept (Nutrition values)

**Datum:** 2026-06-06
**Status:** Approved (brainstorming)

## Ziel

Rezepte können optional vier Nährwerte erhalten: **kcal, Kohlenhydrate, Eiweiß, Fett**.
Die Werte werden als **Gesamtwert für das ganze Rezept** erfasst und in der Detailansicht
**pro Portion** umgerechnet angezeigt (geteilt durch die Portionenzahl). Alle Werte sind
optional — ein Rezept muss keine Nährwerte haben.

## Entscheidungen (aus Brainstorming)

- **Genauigkeit:** Makros (KH/Eiweiß/Fett) mit **1 Nachkommastelle**, kcal **ganzzahlig**.
- **Eingabe:** als **Gesamtwert** fürs ganze Rezept.
- **Pro-Portion ohne Portionenzahl:** Wenn `servings` nicht gesetzt ist, werden **nur die
  Gesamtwerte** angezeigt (keine Division durch Unbekanntes).
- **Anzeige (Variante C):** „Pro Portion" steht im Fokus; die Gesamtwerte erscheinen als
  kleine Zusatzzeile darunter.
- **Umfang:** Nur **Detail- und Bearbeiten-Ansicht**. Die Rezeptliste bleibt unverändert.
- **Keine Migration:** App ist noch nicht in echter Nutzung. DB-Version bleibt `1`, das neue
  Schema wird frisch angelegt. Die lokale Testdatenbank wird einmalig gelöscht
  (`adb shell pm clear com.nwe.recipely` bzw. App neu installieren).

## Ansatz

Die vier Werte sind 1:1 zum Rezept und immer optional → sie werden als **vier nullable
Spalten direkt an der `Recipe`-Entity** gespeichert (analog zu `prepTimeMinutes`/`servings`).

*Verworfene Alternative:* eigene `Nutrition`-Tabelle mit Fremdschlüssel — unnötig, da keine
1:n-Beziehung und keine eigenständige Lebensdauer. Reiner Overhead.

## Änderungen im Detail

### 1. Datenmodell — `data/RecipeEntities.kt`

Neue Felder an `Recipe` (alle Gesamtwert fürs ganze Rezept, alle optional):

```kotlin
val calories: Int? = null,        // kcal, ganzzahlig
val carbsGrams: Double? = null,   // Kohlenhydrate in g
val proteinGrams: Double? = null, // Eiweiß in g
val fatGrams: Double? = null,     // Fett in g
```

### 2. Datenbank — keine Migration

- DB-Version bleibt **`1`**, `exportSchema = false` unverändert.
- Keine Migrationslogik, kein `fallbackToDestructiveMigration`.
- **Einmalige manuelle Aktion:** lokale Testdatenbank löschen, damit Room nicht über das alte
  Schema stolpert (`adb shell pm clear com.nwe.recipely` oder App deinstallieren/neu
  installieren). `adb` liegt unter `<sdk.dir>\platform-tools\adb.exe`.

### 3. Eingabe-Formular — `ui/edit/EditUiState.kt`

- `EditUiState` erhält 4 neue **String-Felder**: `calories`, `carbs`, `protein`, `fat`
  (Strings, damit TextField-tauglich — wie `prepTime`/`servings`).
- `toEntities()`:
  - kcal: `calories.trim().toIntOrNull()`
  - Makros: **locale-tolerant** parsen — Komma zu Punkt normalisieren, dann `toDoubleOrNull()`;
    leere/ungültige Eingabe → `null`.
- `toUiState()`: zurückformatieren — kcal als Zahl-String; Makros mit **1 Nachkommastelle**
  (locale-gerecht, d. h. Komma als Dezimaltrenner in der DE-Anzeige).

### 4. Edit-ViewModel — `ui/edit/RecipeEditViewModel.kt`

Vier neue Setter: `setCalories`, `setCarbs`, `setProtein`, `setFat` (Muster wie `setPrepTime`).

### 5. Edit-UI — `ui/edit/RecipeEditScreen.kt`

Neuer Abschnitt **„Nährwerte (optional)"** mit 4 `OutlinedTextField`:
- kcal: numerische Ganzzahl-Tastatur (`KeyboardType.Number`).
- Makros: Dezimal-Tastatur (`KeyboardType.Decimal`).
- Platzierung: nach dem Feld „Portionen", vor der Zutatenliste.

### 6. Pro-Portion-Berechnung — reine, testbare Funktion

Eine **pure Funktion / Mapping** (z. B. in `ui/detail`), die aus den `Recipe`-Feldern +
`servings` ein anzeigefertiges Modell erzeugt:
- Division Gesamt ÷ `servings`, kcal **gerundet**, Makros auf **1 Dezimal** formatiert.
- Null-Behandlung: einzelne fehlende Werte werden ausgelassen; fehlt `servings`, werden nur
  Gesamtwerte geliefert.
- Keine Compose-Abhängigkeit → JVM-unit-testbar.

### 7. Detail-UI — `ui/detail/RecipeDetailScreen.kt`

Neuer Abschnitt **„Nährwerte"**, **nur sichtbar wenn mindestens ein Wert gesetzt** ist.
Platzierung: nach den Meta-Chips, vor den Zutaten.
- **Mit `servings`:** „PRO PORTION" prominent (je Wert ÷ servings), darunter kleine Zeile
  „Gesamt: …".
- **Ohne `servings`:** nur Gesamtwerte.

### 8. Lokalisierung — `values/strings.xml` + `values-de/strings.xml`

Neue Strings (EN + DE):
- `section_nutrition` — „Nutrition" / „Nährwerte"
- `nutrition_per_portion` — „Per portion" / „Pro Portion"
- `nutrition_total` / `nutrition_total_inline` — Gesamt-Label bzw. Zusatzzeile
- `label_calories`, `label_carbs`, `label_protein`, `label_fat`
- Einheiten/Formatierung: `kcal`, `g` (in Wert-Formatierungs-Strings)

## Tests (TDD für reine Logik)

- **`RecipeMappingTest`** (JVM): Nährwerte-Mapping hin und zurück; Eingabe mit **Komma und
  Punkt**; leere Felder → `null`; kcal als Int.
- **Neuer Test für die Pro-Portion-Funktion** (JVM): Division + Rundung, 1 Dezimalstelle,
  `servings = null` (nur Gesamt), normale `servings`-Werte, einzelne fehlende Werte.
- **Compose-UI** (Edit/Detail): durch erfolgreichen Build/Run verifiziert (Konvention).

## Out of Scope

- Nährwerte in der Rezeptliste/Übersicht.
- „Pro 100 g"-Angaben, automatische Berechnung aus Zutaten, Datenbank-Migration.
