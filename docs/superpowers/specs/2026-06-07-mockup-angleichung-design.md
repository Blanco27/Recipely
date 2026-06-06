# UI-Angleichung an das „Warm Editorial Kitchen"-Mockup

**Datum:** 2026-06-07
**Status:** Entwurf genehmigt → bereit für Implementierungsplan
**Mockup:** `docs/mockups/ui-redesign.html`

## Ziel

Die laufende App weicht in vielen Details vom genehmigten Mockup ab. Diese Spec
gleicht alle 21 gefundenen Abweichungen an das Mockup an. Die Arbeit ist **rein
kosmetisch**: keine Datenmodell-Änderung, keine Room-Migration, keine neuen
Routen, ViewModels oder Repository-Methoden. Betroffen sind ausschließlich
`ui/theme/` und die drei Screen-Composables.

Out of scope (unverändert): Datenmodell, Navigation, Bild-Lifecycle, ViewModels,
Lokalisierung-Mechanik (neue Strings dürfen ergänzt werden), Dark-Mode-Prinzip.

## Grundproblem: fehlender Papier-Ton

Das Mockup nutzt **drei** warme Töne, die App nur zwei:

| Mockup | Hex | Verwendung | App heute |
|---|---|---|---|
| `--cream` | `#F6F0E6` | Seitenhintergrund | `background` ✓ |
| `--cream-2` | `#FBF7EF` | Screen-/Sheet-Hintergrund | `surface` ✓ |
| `--paper` | `#FFFDF9` | Karten, Felder, Chips, Buttons | ❌ fehlt (App nutzt `surface`) |

Dadurch heben sich im Mockup Felder/Karten als nahezu weißes „Papier" vom
cremefarbenen Hintergrund ab; in der App sind beide gleich. Das Einführen des
Papier-Tons ist die Grundlage für mehrere Einzelpunkte.

## Designentscheidungen (alle bestätigt)

### A. Theme-Fundament — `ui/theme/Color.kt`, `Theme.kt`

- **Papier-Ton:** neues `Paper = Color(0xFFFFFDF9)` und ein leicht erhöhter
  `PaperDark` (dezent heller als `DarkSurface #1F211A`, z. B. `0xFF26281F`).
  Verwendet für: Eingabefelder, Karten, Stat-/Zutaten-/Schritt-Karten,
  Meta-Chips, inaktive Filter-Pillen, eckige ✕-Buttons, Titelbild-Steuerung.
- **Weicher Feldrahmen:** eigenes Token `FieldBorderLight = Color(0xFFE6DECF)`
  (+ passendes Dark-Pendant, ~`OutlineVariantDark`), statt des harten
  `outline #8C8578`.
- Dreistufige Tiefe bleibt erhalten: Seite = Cream, Screen = Cream-2,
  Elemente = Paper. Wo nötig wird der Screen-/Scaffold-Hintergrund explizit auf
  Cream-2 gesetzt, damit das Paper sichtbar abhebt.
- Der Papier-Ton wird als **expliziter Farb-Wert** geführt und direkt verwendet
  (so wie die bestehenden Chip-Farben in `Color.kt`), nicht über einen
  Material-Slot erzwungen — das hält den Eingriff minimal und vorhersehbar.

### B. Wiederverwendbare Bausteine

Gegen Copy-Paste über ~12 Felder und mehrere Buttons werden gemeinsame
Composables eingeführt (in `ui/edit/` bzw. einem `ui/components/`-Paket,
passend zur bestehenden Struktur):

- **`RecipelyTextField`** — Wrapper um `OutlinedTextField` mit:
  - Papier-Fill (`focusedContainerColor`/`unfocusedContainerColor = Paper`),
  - weichem warmem Rahmen (`unfocusedBorderColor = FieldBorder`),
  - **Terrakotta-Fokus** (`focusedBorderColor`/`focusedLabelColor =
    colorScheme.secondary`),
  - leichterem Label.
  - Das „Leuchten" (Box-Shadow-Ring im Mockup) wird über den farbigen
    Fokus-Rahmen angenähert; ein echter Glow ist in Compose nicht ohne weiteres
    möglich und wird bewusst weggelassen.
  - Ersetzt **alle** `OutlinedTextField` im Bearbeiten-Screen (Name, Zeit,
    Portionen, 4× Nährwerte, Zutaten, Schritte).
- **`BoxedIconButton`** — Papier-Quadrat, weicher Rahmen, Radius 13,
  Terrakotta-Icon. Für Zutaten-/Schritt-Löschen. Kreis-Variante (gleiche Füllung
  + Rahmen, runde Form) für den Schließen-Button in der Top-Bar.
- **`FrostedIconButton`** — helle, transluzente Kreise (heller Fill ~18–85 %
  Deckkraft) mit feinem hellen Rand und Icon in Tintenfarbe/weiß. Für die
  Detail-Nav (Zurück/Bearbeiten/Löschen) und die Titelbild-Steuerung (✎/✕).

### C. Liste — `RecipeListScreen.kt`, `RecipeCard.kt`

