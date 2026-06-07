# Recipely – Kochmodus · Design-Spec

**Datum:** 2026-06-07
**Status:** Brainstorming abgeschlossen — wartet auf Spec-Review
**Mockup (verbindlich):** [`docs/mockups/cook-mode.html`](../../mockups/cook-mode.html)

## 1. Ziel & Umfang

Ein **Kochmodus**: ein ablenkungsfreier Vollbild-Screen, der ein Rezept Schritt für
Schritt durch die Zubereitung führt, während man tatsächlich am Herd steht. Gewählte
Form: **Vollbild-Schrittkarten** — ein Schritt pro Bildschirm, große Schrift, Display
bleibt an. Zeitangaben im Schritttext werden als antippbare **Timer** erkannt, die mit
Benachrichtigung + Ton im Hintergrund weiterlaufen.

### Bewusst im Umfang
- Neuer Screen + Route `cook/{id}`, Single-Step, Hochformat, `keepScreenOn`.
- Einstieg über einen schwebenden **„🍳 Kochen"-FAB** im Detail-Screen.
- Schritt-Navigation per Wischen + „‹"/„Weiter"-Buttons; Abschluss-Screen am Ende.
- Timer aus Schritttext (DE + EN) als Foreground-Service mit Notification.

### Bewusst NICHT (YAGNI)
- **Keine Zutaten** im Kochmodus (kein Sheet/Tab) — bewusst auf die Schritte fokussiert;
  Zutaten schaut man im Detail-Screen an.
- **Keine Datenmodell-Änderung**, kein Eingriff in den Editor-Screen.
- Keine manuelle Timer-Eingabe, kein eigenes Timer-Feld pro Schritt, keine mehreren
  gleichzeitigen Timer, kein Vorlesen (TTS) — alles spätere, optionale Erweiterungen.

## 2. Visuelle Treue (verbindlich, nicht verhandelbar)

Die umgesetzte Compose-UI **muss exakt** dem Mockup
[`docs/mockups/cook-mode.html`](../../mockups/cook-mode.html) entsprechen — Layout,
Abstände, Farben (Cream/Forest/Terra/Moss/Honey-Palette), Typografie (Fraunces für
Display/Schritttext, Hanken Grotesk für UI), Form-Radien, Schatten und Iconografie.
Das Mockup ist die **verbindliche Vorlage**, keine Inspiration. „Default-Material" oder
„nah genug" ist nicht ausreichend. Die Übereinstimmung mit dem Mockup ist Teil der
Definition-of-Done und wird in der Verifikation gegen die laufende App geprüft.

## 3. Einstieg: Kochen-FAB im Detail-Screen

- Im Detail-Screen (`ui/detail/RecipeDetailScreen.kt`) ein schwebender
  **ExtendedFloatingActionButton** unten rechts: Pfannen-Emoji + „Kochen"
  (String `cook_start`), im selben Stil wie der Listen-FAB (terracotta `secondary`,
  Radius 20 dp).
- **Sichtbar nur, wenn das Rezept mindestens einen nicht-leeren Schritt hat.** Bei
  Rezepten ohne Schritte wird der FAB nicht angezeigt.
- Tippen navigiert zur Route `cook/{id}`.

## 4. Navigation & Routing

- Neue Route in `navigation/RecipelyNavHost.kt` / `Routes`: **`cook/{id}`** mit
  `id: Long` (LongType), analog zu `detail/{id}`.
- Innerhalb des Kochmodus ein **HorizontalPager** (eine Seite pro Schritt):
  - Wischen nach links/rechts blättert.
  - Buttons „‹" (vorheriger Schritt, auf Seite 1 deaktiviert) und „Weiter ›"
    (nächster Schritt) blättern programmatisch.
  - Auf dem **letzten Schritt** heißt der Primärbutton „Fertig" (moss-grün) und führt
    zum Abschluss-Screen.
- **„✕"** oben links verlässt den Kochmodus (zurück zum Detail). Verlässt man, während
  ein Timer läuft → siehe §6 (Timer-Lebenszyklus).
