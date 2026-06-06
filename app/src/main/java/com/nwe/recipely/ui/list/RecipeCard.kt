package com.nwe.recipely.ui.list

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.nwe.recipely.R
import com.nwe.recipely.data.Recipe
import com.nwe.recipely.data.RecipeCategory
import com.nwe.recipely.ui.theme.ForestPrimary
import com.nwe.recipely.ui.theme.Fraunces
import com.nwe.recipely.ui.theme.KcalChipBgDark
import com.nwe.recipely.ui.theme.KcalChipBgLight
import com.nwe.recipely.ui.theme.KcalChipBorderDark
import com.nwe.recipely.ui.theme.KcalChipBorderLight
import com.nwe.recipely.ui.theme.KcalChipFgDark
import com.nwe.recipely.ui.theme.KcalChipFgLight
import com.nwe.recipely.ui.theme.MetaChipBgDark
import com.nwe.recipely.ui.theme.MetaChipBgLight
import com.nwe.recipely.ui.theme.MetaChipBorderDark
import com.nwe.recipely.ui.theme.MetaChipBorderLight
import com.nwe.recipely.ui.theme.MetaChipFgDark
import com.nwe.recipely.ui.theme.MetaChipFgLight
import java.io.File

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecipeCard(recipe: Recipe, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f),
                contentAlignment = Alignment.Center,
            ) {
                val image = recipe.imageUri
                if (image != null) {
                    AsyncImage(
                        model = File(image),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Outlined.Restaurant,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(40.dp),
                        )
                    }
                }
                val category = RecipeCategory.fromKey(recipe.category)
                if (category != null) {
                    CategoryBadge(
                        emoji = category.emoji,
                        label = stringResource(category.labelRes),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(12.dp),
                    )
                }
            }
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = recipe.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontFamily = Fraunces,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                val hasMeta = recipe.prepTimeMinutes != null ||
                    recipe.servings != null ||
                    recipe.calories != null
                if (hasMeta) {
                    FlowRow(
                        modifier = Modifier.padding(top = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        recipe.prepTimeMinutes?.let {
                            MetaChip("⏱", stringResource(R.string.chip_time, it))
                        }
                        recipe.servings?.let {
                            MetaChip("🍽", stringResource(R.string.chip_portions, it))
                        }
                        recipe.calories?.let {
                            MetaChip("🔥", stringResource(R.string.nutrition_value_kcal, it), kcal = true)
                        }
                    }
                }
            }
        }
    }
}

/**
 * Small rounded pill for a single meta fact: a colorful leading emoji plus text, soft
 * bordered background (see mockup `.chip`). `kcal` switches to the warm honey/amber variant.
 */
@Composable
fun MetaChip(emoji: String, text: String, kcal: Boolean = false, modifier: Modifier = Modifier) {
    val dark = isSystemInDarkTheme()
    val bg = when {
        kcal && dark -> KcalChipBgDark
        kcal -> KcalChipBgLight
        dark -> MetaChipBgDark
        else -> MetaChipBgLight
    }
    val fg = when {
        kcal && dark -> KcalChipFgDark
        kcal -> KcalChipFgLight
        dark -> MetaChipFgDark
        else -> MetaChipFgLight
    }
    val border = when {
        kcal && dark -> KcalChipBorderDark
        kcal -> KcalChipBorderLight
        dark -> MetaChipBorderDark
        else -> MetaChipBorderLight
    }
    Surface(
        color = bg,
        contentColor = fg,
        shape = RoundedCornerShape(9.dp),
        border = BorderStroke(1.dp, border),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(text = emoji, style = MaterialTheme.typography.labelMedium)
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

/**
 * Category pill overlaid on a card image (mockup `.tag`): translucent dark-forest background
 * with light text, so it reads over both a photo and the placeholder, in light and dark themes.
 */
@Composable
private fun CategoryBadge(emoji: String, label: String, modifier: Modifier = Modifier) {
    Surface(
        color = ForestPrimary.copy(alpha = 0.82f),
        contentColor = Color.White,
        shape = RoundedCornerShape(100.dp),
        modifier = modifier.semantics(mergeDescendants = true) {},
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(emoji, style = MaterialTheme.typography.labelSmall)
            Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        }
    }
}
