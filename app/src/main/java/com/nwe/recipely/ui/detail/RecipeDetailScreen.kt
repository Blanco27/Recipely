package com.nwe.recipely.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.filled.Check
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import coil.compose.AsyncImage
import com.nwe.recipely.R
import com.nwe.recipely.RecipelyApp
import com.nwe.recipely.data.Ingredient
import com.nwe.recipely.data.RecipeCategory
import com.nwe.recipely.data.RecipeWithDetails
import com.nwe.recipely.data.Step
import com.nwe.recipely.ui.components.FrostedIconButton
import com.nwe.recipely.ui.theme.Fraunces
import com.nwe.recipely.ui.theme.ForestPrimaryDark
import com.nwe.recipely.ui.theme.Honey
import com.nwe.recipely.ui.theme.Moss
import com.nwe.recipely.ui.theme.Paper
import com.nwe.recipely.ui.theme.PaperDark
import java.io.File
import java.util.Locale
import kotlin.math.roundToInt

private val SidePadding = 20.dp

@Composable
fun RecipeDetailScreen(
    recipeId: Long,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDeleted: () -> Unit,
) {
    val container = (LocalContext.current.applicationContext as RecipelyApp).container
    val vm: RecipeDetailViewModel = viewModel(
        factory = viewModelFactory { initializer { RecipeDetailViewModel(container.repository, recipeId) } }
    )
    val details by vm.recipe.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        val current = details
        if (current == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.recipe_not_found))
            }
        } else {
            DetailContent(current)
        }

        // Pinned, translucent nav overlay over the hero.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            OverlayIcon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back), onBack)
            if (current != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OverlayIcon(Icons.Default.Edit, stringResource(R.string.edit), onEdit)
                    OverlayIcon(Icons.Default.Delete, stringResource(R.string.delete)) { showDeleteDialog = true }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_dialog_title)) },
            text = { Text(stringResource(R.string.delete_dialog_text)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    vm.delete(onDeleted)
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

/** The hero nav buttons (back/edit/delete): a frosted overlay icon with a required label. */
@Composable
private fun OverlayIcon(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    FrostedIconButton(icon = icon, contentDescription = contentDescription, onClick = onClick)
}

@Composable
private fun DetailContent(details: RecipeWithDetails, modifier: Modifier = Modifier) {
    val sortedIngredients = details.ingredients.sortedBy { it.position }
    val sortedSteps = details.steps.sortedBy { it.position }
    // Ephemeral check state (not persisted) — resets when leaving the screen.
    val checked = remember { mutableStateMapOf<Long, Boolean>() }
    val category = RecipeCategory.fromKey(details.recipe.category)

    LazyColumn(modifier = modifier.fillMaxSize()) {
        item {
            Hero(
                name = details.recipe.name,
                imageUri = details.recipe.imageUri,
                categoryLabel = category?.let { stringResource(it.labelRes) },
            )
        }
        item {
            DetailSheet(
                details = details,
                ingredients = sortedIngredients,
                steps = sortedSteps,
                checked = checked,
            )
        }
    }
}

/** Rounded content sheet pulled up over the hero (mockup .sheet) with a grabber handle. */
@Composable
private fun DetailSheet(
    details: RecipeWithDetails,
    ingredients: List<Ingredient>,
    steps: List<Step>,
    checked: SnapshotStateMap<Long, Boolean>,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = (-26).dp)
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(top = 14.dp, bottom = 8.dp),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 16.dp)
                .size(width = 42.dp, height = 5.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(MaterialTheme.colorScheme.outlineVariant),
        )

        StatGrid(
            prepTime = details.recipe.prepTimeMinutes,
            servings = details.recipe.servings,
            calories = details.recipe.calories,
            protein = details.recipe.proteinGrams,
        )

        val facts = details.recipe.nutritionFacts()
        if (facts.hasAny) {
            SectionHeader(stringResource(R.string.section_nutrition))
            NutritionCard(facts = facts, servings = details.recipe.servings)
        }

        if (ingredients.isNotEmpty()) {
            val done = ingredients.count { checked[it.id] == true }
            SectionHeader(
                text = stringResource(R.string.section_ingredients),
                meta = stringResource(R.string.ingredients_done, done, ingredients.size),
            )
            IngredientsCard(items = ingredients.map { it.id to it.text }, checked = checked)
        }

        if (steps.isNotEmpty()) {
            SectionHeader(
                text = stringResource(R.string.section_steps),
                meta = pluralStringResource(R.plurals.steps_count, steps.size, steps.size),
            )
            StepsColumn(steps)
        }

        // 26dp recovers the sheet's (-26).dp graphical offset (which doesn't shrink the
        // measured height), + 24dp design breathing room at the bottom of the scroll.
        Spacer(Modifier.height(50.dp))
    }
}

