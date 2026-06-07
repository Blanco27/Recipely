package com.nwe.recipely.ui.theme

import androidx.compose.ui.graphics.Color

// --- Light — every value is a mockup palette token (docs/mockups/ui-redesign.html) ---
val ForestPrimary = Color(0xFF1E3A2B)          // --forest
val OnForest = Color(0xFFFFFFFF)               // text/icons on forest
val ForestContainer = Color(0xFF4F7A4A)        // --moss (dormant primaryContainer slot)
val OnForestContainer = Color(0xFFFFFDF9)       // --paper
val Terracotta = Color(0xFFC75D3C)             // --terra
val OnTerracotta = Color(0xFFFFFFFF)           // text/icons on terracotta
val TerracottaContainer = Color(0xFFE08A63)    // --terra-soft (dormant)
val OnTerracottaContainer = Color(0xFF21201B)  // --ink
val Honey = Color(0xFFE2A03C)                  // --honey
val OnHoney = Color(0xFF21201B)                // --ink
val HoneyContainer = Color(0xFFE2A03C)         // --honey (dormant)
val OnHoneyContainer = Color(0xFF21201B)       // --ink
val Cream = Color(0xFFF6F0E6)                  // --cream (background)
val CreamSurface = Color(0xFFFBF7EF)           // --cream-2 (surface)
val Ink = Color(0xFF21201B)                    // --ink
val CreamSurfaceVariant = Color(0xFFE6DECF)    // --line (image-placeholder / drop-zone fill)
val OnSurfaceVariantLight = Color(0xFF6B655A)  // --ink-soft
val OutlineLight = Color(0xFF6B655A)           // --ink-soft
val OutlineVariantLight = Color(0xFFE6DECF)    // --line

// --- Dark ---
val ForestPrimaryDark = Color(0xFF9CD49A)
val OnForestDark = Color(0xFF06380F)
val ForestContainerDark = Color(0xFF2C5238)
val OnForestContainerDark = Color(0xFFC7E6C0)
val TerracottaDark = Color(0xFFF0A184)
val OnTerracottaDark = Color(0xFF5A1B0A)
val TerracottaContainerDark = Color(0xFF7A3422)
val OnTerracottaContainerDark = Color(0xFFF7D9CC)
val HoneyDark = Color(0xFFE9C07A)
val OnHoneyDark = Color(0xFF3A2600)
val HoneyContainerDark = Color(0xFF5E4316)
val OnHoneyContainerDark = Color(0xFFFBEAC9)
val DarkBg = Color(0xFF15170F)
val DarkSurface = Color(0xFF1F211A)
val OnDark = Color(0xFFE7E2D6)
val DarkSurfaceVariant = Color(0xFF44483D)
val OnSurfaceVariantDark = Color(0xFFC6C2B4)
val OutlineDark = Color(0xFF8F8B7E)
val OutlineVariantDark = Color(0xFF615E54)

// --- Surface container roles ---
// Material 3 components (e.g. AlertDialog uses surfaceContainerHigh) fall back to the
// baseline purple-tinted defaults when these roles aren't set; pin them to warm cream/dark.
// Light (warm cream hierarchy, lightest = paper)
val SurfaceDimLight = Color(0xFFEBE3D3)
val SurfaceBrightLight = Color(0xFFFFFDF9)
val SurfaceContainerLowestLight = Color(0xFFFFFDF9)
val SurfaceContainerLowLight = Color(0xFFFCF8F0)
val SurfaceContainerLight = Color(0xFFF8F2E8)
val SurfaceContainerHighLight = Color(0xFFF3ECDD)
val SurfaceContainerHighestLight = Color(0xFFEEE6D5)
// Dark (warm near-black hierarchy)
val SurfaceDimDark = Color(0xFF15170F)
val SurfaceBrightDark = Color(0xFF3B3D31)
val SurfaceContainerLowestDark = Color(0xFF101109)
val SurfaceContainerLowDark = Color(0xFF1D1F17)
val SurfaceContainerDark = Color(0xFF212318)
val SurfaceContainerHighDark = Color(0xFF2C2E22)
val SurfaceContainerHighestDark = Color(0xFF373930)

// --- Accent extras ---
val Moss = Color(0xFF4F7A4A)
val Forest2 = Color(0xFF2C5238)

// --- Paper: elevated near-white surface for cards/fields/chips (mockup --paper #FFFDF9) ---
val Paper = Color(0xFFFFFDF9)
val PaperDark = Color(0xFF26281F)

// --- Soft warm border for fields/boxed elements (mockup --line #E6DECF) ---
val FieldBorderLight = Color(0xFFE6DECF)
val FieldBorderDark = Color(0xFF45493D)

// --- Meta chips (soft, bordered — see mockup .chip / .chip.kcal) ---
// Light
val MetaChipBgLight = Color(0xFFF6F0E6)
val MetaChipFgLight = Color(0xFF2C5238)
val MetaChipBorderLight = Color(0xFFE6DECF)
val KcalChipBgLight = Color(0xFFFBEEDD)
val KcalChipFgLight = Color(0xFFA85B1E)
val KcalChipBorderLight = Color(0xFFF3DDBE)
// Dark
val MetaChipBgDark = Color(0xFF2A2D23)
val MetaChipFgDark = Color(0xFF9CD49A)
val MetaChipBorderDark = Color(0xFF45493D)
val KcalChipBgDark = Color(0xFF3A2C18)
val KcalChipFgDark = Color(0xFFE9C07A)
val KcalChipBorderDark = Color(0xFF5E4316)

// --- Backup action icon tiles (Settings ▸ Data) — match mockup ---
val ExportIconBgLight = Color(0xFFE9F0E7) // soft green
val ExportIconBgDark = Color(0xFF22301F)
val ImportIconBgLight = Color(0xFFFBEEDD) // soft orange
val ImportIconBgDark = Color(0xFF3A2C18)