- **Abschluss-Screen:** „Guten Appetit!"-Zustand mit Häkchen-Badge, Buttons „Fertig"
  (zurück zum Detail) und „Schritte noch mal ansehen" (springt zurück auf Schritt 1).

## 5. Schritt-Screen (Aufbau gemäß Mockup, Frame 1)

- **Top-Bar:** „✕" links; mittig Rezeptname (Fraunces) + Label „Display bleibt an";
  rechts Platzhalter (Symmetrie).
- **Fortschrittsbalken:** ein Segment pro Schritt — erledigte moss, aktueller terra,
  kommende `line`-Grau.
- **Schritt-Karte (Paper):** Eyebrow „Schritt N von M" (terra, uppercase); Schritttext
  groß in Fraunces; darunter optionales **Schritt-Bild** (Coil, `File(path)`), falls
  gesetzt; darunter optionale **Timer-Pille** (siehe §6), falls eine Dauer erkannt wurde.
- **Untere Leiste:** „‹" + „Weiter ›" (bzw. „Fertig" auf dem letzten Schritt).
- **`keepScreenOn`** ist aktiv, solange der Kochmodus im Vordergrund ist (Flag wird beim
  Verlassen wieder entfernt).
- Schritte werden — wie überall in der App — nach `position` sortiert geladen; leere
  Schritte (kein Text, kein Bild) werden im Kochmodus übersprungen.

## 6. Timer

### 6.1 Erkennung der Dauer (Parser)
- Eine reine Kotlin-Funktion parst den **Schritttext** und liefert die erste erkannte
  Dauer in Sekunden (oder `null`). JVM-unit-testbar, keine Android-Abhängigkeit.
- Erkannte Muster (Groß-/Kleinschreibung egal, optionales Leerzeichen):
  - **Minuten:** `min`, `Min`, `Minute`, `Minuten`, `minute`, `minutes` → z. B.
    „20 Minuten", „20 min", „20Min".
  - **Stunden:** `h`, `Std`, `Stunde`, `Stunden`, `hour`, `hours` → z. B. „1 Std",
    „1 h" (in Sekunden umgerechnet).
- **Erste** erkannte Dauer pro Schritt → genau eine Timer-Pille. (Mehrere Dauern in einem
  Schritt: nur die erste; bewusst einfach gehalten.)
- Findet sich keine Dauer, wird **keine** Pille angezeigt.

### 6.2 Verhalten
- **Pille im Ruhezustand** (Frame 1): forest-grün, Play-Icon + „Timer · MM:SS".
  Tippen startet den Countdown.
- **Pille laufend** (Frame 2): terracotta, Pause-Icon + verbleibende Zeit „MM:SS" +
  Subtext „tippen zum Pausieren". Tippen **pausiert/setzt fort**.
- **Ein aktiver Timer zur Zeit** (app-weit). Startet man auf einem anderen Schritt einen
  Timer, ersetzt er den vorherigen.
- Bei **Ablauf:** Ton + Vibration, Benachrichtigung „Recipely · Schritt N — fertig".

### 6.3 Foreground-Service + Notification
- Ein **Foreground-Service** (`TimerService`) hält den laufenden Countdown, damit er
  weiterläuft, wenn die App in den Hintergrund geht oder der Bildschirm aus ist.
- Eigener **Notification-Channel**; laufende Benachrichtigung zeigt Schritt + Restzeit
  (Frame 2), Abschluss-Benachrichtigung signalisiert das Ende.
- **`POST_NOTIFICATIONS`-Laufzeitberechtigung** (Android 13+/`targetSdk 36`): wird beim
  ersten Timer-Start angefragt. Wird sie verweigert, läuft der Timer trotzdem
  **in-app sichtbar** weiter (nur ohne System-Benachrichtigung) — kein Absturz.
- `FOREGROUND_SERVICE` (+ passender `FOREGROUND_SERVICE_*`-Typ) und
  `POST_NOTIFICATIONS` im `AndroidManifest`.
- **Lebenszyklus:** Verlässt man den Kochmodus, läuft ein aktiver Timer über die
  Benachrichtigung weiter (er gehört zum Rezept-Kochvorgang, nicht zum Screen). Der
  Service stoppt bei Ablauf, bei „Fertig"/Abschluss oder über eine Stopp-Aktion in der
  Benachrichtigung.

## 7. Architektur & Komponenten

Bestehendes MVVM-Muster (UI → ViewModel → Repository → DAO), keine neuen Frameworks.

- **`ui/cook/CookModeScreen.kt`** — Compose-Screen (Pager, Top-Bar, Fortschritt,
  Schritt-Karte, Timer-Pille, Navigationsleiste, Abschluss-Zustand). Bezieht sein
  ViewModel über die übliche `viewModelFactory { initializer { … } }`.
- **`ui/cook/CookModeViewModel.kt`** — lädt `RecipeWithDetails` per id (über
  `container.repository`), exponiert sortierte, nicht-leere Schritte, den aktuellen
  Schritt-Index und den Abschlusszustand als `StateFlow`. JVM-unit-testbar.
- **`ui/cook/StepTimerParser.kt`** (oder gleichwertig) — die reine Parser-Funktion aus
  §6.1.
- **`timer/TimerService.kt`** — Foreground-Service + Notification-Channel; verwaltet den
  einen aktiven Countdown, Ton/Vibration, Restzeit-Updates.
- **Detail-Screen** bekommt den Kochen-FAB + Navigations-Callback (`onCook(id)`),
  verdrahtet im NavHost.
- **Keine** Änderungen an Room-Entities, DAO oder Repository-Datenmodell.

## 8. Lokalisierung

Neue Strings in `values/strings.xml` (EN) **und** `values-de/strings.xml` (DE), via
`stringResource` genutzt. Mindestens: `cook_start` („Cook" / „Kochen"),
Top-Bar-/Display-an-Label, „Schritt N von M", „Weiter"/„Zurück"/„Fertig",
Timer-Pille (Ruhe/laufend, „tippen zum Pausieren"), Abschluss-Texte
(„Guten Appetit!", „Schritte noch mal ansehen"), Notification-Texte. Der **Timer-Parser**
erkennt sowohl deutsche als auch englische Zeit-Einheiten (§6.1), unabhängig von der
UI-Sprache.

## 9. Fehlerbehandlung & Randfälle

- **Rezept ohne Schritte:** Kochen-FAB nicht sichtbar; ruft man `cook/{id}` dennoch auf,
  sauberer „nichts zu kochen"/Zurück-Zustand (kein Absturz).
- **Gelöschte/nicht existente id:** sauber zurücknavigieren (wie Detail).
- **Schritt-Bild fehlt/ladefehler:** Platzhalter bzw. kein Bild, App stürzt nicht ab.
- **Notification-Permission verweigert:** Timer läuft in-app weiter, nur ohne
  System-Benachrichtigung.
- **Konfigurationswechsel** (Rotation/Prozess-Tod): aktueller Schritt-Index überlebt im
  ViewModel-State; laufender Timer überlebt im Service.
- **Letzter Schritt:** „Fertig" statt „Weiter"; danach Abschluss-Screen.

## 10. Teststrategie

- **Unit-Tests (JVM, `app/src/test`)**
  - **`StepTimerParser`** — Kern dieses Features: „20 Minuten", „20 min", „20Min",
    „1 Std", „1 h", englische Varianten, keine Angabe → `null`, erste von mehreren
    Dauern, Groß-/Kleinschreibung, Sekunden-Umrechnung der Stunden.
  - **`CookModeViewModel`** — sortierte/gefilterte Schritte, Schritt-Index vor/zurück,
    Abschlusszustand am Ende, „Schritte noch mal ansehen" → Index 0; gegen
    `FakeRecipeRepository`. `stateIn`-Flows mit `backgroundScope`-Collector aktivieren.
- **Compose-UI / Service** werden durch erfolgreichen Build/Run **und** den
  visuellen Abgleich gegen das Mockup (§2) verifiziert, nicht per Red-Green.

## 11. Pre-Release-Hinweis

Es gibt keine Schema-Änderung, daher **keine Room-Migration** nötig. (Generell gilt in
der Pre-Release-Phase: Schemaänderungen über lokalen DB-Reset statt Migration.)