@Composable
private fun Hero(name: String, imageUri: String?, categoryLabel: String?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp),
    ) {
        if (imageUri != null) {
            AsyncImage(
                model = File(imageUri),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.Restaurant,
                    contentDescription = null,
                    tint = if (isSystemInDarkTheme()) ForestPrimaryDark else Moss,
                    modifier = Modifier.size(72.dp),
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color(0x66101610), // top scrim for status/nav icon legibility
                        0.25f to Color.Transparent,
                        0.55f to Color.Transparent,
                        1.0f to Color(0xCC101610),
                    )
                ),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(SidePadding),
        ) {
            if (categoryLabel != null) {
                Text(
                    text = categoryLabel.uppercase(Locale.ROOT),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    color = Honey,
                    modifier = Modifier.padding(bottom = 6.dp),
                )
            }
            Text(
                text = name,
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 32.sp),
                fontFamily = Fraunces,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
            )
        }
    }
}

private data class StatData(
    val emoji: String,
    val value: String,
    val label: String,
    val accent: Boolean,
)

/** The mockup's nutrition stat bar: up to four emoji/value/label cards, kcal as the accent card. */
@Composable
private fun StatGrid(prepTime: Int?, servings: Int?, calories: Int?, protein: Double?) {
    val stats = ArrayList<StatData>(4)
    if (prepTime != null) {
        stats.add(StatData("⏱", prepTime.toString(), stringResource(R.string.stat_time), accent = false))
    }
    if (servings != null) {
        stats.add(StatData("🍽", servings.toString(), stringResource(R.string.stat_servings), accent = false))
    }
    if (calories != null) {
        stats.add(StatData("🔥", calories.toString(), stringResource(R.string.stat_kcal), accent = true))
    }
    if (protein != null) {
        stats.add(
            StatData(
                emoji = "🥩",
                value = stringResource(R.string.stat_grams, protein.roundToInt().toString()),
                label = stringResource(R.string.stat_protein),
                accent = false,
            )
        )
    }
    if (stats.isEmpty()) return
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = SidePadding),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        stats.forEach { StatCard(it, Modifier.weight(1f)) }
    }
}

@Composable
private fun StatCard(stat: StatData, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val bg = if (stat.accent) cs.primary else (if (isSystemInDarkTheme()) PaperDark else Paper)
    val valueColor = if (stat.accent) cs.onPrimary else cs.primary
    val labelColor = if (stat.accent) cs.onPrimary.copy(alpha = 0.75f) else cs.onSurfaceVariant
    val borderColor = if (stat.accent) cs.primary else cs.outlineVariant
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(bg)
            .border(1.dp, borderColor, RoundedCornerShape(18.dp))
            .padding(vertical = 13.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = stat.emoji, fontSize = 18.sp)
        Text(
            text = stat.value,
            style = MaterialTheme.typography.titleLarge,
            fontFamily = Fraunces,
            fontWeight = FontWeight.SemiBold,
            color = valueColor,
            maxLines = 1,
            modifier = Modifier.padding(top = 5.dp),
        )
        Text(
            text = stat.label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = labelColor,
            maxLines = 1,
            modifier = Modifier.padding(top = 3.dp),
        )
    }
}

@Composable
private fun SectionHeader(text: String, meta: String? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = SidePadding, end = SidePadding, top = 20.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleLarge,
            fontFamily = Fraunces,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        if (meta != null) {
            Text(
                text = meta,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}

@Composable
private fun NutritionCard(facts: NutritionFacts, servings: Int?) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = SidePadding),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSystemInDarkTheme()) PaperDark else Paper,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val perPortion = facts.perPortion(servings)
            if (perPortion != null) {
                Text(
                    text = stringResource(R.string.nutrition_per_portion),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                NutritionRows(perPortion)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = totalSummary(facts),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                NutritionRows(facts)
            }
        }
    }
}

