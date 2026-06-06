package com.nwe.recipely.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = ForestPrimary,
    onPrimary = OnForest,
    primaryContainer = ForestContainer,
    onPrimaryContainer = OnForestContainer,
    secondary = Terracotta,
    onSecondary = OnTerracotta,
    secondaryContainer = TerracottaContainer,
    onSecondaryContainer = OnTerracottaContainer,
    tertiary = Honey,
    onTertiary = OnHoney,
    tertiaryContainer = HoneyContainer,
    onTertiaryContainer = OnHoneyContainer,
    background = Cream,
    onBackground = Ink,
    surface = CreamSurface,
    onSurface = Ink,
    surfaceVariant = CreamSurfaceVariant,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
)

private val DarkColors = darkColorScheme(
    primary = ForestPrimaryDark,
    onPrimary = OnForestDark,
    primaryContainer = ForestContainerDark,
    onPrimaryContainer = OnForestContainerDark,
    secondary = TerracottaDark,
    onSecondary = OnTerracottaDark,
    secondaryContainer = TerracottaContainerDark,
    onSecondaryContainer = OnTerracottaContainerDark,
    tertiary = HoneyDark,
    onTertiary = OnHoneyDark,
    tertiaryContainer = HoneyContainerDark,
    onTertiaryContainer = OnHoneyContainerDark,
    background = DarkBg,
    onBackground = OnDark,
    surface = DarkSurface,
    onSurface = OnDark,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun RecipelyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content,
    )
}