- Karten-Container und inaktive Filter-Pillen auf **Paper**.
- Card-Radius **26** (statt 24).
- Meta-Chip-Hintergrund auf **Creme `#F6F0E6`** (statt `#F3ECDD`).
- FAB-Radius **20** (statt Material-Default 16).
- Eyebrow-Text: **„Offline · N Rezepte"** (Präfix „Offline ·" ergänzen; neuer
  bzw. angepasster String in `values/` und `values-de/`).

### D. Detail — `RecipeDetailScreen.kt`

- **Sheet + Grabber:** Der Inhalt (ab Stat-Leiste) liegt in einem oben mit
  Radius 28 abgerundeten Sheet (Cream-2), das ~**26dp** über das Hero-Bild
  gezogen wird (negativer Offset), mit einem zentrierten Griff-Handle (Grabber,
  ~42×5dp, abgerundet).
- **Hero:**
  - **Kategorie-Eyebrow** (Honig-Farbe, uppercase, getrackt) über dem Titel,
    sofern eine Kategorie gesetzt ist; sonst nur der Titel.
  - Titelgröße ~**33sp** (etwas größer als heute).
  - Verlauf **oben *und* unten**: leichter dunkler Scrim oben für die
    Lesbarkeit der hellen Status-/Nav-Icons, kräftiger unten für den Titel.
- **Frosted** Nav-Buttons (siehe B).
- **Moos-Box-Häkchen** statt Material-`Checkbox`: 22dp, Radius 7, 2dp
  Moos-Rahmen; abgehakt füllt es sich moosgrün mit weißem ✓. Strikethrough +
  gedämpfte Textfarbe bei abgehakten Zeilen bleibt.
- Stat-Karten auf **Paper**.
- Schritt-Bild-Höhe **130dp** (statt 170).

### E. Bearbeiten — `RecipeEditScreen.kt`

- Alle Felder → **`RecipelyTextField`**.
- Zutaten-/Schritt-✕ → **`BoxedIconButton`**; Schließen-Button oben →
  umrandeter Papier-Kreis (Kreis-Variante).
- **Kategorie-Chips** → volle Pillen (Radius **100**, 1.5dp warmer Rahmen),
  konsistent mit den Filter-Pillen der Liste. Ausgewählt: Forest-Fill /
  Cream-2-Text (wie bisher).
- **Add-Buttons** (Zutat/Schritt/Bild): gestrichelter Rahmen + Text in
  **Moos-Grün `#4F7A4A`** statt dunklem Forest.
- **Schritt-Bild und „Bild hinzufügen" um 45dp eingerückt** (bündig unter dem
  Textfeld, rechts neben der Schrittnummer), statt volle Breite.
- Titelbild: helle Steuer-Buttons (Frosted) + „📷 Titelbild"-Label unten links
  + unterer Verlauf (Scrim) auf dem Bild, wenn ein Bild gesetzt ist.

## Architektur & Isolation

- Drei kleine, klar abgegrenzte Composables (`RecipelyTextField`,
  `BoxedIconButton`, `FrostedIconButton`) kapseln die wiederkehrenden Stile.
  Jeder hat einen klaren Zweck, eine schlanke Signatur und ist unabhängig
  verständlich. Sie reduzieren Duplikation und machen spätere Stiländerungen
  zentral.
- Theme-Token (Paper, FieldBorder) liegen in `Color.kt` und werden direkt
  referenziert — gleiche Konvention wie die bestehenden Chip-Farben.
- Keine Änderung an ViewModels, Repository, DAO, Entities, Navigation.

## Verifikation

Gemäß CLAUDE.md werden Compose-UIs per erfolgreichem Build/Run verifiziert
(kein Rot-Grün-TDD für UI). Konkret:

1. `./gradlew assembleDebug` läuft grün (JBR 21 als `JAVA_HOME`).
2. `./gradlew testDebugUnitTest` bleibt grün — reine Logik (Mapping/ViewModels)
   ist unberührt.
3. Visuelle Kontrolle aller drei Screens in **Hell- und Dunkelmodus** gegen das
   Mockup (Felder heben sich ab, Terrakotta-Fokus, eckige ✕-Buttons,
   Detail-Sheet + Grabber, Pillen-Kategorien, Moos-Häkchen).

## Risiken / offene Annäherungen

- **Fokus-Glow:** Der weiche Terrakotta-Ring aus dem Mockup wird nur als
  farbiger Rahmen angenähert (kein echter Box-Shadow-Ring in Compose).
- **Sheet-Overlap:** Der negative Offset über das Hero muss mit dem
  Status-Bar-/Scroll-Verhalten harmonieren; im Zweifel Offset leicht justieren.
- **Dark-Paper:** `PaperDark` muss sich sichtbar, aber dezent von `DarkSurface`
  abheben — Wert ggf. visuell feinjustieren.
- Pixelmaße (33sp, 130dp, Radius 26/20) sind Mockup-Richtwerte und dürfen für
  ein stimmiges Ergebnis um ±1–2 abweichen.
