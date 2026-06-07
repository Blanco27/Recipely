# Settings — About Section (Quiet Footer)

**Date:** 2026-06-07
**Status:** Approved (design)
**Mockup:** `docs/mockups/settings-about.html` — Variant **C (Quiet footer)**

## Goal

Add a small **About** section to the bottom of the Settings screen, showing the app
version, a link to the GitHub repository, and an open-source note. Visual treatment is
Variant C from the mockup: a calm, centered footer — no section label, no panel.

## Scope

In scope:
- A new `AboutFooter` composable rendered at the bottom of `SettingsScreen`.
- App version read dynamically (no hardcoding).
- A tappable GitHub link that opens the repository in the browser.
- EN/DE localized strings.

Out of scope (YAGNI): licenses screen, third-party attributions list, changelog,
"check for updates", build/commit hash, contact/feedback links.

## Visual design

Source of truth is the mockup (`settings-about.html`, Variant C). The shipped Compose UI
must match it. Layout — a centered `Column`, items stacked with ~10dp vertical spacing,
in this order:

1. **App mark** — 44dp rounded square (≈14dp corner radius), background = a Moss→Forest
   linear gradient (`#4F7A4A` → `#1E3A2B`), soft shadow; centered **🍲** emoji at ~22sp.
   (Emoji per mockup — no drawable asset needed.)
2. **Name + version** — one line: "Recipely" in **Fraunces** SemiBold ~18sp (Forest),
   followed by "v1.0" in Hanken Grotesk ~13sp, muted (`onSurfaceVariant`), with a small
   leading gap.
3. **GitHub link** — an inline row: a small GitHub glyph + text "View source on GitHub",
   in **Terracotta** = `MaterialTheme.colorScheme.secondary`, ~13.5sp, SemiBold. The
   whole row is the tap target.
4. **OSS note** — single line "Open source · made with ❤" at ~12sp, muted
   (`onSurfaceVariant`). The heart renders as the standard red ❤ emoji glyph (per the
   user's explicit preference, chosen over the mockup's terracotta heart during
   verification — this is the one intentional, approved deviation from the mockup).

Spacing: ~40dp of breathing room above the footer (the mockup's `padding-top:40px`).
Dark mode uses the existing theme tokens (no new dark-specific colors); the gradient mark
stays the same. Reuse `Fraunces` and the existing color scheme — no new palette.

### Anchoring at the bottom

Settings content is short (Appearance + Language). To make the footer read as a true
footer, the screen's root `Column` gets a `Spacer(Modifier.weight(1f))` between the
Language panel and `AboutFooter`, pushing About to the bottom of the viewport. The Column
stays non-scrollable (content fits comfortably on supported screens). Bottom padding ~8dp.

## Behavior

- **Version:** read at composition via
  `context.packageManager.getPackageInfo(context.packageName, 0).versionName`
  (currently `"1.0"`). Rendered as `"v$versionName"`. No `BuildConfig` change required —
  `buildConfig` build feature stays off. If `versionName` is null, omit the "v…" suffix
  gracefully.
- **GitHub link:** on tap, launch `Intent(Intent.ACTION_VIEW, Uri.parse(repoUrl))` where
  `repoUrl = "https://github.com/Blanco27/Recipely"`. Wrap the launch so a missing
  browser (`ActivityNotFoundException`) is swallowed rather than crashing. The URL is a
  hardcoded constant in the composable (not a string resource — it is not translated).
- **Accessibility:** the link row has a content description / merged semantics so it reads
  as a single actionable "View source on GitHub" element; the app mark is decorative
  (`contentDescription = null`).

## Strings (localized)

Add to `values/strings.xml` and `values-de/strings.xml`:

| key | EN | DE |
|---|---|---|
| `about_app_name` | Recipely | Recipely |
| `about_version` (format) | v%1$s | v%1$s |
| `about_github` | View source on GitHub | Quellcode auf GitHub ansehen |
| `about_open_source` | Open source · made with ❤ | Open Source · mit ❤ gemacht |

Variant C shows no visible section header, so no `settings_about` label string is added.
The GitHub **URL** is not a translated string and lives as a Kotlin constant.

## Implementation notes

- New file: `ui/settings/AboutFooter.kt` (private composable, or a section composable
  used by `SettingsScreen`). Keep `SettingsScreen.kt` lean — follow the existing
  `ThemePanel` / `LanguagePanel` decomposition pattern.
- Reuse existing theme accessors (`Fraunces`, `MaterialTheme.colorScheme`,
  `LocalDarkTheme` only if a token genuinely differs by mode — likely not needed here).
- Obtain `Context` via `LocalContext.current`.

## Testing

Per project convention, Compose UI is verified by a successful build/run, not red-green.
No new pure logic / ViewModel branch is introduced (version read is a direct platform
call in the composable), so no JVM unit test is added. Manual verification:
- Footer renders at the bottom, matches the mockup in light and dark mode.
- Version shows "v1.0".
- Tapping the GitHub link opens `github.com/Blanco27/Recipely`.
- DE locale shows German strings.

## Risks / trade-offs

- Reading `versionName` via `PackageManager` keeps `BuildConfig` disabled (one less build
  feature) at the cost of a tiny runtime lookup — acceptable, runs once per composition.
- `weight(1f)` spacer assumes content fits without scrolling. Given only two short
  sections plus the footer, this holds on `minSdk 24` phones; revisit only if future
  settings sections make the screen overflow (then switch the Column to
  `verticalScroll` and drop the weight spacer).