@Composable
private fun NutritionRows(facts: NutritionFacts) {
    facts.calories?.let {
        NutritionRow(stringResource(R.string.label_calories), stringResource(R.string.nutrition_value_kcal, it))
    }
    facts.carbsGrams?.let {
        NutritionRow(stringResource(R.string.label_carbs), stringResource(R.string.nutrition_value_grams, NutritionFormat.grams(it)))
    }
    facts.proteinGrams?.let {
        NutritionRow(stringResource(R.string.label_protein), stringResource(R.string.nutrition_value_grams, NutritionFormat.grams(it)))
    }
    facts.fatGrams?.let {
        NutritionRow(stringResource(R.string.label_fat), stringResource(R.string.nutrition_value_grams, NutritionFormat.grams(it)))
    }
}

@Composable
private fun NutritionRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun totalSummary(facts: NutritionFacts): String {
    val parts = buildList {
        facts.calories?.let {
            add(stringResource(R.string.nutrition_summary_part,
                stringResource(R.string.label_calories),
                stringResource(R.string.nutrition_value_kcal, it)))
        }
        facts.carbsGrams?.let {
            add(stringResource(R.string.nutrition_summary_part,
                stringResource(R.string.label_carbs),
                stringResource(R.string.nutrition_value_grams, NutritionFormat.grams(it))))
        }
        facts.proteinGrams?.let {
            add(stringResource(R.string.nutrition_summary_part,
                stringResource(R.string.label_protein),
                stringResource(R.string.nutrition_value_grams, NutritionFormat.grams(it))))
        }
        facts.fatGrams?.let {
            add(stringResource(R.string.nutrition_summary_part,
                stringResource(R.string.label_fat),
                stringResource(R.string.nutrition_value_grams, NutritionFormat.grams(it))))
        }
    }
    return "${stringResource(R.string.nutrition_total_prefix)} ${parts.joinToString(" · ")}"
}

@Composable
private fun IngredientsCard(items: List<Pair<Long, String>>, checked: SnapshotStateMap<Long, Boolean>) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = SidePadding),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSystemInDarkTheme()) PaperDark else Paper,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            items.forEach { (id, text) ->
                val isChecked = checked[id] ?: false
                IngredientRow(text = text, checked = isChecked, onToggle = { checked[id] = !isChecked })
            }
        }
    }
}

@Composable
private fun IngredientRow(text: String, checked: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = { onToggle() })
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        MossCheckBox(checked = checked)
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = if (checked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            textDecoration = if (checked) TextDecoration.LineThrough else null,
        )
    }
}

/** Mockup .ing .box: a 22dp rounded square, moss border, fills moss with a check when done. */
@Composable
private fun MossCheckBox(checked: Boolean) {
    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(if (checked) Moss else Color.Transparent)
            .border(2.dp, Moss, RoundedCornerShape(7.dp)),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            Icon(
                Icons.Default.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@Composable
private fun StepsColumn(steps: List<Step>) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = SidePadding)) {
        steps.forEachIndexed { index, step ->
            StepRow(number = index + 1, step = step, isLast = index == steps.lastIndex)
        }
    }
}

@Composable
private fun StepRow(number: Int, step: Step, isLast: Boolean) {
    val connectorColor = MaterialTheme.colorScheme.outlineVariant
    Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.secondary),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = number.toString(),
                    color = MaterialTheme.colorScheme.onSecondary,
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = Fraunces,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            if (!isLast) {
                // Dashed connector between step badges (mockup .step::before).
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .weight(1f)
                        .drawBehind {
                            val x = size.width / 2f
                            drawLine(
                                color = connectorColor,
                                start = Offset(x, 0f),
                                end = Offset(x, size.height),
                                strokeWidth = size.width,
                                pathEffect = PathEffect.dashPathEffect(
                                    floatArrayOf(4.dp.toPx(), 6.dp.toPx()),
                                    0f,
                                ),
                            )
                        },
                )
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 14.dp, bottom = 20.dp),
        ) {
            if (step.text.isNotBlank()) {
                Text(step.text, style = MaterialTheme.typography.bodyLarge)
            }
            if (step.imageUri != null) {
                AsyncImage(
                    model = File(step.imageUri),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(RoundedCornerShape(16.dp)),
                )
            }
        }
    }
}
