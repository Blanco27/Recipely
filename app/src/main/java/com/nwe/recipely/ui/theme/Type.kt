package com.nwe.recipely.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.nwe.recipely.R

/** Distinctive display serif — recipe names, screen titles, section headers. */
val Fraunces = FontFamily(
    Font(R.font.fraunces_semibold, FontWeight.SemiBold),
)

/** Clean grotesque — body text, labels, fields, buttons. */
val Hanken = FontFamily(
    Font(R.font.hanken_grotesk_regular, FontWeight.Normal),
    Font(R.font.hanken_grotesk_medium, FontWeight.Medium),
    Font(R.font.hanken_grotesk_semibold, FontWeight.SemiBold),
    Font(R.font.hanken_grotesk_bold, FontWeight.Bold),
)

private val base = Typography()

/**
 * Everything defaults to Hanken Grotesk. The Fraunces serif is applied explicitly via
 * `fontFamily = Fraunces` on headings/titles in the composables that want the serif look.
 */
val AppTypography = Typography(
    displayLarge = base.displayLarge.copy(fontFamily = Hanken),
    displayMedium = base.displayMedium.copy(fontFamily = Hanken),
    displaySmall = base.displaySmall.copy(fontFamily = Hanken),
    headlineLarge = base.headlineLarge.copy(fontFamily = Hanken),
    headlineMedium = base.headlineMedium.copy(fontFamily = Hanken),
    headlineSmall = base.headlineSmall.copy(fontFamily = Hanken),
    titleLarge = base.titleLarge.copy(fontFamily = Hanken),
    titleMedium = base.titleMedium.copy(fontFamily = Hanken),
    titleSmall = base.titleSmall.copy(fontFamily = Hanken),
    bodyLarge = base.bodyLarge.copy(fontFamily = Hanken),
    bodyMedium = base.bodyMedium.copy(fontFamily = Hanken),
    bodySmall = base.bodySmall.copy(fontFamily = Hanken),
    labelLarge = base.labelLarge.copy(fontFamily = Hanken),
    labelMedium = base.labelMedium.copy(fontFamily = Hanken),
    labelSmall = base.labelSmall.copy(fontFamily = Hanken),
)
