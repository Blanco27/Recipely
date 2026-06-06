package com.nwe.recipely.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import coil.compose.AsyncImage
import com.nwe.recipely.R
import com.nwe.recipely.RecipelyApp
import com.nwe.recipely.data.RecipeWithDetails
import com.nwe.recipely.data.Step
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(details?.recipe?.name ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit))
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete))
                    }
                },
            )
        },
    ) { padding ->
        val current = details
        if (current == null) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.recipe_not_found))
            }
        } else {
            DetailContent(current, Modifier.padding(padding))
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
private fun DetailContent(details: RecipeWithDetails, modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier.fillMaxSize()) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center,
            ) {
                val image = details.recipe.imageUri
                if (image != null) {
                    AsyncImage(
                        model = File(image),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Icon(
                        Icons.Outlined.Restaurant,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(72.dp),
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                details.recipe.prepTimeMinutes?.let {
                    AssistChip(onClick = {}, label = { Text(stringResource(R.string.meta_time, it)) })
                }
                details.recipe.servings?.let {
                    AssistChip(onClick = {}, label = { Text(stringResource(R.string.chip_servings, it)) })
                }
            }
        }

        val facts = details.recipe.nutritionFacts()
        if (facts.hasAny) {
            item { SectionHeader(stringResource(R.string.section_nutrition)) }
            item { NutritionBlock(facts = facts, servings = details.recipe.servings) }
        }

        if (details.ingredients.isNotEmpty()) {
            item { SectionHeader(stringResource(R.string.section_ingredients)) }
            val sortedIngredients = details.ingredients.sortedBy { it.position }
            items(sortedIngredients.size) { index ->
                Text(
                    text = "•  ${sortedIngredients[index].text}",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
        }

        if (details.steps.isNotEmpty()) {
            item { SectionHeader(stringResource(R.string.section_steps)) }
            val sortedSteps = details.steps.sortedBy { it.position }
            items(sortedSteps.size) { index ->
                StepItem(number = index + 1, step = sortedSteps[index])
            }
        }

        item { Box(Modifier.height(24.dp)) }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun NutritionBlock(facts: NutritionFacts, servings: Int?) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        val perPortion = facts.perPortion(servings)
        if (perPortion != null) {
            Text(
                text = stringResource(R.string.nutrition_per_portion),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            NutritionRows(perPortion)
            Spacer(Modifier.height(6.dp))
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
private fun StepItem(number: Int, step: Step) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = number.toString(),
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.labelLarge,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            if (step.text.isNotBlank()) {
                Text(step.text, style = MaterialTheme.typography.bodyLarge)
            }
            if (step.imageUri != null) {
                AsyncImage(
                    model = File(step.imageUri),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(12.dp)),
                )
            }
        }
    }
}
