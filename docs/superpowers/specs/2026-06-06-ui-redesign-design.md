# Design: UI-Refresh „Warm Editorial Kitchen" (Spec A)

**Datum:** 2026-06-06
**Status:** Approved (brainstorming)

## Ziel

Die UI von „Standard-Material 3" zu einer wärmeren, magazinartigen Food-App-Anmutung
heben — **rein optisch**, ohne Änderung am Datenmodell, an ViewModels, Repository, Room
oder DAO. Visuelle Referenz: `docs/mockups/ui-redesign.html` (Screens Liste, Detail,
Bearbeiten).

Dieses Vorhaben ist bewusst in zwei Specs getrennt:
- **Spec A (dieses Dokument):** reiner UI-Refresh.
- **Spec B (später):** Datenfeature „Kategorien" (neue Recipe-Spalte, Edit-Picker,
  Listen-Filter/Badge). Die im Mockup gezeigten Kategorie-Pills/-Badges gehören zu Spec B
  und sind hier **nicht** enthalten.

## Entscheidungen (aus Brainstorming)

- **Zuschnitt:** zwei getrennte Specs/Pläne/Branches (UI vs. Kategorien).
- **Schriften:** Custom-Fonts **als TTF gebündelt** in `res/font/` (passt zu offline-only;
  keine Downloadable Fonts, kein Netz/Play-Services-Bedarf).
- **Farbpalette:** **warme Palette wie im Mockup** (Creme + Waldgrün + Terrakotta/Honey),
  Light + Dark. Weiterhin **kein** Dynamic Color / Material You.
- **Abhakbare Zutaten:** ja, aber **flüchtiger UI-Zustand** (`remember`), **nicht
  persistiert** — setzt sich beim Verlassen der Detailansicht zurück.
- **Favoriten:** **nicht** Teil des Vorhabens (kein Feature).
- **Umsetzungsstruktur:** Foundation-first — erst Theme/Typografie, dann
  wiederverwendbare Composables, dann Liste → Detail → Edit.

## Schriften & Typografie

- **Display:** Fraunces (Weight 600) — Rezeptnamen, Screen-Titel, Section-Header.
- **Body/UI:** Hanken Grotesk (400/500/600/700) — Fließtext, Felder, Chips, Buttons.
- Neue `FontFamily`-Definitionen; `ui/theme/Type.kt` überarbeitet: Display/Headline →
  Fraunces; Title/Body/Label → Hanken Grotesk. Größen/Gewichte gemäß Mockup-Rhythmus.
- TTF-Dateien unter `app/src/main/res/font/` (nur benötigte Schnitte, um APK-Zuwachs
  klein zu halten).

## Farben & Form

- `ui/theme/Color.kt`: warme Palette
  - Forest `#1E3A2B` (primary), Moss `#4F7A4A`, Terrakotta `#C75D3C` (Akzent/FAB/aktive
    Elemente), Honey `#E2A03C` (sekundärer Akzent), Cream `#F6F0E6` / `#FBF7EF`
    (background/surface), Ink `#21201B` (onSurface).
- `ui/theme/Theme.kt`: Mapping auf M3-`ColorScheme`-Rollen für **Light und Dark**
  (primary, onPrimary, secondary, tertiary, surface, surfaceVariant, outline, …).
  Dynamic Color bleibt deaktiviert.
- Formen: `Shapes` mit größeren Radien (Cards/Bilder ~20–24 dp) für die weichere Anmutung.

## Änderungen pro Screen

### Liste — `ui/list/RecipeListScreen.kt`

- `LargeTopAppBar` „My Recipes" (Fraunces) mit `TopAppBarScrollBehavior` (kollabiert beim
  Scrollen).
- `LazyColumn` mit `contentPadding` + `Arrangement.spacedBy(16.dp)`.
- Neue, wiederverwendbare **`RecipeCard`**: Foto im Seitenverhältnis **16:9** oben, darunter
  Name (Fraunces) und **echte Chips** (statt Emoji-Meta-Text) für Zeit / Portionen / kcal;
  kcal-Chip nur, wenn `calories` gesetzt ist.
- Bild-Platzhalter (kein `imageUri`): gefüllte Fläche mit `Restaurant`-Icon statt leerem
  Quadrat.
