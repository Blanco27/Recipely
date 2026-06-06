package com.nwe.recipely.ui.edit

import com.nwe.recipely.data.Ingredient
import com.nwe.recipely.data.Recipe
import com.nwe.recipely.data.RecipeWithDetails
import com.nwe.recipely.data.Step
import java.util.Locale

data class IngredientRow(val text: String = "")

data class StepRow(val text: String = "", val imagePath: String? = null)

data class EditUiState(
    val id: Long = 0,
    val name: String = "",
    val prepTime: String = "",
    val servings: String = "",
    val calories: String = "",
    val carbs: String = "",
    val protein: String = "",
    val fat: String = "",
    val imagePath: String? = null,
    val ingredients: List<IngredientRow> = listOf(IngredientRow()),
    val steps: List<StepRow> = listOf(StepRow()),
) {
    val canSave: Boolean get() = name.isNotBlank()
}

/** Title image plus every step image, in order. */
fun EditUiState.referencedPaths(): List<String> =
    listOfNotNull(imagePath) + steps.mapNotNull { it.imagePath }

/** Maps the form to entities, trimming text and dropping empty rows. recipeId on children is
 * a placeholder — the DAO reassigns it during upsert. */
fun EditUiState.toEntities(): Triple<Recipe, List<Ingredient>, List<Step>> {
    val recipe = Recipe(
        id = id,
        name = name.trim(),
        imageUri = imagePath,
        prepTimeMinutes = prepTime.trim().toIntOrNull(),
        servings = servings.trim().toIntOrNull(),
        calories = calories.trim().toIntOrNull(),
        carbsGrams = carbs.toGramsOrNull(),
        proteinGrams = protein.toGramsOrNull(),
        fatGrams = fat.toGramsOrNull(),
    )
    val ingredients = ingredients
        .map { it.text.trim() }
        .filter { it.isNotEmpty() }
        .mapIndexed { index, text -> Ingredient(recipeId = id, text = text, position = index) }
    val steps = steps
        .filter { it.text.isNotBlank() || it.imagePath != null }
        .mapIndexed { index, row ->
            Step(recipeId = id, text = row.text.trim(), imageUri = row.imagePath, position = index)
        }
    return Triple(recipe, ingredients, steps)
}

fun RecipeWithDetails.toUiState(locale: Locale = Locale.getDefault()): EditUiState = EditUiState(
    id = recipe.id,
    name = recipe.name,
    prepTime = recipe.prepTimeMinutes?.toString() ?: "",
    servings = recipe.servings?.toString() ?: "",
    calories = recipe.calories?.toString() ?: "",
    carbs = recipe.carbsGrams?.toEditString(locale) ?: "",
    protein = recipe.proteinGrams?.toEditString(locale) ?: "",
    fat = recipe.fatGrams?.toEditString(locale) ?: "",
    imagePath = recipe.imageUri,
    ingredients = ingredients.sortedBy { it.position }
        .map { IngredientRow(it.text) }
        .ifEmpty { listOf(IngredientRow()) },
    steps = steps.sortedBy { it.position }
        .map { StepRow(it.text, it.imageUri) }
        .ifEmpty { listOf(StepRow()) },
)

/** Parses a gram value, accepting both comma and period decimals; blank/invalid -> null. */
internal fun String.toGramsOrNull(): Double? =
    trim().replace(',', '.').toDoubleOrNull()

/** Formats a stored gram value for an editable text field: one decimal max, a whole number
 * shown without decimals, otherwise using [locale]'s decimal separator (comma on DE). */
internal fun Double.toEditString(locale: Locale = Locale.getDefault()): String {
    val rounded = String.format(Locale.US, "%.1f", this).toDouble()
    return if (rounded % 1.0 == 0.0) rounded.toLong().toString()
    else String.format(locale, "%.1f", rounded)
}
