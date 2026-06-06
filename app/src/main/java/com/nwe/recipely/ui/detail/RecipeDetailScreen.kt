package com.nwe.recipely.ui.detail

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import coil.compose.AsyncImage
import com.nwe.recipely.R
import com.nwe.recipely.RecipelyApp
import com.nwe.recipely.data.RecipeWithDetails
import com.nwe.recipely.data.Step
import com.nwe.recipely.ui.list.MetaChip
import com.nwe.recipely.ui.theme.Fraunces
import java.io.File

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

@Composable
private fun OverlayIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    FilledIconButton(
        onClick = onClick,
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = Color(0x55000000),
            contentColor = Color.White,
        ),
    ) {
        Icon(icon, contentDescription = contentDescription)
    }
}

@Composable
private fun DetailContent(details: RecipeWithDetails, modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        item { Hero(name = details.recipe.name, imageUri = details.recipe.imageUri) }

        item { Spacer(Modifier.height(16.dp)) }

        item { QuickFacts(prepTime = details.recipe.prepTimeMinutes, servings = details.recipe.servings) }

        val facts = details.recipe.nutritionFacts()
        if (facts.hasAny) {
            item { SectionHeader(stringResource(R.string.section_nutrition)) }
            item { NutritionCard(facts = facts, servings = details.recipe.servings) }
        }

        if (details.ingredients.isNotEmpty()) {
            item { SectionHeader(stringResource(R.string.section_ingredients)) }
            val sorted = details.ingredients.sortedBy { it.position }
            item { IngredientsCard(items = sorted.map { it.id to it.text }) }
        }

        if (details.steps.isNotEmpty()) {
            item { SectionHeader(stringResource(R.string.section_steps)) }
            val sorted = details.steps.sortedBy { it.position }
            item { StepsColumn(sorted) }
        }

        item { Spacer(Modifier.height(32.dp)) }
    }
}

@Composable
private fun Hero(name: String, imageUri: String?) {
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
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Outlined.Restaurant,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(72.dp),
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Transparent,
                        0.55f to Color.Transparent,
                        1.0f to Color(0xCC101610),
                    )
                ),
        )
        Text(
            text = name,
            style = MaterialTheme.typography.headlineMedium,
            fontFamily = Fraunces,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(SidePadding),
        )
    }
}

@Composable
private fun QuickFacts(prepTime: Int?, servings: Int?) {
    if (prepTime == null && servings == null) return
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = SidePadding),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        prepTime?.let { MetaChip(stringResource(R.string.meta_time, it)) }
        servings?.let { MetaChip(stringResource(R.string.chip_servings, it)) }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        fontFamily = Fraunces,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(start = SidePadding, end = SidePadding, top = 20.dp, bottom = 8.dp),
    )
}

@Composable
private fun NutritionCard(facts: NutritionFacts, servings: Int?) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = SidePadding),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
private fun IngredientsCard(items: List<Pair<Long, String>>) {
    // Ephemeral check state (not persisted) — resets when leaving the screen.
    val checked = remember { mutableStateMapOf<Long, Boolean>() }
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = SidePadding),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
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
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = checked, onCheckedChange = { onToggle() })
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = if (checked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            textDecoration = if (checked) TextDecoration.LineThrough else null,
        )
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
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.outlineVariant),
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
                        .height(170.dp)
                        .clip(RoundedCornerShape(16.dp)),
                )
            }
        }
    }
}