- **Extended FAB** in Terrakotta („+ Rezept").
- Empty-State aufgewertet (größeres Icon, Fraunces-Titel, Hinweistext).
- Row+`HorizontalDivider` entfällt.

### Detail — `ui/detail/RecipeDetailScreen.kt`

- **Collapsing/Parallax-Hero** (~300 dp) mit Gradient-Scrim und **Titel-Overlay** auf dem
  Bild. Nav-Aktionen (Back / Edit / Delete) als runde, halbtransparente Buttons auf dem
  Bild; beim Scrollen Kollaps zur normalen TopAppBar.
- **Nutrition-Statleiste:** vorhandene Nährwerte als Reihe von Stat-Karten (z. B. ⏱ Zeit,
  🍽 Portionen, 🔥 kcal, Makro). **Restyle** der bestehenden `NutritionDisplay`; die
  Pro-Portion-/Null-Logik bleibt **unverändert**, nur die Darstellung ändert sich.
- **Zutaten** als Karte mit **abhakbaren** Einträgen (`Checkbox`, Zustand via `remember`,
  nicht persistiert).
- **Schritte** als Karten mit kräftigem Fraunces-Nummern-Badge und dezenter
  Verbindungslinie; Schrittbild gerundet.

### Bearbeiten — `ui/edit/RecipeEditScreen.kt`

Reines Restyle, **gleiche Felder und Funktion wie bisher** (Name, Zeit, Portionen,
Nährwerte, Zutaten, Schritte; Galerie/Kamera-Dialog, Bild-Lifecycle, `discardChanges`/
`BackHandler` unverändert):
- Titelbild-Picker: gerundete Fläche (24 dp); gefüllt mit Foto + Overlay-Steuerung
  (Ändern/Entfernen) bzw. einladender Leerzustand.
- Eingabefelder im neuen Theme (`OutlinedTextField`, Terrakotta-Fokuszustand).
- **Nährwert-Eingabe** (bestehender Abschnitt „Nährwerte (optional)" mit den vier Feldern
  Kalorien (kcal), Kohlenhydrate (g), Eiweiß (g), Fett (g)): im neuen Theme als kompaktes
  2×2-Raster gerundeter Felder, platziert nach „Portionen", vor den Zutaten. Tastaturtypen,
  Parsing und Speicherlogik bleiben **unverändert** (reines Restyle).
- Zutaten-Reihen mit Entfernen-Button; **gestrichelte „+"-Buttons** (Zutat/Schritt) im
  Moss-Ton.
- Schritt-Editor als Karte mit Nummern-Badge, Textfeld und Bild/„Bild hinzufügen".
- **Kein** Kategorie-Picker (→ Spec B).

## Neue/geänderte Dateien (Erwartung)

- **Neu:** `app/src/main/res/font/*` (TTF), ggf. neue Composable-Dateien
  `ui/list/RecipeCard.kt`, `ui/detail/StepCard.kt`, `ui/detail/NutritionStatBar.kt`
  (Aufteilung nach Bedarf, um Screen-Dateien fokussiert zu halten).
- **Geändert:** `ui/theme/Color.kt`, `ui/theme/Theme.kt`, `ui/theme/Type.kt`,
  `ui/list/RecipeListScreen.kt`, `ui/detail/RecipeDetailScreen.kt`,
  `ui/detail/NutritionDisplay.kt`, `ui/edit/RecipeEditScreen.kt`.
- **Unberührt:** alle `data/*`, alle ViewModels, `RecipeRepository`, Navigation,
  `strings.xml`/`values-de` (keine neuen Strings nötig — bestehende Texte bleiben).

## Tests

- **Keine neue reine Logik** → kein neuer JVM-Unit-Test erforderlich.
- Bestehende JVM-Unit-Tests müssen **grün bleiben** (`testDebugUnitTest`).
- Compose-UI wird gemäß Projektkonvention durch **erfolgreichen Build/Run**
  (`assembleDebug` / `installDebug`) verifiziert, nicht per Red-Green.

## Out of Scope

- Kategorien (Spec B), Favoriten, persistierte Zutaten-Häkchen.
- Such-/Sortier-/Filterfunktion.
- **Hinweis:** Der kcal-Chip in der Liste zeigt nur das **bereits vorhandene**
  `Recipe.calories`-Feld an (reine Darstellung, kein neues Datum, keine Berechnung). Eine
  darüber hinausgehende Nährwert-Anzeige in der Liste ist out of scope.
- Jegliche Änderung an Datenmodell, Room, DAO, Repository, ViewModels, Navigation.
- Material You / Dynamic Color.
